#!/usr/bin/env bash
# Ανεβάζει ένα AAB σε track του Google Play (κλειστή δοκιμή «alpha» από προεπιλογή), ClickUp 869fhmxpt.
#
# Μιλά απευθείας με το Google Play Developer API (androidpublisher v3), με service account:
#   1. token: JWT υπογεγραμμένο με το private_key του service account (openssl, RS256) → access token
#   2. edits.insert → 3. edits.bundles.upload → 4. edits.tracks.update (status completed) → 5. edits.commit
# Αν κάτι αποτύχει μετά το 2, το edit σβήνεται και στο Play δεν αλλάζει τίποτα.
#
# Το service account χρειάζεται στο Play Console μόνο αυτή την εφαρμογή και το δικαίωμα
# «Release to testing tracks»: με αυτό δεν μπορεί να βγάλει έκδοση σε όλους τους χρήστες.
#
# Χρήση:
#   play-upload.sh --aab <file.aab> --version-code <N> [--track alpha] [--notes-file <file>] [--dry-run]
#     --version-code   το versionCode που περιμένουμε· αν το Play διαβάσει άλλο, τίποτα δεν δημοσιεύεται
#     --notes-file     «Τι νέο υπάρχει» στα el-GR (κόβεται στους 500 χαρακτήρες του Play)
#     --dry-run        ελέγχει τα ορίσματα και τυπώνει τι θα γινόταν· καμία κλήση στο δίκτυο
# Το κλειδί έρχεται ΜΟΝΟ από το env PLAY_SERVICE_ACCOUNT_JSON (το JSON του service account). Δεν τυπώνεται ποτέ.
# Exit 0 όταν το commit πέρασε, 1 όταν το Play αρνήθηκε, 2 όταν ο έλεγχος δεν μπορεί να τρέξει.
# Τα CURL / OPENSSL αντικαθιστούν τα εργαλεία (για το scripts/tests/test_play_upload.sh).
set -euo pipefail

PACKAGE="com.johnchourp.learnbyzantinemusic.app"
SCOPE="https://www.googleapis.com/auth/androidpublisher"
TOKEN_URL="https://oauth2.googleapis.com/token"
API="https://androidpublisher.googleapis.com/androidpublisher/v3/applications/$PACKAGE"
UPLOAD_API="https://androidpublisher.googleapis.com/upload/androidpublisher/v3/applications/$PACKAGE"
CURL="${CURL:-curl}"
OPENSSL="${OPENSSL:-openssl}"

AAB=""
VERSION_CODE=""
TRACK="alpha"
NOTES_FILE=""
DRY_RUN=0

usage() {
    echo "Usage: play-upload.sh --aab <file.aab> --version-code <N> [--track alpha] [--notes-file <file>] [--dry-run]" >&2
}

fail_setup() {
    echo "ERROR: $*" >&2
    exit 2
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --aab|--version-code|--track|--notes-file)
            [[ $# -ge 2 ]] || { usage; fail_setup "Το $1 θέλει τιμή."; } ;;
    esac
    case "$1" in
        --aab) AAB="$2"; shift 2 ;;
        --version-code) VERSION_CODE="$2"; shift 2 ;;
        --track) TRACK="$2"; shift 2 ;;
        --notes-file) NOTES_FILE="$2"; shift 2 ;;
        --dry-run) DRY_RUN=1; shift ;;
        -h|--help) usage; exit 0 ;;
        *) usage; fail_setup "Άγνωστο όρισμα: $1" ;;
    esac
done

[[ -n "$AAB" && -f "$AAB" ]] || fail_setup "Δεν βρέθηκε το AAB: '${AAB}'."
[[ "$VERSION_CODE" =~ ^[1-9][0-9]*$ ]] || fail_setup "Μη έγκυρο --version-code: '${VERSION_CODE}'."
[[ "$TRACK" =~ ^[a-z][a-z0-9_:-]*$ ]] || fail_setup "Μη έγκυρο --track: '${TRACK}'."
if [[ -n "$NOTES_FILE" && ! -f "$NOTES_FILE" ]]; then
    fail_setup "Δεν βρέθηκε το --notes-file: '${NOTES_FILE}'."
