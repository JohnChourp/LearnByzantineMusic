#!/usr/bin/env bash
# Tests για το scripts/play-upload.sh με ψεύτικο curl: η σειρά των κλήσεων προς το Google Play, και ότι
# κάθε αποτυχία μετά το edits.insert σβήνει το edit χωρίς commit (στο Play δεν αλλάζει τίποτα).
# Το openssl είναι το πραγματικό, με κλειδί RSA που φτιάχνεται εδώ, για να ελεγχθεί και η υπογραφή του JWT.
#
# Τρέχει με: bash scripts/tests/test_play_upload.sh
set -uo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
UPLOAD="$HERE/../play-upload.sh"

tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
printf 'fake aab' > "$tmp/app.aab"
printf '«Μάντεψε τον ήχο», καθαρό εικονίδιο\n' > "$tmp/notes.txt"
openssl genpkey -algorithm RSA -out "$tmp/key.pem" >/dev/null 2>&1 \
    || openssl genrsa -out "$tmp/key.pem" 2048 >/dev/null 2>&1
openssl rsa -in "$tmp/key.pem" -pubout -out "$tmp/pub.pem" >/dev/null 2>&1
SECRET="$(jq -cn --rawfile key "$tmp/key.pem" '{type: "service_account", client_email: "play-upload@example.iam.gserviceaccount.com", private_key: $key}')"

# Ψεύτικο curl: γράφει κάθε βήμα στο $FAKE_LOG, απαντά με FAKE_STATUS_<βήμα> / FAKE_BODY_<βήμα>.
cat > "$tmp/curl" <<'EOF'
#!/usr/bin/env bash
printf '%s\n' "$@" >> "$FAKE_LOG.args"
out="" method="GET" url="" headers="" track_body="" has_body=0 max_time=""
while [[ $# -gt 0 ]]; do
    case "$1" in
        -o) out="$2"; shift 2 ;;
        -X) method="$2"; shift 2 ;;
        -H) [[ "$2" == @* ]] && headers+="$(cat "${2#@}")"; shift 2 ;;
        --data-binary) has_body=1; [[ "$2" == *track.json ]] && track_body="${2#@}"; shift 2 ;;
        --data-urlencode)
            has_body=1
            [[ "$2" == assertion@* ]] && cp "${2#assertion@}" "$FAKE_LOG.assertion"
            shift 2 ;;
        --data) has_body=1; shift 2 ;;
        --max-time) max_time="$2"; shift 2 ;;
        -w|--connect-timeout) shift 2 ;;
        -sS) shift ;;
        *) url="$1"; shift ;;
    esac
done
case "$method $url" in
    "POST https://oauth2.googleapis.com/token") step=token; body='{"access_token":"tok-123"}' ;;
    POST*/edits) step=insert; body='{"id":"edit-1"}' ;;
    POST*/edits/edit-1/bundles?uploadType=media) step=upload; body='{"versionCode":61}' ;;
    PUT*/edits/edit-1/tracks/*) step=tracks; body='{}' ;;
    POST*/edits/edit-1:commit) step=commit; body='{}' ;;
    DELETE*/edits/edit-1) step=delete; body='{}' ;;
    *) step="unexpected:$method:$url"; body='{}' ;;
esac
echo "$step" >> "$FAKE_LOG"
if [[ "$step" != token && "$headers" != *"Authorization: Bearer tok-123"* ]]; then
    echo "no-auth:$step" >> "$FAKE_LOG"
fi
# Η Google απαντά 411 σε POST/PUT χωρίς σώμα (χωρίς Content-Length).
if [[ "$method" == POST || "$method" == PUT ]] && [[ "$has_body" == 0 ]]; then
    echo "no-body:$step" >> "$FAKE_LOG"
fi
[[ -n "$max_time" ]] || echo "no-timeout:$step" >> "$FAKE_LOG"
[[ "$step" == upload && "$max_time" != 600 ]] && echo "short-upload-timeout" >> "$FAKE_LOG"
[[ -n "$track_body" ]] && cp "$track_body" "$FAKE_LOG.track.json"
# FAKE_FAIL_<βήμα>: το δίκτυο κόβεται, χωρίς απάντηση (όπως ένα curl που λήγει).
fail_var="FAKE_FAIL_$step"
if [[ -n "${!fail_var:-}" ]]; then
    printf '000'
    exit 28
