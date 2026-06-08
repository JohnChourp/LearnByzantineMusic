#!/usr/bin/env bash
set -euo pipefail

usage() {
    cat <<'EOF'
Usage:
  learn-byzantine-android-debug-run [options]

Options:
  --serial <id>          ADB serial for the target connected device
  --archive-dir <path>   Output directory for archived APK copies
  --clean                Run clean before installDebug
  --skip-launch          Do not launch the app after install
  -h, --help             Show help
EOF
}

log() {
    printf '[learn-byzantine-debug] %s\n' "$*"
}

SCRIPT_DIR="$(cd -- "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd -- "$SCRIPT_DIR/../../../.." && pwd)"
PACKAGE_NAME="com.johnchourp.learnbyzantinemusic"
SERIAL="${ADB_SERIAL:-}"
ARCHIVE_DIR=""
CLEAN_BUILD=0
SKIP_LAUNCH=0
ADB_BIN=""

while (($# > 0)); do
    case "$1" in
        --serial)
            SERIAL="${2:-}"
            shift 2
            ;;
        --archive-dir)
            ARCHIVE_DIR="${2:-}"
            shift 2
            ;;
        --clean)
            CLEAN_BUILD=1
            shift
            ;;
        --skip-launch)
            SKIP_LAUNCH=1
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

if [[ ! -x "$PROJECT_DIR/gradlew" ]]; then
    echo "gradlew not found or not executable in $PROJECT_DIR" >&2
    exit 1
fi

ensure_adb() {
    if command -v adb >/dev/null 2>&1; then
        ADB_BIN="$(command -v adb)"
        return 0
    fi

    local candidates=()
    if [[ -n "${ANDROID_HOME:-}" ]]; then
        candidates+=("${ANDROID_HOME}/platform-tools/adb")
    fi
    if [[ -n "${ANDROID_SDK_ROOT:-}" ]]; then
        candidates+=("${ANDROID_SDK_ROOT}/platform-tools/adb")
    fi
    candidates+=(
        "${HOME}/Library/Android/sdk/platform-tools/adb"
        "/Users/${USER}/Library/Android/sdk/platform-tools/adb"
    )

    local candidate
    for candidate in "${candidates[@]}"; do
        if [[ -x "$candidate" ]]; then
            export PATH="$(dirname "$candidate"):$PATH"
            ADB_BIN="$candidate"
            log "Using adb from fallback path: $candidate"
            return 0
        fi
    done
    return 1
}

if ! ensure_adb; then
    echo "adb not found in PATH or common Android SDK locations." >&2
    echo "Try: export PATH=\"\$HOME/Library/Android/sdk/platform-tools:\$PATH\"" >&2
    exit 1
fi

"$ADB_BIN" start-server >/dev/null 2>&1 || true

ADB_CMD=("$ADB_BIN")
if [[ -n "$SERIAL" ]]; then
    ADB_CMD+=( -s "$SERIAL" )
    if [[ "$("${ADB_CMD[@]}" get-state 2>/dev/null || true)" != "device" ]]; then
        echo "Device '$SERIAL' is not connected and ready." >&2
        "$ADB_BIN" devices -l >&2 || true
        exit 1
    fi
else
    connected=()
    while IFS= read -r dev_serial; do
        [[ -n "$dev_serial" ]] && connected+=("$dev_serial")
    done < <("$ADB_BIN" devices | awk 'NR>1 && $2=="device" {print $1}')

    if [[ ${#connected[@]} -eq 0 ]]; then
        echo "No connected Android adb device found. Connect the phone and enable USB debugging." >&2
        "$ADB_BIN" devices -l >&2 || true
        exit 1
    fi

    if [[ ${#connected[@]} -gt 1 ]]; then
        echo "Multiple connected Android adb devices found. Rerun with --serial <id>." >&2
        "$ADB_BIN" devices -l >&2 || true
        exit 1
    fi

    SERIAL="${connected[0]}"
    ADB_CMD+=( -s "$SERIAL" )
fi

if [[ -z "$ARCHIVE_DIR" ]]; then
    ARCHIVE_DIR="$PROJECT_DIR/build-artifacts/apk"
elif [[ "$ARCHIVE_DIR" != /* ]]; then
    ARCHIVE_DIR="$PROJECT_DIR/$ARCHIVE_DIR"
fi

log "Project: $PROJECT_DIR"
log "Package: $PACKAGE_NAME"
log "Device: $SERIAL"

run_gradle_install() {
    local tmp status
    tmp="$(mktemp)"
    set +e
    (
        cd "$PROJECT_DIR"
        ./gradlew "$@" 2>&1
    ) | tee "$tmp"
    status=${PIPESTATUS[0]}
    set -e
    if [[ $status -ne 0 ]]; then
        if grep -q "INSTALL_FAILED_UPDATE_INCOMPATIBLE" "$tmp"; then
            rm -f "$tmp"
            return 2
        fi
        rm -f "$tmp"
        return "$status"
    fi
    rm -f "$tmp"
    return 0
}

GRADLE_ARGS=()
if [[ $CLEAN_BUILD -eq 1 ]]; then
    GRADLE_ARGS+=(clean)
fi
GRADLE_ARGS+=(installDebug --warning-mode all)

set +e
run_gradle_install "${GRADLE_ARGS[@]}"
install_status=$?
set -e

if [[ $install_status -eq 2 ]]; then
    log "Detected INSTALL_FAILED_UPDATE_INCOMPATIBLE. Uninstalling old app and retrying."
    "${ADB_CMD[@]}" uninstall "$PACKAGE_NAME" >/dev/null 2>&1 || true
    (
        cd "$PROJECT_DIR"
        ./gradlew installDebug --warning-mode all
    )
elif [[ $install_status -ne 0 ]]; then
    exit "$install_status"
fi

APK_SOURCE="$(find "$PROJECT_DIR/app/build/outputs/apk/debug" -type f -name '*.apk' | sort | tail -n 1 || true)"
if [[ -z "$APK_SOURCE" ]]; then
    echo "Could not find generated debug APK in app/build/outputs/apk/debug." >&2
    exit 1
fi

mkdir -p "$ARCHIVE_DIR"
timestamp="$(date +%Y%m%d_%H%M%S)"
apk_base="$(basename "${APK_SOURCE%.apk}")"
APK_ARCHIVE_PATH="$ARCHIVE_DIR/${apk_base}-${timestamp}.apk"
cp "$APK_SOURCE" "$APK_ARCHIVE_PATH"
log "Archived APK: $APK_ARCHIVE_PATH"

if [[ $SKIP_LAUNCH -eq 0 ]]; then
    set +e
    launch_output="$("${ADB_CMD[@]}" shell monkey -p "$PACKAGE_NAME" -c android.intent.category.LAUNCHER 1 2>&1)"
    launch_status=$?
    set -e
    printf '%s\n' "$launch_output"
    if [[ $launch_status -ne 0 ]]; then
        echo "Launch intent failed for package $PACKAGE_NAME." >&2
        exit "$launch_status"
    fi
    log "Launch intent sent for package: $PACKAGE_NAME"
else
    log "Launch skipped (--skip-launch)."
fi

log "Done."
