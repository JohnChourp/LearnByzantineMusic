#!/usr/bin/env bash
set -euo pipefail

usage() {
    cat <<'USAGE'
Usage:
  setup-release-signing.sh [--keystore-path <path>] [--alias <name>] [--set-github-secrets]

Examples:
  setup-release-signing.sh
  setup-release-signing.sh --set-github-secrets
  setup-release-signing.sh --keystore-path "$HOME/.android/learnbyzantine/release-upload-key.jks" --alias learnbyzantine_upload --set-github-secrets
USAGE
}

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

KEYSTORE_PATH="${HOME}/.android/learnbyzantine/release-upload-key.jks"
KEY_ALIAS="learnbyzantine_upload"
SET_GITHUB_SECRETS=0

while (($# > 0)); do
    case "$1" in
        --keystore-path)
            KEYSTORE_PATH="${2:-}"
            shift 2
            ;;
        --alias)
            KEY_ALIAS="${2:-}"
            shift 2
            ;;
        --set-github-secrets)
            SET_GITHUB_SECRETS=1
            shift
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            echo "ERROR: Άγνωστη παράμετρος: $1" >&2
            usage
            exit 1
            ;;
    esac
done

if ! command -v keytool >/dev/null 2>&1; then
    echo "ERROR: Δεν βρέθηκε το keytool. Εγκατάστησε JDK και ξανατρέξε." >&2
    exit 1
fi

KEYSTORE_DIR="$(dirname "$KEYSTORE_PATH")"
mkdir -p "$KEYSTORE_DIR"

generate_password() {
    if command -v openssl >/dev/null 2>&1; then
        openssl rand -hex 24
        return 0
    fi
    python3 - <<'PY'
import secrets
import string

alphabet = string.ascii_letters + string.digits
print("".join(secrets.choice(alphabet) for _ in range(48)))
PY
}

STORE_PASSWORD="$(generate_password)"
LOCAL_ENV_FILE="$KEYSTORE_DIR/release-signing.env"
# Για default PKCS12, το key password πρέπει να είναι ίδιο με το store password.
KEY_PASSWORD="$STORE_PASSWORD"

if [[ -f "$KEYSTORE_PATH" ]]; then
    if [[ ! -f "$LOCAL_ENV_FILE" ]]; then
        echo "ERROR: Υπάρχει keystore αλλά λείπει το $LOCAL_ENV_FILE για ασφαλή επαναχρήση credentials." >&2
        exit 1
    fi
    # shellcheck disable=SC1090
    source "$LOCAL_ENV_FILE"
    STORE_PASSWORD="${ANDROID_SIGNING_STORE_PASSWORD:-}"
    KEY_PASSWORD="${ANDROID_SIGNING_KEY_PASSWORD:-}"
    KEY_ALIAS="${ANDROID_SIGNING_KEY_ALIAS:-$KEY_ALIAS}"
    if [[ -z "$STORE_PASSWORD" || -z "$KEY_PASSWORD" || -z "$KEY_ALIAS" ]]; then
        echo "ERROR: Το $LOCAL_ENV_FILE είναι ελλιπές. Χρειάζονται store/key passwords και alias." >&2
        exit 1
    fi
    # Για PKCS12, key password == store password. Ευθυγράμμιση για αποφυγή signing αποτυχιών.
    KEY_PASSWORD="$STORE_PASSWORD"
    echo "[signing] Χρήση υπάρχοντος keystore: $KEYSTORE_PATH"
else
    echo "[signing] Δημιουργία νέου release keystore στο $KEYSTORE_PATH"
    keytool -genkeypair -v \
        -keystore "$KEYSTORE_PATH" \
        -storepass "$STORE_PASSWORD" \
        -keypass "$KEY_PASSWORD" \
        -alias "$KEY_ALIAS" \
        -keyalg RSA \
        -keysize 4096 \
        -validity 10000 \
        -dname "CN=LearnByzantineMusic,O=LearnByzantineMusic,OU=Android,L=Athens,ST=Attica,C=GR" \
        -noprompt >/dev/null

    cat > "$LOCAL_ENV_FILE" <<EOF
export ANDROID_SIGNING_STORE_FILE="$KEYSTORE_PATH"
export ANDROID_SIGNING_STORE_PASSWORD="$STORE_PASSWORD"
export ANDROID_SIGNING_KEY_ALIAS="$KEY_ALIAS"
export ANDROID_SIGNING_KEY_PASSWORD="$KEY_PASSWORD"
EOF
    chmod 600 "$LOCAL_ENV_FILE"
fi

# Διασφάλιση ότι το env file αντικατοπτρίζει τα ενεργά credentials (και σε reuse mode).
cat > "$LOCAL_ENV_FILE" <<EOF
export ANDROID_SIGNING_STORE_FILE="$KEYSTORE_PATH"
export ANDROID_SIGNING_STORE_PASSWORD="$STORE_PASSWORD"
export ANDROID_SIGNING_KEY_ALIAS="$KEY_ALIAS"
export ANDROID_SIGNING_KEY_PASSWORD="$KEY_PASSWORD"
EOF
chmod 600 "$LOCAL_ENV_FILE"

KEYSTORE_BASE64_PATH="$KEYSTORE_DIR/release-upload-key.base64"
base64 -w 0 "$KEYSTORE_PATH" > "$KEYSTORE_BASE64_PATH"

if [[ "$SET_GITHUB_SECRETS" -eq 1 ]]; then
    if ! command -v gh >/dev/null 2>&1; then
        echo "ERROR: Δεν βρέθηκε gh CLI για ρύθμιση GitHub Secrets." >&2
        exit 1
    fi
    if ! gh auth status >/dev/null 2>&1; then
        echo "ERROR: Δεν υπάρχει ενεργό gh auth session. Τρέξε 'gh auth login' και ξαναδοκίμασε." >&2
        exit 1
    fi
    echo "[signing] Ενημέρωση GitHub Actions Secrets..."
    gh secret set ANDROID_KEYSTORE_BASE64 < "$KEYSTORE_BASE64_PATH"
    gh secret set ANDROID_KEYSTORE_PASSWORD -b "$STORE_PASSWORD"
    gh secret set ANDROID_KEY_ALIAS -b "$KEY_ALIAS"
    gh secret set ANDROID_KEY_PASSWORD -b "$KEY_PASSWORD"
fi

echo "[signing] Ολοκληρώθηκε."
echo "[signing] Local env file: $LOCAL_ENV_FILE"
echo "[signing] Keystore base64 file: $KEYSTORE_BASE64_PATH"
echo "[signing] Για τοπικό release: source \"$LOCAL_ENV_FILE\""
echo "[signing] Κράτησε backup του keystore + env credentials σε ασφαλές offline σημείο."
