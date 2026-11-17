#!/bin/bash
# tester for authentication security (F01): signup / login with email + password,
# passwords hashed and salted with BCrypt.
#
# 1. Start the project (./start.sh).
# 2. Run:  ./testers/test-auth.sh
#
# The script creates its own throwaway users (auth-test-...@test.fr): your accounts are
# never touched.



BASE="${BASE:-https://localhost:8443}"
AUTH="$BASE/api/auth"
RUN="$(date +%s)"
EMAIL="auth-test-$RUN@test.fr"
EMAIL2="auth-test-$RUN-b@test.fr"
PASSWORD="correcthorsebattery"

GREEN=$'\033[32m'; RED=$'\033[31m'; BOLD=$'\033[1m'; DIM=$'\033[2m'; RESET=$'\033[0m'
PASS=0; FAIL=0
TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT

# call <cookie jar> <METHOD> <path> [curl args...] -> HTTP code; body in $TMP/body
# Sends the CSRF token (XSRF-TOKEN cookie -> X-XSRF-TOKEN header) like the web application.
call() {
  local jar="$TMP/$1.cookies" method=$2 path=$3; shift 3
  local xsrf; xsrf="$(awk '$6 == "XSRF-TOKEN" { print $7 }' "$jar" 2>/dev/null | tail -1)"
  curl -sk -X "$method" -b "$jar" -c "$jar" -o "$TMP/body" -w '%{http_code}' \
    -H "X-XSRF-TOKEN: $xsrf" "$@" "$AUTH$path"
}

json() {   # json <email> <password> -> body of a register / login request
  printf '{"email":"%s","password":"%s","firstName":"Auth","lastName":"Tester"}' "$1" "$2"
}

# check <description> <expected> <actual> <what it means>
check() {
  if [ "$3" = "$2" ]; then
    PASS=$((PASS + 1)); echo "  ${GREEN}✓${RESET} $1 ${GREEN}(expected $2, got $3)${RESET}"
  else
    FAIL=$((FAIL + 1)); echo "  ${RED}✗ $1 (expected $2, got $3)${RESET}"
    echo "      $(head -c 300 "$TMP/body")"
  fi
  [ -n "$4" ] && echo "      ${DIM}↳ $4${RESET}"
}

section() { echo; echo; echo "${BOLD}$1${RESET} — $2"; }
info() { echo "      • $1"; }

sql() {   # sql <query> -> runs the query in the database container
  docker exec facturx-postgres sh -c "psql -U \"\$POSTGRES_USER\" -d \"\$POSTGRES_DB\" -At -c \"$1\""
}

if ! curl -sk -o /dev/null "$BASE/api/healthcheck"; then
  echo "${RED}$BASE does not answer: start the project with ./start.sh${RESET}"; exit 2
fi

echo "${BOLD}Authentication tester (F01) — $AUTH${RESET}"
echo "Test users: $EMAIL, $EMAIL2 (password: $PASSWORD)"

# Get the CSRF cookie, like the browser does on its first request
call user GET /me > /dev/null; call user2 GET /me > /dev/null

section "1. Signup" "email + password"
check "POST /register -> 201 Created" 201 \
  "$(call user POST /register -H 'Content-Type: application/json' -d "$(json "$EMAIL" "$PASSWORD")")" \
  "The account is created and the user is logged in right away."
if grep -qi 'password' "$TMP/body"; then RESULT="present"; else RESULT="absent"; fi
check "Response contains no password or hash" absent "$RESULT" \
  "The API never sends the password or its hash back: $(head -c 120 "$TMP/body")…"

section "2. Password storage" "hashed and salted with BCrypt, never plain text"
HASH="$(sql "select password_hash from users where email = '$EMAIL'")"
info "database row: $EMAIL | $HASH"
check "Hash format is BCrypt cost 10 (\$2a\$10\$…, 60 characters)" yes \
  "$(echo "$HASH" | grep -Eq '^\$2[aby]\$10\$.{53}$' && echo yes || echo no)" \
  "\$2a\$ = BCrypt, 10 = cost (2^10 rounds), then 22 characters of salt and 31 of hash."
call user2 POST /register -H 'Content-Type: application/json' -d "$(json "$EMAIL2" "$PASSWORD")" > /dev/null
HASH2="$(sql "select password_hash from users where email = '$EMAIL2'")"
info "second user, SAME password: $HASH2"
check "Same password, two users -> two different hashes" different \
  "$([ -n "$HASH2" ] && [ "$HASH" != "$HASH2" ] && echo different || echo same)" \
  "Each password gets its own random salt: rainbow tables and hash comparison are useless."
NOT_BCRYPT="$(sql "select count(*) from users where password_hash not like '\\\$2_\\\$%'")"
check "Users in the database with a non-BCrypt password" 0 "$NOT_BCRYPT" \
  "Checked on ALL $(sql "select count(*) from users") users: no password is stored in plain text."

section "3. Login" "email + password checked against the hash"
check "POST /login with a wrong password -> 401" 401 \
  "$(call user POST /login -H 'Content-Type: application/json' -d "$(json "$EMAIL" wrongpassword)")" \
  "BCrypt hashes the attempt with the stored salt and compares: no match, access refused."
check "POST /login with the right password -> 200" 200 \
  "$(call user POST /login -H 'Content-Type: application/json' -d "$(json "$EMAIL" "$PASSWORD")")" \
  "Same hash: the server creates a session for the user."

echo
echo
echo "${BOLD}Show the stored passwords yourself:${RESET}"
echo "  docker exec -it facturx-postgres sh -c 'psql -U \"\$POSTGRES_USER\" -d facturx -c \"select email, password_hash from users;\"'"

echo
if [ $FAIL -eq 0 ]; then
  echo "${GREEN}${BOLD}$PASS/$PASS tests OK${RESET}"
else
  echo "${RED}${BOLD}$FAIL failed, $PASS passed${RESET}"
  exit 1
fi
