#!/usr/bin/env bash
# F13 - Echantillon de reference sur toute la chaine, contre la stack qui tourne
# (docker compose up). Lance par la CI apres "Start containers" : le job echoue si la
# facture generee n'est pas conforme pour le moteur Java (Mustang).
#
#   ./scripts/test-f13-generation.sh                 # https://localhost:8443 par defaut
#   BASE=https://localhost:8443 ./scripts/test-f13-generation.sh
#
# Chaine : inscription -> organisation -> depot d'un PDF -> extraction (service
# Python) -> brouillon corrige et enregistre (F12) -> generation EN 16931 (Python)
# -> validation independante (Java) -> statut VALID, telechargement, rapport.

set -uo pipefail

BASE="${BASE:-https://localhost:8443}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SAMPLE="$ROOT/backend/src/test/resources/facturx-samples/PDF_only_facture.pdf"
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
  req "$user" GET /api/healthcheck > /dev/null   # depose le cookie XSRF-TOKEN
  setup "inscription $user" 201 "$(req "$user" POST /api/auth/register \
    -H 'Content-Type: application/json' \
    -d "{\"email\":\"f13-$user-$RUN@test.fr\",\"password\":\"correcthorsebattery\",\"firstName\":\"F13\",\"lastName\":\"$user\"}")"
}

# Brouillon complet : numero, dates, vendeur avec SIREN et numero de TVA (BR-S-02),
# acheteur, deux taux de TVA.
DRAFT='{"fields":{"invoiceNumber":"CI-F13-0001","invoiceDate":"28/07/2026","dueDate":"27/08/2026",
  "sellerName":"SARL Dupont Informatique","sellerSiren":"452 891 237","sellerVat":"FR45452891237",
  "sellerAddress":"14 rue des Lilas, 75011 Paris","buyerName":"SAS Martin & Associes",
  "buyerAddress":"8 avenue Foch, 69002 Lyon","vatRate":"20"},
 "lines":[{"description":"Prestation conseil IT","quantity":"5","unitPrice":"750.00"},
          {"description":"Livre technique","quantity":"3","unitPrice":"12,50","vatRate":"5,5"}]}'

# --- preparation ------------------------------------------------------------------

echo "${BOLD}F13 - generation Factur-X sur toute la chaine, contre $BASE${RESET}"
command -v jq > /dev/null || { echo "jq est requis"; exit 2; }
curl -sk -o /dev/null "$BASE/api/healthcheck" || { echo "${RED}$BASE ne repond pas (docker compose up ?)${RESET}"; exit 2; }
[[ -f "$SAMPLE" ]] || { echo "${RED}Echantillon introuvable : $SAMPLE${RESET}"; exit 2; }

echo "Preparation..."
register owner
register outsider

setup "creation organisation" 200 "$(req owner POST "/api/organizations?name=F13-CI-$RUN")"
ORG=$(jq -r .id "$WORK/body")
setup "creation organisation outsider" 200 "$(req outsider POST "/api/organizations?name=F13-Autre-$RUN")"

setup "depot du PDF" 200 "$(req owner POST "/api/documents?organizationId=$ORG" \
  -F "file=@$SAMPLE;filename=facture-ci-f13.pdf;type=application/pdf")"
DOC=$(jq -r .id "$WORK/body")

# --- chaine -----------------------------------------------------------------------

echo; echo "${BOLD}Extraction et correction${RESET}"
check "extraction par le service Python" 200 "$(req owner POST "/api/documents/$DOC/extract")"
check "generation refusee tant que le brouillon n'a pas ete enregistre" 400 \
  "$(req owner POST "/api/documents/$DOC/generate" -H 'Content-Type: application/json' -d '{"profile":"EN16931"}')" \
  '.error == "GENERATION_DRAFT_NOT_CHECKED" or .error == "DRAFT_INVALID"'
check "brouillon corrige et enregistre" 200 \
  "$(req owner PUT "/api/documents/$DOC/draft" -H 'Content-Type: application/json' -d "$DRAFT")"

echo; echo "${BOLD}Generation et validation${RESET}"
for PROFILE in EN16931 BASIC; do
  check "generation $PROFILE -> VALID (moteur Java)" 200 \
    "$(req owner POST "/api/documents/$DOC/generate" -H 'Content-Type: application/json' -d "{\"profile\":\"$PROFILE\"}")" \
    ".status == \"VALID\" and .profile == \"$PROFILE\" and .errorCount == 0"
  GENERATED=$(jq -r '.generatedDocumentId // empty' "$WORK/body")
done

if [[ -n "$GENERATED" ]]; then
  CODE=$(curl -sk -o "$WORK/generated.pdf" -w '%{http_code}' \
    -b "$(jar owner)" "$BASE/api/documents/$GENERATED/download")
  echo '{}' > "$WORK/body"
  check "telechargement du fichier genere" 200 "$CODE"
  [[ "$(head -c 5 "$WORK/generated.pdf")" == "%PDF-" ]]
  check "le fichier telecharge est un PDF" 0 $?
  check "rapport de validation : conforme" 200 "$(req owner GET "/api/documents/$GENERATED/report")" '.valid == true'
  check "historique : 2 generations" 200 "$(req owner GET "/api/documents/$DOC/generations")" 'length == 2'
  check "autre organisation : telechargement refuse" 403 "$(req outsider GET "/api/documents/$GENERATED/download")"
  check "autre organisation : rapport refuse" 403 "$(req outsider GET "/api/documents/$GENERATED/report")"
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
