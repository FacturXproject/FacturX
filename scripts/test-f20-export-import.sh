#!/usr/bin/env bash
# F20 - Export / import : batterie de bout en bout avec curl, a travers Nginx, contre
# la stack qui tourne (docker compose up). Rejouable : chaque lancement cree ses
# propres comptes.
#
#   ./scripts/test-f20-export-import.sh                 # https://localhost:8443 par defaut
#   BASE=https://localhost:8443 ./scripts/test-f20-export-import.sh
#
# Scenario (Definition of Done : un admin exporte ses donnees et importe un fichier valide) :
#   - admin cree une organisation et y invite un comptable et un client par IMPORT CSV
#     (apercu a blanc, puis import) ; les liens arrivent dans Mailpit et sont acceptes
#   - admin depose et valide deux factures (une conforme, une non conforme)
#   - exports documents CSV / JSON / PDF et membres CSV / JSON : colonnes, contenu,
#     isolation entre organisations, droits par role
#   - import invalide refuse en bloc sans rien creer, fichier mal forme, CSRF
#   - l'export CSV des membres se reimporte dans une autre organisation

set -uo pipefail

BASE="${BASE:-https://localhost:8443}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SAMPLES="$ROOT/backend/src/test/resources/facturx-samples"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

RUN="$(date +%s)-$RANDOM"

PASS=0
FAIL=0
GREEN=$'\e[32m'; RED=$'\e[31m'; BOLD=$'\e[1m'; RESET=$'\e[0m'

DOCS_HEADER='"Identifiant","Organisation","Déposé par","Nom du fichier","Type","Taille (MB)","Statut","Date de dépôt","Adresse e-mail","Conforme","Nombre d’erreurs","Codes des règles"'
MEMBERS_HEADER='"Nom complet","Adresse e-mail","Rôle"'

# --- outils -----------------------------------------------------------------------

jar()   { echo "$WORK/$1.cookies"; }
xsrf()  { awk '$6 == "XSRF-TOKEN" { print $7 }' "$(jar "$1")" 2>/dev/null | tail -1; }

# req <user> <METHOD> <path> [args curl...] -> code HTTP sur stdout, corps dans $WORK/body,
# en-tetes dans $WORK/headers
req() {
  local user=$1 method=$2 path=$3; shift 3
  curl -sk -o "$WORK/body" -D "$WORK/headers" -w '%{http_code}' -X "$method" \
    -b "$(jar "$user")" -c "$(jar "$user")" \
    -H "X-XSRF-TOKEN: $(xsrf "$user")" \
    "$@" "$BASE$path"
}

header_value() { grep -i "^$1:" "$WORK/headers" | head -1 | cut -d' ' -f2- | tr -d '\r'; }

# export <user> <documents|members> <format>
export_as() { req "$1" GET "/api/organizations/${4:-$ORG}/export/$2?format=$3"; }

# import <user> <fichier> <true|false> [org]
import_csv() {
  req "$1" POST "/api/organizations/${4:-$ORG}/import/invitations?dryRun=$3" -F "file=@$2;type=text/csv"
}

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
    echo "      corps : $(head -c 600 "$WORK/body")"
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
  local email="f20-$user-$RUN@test.fr"
  # JSON prepare hors de "$(...)" : le bash 3.2 de macOS y developpe les accolades.
  local body="{\"email\":\"$email\",\"password\":\"correcthorsebattery\",\"firstName\":\"F20\",\"lastName\":\"$user\"}"
  req "$user" GET /api/healthcheck > /dev/null   # depose le cookie XSRF-TOKEN
  setup "inscription $user" 201 "$(req "$user" POST /api/auth/register \
    -H 'Content-Type: application/json' -d "$body")" >&2
  echo "$email"
}

upload() {   # upload <user> <org> <fichier source> <nom> <type mime> -> id du document
  setup "depot $4" 200 "$(req "$1" POST "/api/documents?organizationId=$2" \
    -F "file=@$3;filename=$4;type=$5")"
  jq -r .id "$WORK/body"
}

