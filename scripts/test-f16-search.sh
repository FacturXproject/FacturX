#!/usr/bin/env bash
# F16 - Recherche avancee : test de bout en bout avec curl, contre la stack qui tourne
# (docker compose up). Rejouable : chaque lancement cree ses propres comptes.
#
#   ./scripts/test-f16-search.sh                 # https://localhost:8443 par defaut
#   BASE=https://localhost:8443 ./scripts/test-f16-search.sh
#
# Scenario :
#   - admin cree une organisation, invite un client (lien recupere dans Mailpit)
#   - admin depose 2 PDF (un valide, un sans numero de facture -> BR-02) et les verifie
#   - client depose 1 XML et 1 PDF
#   - "outsider" a sa propre organisation avec un fichier du meme nom
#   - puis chaque filtre de GET /api/documents/search est verifie, ainsi que
#     l'isolation entre organisations, les roles et les filtres invalides.

set -uo pipefail

BASE="${BASE:-https://localhost:8443}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SAMPLES="$ROOT/backend/src/test/resources/facturx-samples"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

RUN="$(date +%s)-$RANDOM"
TODAY="$(date +%F)"
TOMORROW="$(date -d tomorrow +%F)"

PASS=0
FAIL=0
GREEN=$'\e[32m'; RED=$'\e[31m'; BOLD=$'\e[1m'; RESET=$'\e[0m'

# --- outils -----------------------------------------------------------------------

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

# search <user> [param=valeur ...] -> GET /api/documents/search (organizationId=$ORG par defaut)
search() {
  local user=$1; shift
  local args=()
  local has_org=0
  for p in "$@"; do
    [[ $p == organizationId=* ]] && has_org=1
    args+=(--data-urlencode "$p")
  done
  [[ $has_org == 0 && -n "${ORG:-}" ]] && args+=(--data-urlencode "organizationId=$ORG")
  req "$user" GET /api/documents/search -G "${args[@]}"
}

body() { cat "$WORK/body"; }

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
    echo "      corps : $(head -c 400 "$WORK/body")"
  fi
}

# setup <description> <code attendu> <code recu> : une etape de preparation doit reussir
setup() {
  if [[ "$3" != "$2" ]]; then
    echo "${RED}Preparation impossible : $1 (HTTP $3, attendu $2)${RESET}"
    head -c 400 "$WORK/body"; echo
    exit 2
  fi
}

register() {
  local user=$1
  local email="f16-$user-$RUN@test.fr"
  req "$user" GET /api/healthcheck > /dev/null   # depose le cookie XSRF-TOKEN
  setup "inscription $user" 201 "$(req "$user" POST /api/auth/register \
    -H 'Content-Type: application/json' \
    -d "{\"email\":\"$email\",\"password\":\"correcthorsebattery\",\"firstName\":\"F16\",\"lastName\":\"$user\"}")"
  echo "$email"
}

upload() {   # upload <user> <org> <fichier source> <nom> <type mime> -> id du document
  setup "depot $4" 200 "$(req "$1" POST "/api/documents?organizationId=$2" \
    -F "file=@$3;filename=$4;type=$5")"
  jq -r .id "$WORK/body"
}

# --- preparation ------------------------------------------------------------------

echo "${BOLD}F16 - recherche avancee, contre $BASE${RESET}"
command -v jq > /dev/null || { echo "jq est requis"; exit 2; }
curl -sk -o /dev/null "$BASE/api/healthcheck" || { echo "${RED}$BASE ne repond pas (docker compose up ?)${RESET}"; exit 2; }

echo "Preparation..."
ADMIN_EMAIL=$(register admin)
CLIENT_EMAIL=$(register client)
register outsider > /dev/null

setup "creation organisation" 200 "$(req admin POST "/api/organizations?name=F16-Cabinet-$RUN")"
ORG=$(jq -r .id "$WORK/body")
setup "creation organisation outsider" 200 "$(req outsider POST "/api/organizations?name=F16-Autre-$RUN")"
OTHER_ORG=$(jq -r .id "$WORK/body")

req admin GET /api/auth/me > /dev/null; ADMIN_ID=$(jq -r .id "$WORK/body")
req client GET /api/auth/me > /dev/null; CLIENT_ID=$(jq -r .id "$WORK/body")