fi
command -v jq >/dev/null 2>&1 || fail_setup "Λείπει το jq."

NOTES=""
if [[ -n "$NOTES_FILE" ]]; then
    NOTES="$(head -c 2000 "$NOTES_FILE" | jq -Rrs '.[0:500] | sub("\\s+$"; "")')"
fi

if [[ "$DRY_RUN" == 1 ]]; then
    echo "[play] dry run: θα ανέβαινε το $AAB (versionCode $VERSION_CODE) στο track '$TRACK' του $PACKAGE."
    if [[ -n "$NOTES" ]]; then
        echo "[play] dry run: σημειώσεις el-GR: $NOTES"
    fi
    echo "[play] dry run: καμία κλήση στο Google Play."
    exit 0
fi

[[ -n "${PLAY_SERVICE_ACCOUNT_JSON:-}" ]] || fail_setup "Λείπει το PLAY_SERVICE_ACCOUNT_JSON (το JSON του service account)."
command -v "$CURL" >/dev/null 2>&1 || fail_setup "Λείπει το curl."
command -v "$OPENSSL" >/dev/null 2>&1 || fail_setup "Λείπει το openssl."

CLIENT_EMAIL="$(printf '%s' "$PLAY_SERVICE_ACCOUNT_JSON" | jq -er '.client_email' 2>/dev/null)" \
    || fail_setup "Το PLAY_SERVICE_ACCOUNT_JSON δεν έχει client_email."

work="$(mktemp -d)"
EDIT_ID=""
cleanup() {
    rm -rf "$work"
}
trap cleanup EXIT
umask 077

printf '%s' "$PLAY_SERVICE_ACCOUNT_JSON" | jq -er '.private_key' > "$work/key.pem" 2>/dev/null \
    || fail_setup "Το PLAY_SERVICE_ACCOUNT_JSON δεν έχει private_key."

b64url() {
    "$OPENSSL" base64 -A | tr '+/' '-_' | tr -d '='
}

# api <name> <method> <url> [curl args...]: γράφει την απάντηση στο $work/resp, επιστρέφει 0 μόνο σε 2xx.
# Κάθε κλήση έχει όριο χρόνου (MAX_TIME, 120 s· το upload του AAB 600 s), ώστε ένα κολλημένο δίκτυο να μην κρεμάει το job.
api() {
    local name="$1" method="$2" url="$3" status
    shift 3
    rm -f "$work/resp"
    status="$("$CURL" -sS --connect-timeout 20 --max-time "${MAX_TIME:-120}" -X "$method" -o "$work/resp" \
        -w '%{http_code}' "$@" "$url")" || status="000"
    if [[ ! "$status" =~ ^2[0-9][0-9]$ ]]; then
        local message
        message="$(jq -r '.error.message // .error_description // .error // empty' "$work/resp" 2>/dev/null || true)"
        echo "ERROR: $name: HTTP $status${message:+ — $message}" >&2
        return 1
    fi
}

# Από εδώ και πέρα κάθε αποτυχία σβήνει το edit, ώστε να μη μείνει τίποτα μισό στο Play.
abort() {
    echo "ERROR: $*" >&2
    if [[ -n "$EDIT_ID" ]]; then
        if api "edits.delete" DELETE "$API/edits/$EDIT_ID" -H @"$work/auth"; then
            echo "[play] Το edit $EDIT_ID σβήστηκε· στο Google Play δεν άλλαξε τίποτα." >&2
        else
            echo "ERROR: Δεν σβήστηκε το edit ${EDIT_ID}· δεν έγινε commit, άρα δεν δημοσιεύτηκε τίποτα." >&2
        fi
    fi
    exit 1
}

