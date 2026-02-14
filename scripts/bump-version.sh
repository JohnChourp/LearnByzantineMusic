#!/usr/bin/env bash
set -euo pipefail

usage() {
    cat <<'USAGE'
Usage:
  bump-version.sh [--bump patch|minor|major] [--version X.Y.Z] [--code N]

Examples:
  bump-version.sh --bump patch
  bump-version.sh --bump minor
  bump-version.sh --version 1.4.0
USAGE
}

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
BUILD_FILE="$ROOT_DIR/app/build.gradle.kts"

if [[ ! -f "$BUILD_FILE" ]]; then
    echo "ERROR: Δεν βρέθηκε το $BUILD_FILE" >&2
    exit 1
fi

BUMP_KIND="patch"
EXPLICIT_VERSION=""
EXPLICIT_CODE=""
BUMP_FLAG_SET=0

while (($# > 0)); do
    case "$1" in
        --bump)
            BUMP_KIND="${2:-}"
            BUMP_FLAG_SET=1
            shift 2
            ;;
        --version)
            EXPLICIT_VERSION="${2:-}"
            shift 2
            ;;
        --code)
            EXPLICIT_CODE="${2:-}"
            shift 2
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

if [[ -n "$EXPLICIT_VERSION" && "$BUMP_FLAG_SET" -eq 1 ]]; then
    echo "ERROR: Χρησιμοποίησε είτε --version είτε --bump (όχι και τα δύο)." >&2
    exit 1
fi

if [[ -z "$EXPLICIT_VERSION" ]]; then
    case "$BUMP_KIND" in
        patch|minor|major) ;;
        *)
            echo "ERROR: Μη έγκυρο --bump: $BUMP_KIND (επιτρεπτά: patch|minor|major)." >&2
            exit 1
            ;;
    esac
fi

current_version_name="$(sed -nE 's/^[[:space:]]*versionName[[:space:]]*=[[:space:]]*"([^"]+)".*/\1/p' "$BUILD_FILE" | head -n1)"
current_version_code="$(sed -nE 's/^[[:space:]]*versionCode[[:space:]]*=[[:space:]]*([0-9]+).*/\1/p' "$BUILD_FILE" | head -n1)"

if [[ -z "$current_version_name" || -z "$current_version_code" ]]; then
    echo "ERROR: Δεν μπόρεσα να εντοπίσω versionName/versionCode στο $BUILD_FILE" >&2
    exit 1
fi

if [[ ! "$current_version_name" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    echo "ERROR: Το τρέχον versionName ($current_version_name) δεν είναι μορφής X.Y.Z" >&2
    exit 1
fi

IFS='.' read -r cur_major cur_minor cur_patch <<< "$current_version_name"

if [[ -n "$EXPLICIT_VERSION" ]]; then
    if [[ ! "$EXPLICIT_VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
        echo "ERROR: Το --version πρέπει να είναι μορφής X.Y.Z" >&2
        exit 1
    fi
    new_version_name="$EXPLICIT_VERSION"
else
    case "$BUMP_KIND" in
        patch)
            new_version_name="$cur_major.$cur_minor.$((cur_patch + 1))"
            ;;
        minor)
            new_version_name="$cur_major.$((cur_minor + 1)).0"
            ;;
        major)
            new_version_name="$((cur_major + 1)).0.0"
            ;;
    esac
fi

if [[ -n "$EXPLICIT_CODE" ]]; then
    if [[ ! "$EXPLICIT_CODE" =~ ^[0-9]+$ ]]; then
        echo "ERROR: Το --code πρέπει να είναι ακέραιος." >&2
        exit 1
    fi
    new_version_code="$EXPLICIT_CODE"
else
    new_version_code="$((current_version_code + 1))"
fi

NEW_VERSION_CODE="$new_version_code" NEW_VERSION_NAME="$new_version_name" \
    perl -0pi -e 's/(versionCode\s*=\s*)\d+/$1 . $ENV{NEW_VERSION_CODE}/e; s/(versionName\s*=\s*")[^"]+(")/$1 . $ENV{NEW_VERSION_NAME} . $2/e' "$BUILD_FILE"

echo "OLD_VERSION_NAME=$current_version_name"
echo "OLD_VERSION_CODE=$current_version_code"
echo "NEW_VERSION_NAME=$new_version_name"
echo "NEW_VERSION_CODE=$new_version_code"

if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
    {
        echo "old_version_name=$current_version_name"
        echo "old_version_code=$current_version_code"
        echo "version_name=$new_version_name"
        echo "version_code=$new_version_code"
    } >> "$GITHUB_OUTPUT"
fi
