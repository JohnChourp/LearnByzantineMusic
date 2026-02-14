#!/usr/bin/env bash
set -euo pipefail

usage() {
    cat <<'USAGE'
Usage:
  release-and-tag.sh [--bump patch|minor|major] [--version X.Y.Z] [--code N] [--no-push] [--skip-gh-release]

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
PUBLISH_GH_RELEASE=1

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
        --skip-gh-release)
            PUBLISH_GH_RELEASE=0
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

ensure_gh_release() {
    local tag="$1"
    local target_branch="$2"
    shift 2
    local assets=("$@")

    if [[ "$PUBLISH_GH_RELEASE" -eq 0 ]]; then
        echo "[release] Παράλειψη direct GitHub Release (--skip-gh-release)."
        return 0
    fi

    if ! command -v gh >/dev/null 2>&1; then
        echo "ERROR: Το gh CLI δεν είναι διαθέσιμο. Εγκατάστησέ το ή τρέξε με --skip-gh-release." >&2
        return 1
    fi

    if ! gh auth status >/dev/null 2>&1; then
        echo "ERROR: Δεν υπάρχει ενεργό gh auth session. Τρέξε 'gh auth login' ή χρησιμοποίησε --skip-gh-release." >&2
        return 1
    fi

    local release_title="LearnByzantineMusic $tag"
    if gh release view "$tag" >/dev/null 2>&1; then
        gh release upload "$tag" "${assets[@]}" --clobber
        gh release edit "$tag" --title "$release_title" --latest
    else
        gh release create "$tag" "${assets[@]}" \
            --title "$release_title" \
            --target "$target_branch" \
            --generate-notes \
            --latest
    fi

    local release_url
    release_url="$(gh release view "$tag" --json url --jq .url)"
    if [[ -z "$release_url" ]]; then
        echo "ERROR: Δεν μπόρεσα να επιβεβαιώσω URL για το GitHub Release $tag." >&2
        return 1
    fi
    echo "[release] GitHub Release URL: $release_url"
}

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

# Stable asset aliases for direct end-user downloads.
cp "$APK_PATH" "$RELEASE_DIR/apk-release.apk"
cp "$AAB_PATH" "$RELEASE_DIR/aab-release.aab"

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

APK_ALIAS_PATH="$RELEASE_DIR/apk-release.apk"
AAB_ALIAS_PATH="$RELEASE_DIR/aab-release.aab"
CHECKSUMS_PATH="$RELEASE_DIR/SHA256SUMS.txt"


git add app/build.gradle.kts
git commit -m "release: $TAG"
git tag -a "$TAG" -m "Release $TAG"

BRANCH="$(git branch --show-current)"
if [[ "$PUSH_CHANGES" -eq 1 ]]; then
    git push origin "$BRANCH"
    git push origin "$TAG"
    ensure_gh_release "$TAG" "$BRANCH" "$APK_ALIAS_PATH" "$AAB_ALIAS_PATH" "$ZIP_PATH" "$CHECKSUMS_PATH"
    echo "[release] Έγινε push branch=$BRANCH και tag=$TAG"
    echo "[release] Το GitHub Action παραμένει ενεργό ως επιπλέον fallback."
else
    echo "[release] Δημιουργήθηκαν τοπικά commit+tag."
    echo "[release] Τρέξε: git push origin $BRANCH && git push origin $TAG"
fi

echo "[release] Έτοιμο. Έκδοση: $NEW_VERSION_NAME (code $NEW_VERSION_CODE)"
echo "[release] Local package folder: $RELEASE_DIR"