# invitation du client : le lien part par mail, on le lit dans Mailpit
setup "invitation client" 201 "$(req admin POST "/api/organizations/$ORG/invitations" \
  -H 'Content-Type: application/json' -d "{\"email\":\"$CLIENT_EMAIL\",\"role\":\"CLIENT\"}")"
TOKEN=""
for _ in 1 2 3 4 5 6 7 8 9 10; do
  MSG_ID=$(curl -sk "$BASE/mailpit/api/v1/search?query=to:$CLIENT_EMAIL" | jq -r '.messages[0].ID // empty')
  if [[ -n "$MSG_ID" ]]; then
    TOKEN=$(curl -sk "$BASE/mailpit/api/v1/message/$MSG_ID" | jq -r .Text | grep -o 'invitations/[A-Za-z0-9-]*' | head -1 | cut -d/ -f2)
    [[ -n "$TOKEN" ]] && break
  fi
  sleep 1
done
[[ -n "$TOKEN" ]] || { echo "${RED}Lien d'invitation introuvable dans Mailpit${RESET}"; exit 2; }
setup "acceptation invitation" 200 "$(req client POST "/api/invitations/accept?token=$TOKEN")"

printf '<?xml version="1.0" encoding="UTF-8"?>\n<avoir/>\n' > "$WORK/avoir.xml"

DOC_VALID=$(upload admin "$ORG" "$SAMPLES/EN16931_Einfach.pdf" "Facture_ACME_2026-001.pdf" application/pdf)
DOC_INVALID=$(upload admin "$ORG" "$SAMPLES/missing-invoice-number.pdf" "facture_acme_2026-002.pdf" application/pdf)
DOC_XML=$(upload client "$ORG" "$WORK/avoir.xml" "avoir-client.xml" text/xml)
DOC_CLIENT_PDF=$(upload client "$ORG" "$SAMPLES/wrong-total-amount.pdf" "client-facture.pdf" application/pdf)
upload outsider "$OTHER_ORG" "$SAMPLES/EN16931_Einfach.pdf" "Facture_ACME_2026-001.pdf" application/pdf > /dev/null

echo "Validation Factur-X des 2 factures de l'admin (Mustang)..."
setup "validation $DOC_VALID" 200 "$(req admin POST "/api/documents/$DOC_VALID/validate")"
setup "validation $DOC_INVALID" 200 "$(req admin POST "/api/documents/$DOC_INVALID/validate")"

echo "Organisation $ORG : 4 documents (2 admin, 2 client) - organisation $OTHER_ORG : 1 document"

# --- tests ------------------------------------------------------------------------

echo; echo "${BOLD}Authentification et isolation${RESET}"
check "sans session -> 401" 401 "$(curl -sk -o "$WORK/body" -w '%{http_code}' "$BASE/api/documents/search?organizationId=$ORG")"
check "outsider sur l'organisation de l'admin -> 403" 403 "$(search outsider)" '.error == "ACCESS_DENIED"'
check "meme nom de fichier dans 2 organisations : seul celui de l'organisation demandee" 200 \
  "$(search admin q=Facture_ACME_2026-001)" "[.content[].id] == [$DOC_VALID] and all(.content[]; .organizationId == $ORG)"
check "outsider dans sa propre organisation ne voit que son document" 200 \
  "$(search outsider organizationId="$OTHER_ORG")" '.totalElements == 1'

echo; echo "${BOLD}Filtres${RESET}"
check "sans filtre : les 4 documents, plus recent en premier" 200 "$(search admin)" \
  ".totalElements == 4 and .sort == \"uploadedAt,desc\" and .content[0].id == $DOC_CLIENT_PDF"
check "nom : q=acme (insensible a la casse) -> 2" 200 "$(search admin q=acme)" \
  "([.content[].id] | sort) == ([$DOC_VALID, $DOC_INVALID] | sort)"
check "statut : status=INVALID -> la facture sans numero" 200 "$(search admin status=INVALID)" \
  "[.content[].id] == [$DOC_INVALID]"