# accept_from_mailpit <user> <email> : lit le lien d'invitation dans Mailpit et l'accepte
accept_from_mailpit() {
  local token=""
  for _ in 1 2 3 4 5 6 7 8 9 10; do
    local msg_id
    msg_id=$(curl -sk "$BASE/mailpit/api/v1/search?query=to:$2" | jq -r '.messages[0].ID // empty')
    if [[ -n "$msg_id" ]]; then
      token=$(curl -sk "$BASE/mailpit/api/v1/message/$msg_id" | jq -r .Text | grep -o 'invitations/[A-Za-z0-9-]*' | head -1 | cut -d/ -f2)
      [[ -n "$token" ]] && break
    fi
    sleep 1
  done
  [[ -n "$token" ]] || return 1
  [[ "$(req "$1" POST "/api/invitations/accept?token=$token")" == 200 ]]
}

invitation_count() {
  req admin GET "/api/organizations/$ORG/invitations" > /dev/null
  jq length "$WORK/body"
}

# --- preparation ------------------------------------------------------------------

echo "${BOLD}F20 - export / import, contre $BASE${RESET}"
command -v jq > /dev/null || { echo "jq est requis"; exit 2; }
curl -sk -o /dev/null "$BASE/api/healthcheck" || { echo "${RED}$BASE ne repond pas (docker compose up ?)${RESET}"; exit 2; }

echo "Preparation..."
ADMIN_EMAIL=$(register admin)
ACCOUNTANT_EMAIL=$(register accountant)
CLIENT_EMAIL=$(register client)
register outsider > /dev/null

setup "creation organisation" 200 "$(req admin POST "/api/organizations?name=F20-Cabinet-$RUN")"
ORG=$(jq -r .id "$WORK/body")
ORG_NAME=$(jq -r .name "$WORK/body")
setup "creation organisation outsider" 200 "$(req outsider POST "/api/organizations?name=F20-Autre-$RUN")"
OTHER_ORG=$(jq -r .id "$WORK/body")

# --- import : le chemin nominal ---------------------------------------------------

echo; echo "${BOLD}Import d'invitations (chemin nominal)${RESET}"
# Style Excel francais : BOM, point-virgule, libelles de roles de l'application.
printf '\xEF\xBB\xBFemail;role\n%s;Comptable\n%s;CLIENT\n' "$ACCOUNTANT_EMAIL" "$CLIENT_EMAIL" > "$WORK/invitations.csv"

check "apercu a blanc : 2 lignes valides" 200 "$(import_csv admin "$WORK/invitations.csv" true)" \
  'length == 2 and all(.valid) and .[0].role == "ACCOUNTANT" and .[0].lineNumber == 2'
assert "l'apercu ne cree aucune invitation" "[[ $(invitation_count) == 0 ]]"
check "import : 200" 200 "$(import_csv admin "$WORK/invitations.csv" false)" 'length == 2 and all(.valid)'
check "2 invitations PENDING creees" 200 "$(req admin GET "/api/organizations/$ORG/invitations")" \
  "length == 2 and all(.status == \"PENDING\") and ([.[].email] | sort) == ([\"$ACCOUNTANT_EMAIL\", \"$CLIENT_EMAIL\"] | sort)"
assert "le comptable recoit le lien dans Mailpit et l'accepte" "accept_from_mailpit accountant '$ACCOUNTANT_EMAIL'"
assert "le client recoit le lien dans Mailpit et l'accepte" "accept_from_mailpit client '$CLIENT_EMAIL'"
check "le comptable est membre avec le bon role" 200 "$(req admin GET "/api/organizations/$ORG/members")" \
  "any(.[]; .email == \"$ACCOUNTANT_EMAIL\" and .role == \"ACCOUNTANT\") and length == 3"

