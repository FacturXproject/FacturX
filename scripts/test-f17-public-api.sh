#!/usr/bin/env bash
# F17 - API publique securisee : batterie de bout en bout avec curl, a travers Nginx,
# contre la stack qui tourne (docker compose up). Rejouable : chaque lancement cree
# ses propres comptes.
#
#   ./scripts/test-f17-public-api.sh                 # https://localhost:8443 par defaut
#   BASE=https://localhost:8443 ./scripts/test-f17-public-api.sh
#
# Scenario :
#   - owner cree une organisation et des cles API depuis sa session (page "Cles API") ;
#     outsider a sa propre organisation et sa propre cle
#   - avec la cle : depot d'une vraie facture Factur-X, liste, statut, validation
#     asynchrone jusqu'a VALID, rapport, telechargement identique a l'original
#   - refus : sans cle, cle invalide, cookie de session seul, scope manquant, autre
#     organisation, cle revoquee, limite de debit (429)
#   - documentation OpenAPI / Swagger UI publique

set -uo pipefail

BASE="${BASE:-https://localhost:8443}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SAMPLE="$ROOT/backend/src/test/resources/facturx-samples/EN16931_Einfach.pdf"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

RUN="$(date +%s)-$RANDOM"
API="/api/public/v1/documents"

PASS=0
FAIL=0
GREEN=$'\e[32m'; RED=$'\e[31m'; BOLD=$'\e[1m'; RESET=$'\e[0m'

# --- outils -----------------------------------------------------------------------

jar()   { echo "$WORK/$1.cookies"; }
xsrf()  { awk '$6 == "XSRF-TOKEN" { print $7 }' "$(jar "$1")" 2>/dev/null | tail -1; }

# req <user> <METHOD> <path> [args curl...] : requete de session -> code HTTP, corps dans $WORK/body
req() {
  local user=$1 method=$2 path=$3; shift 3
  curl -sk -o "$WORK/body" -w '%{http_code}' -X "$method" \
    -b "$(jar "$user")" -c "$(jar "$user")" \
    -H "X-XSRF-TOKEN: $(xsrf "$user")" \
    "$@" "$BASE$path"
}

# pub <cle> <METHOD> <path> [args curl...] : requete API publique -> code HTTP
# (corps dans $WORK/body, en-tetes dans $WORK/headers)
pub() {
  local key=$1 method=$2 path=$3; shift 3
  curl -sk -o "$WORK/body" -D "$WORK/headers" -w '%{http_code}' -X "$method" \
    -H "X-API-Key: $key" "$@" "$BASE$path"
}

header_value() { grep -i "^$1:" "$WORK/headers" | head -1 | cut -d' ' -f2- | tr -d '\r'; }

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

assert() {   # assert <description> <condition bash evaluee>
  if eval "$2"; then
    PASS=$((PASS + 1)); echo "  ${GREEN}✓${RESET} $1"
  else
    FAIL=$((FAIL + 1)); echo "  ${RED}✗ $1${RESET}"
  fi
}

setup() {
  if [[ "$3" != "$2" ]]; then
    echo "${RED}Preparation impossible : $1 (HTTP $3, attendu $2)${RESET}"
    head -c 600 "$WORK/body"; echo
    exit 2
  fi
}

register() {
  local user=$1
  # JSON prepare hors de "$(...)" : le bash 3.2 de macOS y developpe les accolades.
  local body="{\"email\":\"f17-$user-$RUN@test.fr\",\"password\":\"correcthorsebattery\",\"firstName\":\"F17\",\"lastName\":\"$user\"}"
  req "$user" GET /api/healthcheck > /dev/null   # depose le cookie XSRF-TOKEN
  setup "inscription $user" 201 "$(req "$user" POST /api/auth/register -H 'Content-Type: application/json' -d "$body")"
}

# create_key <user> <org> <scopes JSON> -> secret de la cle (id dans $WORK/key_id)
create_key() {
  local body="{\"name\":\"ERP $RUN\",\"organizationId\":$2,\"scopes\":$3}"
  setup "creation cle API ($3)" 201 "$(req "$1" POST /api/api-keys -H 'Content-Type: application/json' -d "$body")"
  jq -r .apiKey.id "$WORK/body" > "$WORK/key_id"
  jq -r .key "$WORK/body"
}

