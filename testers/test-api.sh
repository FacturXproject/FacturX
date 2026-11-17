#!/bin/bash
# tester for the public API (F17).
#
# 1. Start the project (./start.sh) and log in.
# 2. On the "Clés API" page, create a key with the 3 scopes:
#    DOCUMENTS_READ, DOCUMENTS_WRITE, DOCUMENTS_VALIDATE.
# 3. Run:  ./testers/test-api.sh [file.pdf]   (the script asks for the key)
#    or:   KEY=fxk_... ./testers/test-api.sh [file.pdf]
#
# The last step sends requests until the rate limit answers 429: the key is then
# blocked for up to 60 seconds.



BASE="${BASE:-https://localhost:8443}"
API="$BASE/api/public/v1/documents"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
FILE="${1:-$ROOT/Factures/01_plombier_renovation-salle-de-bain.pdf}"

GREEN=$'\033[32m'; RED=$'\033[31m'; BOLD=$'\033[1m'; DIM=$'\033[2m'; RESET=$'\033[0m'
PASS=0; FAIL=0
TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT

if [ -z "$KEY" ]; then
  echo "Create a key on the \"Clés API\" page (scopes READ, WRITE, VALIDATE)."
  read -r -p "API key (fxk_...): " KEY
  KEY="$(echo "$KEY" | tr -d '[:space:]')"
fi
if [ -z "$KEY" ]; then
  echo "No API key given."
  exit 2
fi
if [ ! -f "$FILE" ]; then
  echo "File not found: $FILE"
  exit 2
fi

# call <METHOD> <url> [curl args...] -> HTTP code on stdout, body in $TMP/body, headers in $TMP/headers
call() {
  local method=$1 url=$2; shift 2
  curl -sk -X "$method" -o "$TMP/body" -D "$TMP/headers" -w '%{http_code}' "$@" "$url"
}

# check <description> <expected code> <actual code> <what it means>
check() {
  if [ "$3" = "$2" ]; then
    PASS=$((PASS + 1)); echo "  ${GREEN}✓${RESET} $1 ${GREEN}(expected $2, got $3)${RESET}"
  else
    FAIL=$((FAIL + 1)); echo "  ${RED}✗ $1 (expected $2, got $3)${RESET}"
    echo "      $(head -c 300 "$TMP/body")"
  fi
  [ -n "$4" ] && echo "      ${DIM}↳ $4${RESET}"
}

# section <title> <purpose>
section() {
  echo
  echo
  echo "${BOLD}$1${RESET} — $2"
}

info() {   # extra detail printed under a test
  echo "      • $1"
}

json_field() {   # json_field <name> -> value of a top-level field of the last body
  sed -n "s/.*\"$1\":\"\{0,1\}\([^\",}]*\)\"\{0,1\}.*/\1/p" "$TMP/body" | head -1
}

echo "${BOLD}Public API tester (F17) — $API${RESET}"
echo "Every request sends the key in the X-API-Key header, like an accounting software would."

section "1. Security" "the API can only be used with a valid key"
check "GET  without a key -> 401 Unauthorized" 401 "$(call GET "$API")" \
  "No X-API-Key header: the request is refused, nobody can use the API anonymously."
check "GET  with a wrong key -> 401 Unauthorized" 401 "$(call GET "$API" -H "X-API-Key: fxk_wrong")" \
  "The server hashes the key (SHA-256) and finds no match: unknown or revoked keys are refused."

section "2. CRUD" "the 4 HTTP methods required by the subject: GET, POST, PUT, DELETE"
check "GET  list documents -> 200 OK" 200 "$(call GET "$API" -H "X-API-Key: $KEY")" \
  "Lists the documents of the key's organization only (scope DOCUMENTS_READ)."
info "documents visible with this key: $(grep -o '"id":' "$TMP/body" | wc -l | tr -d ' ')"

check "POST upload $(basename "$FILE") -> 201 Created" 201 \
  "$(call POST "$API" -H "X-API-Key: $KEY" -F "file=@$FILE")" \
  "Uploads a PDF or XML (multipart field \"file\", 10 MB max); the new document starts as UPLOADED (scope DOCUMENTS_WRITE)."
