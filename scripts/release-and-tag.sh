#!/usr/bin/env bash
set -euo pipefail

usage() {
    cat <<'USAGE'
Usage:
  release-and-tag.sh [--bump patch|minor|major] [--version X.Y.Z] [--code N] [--no-push]

Examples:
  release-and-tag.sh --bump patch
  release-and-tag.sh --bump minor
  release-and-tag.sh --version 1.2.0
USAGE
}

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
BUMP_SCRIPT="$SCRIPT_DIR/bump-version.sh"

if [[ ! -x "$BUMP_SCRIPT" ]]; then
    echo "ERROR: Δεν βρέθηκε εκτελέσιμο bump script στο $BUMP_SCRIPT" >&2
    exit 1
fi

BUMP_ARGS=()
PUSH_CHANGES=1

while (($# > 0)); do
    case "$1" in
        --bump|--version|--code)
            BUMP_ARGS+=("$1" "${2:-}")
            shift 2
            ;;
        --no-push)
            PUSH_CHANGES=0
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

cd "$ROOT_DIR"

if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    echo "ERROR: Δεν βρίσκομαι σε git repository." >&2
    exit 1
fi

if [[ -n "$(git status --porcelain)" ]]; then
    echo "ERROR: Υπάρχουν μη αποθηκευμένες αλλαγές (tracked ή untracked). Κάνε commit/stash πρώτα." >&2
    exit 1
fi

mapfile -t bump_output < <("$BUMP_SCRIPT" "${BUMP_ARGS[@]}")
printf '%s\n' "${bump_output[@]}"

NEW_VERSION_NAME=""
NEW_VERSION_CODE=""
for line in "${bump_output[@]}"; do
    case "$line" in
        NEW_VERSION_NAME=*) NEW_VERSION_NAME="${line#NEW_VERSION_NAME=}" ;;
        NEW_VERSION_CODE=*) NEW_VERSION_CODE="${line#NEW_VERSION_CODE=}" ;;
    esac
done

if [[ -z "$NEW_VERSION_NAME" || -z "$NEW_VERSION_CODE" ]]; then
    echo "ERROR: Δεν μπόρεσα να διαβάσω τη νέα έκδοση από το bump script." >&2
    exit 1
fi

TAG="v${NEW_VERSION_NAME}"
if git rev-parse "$TAG" >/dev/null 2>&1; then
    echo "ERROR: Το tag $TAG υπάρχει ήδη." >&2
    exit 1
fi
if git ls-remote --tags origin "$TAG" | grep -q "$TAG"; then
    echo "ERROR: Το tag $TAG υπάρχει ήδη στο origin." >&2
    exit 1
fi

echo "[release] Build release artifacts για $TAG"
./gradlew clean assembleRelease bundleRelease

RELEASE_DIR="$ROOT_DIR/build-artifacts/release/$TAG"
mkdir -p "$RELEASE_DIR"

APK_PATH="$(find "$ROOT_DIR/app/build/outputs/apk/release" -maxdepth 1 -type f -name '*.apk' | sort | tail -n1 || true)"
AAB_PATH="$(find "$ROOT_DIR/app/build/outputs/bundle/release" -maxdepth 1 -type f -name '*.aab' | sort | tail -n1 || true)"
MAPPING_PATH="$(find "$ROOT_DIR/app/build/outputs/mapping/release" -maxdepth 1 -type f -name 'mapping.txt' | sort | tail -n1 || true)"

if [[ -z "$APK_PATH" || -z "$AAB_PATH" ]]; then
    echo "ERROR: Δεν βρέθηκαν APK/AAB release artifacts." >&2
    exit 1
fi

cp "$APK_PATH" "$RELEASE_DIR/"
cp "$AAB_PATH" "$RELEASE_DIR/"
if [[ -n "$MAPPING_PATH" ]]; then
    cp "$MAPPING_PATH" "$RELEASE_DIR/"
fi

ZIP_PATH="$RELEASE_DIR/LearnByzantineMusic-$TAG-packages.zip"
archive_files=()
while IFS= read -r file; do
    archive_files+=("$(basename "$file")")
done < <(find "$RELEASE_DIR" -maxdepth 1 -type f \( -name '*.apk' -o -name '*.aab' -o -name 'mapping.txt' \) | sort)

if [[ "${#archive_files[@]}" -eq 0 ]]; then
    echo "ERROR: Δεν υπάρχουν αρχεία για zip package." >&2
    exit 1
fi

(
    cd "$RELEASE_DIR"
    zip -q "$(basename "$ZIP_PATH")" "${archive_files[@]}"
)

(
    cd "$RELEASE_DIR"
    sha256sum ./* > SHA256SUMS.txt
)


git add app/build.gradle.kts
git commit -m "release: $TAG"
git tag -a "$TAG" -m "Release $TAG"

BRANCH="$(git branch --show-current)"
if [[ "$PUSH_CHANGES" -eq 1 ]]; then
    git push origin "$BRANCH"
    git push origin "$TAG"
    echo "[release] Έγινε push branch=$BRANCH και tag=$TAG"
    echo "[release] Το GitHub Action θα δημιουργήσει αυτόματα release με assets."
else
    echo "[release] Δημιουργήθηκαν τοπικά commit+tag."
    echo "[release] Τρέξε: git push origin $BRANCH && git push origin $TAG"
fi

echo "[release] Έτοιμο. Έκδοση: $NEW_VERSION_NAME (code $NEW_VERSION_CODE)"
echo "[release] Local package folder: $RELEASE_DIR"