echo "Depot et validation Factur-X de 2 factures (Mustang)..."
DOC_VALID=$(upload admin "$ORG" "$SAMPLES/EN16931_Einfach.pdf" "F20-conforme.pdf" application/pdf)
DOC_INVALID=$(upload client "$ORG" "$SAMPLES/missing-invoice-number.pdf" "=F20-non-conforme.pdf" application/pdf)
upload outsider "$OTHER_ORG" "$SAMPLES/EN16931_Einfach.pdf" "F20-secret-autre.pdf" application/pdf > /dev/null
setup "validation $DOC_VALID" 200 "$(req admin POST "/api/documents/$DOC_VALID/validate")"
setup "validation $DOC_INVALID" 200 "$(req admin POST "/api/documents/$DOC_INVALID/validate")"

# --- export -----------------------------------------------------------------------

echo; echo "${BOLD}Export des documents${RESET}"
check "CSV : 200" 200 "$(export_as admin documents csv)"
cp "$WORK/body" "$WORK/documents.csv"
assert "CSV : Content-Type text/csv UTF-8" "[[ '$(header_value Content-Type)' == 'text/csv;charset=UTF-8' ]]"
assert "CSV : piece jointe documents.csv" "[[ '$(header_value Content-Disposition)' == 'attachment; filename=\"documents.csv\"' ]]"
assert "CSV : commence par le BOM UTF-8" "[[ \"\$(head -c 3 '$WORK/documents.csv' | od -An -tx1 | tr -d ' ')\" == efbbbf ]]"
assert "CSV : en-tete avec les 12 colonnes attendues" "[[ \"\$(head -1 '$WORK/documents.csv' | tail -c +4)\" == '$DOCS_HEADER' ]]"
assert "CSV : une ligne par document de l'organisation" "(( \$(wc -l < '$WORK/documents.csv') == 3 ))"
assert "CSV : facture conforme -> Oui, 0 erreur" "grep -q '\"F20-conforme.pdf\".*\"VALID\".*\"Oui\",\"0\"' '$WORK/documents.csv'"
assert "CSV : facture non conforme -> Non, regle BR-02" "grep -q '\"INVALID\".*\"Non\".*BR-02' '$WORK/documents.csv'"
assert "CSV : formule neutralisee ('=...)" "grep -q \"\\\"'=F20-non-conforme.pdf\\\"\" '$WORK/documents.csv'"
assert "CSV : rien d'une autre organisation" "! grep -q 'F20-secret-autre' '$WORK/documents.csv'"

check "JSON : 2 documents avec leur resultat de validation" 200 "$(export_as admin documents json)" \
  "length == 2 and ([.[] | select(.filename == \"F20-conforme.pdf\")][0].valid == \"Oui\") and ([.[] | select(.status == \"INVALID\")][0].ruleCodes | index(\"BR-02\") != null) and all(.organizationName == \"$ORG_NAME\")"

check "PDF : 200" 200 "$(export_as admin documents pdf)"
assert "PDF : Content-Type application/pdf" "[[ '$(header_value Content-Type)' == 'application/pdf' ]]"
assert "PDF : fichier PDF" "[[ \"\$(head -c 5 '$WORK/body')\" == '%PDF-' ]]"

echo; echo "${BOLD}Export des membres${RESET}"
check "CSV : 200" 200 "$(export_as admin members csv)"
cp "$WORK/body" "$WORK/members.csv"
assert "CSV : en-tete Nom complet, Adresse e-mail, Role" "[[ \"\$(head -1 '$WORK/members.csv' | tail -c +4)\" == '$MEMBERS_HEADER' ]]"
assert "CSV : 3 membres avec leur role" "(( \$(wc -l < '$WORK/members.csv') == 4 )) && grep -q '\"$ACCOUNTANT_EMAIL\",\"ACCOUNTANT\"' '$WORK/members.csv'"
check "JSON : 3 membres" 200 "$(export_as admin members json)" \
  "length == 3 and any(.[]; .email == \"$ADMIN_EMAIL\" and .role == \"ADMIN\")"

