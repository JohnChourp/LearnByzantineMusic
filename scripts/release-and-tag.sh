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
SECRETS_GUARD_SCRIPT="$SCRIPT_DIR/check-no-secrets.sh"
RELEASE_NOTES_SCRIPT="$SCRIPT_DIR/generate-release-notes.sh"

if [[ ! -x "$BUMP_SCRIPT" ]]; then
    echo "ERROR: Δεν βρέθηκε εκτελέσιμο bump script στο $BUMP_SCRIPT" >&2
    exit 1
fi

if [[ ! -x "$SECRETS_GUARD_SCRIPT" ]]; then
    echo "ERROR: Δεν βρέθηκε εκτελέσιμο secrets guard script στο $SECRETS_GUARD_SCRIPT" >&2
    exit 1
fi

if [[ ! -f "$RELEASE_NOTES_SCRIPT" ]]; then
    echo "ERROR: Δεν βρέθηκε το release notes script στο $RELEASE_NOTES_SCRIPT" >&2
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
    echo "[release] Εντοπίστηκαν αλλαγές στο working tree. Θα συμπεριληφθούν στο release commit."
fi

"$SECRETS_GUARD_SCRIPT"

ensure_release_signing_env() {
    local -a missing_vars=()
    [[ -z "${ANDROID_SIGNING_STORE_FILE:-}" ]] && missing_vars+=("ANDROID_SIGNING_STORE_FILE")
    [[ -z "${ANDROID_SIGNING_STORE_PASSWORD:-}" ]] && missing_vars+=("ANDROID_SIGNING_STORE_PASSWORD")
    [[ -z "${ANDROID_SIGNING_KEY_ALIAS:-}" ]] && missing_vars+=("ANDROID_SIGNING_KEY_ALIAS")
    [[ -z "${ANDROID_SIGNING_KEY_PASSWORD:-}" ]] && missing_vars+=("ANDROID_SIGNING_KEY_PASSWORD")

    if [[ "${#missing_vars[@]}" -gt 0 ]]; then
        echo "ERROR: Λείπουν υποχρεωτικά Android release signing env vars: ${missing_vars[*]}" >&2
        echo "ERROR: Χωρίς signing το APK είναι invalid για εγκατάσταση. Όρισε τα vars και ξανατρέξε release." >&2
        exit 1
    fi

    if [[ ! -f "${ANDROID_SIGNING_STORE_FILE}" ]]; then
        echo "ERROR: Δεν βρέθηκε keystore αρχείο στο ANDROID_SIGNING_STORE_FILE=${ANDROID_SIGNING_STORE_FILE}" >&2
        exit 1
    fi
}