fi
status_var="FAKE_STATUS_$step"
body_var="FAKE_BODY_$step"
printf '%s' "${!body_var:-$body}" > "$out"
printf '%s' "${!status_var:-200}"
EOF
chmod +x "$tmp/curl"

failures=0
n=0
# run <name> <want_exit> <want_text> <want_steps> [env assignments...]
run() {
    local name="$1" want_exit="$2" want_text="$3" want_steps="$4"
    shift 4
    n=$((n + 1))
    local log="$tmp/log$n" output status steps
    : > "$log"
    output="$(env CURL="$tmp/curl" FAKE_LOG="$log" PLAY_SERVICE_ACCOUNT_JSON="$SECRET" "$@" 2>&1)"
    status=$?
    steps="$(paste -sd' ' "$log")"
    if [[ "$status" != "$want_exit" ]]; then
        echo "FAIL $name: exit $status, expected $want_exit"
        printf '%s\n' "$output" | sed 's/^/    /'
        failures=$((failures + 1))
    elif [[ "$output" != *"$want_text"* ]]; then
        echo "FAIL $name: output lacks '$want_text'"
        printf '%s\n' "$output" | sed 's/^/    /'
        failures=$((failures + 1))
    elif [[ "$steps" != "$want_steps" ]]; then
        echo "FAIL $name: calls '$steps', expected '$want_steps'"
        failures=$((failures + 1))
    else
        echo "ok   $name"
    fi
}
check() {
    local name="$1"
    shift
    if "$@" >/dev/null; then echo "ok   $name"; else echo "FAIL $name"; failures=$((failures + 1)); fi
}

ARGS=(bash "$UPLOAD" --aab "$tmp/app.aab" --version-code 61)

run "happy path: token, insert, upload, track, commit" 0 "[play] OK" \
    "token insert upload tracks commit" "${ARGS[@]}" --notes-file "$tmp/notes.txt"
check "track body: alpha, versionCode 61, completed, el-GR notes" \
    jq -e '.track == "alpha" and .releases[0].versionCodes == ["61"] and .releases[0].status == "completed"
        and .releases[0].releaseNotes[0].language == "el-GR"
        and (.releases[0].releaseNotes[0].text | startswith("«Μάντεψε"))' "$tmp/log1.track.json"
check "the private key and the token never reach the command line" \
    bash -c "! grep -q -e 'PRIVATE KEY' -e 'tok-123' '$tmp/log1.args'"
