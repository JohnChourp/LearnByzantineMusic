#!/usr/bin/env bash
set -euo pipefail

usage() {
    cat <<'USAGE'
Usage:
  learn-byzantine-android-release [--bump patch|minor|major] [--version X.Y.Z] [--code N] [--no-push] [--skip-gh-release]
USAGE
}

SCRIPT_DIR="$(cd -- "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_PATH="$(cd -- "$SCRIPT_DIR/../../../.." && pwd)"
ARGS=()
SKIP_GH_RELEASE=0

while (($# > 0)); do
    case "$1" in
        --bump|--version|--code)
            if [[ -z "${2:-}" ]]; then
                echo "Missing value for $1" >&2
                usage
                exit 1
            fi
            ARGS+=("$1" "$2")
            shift 2
            ;;
        --no-push)
            ARGS+=("$1")
            shift
            ;;
        --skip-gh-release)
            ARGS+=("$1")
            SKIP_GH_RELEASE=1
            shift
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            echo "Unknown option: $1" >&2
            usage
            exit 1
            ;;
    esac
done

if [[ ! -x "$PROJECT_PATH/scripts/release-and-tag.sh" ]]; then
    echo "ERROR: release script not found at $PROJECT_PATH/scripts/release-and-tag.sh" >&2
    exit 1
fi

pick_release_bash() {
    local selected=""
    local candidate major
    local candidates=(
        "${HOME}/.homebrew/bin/bash"
        "/opt/homebrew/bin/bash"
        "/usr/local/bin/bash"
        "$(command -v bash || true)"
    )

    for candidate in "${candidates[@]}"; do
        if [[ -z "$candidate" || ! -x "$candidate" ]]; then
            continue
        fi
        major="$("$candidate" -lc 'echo "${BASH_VERSINFO[0]:-0}"' 2>/dev/null || echo 0)"
        if [[ "$major" =~ ^[0-9]+$ ]] && (( major >= 4 )); then
            selected="$candidate"
            break
        fi
    done

    if [[ -z "$selected" ]]; then
        echo "ERROR: GNU bash >= 4 is required for scripts/release-and-tag.sh." >&2
        echo "On macOS, run: brew install bash" >&2
        exit 1
    fi
    echo "$selected"
}

ensure_release_signing_env() {
    local env_file="$HOME/.android/learnbyzantine/release-signing.env"
    local setup_script="$PROJECT_PATH/scripts/setup-release-signing.sh"
    local keystore_path base64_path

    if [[ -f "$env_file" ]]; then
        # shellcheck disable=SC1090
        source "$env_file"
    fi

    if [[ -z "${ANDROID_SIGNING_STORE_FILE:-}" || -z "${ANDROID_SIGNING_STORE_PASSWORD:-}" || -z "${ANDROID_SIGNING_KEY_ALIAS:-}" || -z "${ANDROID_SIGNING_KEY_PASSWORD:-}" ]]; then
        if [[ -x "$setup_script" ]]; then
            set +e
            "$setup_script"
            setup_status=$?
            set -e
            if [[ $setup_status -ne 0 ]]; then
                echo "[release-skill] setup-release-signing.sh exited with $setup_status; checking whether env file was still created." >&2
            fi
        fi
        if [[ -f "$env_file" ]]; then
            # shellcheck disable=SC1090
            source "$env_file"
        fi
    fi

    if [[ -z "${ANDROID_SIGNING_STORE_FILE:-}" || -z "${ANDROID_SIGNING_STORE_PASSWORD:-}" || -z "${ANDROID_SIGNING_KEY_ALIAS:-}" || -z "${ANDROID_SIGNING_KEY_PASSWORD:-}" ]]; then
        echo "ERROR: Valid Android release signing env vars are missing." >&2
        echo "Run: $PROJECT_PATH/scripts/setup-release-signing.sh" >&2
        echo "Then: source \"$env_file\"" >&2
        exit 1
    fi

    if [[ ! -f "${ANDROID_SIGNING_STORE_FILE}" ]]; then
        echo "ERROR: Keystore not found at ANDROID_SIGNING_STORE_FILE=${ANDROID_SIGNING_STORE_FILE}" >&2
        exit 1
    fi

    keystore_path="${ANDROID_SIGNING_STORE_FILE}"
    base64_path="$HOME/.android/learnbyzantine/release-upload-key.base64"
    if [[ ! -f "$base64_path" ]]; then
        mkdir -p "$(dirname "$base64_path")"
        base64 < "$keystore_path" > "$base64_path"
    fi
}

ensure_gh_or_fallback_skip() {
    if (( SKIP_GH_RELEASE == 1 )); then
        return
    fi

    if ! command -v gh >/dev/null 2>&1; then
        echo "[release-skill] gh CLI not found. Adding --skip-gh-release; tag push remains the fallback." >&2
        ARGS+=("--skip-gh-release")
        return
    fi

    if ! gh auth status >/dev/null 2>&1; then
        echo "[release-skill] gh is not authenticated. Adding --skip-gh-release; tag push remains the fallback." >&2
        ARGS+=("--skip-gh-release")
    fi
}

cd "$PROJECT_PATH"
RELEASE_BASH="$(pick_release_bash)"
ensure_release_signing_env
ensure_gh_or_fallback_skip
"$RELEASE_BASH" ./scripts/release-and-tag.sh "${ARGS[@]}"