# --- preparation ------------------------------------------------------------------

echo "${BOLD}F17 - API publique, contre $BASE${RESET}"
command -v jq > /dev/null || { echo "jq est requis"; exit 2; }
curl -sk -o /dev/null "$BASE/api/healthcheck" || { echo "${RED}$BASE ne repond pas (docker compose up ?)${RESET}"; exit 2; }
[[ -f "$SAMPLE" ]] || { echo "${RED}Echantillon introuvable : $SAMPLE${RESET}"; exit 2; }

echo "Preparation..."
register owner
register outsider
setup "creation organisation" 200 "$(req owner POST "/api/organizations?name=F17-$RUN")"
ORG=$(jq -r .id "$WORK/body")
setup "creation organisation outsider" 200 "$(req outsider POST "/api/organizations?name=F17-Autre-$RUN")"
OTHER_ORG=$(jq -r .id "$WORK/body")

FULL_KEY=$(create_key owner "$ORG" '["DOCUMENTS_READ","DOCUMENTS_WRITE","DOCUMENTS_VALIDATE"]')
READ_KEY=$(create_key owner "$ORG" '["DOCUMENTS_READ"]')
READ_KEY_ID=$(cat "$WORK/key_id")
RATE_KEY=$(create_key owner "$ORG" '["DOCUMENTS_READ"]')
OUTSIDER_KEY=$(create_key outsider "$OTHER_ORG" '["DOCUMENTS_READ"]')

# --- tests ------------------------------------------------------------------------

echo; echo "${BOLD}Gestion des cles (session)${RESET}"
check "liste de mes cles : 3 cles, aucun secret" 200 "$(req owner GET /api/api-keys)" 'length == 3'
assert "le secret n'apparait jamais dans la liste" "! grep -q '$FULL_KEY' '$WORK/body'"
KEY_BODY="{\"name\":\"x\",\"organizationId\":$ORG,\"scopes\":[\"DOCUMENTS_READ\"]}"
check "creation sans jeton CSRF -> 403" 403 "$(curl -sk -o "$WORK/body" -w '%{http_code}' -b "$(jar owner)" \
  -H 'Content-Type: application/json' -d "$KEY_BODY" "$BASE/api/api-keys")"
check "creation pour une organisation dont on n'est pas membre -> 403" 403 "$(req outsider POST /api/api-keys \
  -H 'Content-Type: application/json' -d "$KEY_BODY")" '.error == "ACCESS_DENIED"'

echo; echo "${BOLD}Authentification${RESET}"
check "sans cle -> 401 API_KEY_MISSING" 401 "$(curl -sk -o "$WORK/body" -w '%{http_code}' "$BASE$API")" '.error == "API_KEY_MISSING"'
check "cle inconnue -> 401 INVALID_API_KEY" 401 "$(pub "fxk_inconnue" GET "$API")" '.error == "INVALID_API_KEY"'
check "cookie de session seul -> 401" 401 "$(req owner GET "$API")"
check "Authorization: Bearer accepte" 200 "$(curl -sk -o "$WORK/body" -w '%{http_code}' -H "Authorization: Bearer $READ_KEY" "$BASE$API")"

echo; echo "${BOLD}Cycle de vie d'un document par API${RESET}"
check "depot d'une facture Factur-X -> 201" 201 "$(pub "$FULL_KEY" POST "$API" -F "file=@$SAMPLE;filename=facture-api-f17.pdf")" \
  '.status == "UPLOADED" and .type == "application/pdf"'
DOC=$(jq -r .id "$WORK/body")
check "liste -> contient le document" 200 "$(pub "$FULL_KEY" GET "$API?page=0&size=5")" "[.content[].id] | index($DOC) != null"
assert "en-tetes X-RateLimit-* presents (limite $(header_value X-RateLimit-Limit))" \
  "[[ -n \"$(header_value X-RateLimit-Limit)\" && -n \"$(header_value X-RateLimit-Remaining)\" ]]"
