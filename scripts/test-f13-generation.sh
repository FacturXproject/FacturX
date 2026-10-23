#!/usr/bin/env bash
# F13 - Generation Factur-X : batterie de bout en bout avec curl, contre la stack qui
# tourne (docker compose up). Rejouable : chaque lancement cree ses propres comptes.
# Lance par la CI apres "Start containers" : le job echoue si la facture generee
# n'est pas conforme pour le moteur Java (Mustang) ou si un controle echoue.
#
#   ./scripts/test-f13-generation.sh                 # https://localhost:8443 par defaut
#   BASE=https://localhost:8443 ./scripts/test-f13-generation.sh
#
# Scenario :
#   - owner (ADMIN) cree une organisation, invite un comptable (ACCOUNTANT) et un client
#     (CLIENT), liens recuperes dans Mailpit ; "outsider" a sa propre organisation
#   - owner depose un PDF, le fait extraire (service Python), corrige et enregistre
#     le brouillon (F12)
#   - puis : droits (401/403/404), preconditions sur le brouillon (400), generation
#     EN 16931 et BASIC validee par Mustang, telechargement, rapport, historique,
#     et acces au fichier genere selon le role.

set -uo pipefail

BASE="${BASE:-https://localhost:8443}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SAMPLES="$ROOT/backend/src/test/resources/facturx-samples"
SAMPLE="$SAMPLES/PDF_only_facture.pdf"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

RUN="$(date +%s)-$RANDOM"

PASS=0
FAIL=0
GREEN=$'\e[32m'; RED=$'\e[31m'; BOLD=$'\e[1m'; RESET=$'\e[0m'

# --- outils -----------------------------------------------------------------------

# Le cookie de session est "secure" : curl ne le renvoie qu'en https, d'ou BASE en https.
jar()   { echo "$WORK/$1.cookies"; }
xsrf()  { awk '$6 == "XSRF-TOKEN" { print $7 }' "$(jar "$1")" 2>/dev/null | tail -1; }

# req <user> <METHOD> <path> [args curl...] -> code HTTP sur stdout, corps dans $WORK/body
req() {
  local user=$1 method=$2 path=$3; shift 3
  curl -sk -o "$WORK/body" -w '%{http_code}' -X "$method" \
    -b "$(jar "$user")" -c "$(jar "$user")" \
    -H "X-XSRF-TOKEN: $(xsrf "$user")" \
    "$@" "$BASE$path"
}

# generate <user> <document> [profil] -> POST /api/documents/{id}/generate
generate() {
  local user=$1 doc=$2 profile=${3:-}
  if [[ -n "$profile" ]]; then
    req "$user" POST "/api/documents/$doc/generate" -H 'Content-Type: application/json' -d "{\"profile\":\"$profile\"}"
  else
    req "$user" POST "/api/documents/$doc/generate"
  fi
}

# check <description> <code attendu> <code recu> [expression jq qui doit valoir true]
check() {
  local desc=$1 expected=$2 actual=$3 expr=${4:-}
  local ok=1
  [[ "$actual" == "$expected" ]] || ok=0
  if [[ $ok == 1 && -n "$expr" ]]; then
    [[ "$(jq -r "$expr" "$WORK/body" 2>/dev/null)" == "true" ]] || ok=0
  fi
  if [[ $ok == 1 ]]; then
    PASS=$((PASS + 1)); echo "  ${GREEN}✓${RESET} $desc"
  else
    FAIL=$((FAIL + 1)); echo "  ${RED}✗ $desc${RESET}"
    echo "      attendu HTTP $expected${expr:+ et $expr}, recu HTTP $actual"
    echo "      corps : $(head -c 600 "$WORK/body")"
  fi
}

# setup <description> <code attendu> <code recu> : une etape de preparation doit reussir
setup() {
  if [[ "$3" != "$2" ]]; then
    echo "${RED}Preparation impossible : $1 (HTTP $3, attendu $2)${RESET}"
    head -c 600 "$WORK/body"; echo
    exit 2
  fi
}