check "statut : status=VALID,INVALID -> 2" 200 "$(search admin status=VALID,INVALID)" '.totalElements == 2'
check "statut : status=UPLOADED -> les 2 du client" 200 "$(search admin status=UPLOADED)" '.totalElements == 2'
check "type : type=XML -> l'avoir" 200 "$(search admin type=XML)" "[.content[].id] == [$DOC_XML]"
check "type : type=PDF -> 3" 200 "$(search admin type=PDF)" '.totalElements == 3'
check "utilisateur : ownerId=client -> 2" 200 "$(search admin ownerId="$CLIENT_ID")" \
  "([.content[].id] | sort) == ([$DOC_XML, $DOC_CLIENT_PDF] | sort)"
check "date : from=to=aujourd'hui -> 4" 200 "$(search admin from="$TODAY" to="$TODAY")" '.totalElements == 4'
check "date : from=demain -> 0" 200 "$(search admin from="$TOMORROW")" '.totalElements == 0'
check "erreur : error=BR-02 (code) -> la facture sans numero" 200 "$(search admin error=BR-02)" \
  "[.content[].id] == [$DOC_INVALID]"
check "erreur : error=numéro de facture (titre francais) -> idem" 200 "$(search admin "error=numéro de facture")" \
  "[.content[].id] == [$DOC_INVALID]"
check "filtres combines : q=acme & status=VALID & type=PDF -> 1" 200 \
  "$(search admin q=acme status=VALID type=PDF)" "[.content[].id] == [$DOC_VALID]"
check "filtres combines sans resultat : q=acme & type=XML -> 0" 200 "$(search admin q=acme type=XML)" '.totalElements == 0'

echo; echo "${BOLD}Tri et pagination${RESET}"
check "tri : sort=filename,asc renvoye dans la reponse" 200 "$(search admin sort=filename,asc)" '.sort == "filename,asc"'
search admin size=2 page=0 > /dev/null; PAGE0=$(jq -c '[.content[].id]' "$WORK/body")
search admin size=2 page=1 > /dev/null; PAGE1=$(jq -c '[.content[].id]' "$WORK/body")
check "pagination : size=2 -> 2 pages" 200 "$(search admin size=2)" '.totalPages == 2 and (.content | length) == 2'
echo "{\"p0\":$PAGE0,\"p1\":$PAGE1}" > "$WORK/body"
check "pagination : pages 0 et 1 = les 4 documents, sans doublon" 200 200 '(.p0 + .p1 | unique | length) == 4'
search admin size=2 page=1 > /dev/null
check "pagination : meme page demandee deux fois = meme resultat" 200 200 "[.content[].id] == $PAGE1"

echo; echo "${BOLD}Roles${RESET}"
check "client : ne voit que ses 2 documents" 200 "$(search client)" \
  "([.content[].id] | sort) == ([$DOC_XML, $DOC_CLIENT_PDF] | sort)"
check "client : q=acme -> 0 (documents de l'admin invisibles)" 200 "$(search client q=acme)" '.totalElements == 0'
check "client : ownerId=admin -> 403" 403 "$(search client ownerId="$ADMIN_ID")"

echo; echo "${BOLD}Filtres invalides -> 400 INVALID_SEARCH_FILTER${RESET}"
check "organisation manquante" 400 "$(req admin GET /api/documents/search)" '.field == "organizationId"'
check "status=DONE" 400 "$(search admin status=DONE)" '.error == "INVALID_SEARCH_FILTER" and .field == "status"'
check "type=DOCX" 400 "$(search admin type=DOCX)" '.field == "type"'
check "from=15/03/2026" 400 "$(search admin from=15/03/2026)" '.field == "from"'
check "from apres to" 400 "$(search admin from="$TOMORROW" to="$TODAY")" '.field == "from"'
check "sort=storagePath,asc" 400 "$(search admin sort=storagePath,asc)" '.field == "sort"'
check "size=500" 400 "$(search admin size=500)" '.field == "size"'
check "ownerId=abc" 400 "$(search admin ownerId=abc)" '.field == "ownerId"'

# --- bilan ------------------------------------------------------------------------

echo
if [[ $FAIL == 0 ]]; then
  echo "${GREEN}${BOLD}$PASS/$((PASS + FAIL)) tests OK${RESET}"
else
  echo "${RED}${BOLD}$FAIL echec(s) sur $((PASS + FAIL))${RESET}"
fi
[[ $FAIL == 0 ]]