ID="$(json_field id)"
if [ -z "$ID" ]; then
  echo "${RED}Upload failed, cannot continue.${RESET}"; exit 1
fi
info "new document: id $ID, status $(json_field status)"

check "GET  one document (id $ID) -> 200 OK" 200 "$(call GET "$API/$ID" -H "X-API-Key: $KEY")" \
  "Reads one document by id; a document of another organization would answer 404."

check "PUT  rename the document -> 200 OK" 200 \
  "$(call PUT "$API/$ID" -H "X-API-Key: $KEY" -H "Content-Type: application/json" \
      -d '{"filename":"renamed-by-api.pdf"}')" \
  "Updates the document with a JSON body {\"filename\": ...} (scope DOCUMENTS_WRITE)."
info "new filename: $(json_field filename)"

check "GET  download the file -> 200 OK" 200 "$(call GET "$API/$ID/download" -H "X-API-Key: $KEY")" \
  "Returns the original file, byte for byte."
info "$(grep -i '^Content-Type:' "$TMP/headers" | tr -d '\r')"

section "3. Factur-X validation" "the business feature behind the API"
check "POST start the validation -> 202 Accepted" 202 "$(call POST "$API/$ID/validate" -H "X-API-Key: $KEY")" \
  "202 = accepted, not finished yet: the validation runs in the background job queue (scope DOCUMENTS_VALIDATE)."
STATUS=""
for _ in 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15; do
  call GET "$API/$ID" -H "X-API-Key: $KEY" > /dev/null
  STATUS="$(json_field status)"
  case "$STATUS" in VALID|INVALID|FAILED) break ;; esac
  sleep 2
done
info "waiting for the job: UPLOADED -> QUEUED -> PROCESSING -> $STATUS"
check "GET  validation report -> 200 OK" 200 "$(call GET "$API/$ID/report" -H "X-API-Key: $KEY")" \
  "Readable report of the Mustang engine: is the file a compliant Factur-X, and which rules fail."
info "valid: $(json_field valid), errors: $(json_field errorCount)"
info "a plain PDF has no embedded XML, so INVALID is the expected result here"

section "4. Delete" "same rules as in the web application"
check "DELETE the document -> 204 No Content" 204 "$(call DELETE "$API/$ID" -H "X-API-Key: $KEY")" \
  "204 = deleted, nothing to return. The key acts as its owner: own documents or admin (scope DOCUMENTS_WRITE)."
check "GET  the deleted document -> 404 Not Found" 404 "$(call GET "$API/$ID" -H "X-API-Key: $KEY")" \
  "Proof that the document and its file are really gone."

section "5. Documentation" "every endpoint is documented"
check "GET  Swagger UI -> 200 OK" 200 "$(call GET "$BASE/api/docs/swagger-ui/index.html")" \
  "Interactive OpenAPI documentation: $BASE/api/docs/swagger-ui/index.html"

section "6. Rate limiting" "protection against abuse"
CODE=""; N=0
while [ $N -lt 80 ]; do
  N=$((N + 1))
  CODE="$(call GET "$API" -H "X-API-Key: $KEY")"
  [ "$CODE" = "429" ] && break
done
check "GET  sent $N more times -> 429 Too Many Requests" 429 "$CODE" \
  "Each key may send 60 requests per minute; above that the API refuses with 429 until the minute ends."
info "$(grep -i '^Retry-After:' "$TMP/headers" | tr -d '\r')  (seconds to wait)"
info "$(grep -i '^X-RateLimit-Limit:' "$TMP/headers" | tr -d '\r'), $(grep -i '^X-RateLimit-Remaining:' "$TMP/headers" | tr -d '\r')"
info "this key is now blocked for about a minute: wait before running the tester again"

echo
if [ $FAIL -eq 0 ]; then
  echo "${GREEN}${BOLD}$PASS/$PASS tests OK${RESET}"
else
  echo "${RED}${BOLD}$FAIL failed, $PASS passed${RESET}"
  exit 1
fi