register() {
  local user=$1
  local email="f13-$user-$RUN@test.fr"
  req "$user" GET /api/healthcheck > /dev/null   # depose le cookie XSRF-TOKEN
  setup "inscription $user" 201 "$(req "$user" POST /api/auth/register \
    -H 'Content-Type: application/json' \
    -d "{\"email\":\"$email\",\"password\":\"correcthorsebattery\",\"firstName\":\"F13\",\"lastName\":\"$user\"}")"
  echo "$email"
}

# invite <user> <email> <ROLE> : invitation par l'owner, lien lu dans Mailpit, acceptation
invite() {
  local user=$1 email=$2 role=$3
  setup "invitation $user ($role)" 201 "$(req owner POST "/api/organizations/$ORG/invitations" \
    -H 'Content-Type: application/json' -d "{\"email\":\"$email\",\"role\":\"$role\"}")"
  local token=""
  for _ in 1 2 3 4 5 6 7 8 9 10; do
    local msg_id
    msg_id=$(curl -sk "$BASE/mailpit/api/v1/search?query=to:$email" | jq -r '.messages[0].ID // empty')
    if [[ -n "$msg_id" ]]; then
      token=$(curl -sk "$BASE/mailpit/api/v1/message/$msg_id" | jq -r .Text | grep -o 'invitations/[A-Za-z0-9-]*' | head -1 | cut -d/ -f2)
      [[ -n "$token" ]] && break
    fi
    sleep 1
  done
  [[ -n "$token" ]] || { echo "${RED}Lien d'invitation introuvable dans Mailpit pour $email${RESET}"; exit 2; }
  setup "acceptation invitation $user" 200 "$(req "$user" POST "/api/invitations/accept?token=$token")"
}

upload() {   # upload <user> <org> <fichier source> <nom> -> id du document
  setup "depot $4" 200 "$(req "$1" POST "/api/documents?organizationId=$2" \
    -F "file=@$3;filename=$4;type=application/pdf")"
  jq -r .id "$WORK/body"
}

# Brouillon complet : numero, dates, vendeur avec SIREN et numero de TVA (BR-S-02),
# acheteur, deux taux de TVA.
draft() {   # draft <numero de TVA vendeur>
  cat <<JSON
{"fields":{"invoiceNumber":"CI-F13-0001","invoiceDate":"28/07/2026","dueDate":"27/08/2026",
  "sellerName":"SARL Dupont Informatique","sellerSiren":"452 891 237","sellerVat":"$1",
  "sellerAddress":"14 rue des Lilas, 75011 Paris","buyerName":"SAS Martin & Associes",
  "buyerAddress":"8 avenue Foch, 69002 Lyon","vatRate":"20"},
 "lines":[{"description":"Prestation conseil IT","quantity":"5","unitPrice":"750.00"},
          {"description":"Livre technique","quantity":"3","unitPrice":"12,50","vatRate":"5,5"}]}
JSON
}

# --- preparation ------------------------------------------------------------------

echo "${BOLD}F13 - generation Factur-X, contre $BASE${RESET}"
command -v jq > /dev/null || { echo "jq est requis"; exit 2; }
curl -sk -o /dev/null "$BASE/api/healthcheck" || { echo "${RED}$BASE ne repond pas (docker compose up ?)${RESET}"; exit 2; }
[[ -f "$SAMPLE" ]] || { echo "${RED}Echantillon introuvable : $SAMPLE${RESET}"; exit 2; }

echo "Preparation..."
register owner > /dev/null
ACCOUNTANT_EMAIL=$(register accountant)
CLIENT_EMAIL=$(register client)
register outsider > /dev/null

setup "creation organisation" 200 "$(req owner POST "/api/organizations?name=F13-CI-$RUN")"
ORG=$(jq -r .id "$WORK/body")
setup "creation organisation outsider" 200 "$(req outsider POST "/api/organizations?name=F13-Autre-$RUN")"

invite accountant "$ACCOUNTANT_EMAIL" ACCOUNTANT
invite client "$CLIENT_EMAIL" CLIENT

DOC=$(upload owner "$ORG" "$SAMPLE" "facture-ci-f13.pdf")
DOC_NO_DRAFT=$(upload owner "$ORG" "$SAMPLE" "facture-sans-extraction.pdf")
DOC_CLIENT=$(upload client "$ORG" "$SAMPLE" "facture-du-client.pdf")