ensure_gh_release() {
    local tag="$1"
    local target_branch="$2"
    local notes_file="$3"
    shift 3
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
        gh release edit "$tag" \
            --title "$release_title" \
            --notes-file "$notes_file" \
            --latest
    else
        gh release create "$tag" "${assets[@]}" \
            --title "$release_title" \
            --target "$target_branch" \
            --notes-file "$notes_file" \
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

find_previous_tag() {
    local current_tag="$1"
    local old_version_name="$2"
    local candidate_tag=""

    if [[ -n "$old_version_name" ]]; then
        candidate_tag="v${old_version_name}"
        if [[ "$candidate_tag" != "$current_tag" ]] && git rev-parse "$candidate_tag" >/dev/null 2>&1; then
            echo "$candidate_tag"
            return 0
        fi
    fi

    candidate_tag="$(git describe --tags --abbrev=0 2>/dev/null || true)"
    if [[ -n "$candidate_tag" && "$candidate_tag" != "$current_tag" ]]; then
        echo "$candidate_tag"
        return 0
    fi

    echo ""
}

build_release_commit_summary() {
    local -a staged_files=()
    mapfile -t staged_files < <(git diff --cached --name-only --diff-filter=ACMRD)

    if [[ "${#staged_files[@]}" -eq 0 ]]; then
        echo "version bump"
        return
    fi

    local -A seen_areas=()
    local -a ordered_areas=()
    local file area
    for file in "${staged_files[@]}"; do
        area="${file%%/*}"
        if [[ "$file" != */* ]]; then
            area="root"
        fi
        if [[ -z "${seen_areas[$area]+x}" ]]; then
            seen_areas["$area"]=1
            ordered_areas+=("$area")
        fi
    done

    local area_count="${#ordered_areas[@]}"
    if [[ "$area_count" -eq 1 ]]; then
        echo "updates in ${ordered_areas[0]}"
    elif [[ "$area_count" -eq 2 ]]; then
        echo "updates in ${ordered_areas[0]}, ${ordered_areas[1]}"
    else
        echo "updates in ${ordered_areas[0]}, ${ordered_areas[1]} +$((area_count - 2)) more"
    fi
}

write_release_notes() {
    local previous_tag="$1"
    local current_tag="$2"
    local notes_path="$3"
    local apk_path="$4"
    local -a notes_args=(--tag "$current_tag" --out "$notes_path")
    if [[ -n "$previous_tag" ]]; then
        notes_args+=(--previous-tag "$previous_tag")
    fi
    if [[ -n "$apk_path" ]]; then
        notes_args+=(--apk "$apk_path")
    fi

    # Same generator as the tag workflow, so both release paths publish the same report.
    "$BASH" "$RELEASE_NOTES_SCRIPT" "${notes_args[@]}"
}

mapfile -t bump_output < <("$BUMP_SCRIPT" "${BUMP_ARGS[@]}")
printf '%s\n' "${bump_output[@]}"

NEW_VERSION_NAME=""
NEW_VERSION_CODE=""
OLD_VERSION_NAME=""
for line in "${bump_output[@]}"; do
    case "$line" in
        OLD_VERSION_NAME=*) OLD_VERSION_NAME="${line#OLD_VERSION_NAME=}" ;;
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

PREVIOUS_TAG="$(find_previous_tag "$TAG" "$OLD_VERSION_NAME")"
if [[ -n "$PREVIOUS_TAG" ]]; then
    echo "[release] Previous release tag: $PREVIOUS_TAG"
else
    echo "[release] Previous release tag: none (first release baseline)"
fi

echo "[release] Build release artifacts για $TAG"
ensure_release_signing_env
./gradlew clean assembleRelease bundleRelease

RELEASE_DIR="$ROOT_DIR/build-artifacts/release/$TAG"
mkdir -p "$RELEASE_DIR"

APK_PATH="$(find "$ROOT_DIR/app/build/outputs/apk/release" -maxdepth 1 -type f -name '*.apk' ! -name '*-unsigned.apk' | sort | tail -n1 || true)"
UNSIGNED_APK_PATH="$(find "$ROOT_DIR/app/build/outputs/apk/release" -maxdepth 1 -type f -name '*-unsigned.apk' | sort | tail -n1 || true)"

if [[ -z "$APK_PATH" ]]; then
    if [[ -n "$UNSIGNED_APK_PATH" ]]; then
        echo "ERROR: Βρέθηκε μόνο unsigned APK: $UNSIGNED_APK_PATH" >&2
    fi
    echo "ERROR: Δεν βρέθηκε signed APK release artifact." >&2
    exit 1
fi

cp "$APK_PATH" "$RELEASE_DIR/"

# Stable asset aliases for direct end-user downloads.
cp "$APK_PATH" "$RELEASE_DIR/apk-release.apk"

APK_ALIAS_PATH="$RELEASE_DIR/apk-release.apk"


git add -A
if git diff --cached --quiet; then
    echo "ERROR: Δεν βρέθηκαν αλλαγές για commit στο release." >&2
    exit 1
fi

COMMIT_SUMMARY="$(build_release_commit_summary)"
COMMIT_MESSAGE="release: $TAG - $COMMIT_SUMMARY"
git commit -m "$COMMIT_MESSAGE"
git tag -a "$TAG" -m "Release $TAG"

RELEASE_NOTES_PATH="$RELEASE_DIR/RELEASE_NOTES.md"
write_release_notes "$PREVIOUS_TAG" "$TAG" "$RELEASE_NOTES_PATH" "$APK_ALIAS_PATH"
echo "[release] Δημιουργήθηκαν release notes: $RELEASE_NOTES_PATH"

BRANCH="$(git branch --show-current)"
if [[ "$PUSH_CHANGES" -eq 1 ]]; then
    git push origin "$BRANCH"
    git push origin "$TAG"
    ensure_gh_release "$TAG" "$BRANCH" "$RELEASE_NOTES_PATH" "$APK_ALIAS_PATH"
    echo "[release] Έγινε push branch=$BRANCH και tag=$TAG"
    echo "[release] Το GitHub Action παραμένει ενεργό ως επιπλέον fallback."
else
    echo "[release] Δημιουργήθηκαν τοπικά commit+tag."
    echo "[release] Τρέξε: git push origin $BRANCH && git push origin $TAG"
fi

echo "[release] Έτοιμο. Έκδοση: $NEW_VERSION_NAME (code $NEW_VERSION_CODE)"
echo "[release] Local package folder: $RELEASE_DIR"