echo; echo "${BOLD}Droits${RESET}"
check "comptable : export documents autorise" 200 "$(export_as accountant documents csv)"
check "comptable : export membres refuse" 403 "$(export_as accountant members csv)" '.error == "ACCESS_DENIED"'
check "comptable : import refuse" 403 "$(import_csv accountant "$WORK/invitations.csv" true)"
check "client : export documents refuse" 403 "$(export_as client documents json)"
check "client : import refuse" 403 "$(import_csv client "$WORK/invitations.csv" true)"
check "autre organisation : export refuse" 403 "$(export_as outsider documents csv)"
check "sans session : 401" 401 "$(curl -sk -o "$WORK/body" -w '%{http_code}' "$BASE/api/organizations/$ORG/export/documents?format=csv")"
check "format inconnu : 400" 400 "$(export_as admin documents xlsx)" '.error == "INVALID_EXPORT_FORMAT"'
check "PDF des membres : 400" 400 "$(export_as admin members pdf)" '.error == "INVALID_EXPORT_FORMAT"'

# --- import : refus ---------------------------------------------------------------

echo; echo "${BOLD}Import refuse sans corruption${RESET}"
BEFORE=$(invitation_count)
NEW_EMAIL="f20-nouveau-$RUN@test.fr"
printf 'email,role\n%s,CLIENT\npas-un-email,CLIENT\n%s,CLIENT\n%s,DIRECTEUR\n' \
  "$NEW_EMAIL" "$CLIENT_EMAIL" "f20-role-$RUN@test.fr" > "$WORK/invalid.csv"
check "apercu : chaque erreur sur sa ligne" 200 "$(import_csv admin "$WORK/invalid.csv" true)" \
  '.[0].valid and .[1].error == "Email vide ou invalide" and .[2].error == "Déjà membre de l'"'"'organisation" and (.[3].error | startswith("Rôle invalide"))'
check "import : 400 avec la liste des lignes" 400 "$(import_csv admin "$WORK/invalid.csv" false)" 'length == 4 and (map(.valid) == [true, false, false, false])'
assert "aucune invitation creee, meme pour la ligne valide" "[[ $(invitation_count) == $BEFORE ]]"
assert "aucun e-mail envoye pour la ligne valide" "[[ \$(curl -sk '$BASE/mailpit/api/v1/search?query=to:$NEW_EMAIL' | jq .messages_count) == 0 ]]"

printf 'mail,profil\na@x.fr,CLIENT\n' > "$WORK/columns.csv"
check "colonnes manquantes : 400" 400 "$(import_csv admin "$WORK/columns.csv" true)" '.error == "INVALID_IMPORT_FILE"'
printf 'email,role\n"a@x.fr,CLIENT\n' > "$WORK/broken.csv"
check "CSV mal forme : 400" 400 "$(import_csv admin "$WORK/broken.csv" true)" '.message == "Fichier CSV invalide ou illisible."'
: > "$WORK/empty.csv"
check "fichier vide : 400" 400 "$(import_csv admin "$WORK/empty.csv" true)" '.message == "Le fichier est vide."'
check "sans jeton CSRF : 403" 403 "$(curl -sk -o "$WORK/body" -w '%{http_code}' -b "$(jar admin)" \
  -F "file=@$WORK/invitations.csv;type=text/csv" "$BASE/api/organizations/$ORG/import/invitations?dryRun=false")"

echo; echo "${BOLD}Aller-retour export -> import${RESET}"
setup "seconde organisation" 200 "$(req admin POST "/api/organizations?name=F20-Cible-$RUN")"
TARGET_ORG=$(jq -r .id "$WORK/body")
check "l'export des membres se reimporte (apercu)" 200 "$(import_csv admin "$WORK/members.csv" true "$TARGET_ORG")" \
  "length == 3 and ([.[] | select(.email == \"$ADMIN_EMAIL\")][0].error == \"Déjà membre de l'organisation\") and ([.[] | select(.email == \"$ACCOUNTANT_EMAIL\")][0].role == \"ACCOUNTANT\") and ([.[] | select(.valid)] | length) == 2"

# --- bilan ------------------------------------------------------------------------

echo
if [[ $FAIL == 0 ]]; then
  echo "${GREEN}${BOLD}$PASS/$((PASS + FAIL)) tests OK${RESET}"
else
  echo "${RED}${BOLD}$FAIL echec(s) sur $((PASS + FAIL))${RESET}"
fi
[[ $FAIL == 0 ]]