echo "Organisation $ORG : document $DOC (owner), $DOC_NO_DRAFT (owner, jamais extrait), $DOC_CLIENT (client)"

# --- tests ------------------------------------------------------------------------

echo; echo "${BOLD}Authentification et droits${RESET}"
check "sans session -> 401" 401 "$(curl -sk -o "$WORK/body" -w '%{http_code}' "$BASE/api/documents/$DOC/generations")"
check "outsider : generation -> 403 ACCESS_DENIED" 403 "$(generate outsider "$DOC")" '.error == "ACCESS_DENIED"'
check "outsider : historique -> 403" 403 "$(req outsider GET "/api/documents/$DOC/generations")"
check "client (sans GENERATE_DOCUMENT) : generation sur son propre document -> 403" 403 \
  "$(generate client "$DOC_CLIENT")" '.error == "ACCESS_DENIED"'
check "client : historique -> 403" 403 "$(req client GET "/api/documents/$DOC_CLIENT/generations")"
check "document inexistant -> 404 DOCUMENT_NOT_FOUND" 404 "$(generate owner 999999999)" '.error == "DOCUMENT_NOT_FOUND"'
check "document jamais extrait -> 404 DRAFT_NOT_FOUND" 404 "$(generate owner "$DOC_NO_DRAFT")" '.error == "DRAFT_NOT_FOUND"'
check "historique d'un document jamais extrait : vide" 200 "$(req owner GET "/api/documents/$DOC_NO_DRAFT/generations")" 'length == 0'

echo; echo "${BOLD}Preconditions sur le brouillon (F12 -> F13)${RESET}"
check "extraction par le service Python" 200 "$(req owner POST "/api/documents/$DOC/extract")"
check "brouillon extrait mais jamais verifie par un humain -> 400" 400 "$(generate owner "$DOC")" \
  '.error == "GENERATION_DRAFT_NOT_CHECKED" or .error == "DRAFT_INVALID"'
check "profil inconnu (EXTENDED) -> 400 GENERATION_PROFILE_UNSUPPORTED" 400 "$(generate owner "$DOC" EXTENDED)" \
  '.error == "GENERATION_PROFILE_UNSUPPORTED" and (.message | test("EN16931, BASIC"))'
check "brouillon enregistre sans numero de TVA vendeur" 200 \
  "$(req owner PUT "/api/documents/$DOC/draft" -H 'Content-Type: application/json' -d "$(draft '')")"
check "TVA facturee sans numero de TVA vendeur -> 400 GENERATION_DRAFT_INCOMPLETE (BR-S-02)" 400 \
  "$(generate owner "$DOC")" '.error == "GENERATION_DRAFT_INCOMPLETE" and (.errors[0] | test("TVA intracommunautaire du vendeur"))'
check "brouillon complet enregistre" 200 \
  "$(req owner PUT "/api/documents/$DOC/draft" -H 'Content-Type: application/json' -d "$(draft FR45452891237)")"
check "historique avant generation : vide" 200 "$(req owner GET "/api/documents/$DOC/generations")" 'length == 0'

echo; echo "${BOLD}Generation et validation (moteur Java)${RESET}"
check "generation sans profil -> EN16931 par defaut, VALID, 0 erreur" 200 "$(generate owner "$DOC")" \
  ".status == \"VALID\" and .profile == \"EN16931\" and .errorCount == 0 and .sourceDocumentId == $DOC
   and .filename == \"Factur-X_CI-F13-0001.pdf\" and (.generatedDocumentId | type) == \"number\"
   and (.validationRunId | type) == \"number\" and .errorMessage == null"