# Η υπογραφή του JWT επαληθεύεται με το δημόσιο κλειδί: Google δέχεται μόνο RS256 από αυτό το κλειδί.
verify_jwt() {
    local jwt payload sig
    jwt="$(cat "$tmp/log1.assertion")"
    payload="${jwt%.*}"
    sig="${jwt##*.}"
    sig="$(printf '%s' "$sig" | tr '_-' '/+')"
    while (( ${#sig} % 4 )); do sig+="="; done
    printf '%s' "$sig" | openssl base64 -d -A > "$tmp/sig.bin"
    printf '%s' "$payload" | openssl dgst -sha256 -verify "$tmp/pub.pem" -signature "$tmp/sig.bin" >/dev/null
}
check "the JWT is signed RS256 with the service account key" verify_jwt
claims_of_jwt() {
    local c
    c="$(cut -d. -f2 "$tmp/log1.assertion" | tr '_-' '/+')"
    while (( ${#c} % 4 )); do c+="="; done
    printf '%s' "$c" | openssl base64 -d -A
}
check "JWT claims: issuer, androidpublisher scope, token audience" \
    bash -c "$(declare -f claims_of_jwt); tmp='$tmp'; claims_of_jwt | jq -e '.iss == \"play-upload@example.iam.gserviceaccount.com\"
        and .scope == \"https://www.googleapis.com/auth/androidpublisher\" and .aud == \"https://oauth2.googleapis.com/token\"
        and .exp - .iat == 3600' >/dev/null"

run "without notes the track has no releaseNotes" 0 "[play] OK" \
    "token insert upload tracks commit" "${ARGS[@]}"
check "track body without notes" jq -e '.releases[0] | has("releaseNotes") | not' "$tmp/log$n.track.json"

run "another track is passed through" 0 "track 'beta'" \
    "token insert upload tracks commit" "${ARGS[@]}" --track beta

run "dry run calls nothing" 0 "καμία κλήση" "" "${ARGS[@]}" --dry-run
run "dry run needs no secret" 0 "dry run" "" PLAY_SERVICE_ACCOUNT_JSON= "${ARGS[@]}" --dry-run

run "missing secret cannot pass" 2 "Λείπει το PLAY_SERVICE_ACCOUNT_JSON" "" PLAY_SERVICE_ACCOUNT_JSON= "${ARGS[@]}"
run "secret without private_key cannot pass" 2 "private_key" "" \
    PLAY_SERVICE_ACCOUNT_JSON='{"client_email":"x@example.com"}' "${ARGS[@]}"
run "secret that is not JSON cannot pass" 2 "client_email" "" PLAY_SERVICE_ACCOUNT_JSON='not json' "${ARGS[@]}"
run "missing AAB cannot pass" 2 "Δεν βρέθηκε το AAB" "" bash "$UPLOAD" --aab "$tmp/missing.aab" --version-code 61
run "missing version code cannot pass" 2 "version-code" "" bash "$UPLOAD" --aab "$tmp/app.aab"
run "bad track cannot pass" 2 "Μη έγκυρο --track" "" "${ARGS[@]}" --track 'alpha;rm'
run "an option without its value cannot pass" 2 "Το --track θέλει τιμή" "" "${ARGS[@]}" --track
run "unknown argument cannot pass" 2 "Άγνωστο όρισμα" "" "${ARGS[@]}" --production

run "refused token: no edit is opened" 1 "δεν έδωσε access token" "token" \
    FAKE_STATUS_token=400 FAKE_BODY_token='{"error":"invalid_grant","error_description":"Invalid JWT"}' "${ARGS[@]}"
run "refused edit: nothing to delete" 1 "HTTP 403" "token insert" \
    FAKE_STATUS_insert=403 FAKE_BODY_insert='{"error":{"message":"The caller does not have permission"}}' "${ARGS[@]}"
run "refused upload: the edit is deleted, no commit" 1 "σβήστηκε" "token insert upload delete" \
    FAKE_STATUS_upload=400 FAKE_BODY_upload='{"error":{"message":"APK specifies a version code that has already been used."}}' "${ARGS[@]}"
run "another versionCode read by Play: deleted, no commit" 1 "περιμέναμε 61" "token insert upload delete" \
    FAKE_BODY_upload='{"versionCode":60}' "${ARGS[@]}"
run "refused track: the edit is deleted, no commit" 1 "track 'alpha'" "token insert upload tracks delete" \
    FAKE_STATUS_tracks=403 "${ARGS[@]}"
run "refused commit: the edit is deleted" 1 "commit" "token insert upload tracks commit delete" \
    FAKE_STATUS_commit=400 "${ARGS[@]}"
run "a failed delete still says nothing was published" 1 "δεν δημοσιεύτηκε τίποτα" "token insert upload delete" \
    FAKE_STATUS_upload=500 FAKE_STATUS_delete=500 "${ARGS[@]}"
run "an error message belongs to its own call" 1 "edits.delete: HTTP 000
" "token insert upload delete" \
    FAKE_STATUS_upload=400 FAKE_BODY_upload='{"error":{"message":"version code already used"}}' \
    FAKE_FAIL_delete=1 "${ARGS[@]}"
run "a commit with no answer may have published: no delete, no false claim" 1 "ίσως δημοσιεύτηκε" \
    "token insert upload tracks commit" FAKE_FAIL_commit=1 "${ARGS[@]}"
check "that case never says nothing was published" \
    bash -c "! env CURL='$tmp/curl' FAKE_LOG='$tmp/commitfail' PLAY_SERVICE_ACCOUNT_JSON='$SECRET' FAKE_FAIL_commit=1 \
        bash '$UPLOAD' --aab '$tmp/app.aab' --version-code 61 2>&1 | grep -q 'δεν δημοσιεύτηκε τίποτα\\|δεν άλλαξε τίποτα'"

if [[ "$failures" -gt 0 ]]; then
    echo "$failures test(s) failed"
    exit 1
fi
echo "all play-upload tests passed"