check "statut -> UPLOADED" 200 "$(pub "$FULL_KEY" GET "$API/$DOC")" '.status == "UPLOADED"'
check "rapport avant validation -> 404 JSON" 404 "$(pub "$FULL_KEY" GET "$API/$DOC/report")" '.error == "NOT_FOUND"'
check "validation -> 202 job VALIDATION" 202 "$(pub "$FULL_KEY" POST "$API/$DOC/validate")" '.type == "VALIDATION"'

STATUS=""
for _ in $(seq 1 60); do
  pub "$READ_KEY" GET "$API/$DOC" > /dev/null
  STATUS=$(jq -r .status "$WORK/body")
  [[ "$STATUS" == "VALID" || "$STATUS" == "INVALID" || "$STATUS" == "FAILED" ]] && break
  sleep 1
done
assert "statut final apres validation asynchrone : VALID (recu $STATUS)" "[[ '$STATUS' == VALID ]]"
check "rapport -> conforme" 200 "$(pub "$READ_KEY" GET "$API/$DOC/report")" '.valid == true'
curl -sk -o "$WORK/downloaded.pdf" -H "X-API-Key: $READ_KEY" "$BASE$API/$DOC/download"
assert "telechargement identique au fichier depose" "cmp -s '$SAMPLE' '$WORK/downloaded.pdf'"

echo; echo "${BOLD}Scopes et isolation${RESET}"
check "cle lecture seule : depot -> 403 INSUFFICIENT_SCOPE" 403 "$(pub "$READ_KEY" POST "$API" -F "file=@$SAMPLE")" '.error == "INSUFFICIENT_SCOPE"'
check "cle lecture seule : validation -> 403 INSUFFICIENT_SCOPE" 403 "$(pub "$READ_KEY" POST "$API/$DOC/validate")" '.error == "INSUFFICIENT_SCOPE"'
check "autre organisation : statut -> 404" 404 "$(pub "$OUTSIDER_KEY" GET "$API/$DOC")" '.error == "DOCUMENT_NOT_FOUND"'
check "autre organisation : telechargement -> 404" 404 "$(pub "$OUTSIDER_KEY" GET "$API/$DOC/download")"
check "autre organisation : liste vide" 200 "$(pub "$OUTSIDER_KEY" GET "$API")" '.totalElements == 0'

echo; echo "${BOLD}Revocation${RESET}"
check "revocation -> 200, cle inactive" 200 "$(req owner DELETE "/api/api-keys/$READ_KEY_ID")" '.active == false'
check "cle revoquee -> 401" 401 "$(pub "$READ_KEY" GET "$API")" '.error == "INVALID_API_KEY"'

echo; echo "${BOLD}Limite de debit${RESET}"
pub "$RATE_KEY" GET "$API" > /dev/null
LIMIT=$(header_value X-RateLimit-Limit)
CODE=200
for _ in $(seq 1 "$LIMIT"); do
  CODE=$(pub "$RATE_KEY" GET "$API")
  [[ "$CODE" == 429 ]] && break
done
check "au-dela de $LIMIT requetes/min -> 429 RATE_LIMITED" 429 "$CODE" '.error == "RATE_LIMITED"'
assert "en-tete Retry-After present ($(header_value Retry-After) s)" "[[ -n \"$(header_value Retry-After)\" ]]"
check "une autre cle n'est pas affectee" 200 "$(pub "$FULL_KEY" GET "$API")"

echo; echo "${BOLD}Documentation${RESET}"
check "specification OpenAPI publique, >= 5 operations" 200 \
  "$(curl -sk -o "$WORK/body" -w '%{http_code}' "$BASE/api/docs/openapi")" '[.paths[] | keys[]] | length >= 5'
check "Swagger UI accessible" 200 "$(curl -sk -o "$WORK/body" -w '%{http_code}' "$BASE/api/docs/swagger-ui/index.html")"
check "configuration Swagger UI" 200 "$(curl -sk -o "$WORK/body" -w '%{http_code}' "$BASE/api/docs/openapi/swagger-config")" \
  '.url == "/api/docs/openapi"'

# --- bilan ------------------------------------------------------------------------

echo
if [[ $FAIL == 0 ]]; then
  echo "${GREEN}${BOLD}$PASS/$((PASS + FAIL)) tests OK${RESET}"
else
  echo "${RED}${BOLD}$FAIL echec(s) sur $((PASS + FAIL))${RESET}"
fi
[[ $FAIL == 0 ]]