GENERATED=$(jq -r '.generatedDocumentId // empty' "$WORK/body")
check "generation BASIC -> VALID" 200 "$(generate owner "$DOC" BASIC)" '.status == "VALID" and .profile == "BASIC" and .errorCount == 0'
GENERATED_BASIC=$(jq -r '.generatedDocumentId // empty' "$WORK/body")
check "comptable (GENERATE_DOCUMENT) : generation -> VALID" 200 "$(generate accountant "$DOC")" '.status == "VALID"'
check "historique : 3 generations, plus recente en premier, toutes VALID" 200 \
  "$(req owner GET "/api/documents/$DOC/generations")" \
  "length == 3 and all(.[]; .status == \"VALID\") and map(.profile) == [\"EN16931\", \"BASIC\", \"EN16931\"]
   and .[1].generatedDocumentId == ${GENERATED_BASIC:-0} and .[2].generatedDocumentId == ${GENERATED:-0}"
check "comptable voit le meme historique" 200 "$(req accountant GET "/api/documents/$DOC/generations")" 'length == 3'

if [[ -n "$GENERATED" ]]; then
  echo; echo "${BOLD}Fichier genere${RESET}"
  CODE=$(curl -sk -o "$WORK/generated.pdf" -D "$WORK/headers" -w '%{http_code}' \
    -b "$(jar owner)" "$BASE/api/documents/$GENERATED/download")
  echo '{}' > "$WORK/body"
  check "telechargement par le owner" 200 "$CODE"
  [[ "$(head -c 5 "$WORK/generated.pdf")" == "%PDF-" ]]
  check "le fichier telecharge est un PDF" 0 $?
  grep -qi '^content-disposition:.*Factur-X_CI-F13-0001.pdf' "$WORK/headers"
  check "nom de fichier Factur-X_CI-F13-0001.pdf dans Content-Disposition" 0 $?
  grep -qi '^content-type: application/pdf' "$WORK/headers"
  check "Content-Type application/pdf" 0 $?
  # Le rapport peut contenir des avertissements (ex. BR-FR-12, adresse electronique de
  # l'acheteur sans SIREN) : ils ne rendent pas la facture non conforme.
  check "rapport de validation (F09) : conforme, aucune erreur (avertissements toleres)" 200 \
    "$(req owner GET "/api/documents/$GENERATED/report")" \
    '.valid == true and .errorCount == 0 and all(.errors[]; .severity != "ERROR")'
  check "le fichier genere est un document de l'organisation, statut VALID" 200 "$(req owner GET /api/documents/mine)" \
    "any(.[]; .id == $GENERATED and .status == \"VALID\" and .organizationId == $ORG and .filename == \"Factur-X_CI-F13-0001.pdf\")"
  check "le fichier genere n'a pas de brouillon : pas de generation en cascade -> 404" 404 \
    "$(generate owner "$GENERATED")" '.error == "DRAFT_NOT_FOUND"'

  echo; echo "${BOLD}Acces au fichier genere selon le role${RESET}"
  check "sans session : telechargement -> 401" 401 "$(curl -sk -o "$WORK/body" -w '%{http_code}' "$BASE/api/documents/$GENERATED/download")"
  check "comptable (VIEW_ALL_DOCUMENTS) : telechargement -> 200" 200 "$(req accountant GET "/api/documents/$GENERATED/download")"
  check "comptable : rapport -> 200" 200 "$(req accountant GET "/api/documents/$GENERATED/report")" '.valid == true'
  check "client (VIEW_OWN_DOCUMENTS, pas proprietaire) : telechargement -> 403" 403 "$(req client GET "/api/documents/$GENERATED/download")"
  check "client : rapport -> 403" 403 "$(req client GET "/api/documents/$GENERATED/report")"
  check "autre organisation : telechargement -> 403" 403 "$(req outsider GET "/api/documents/$GENERATED/download")"
  check "autre organisation : rapport -> 403" 403 "$(req outsider GET "/api/documents/$GENERATED/report")"
else
  FAIL=$((FAIL + 1)); echo "  ${RED}✗ aucun document genere, verifications du fichier sautees${RESET}"
fi

# --- bilan ------------------------------------------------------------------------

echo
if [[ $FAIL == 0 ]]; then
  echo "${GREEN}${BOLD}$PASS/$((PASS + FAIL)) tests OK${RESET}"
else
  echo "${RED}${BOLD}$FAIL echec(s) sur $((PASS + FAIL))${RESET}"
fi
[[ $FAIL == 0 ]]