now="$(date +%s)"
header='{"alg":"RS256","typ":"JWT"}'
claims="$(jq -cn --arg iss "$CLIENT_EMAIL" --arg scope "$SCOPE" --arg aud "$TOKEN_URL" \
    --argjson iat "$now" --argjson exp "$((now + 3600))" \
    '{iss: $iss, scope: $scope, aud: $aud, iat: $iat, exp: $exp}')"
unsigned="$(printf '%s' "$header" | b64url).$(printf '%s' "$claims" | b64url)"
signature="$(printf '%s' "$unsigned" | "$OPENSSL" dgst -sha256 -sign "$work/key.pem" -binary | b64url)" \
    || fail_setup "Δεν υπογράφτηκε το JWT με το private_key του service account."
rm -f "$work/key.pem"
printf '%s' "$unsigned.$signature" > "$work/assertion"

api "token" POST "$TOKEN_URL" \
    --data-urlencode "grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer" \
    --data-urlencode "assertion@$work/assertion" \
    || { echo "ERROR: Η Google δεν έδωσε access token στο $CLIENT_EMAIL." >&2; exit 1; }
rm -f "$work/assertion"
ACCESS_TOKEN="$(jq -er '.access_token' "$work/resp" 2>/dev/null)" \
    || { echo "ERROR: Η απάντηση του token δεν έχει access_token." >&2; exit 1; }
if [[ "${GITHUB_ACTIONS:-}" == "true" ]]; then
    echo "::add-mask::$ACCESS_TOKEN"
fi
# Ο header γράφεται σε αρχείο, ώστε το token να μη φαίνεται στη γραμμή εντολών (ps).
printf 'Authorization: Bearer %s\n' "$ACCESS_TOKEN" > "$work/auth"
echo "[play] Access token για $CLIENT_EMAIL."

api "edits.insert" POST "$API/edits" -H @"$work/auth" -H "Content-Type: application/json" --data '{}' \
    || exit 1
EDIT_ID="$(jq -er '.id' "$work/resp" 2>/dev/null)" || { echo "ERROR: Το edits.insert δεν έδωσε id." >&2; exit 1; }
echo "[play] Edit $EDIT_ID."

MAX_TIME=600 api "edits.bundles.upload" POST "$UPLOAD_API/edits/$EDIT_ID/bundles?uploadType=media" \
    -H @"$work/auth" -H "Content-Type: application/octet-stream" --data-binary @"$AAB" \
    || abort "Το Google Play δεν δέχτηκε το AAB."
UPLOADED_CODE="$(jq -r '.versionCode // empty' "$work/resp")"
if [[ "$UPLOADED_CODE" != "$VERSION_CODE" ]]; then
    abort "Το Play διάβασε versionCode '${UPLOADED_CODE}', ενώ περιμέναμε $VERSION_CODE."
fi
echo "[play] Ανέβηκε το AAB (versionCode $UPLOADED_CODE)."

jq -n --arg track "$TRACK" --arg code "$VERSION_CODE" --arg notes "$NOTES" '{
    track: $track,
    releases: [{
        versionCodes: [$code],
        status: "completed"
    } + (if $notes == "" then {} else {releaseNotes: [{language: "el-GR", text: $notes}]} end)]
}' > "$work/track.json"
api "edits.tracks.update" PUT "$API/edits/$EDIT_ID/tracks/$TRACK" \
    -H @"$work/auth" -H "Content-Type: application/json" --data-binary @"$work/track.json" \
    || abort "Το Google Play δεν δέχτηκε την έκδοση στο track '$TRACK'."
echo "[play] Track '$TRACK' → versionCode $VERSION_CODE."

# Κενό σώμα: χωρίς αυτό το curl δεν στέλνει Content-Length και η Google απαντά 411 σε κάθε POST.
api "edits.commit" POST "$API/edits/$EDIT_ID:commit" -H @"$work/auth" --data '' \
    || abort "Το Google Play δεν δέχτηκε το commit του edit."
EDIT_ID=""
echo "[play] OK: το versionCode $VERSION_CODE δημοσιεύτηκε στο track '$TRACK' του $PACKAGE."
