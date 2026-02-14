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
    echo "[release] Εντοπίστηκαν αλλαγές στο working tree. Θα συμπεριληφθούν στο release commit."
fi

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

get_origin_repo_slug() {
    local origin_url
    origin_url="$(git remote get-url origin 2>/dev/null || true)"
    if [[ -z "$origin_url" ]]; then
        return 0
    fi

    origin_url="${origin_url#git@github.com:}"
    origin_url="${origin_url#https://github.com/}"
    origin_url="${origin_url#http://github.com/}"
    origin_url="${origin_url%.git}"

    if [[ "$origin_url" == */* ]]; then
        echo "$origin_url"
    fi
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
    local range_spec="$current_tag"
    if [[ -n "$previous_tag" ]]; then
        range_spec="${previous_tag}..${current_tag}"
    fi

    local -a commits=()
    while IFS=$'\x1f' read -r sha subject; do
        [[ -z "$sha" || -z "$subject" ]] && continue
        if [[ "$subject" =~ ^release:\ v[0-9]+\.[0-9]+\.[0-9]+($|[[:space:]]-[[:space:]]) ]]; then
            continue
        fi
        commits+=("${sha}"$'\x1f'"${subject}")
    done < <(git log --reverse --pretty=format:'%H%x1f%s' "$range_spec")

    local -a changed_files=()
    mapfile -t changed_files < <(git log --pretty='' --name-only "$range_spec" | sed '/^$/d' | sort -u)

    local commit_count="${#commits[@]}"
    local file_count="${#changed_files[@]}"
    local contributor_count
    contributor_count="$(git log --pretty='%an' "$range_spec" | sed '/^$/d' | sort -u | wc -l | tr -d ' ')"

    local -A area_counts=()
    local file area
    for file in "${changed_files[@]}"; do
        area="${file%%/*}"
        if [[ "$file" != */* ]]; then
            area="(root)"
        fi
        area_counts["$area"]=$((area_counts["$area"] + 1))
    done

    local repo_slug
    repo_slug="$(get_origin_repo_slug)"
    local compare_url=""
    if [[ -n "$previous_tag" && -n "$repo_slug" ]]; then
        compare_url="https://github.com/${repo_slug}/compare/${previous_tag}...${current_tag}"
    fi

    {
        echo "## Τι νέο περιλαμβάνει αυτή η έκδοση"
        if [[ -n "$previous_tag" ]]; then
            echo "- Συνοπτική εικόνα αλλαγών από \`${previous_tag}\` έως \`${current_tag}\`."
        else
            echo "- Πρώτη διαθέσιμη έκδοση με συνοπτική καταγραφή όλων των αλλαγών έως \`${current_tag}\`."
        fi
        echo "- Συνολικά commits: **${commit_count}**"
        echo "- Επηρεασμένα αρχεία: **${file_count}**"
        echo "- Συνεισφέροντες: **${contributor_count}**"
        if [[ -n "$compare_url" ]]; then
            echo "- Σύγκριση στο GitHub: [${previous_tag}...${current_tag}](${compare_url})"
        fi
        echo
        echo "## Περιοχές που επηρεάστηκαν περισσότερο"
        if [[ "${#area_counts[@]}" -eq 0 ]]; then
            echo "- Δεν εντοπίστηκαν αλλαγές αρχείων για το συγκεκριμένο εύρος."
        else
            while IFS=$'\t' read -r count top_area; do
                echo "- \`${top_area}\`: ${count} αρχείο(α)"
            done < <(
                for top_area in "${!area_counts[@]}"; do
                    printf '%s\t%s\n' "${area_counts[$top_area]}" "$top_area"
                done | sort -rn | head -n 8
            )
        fi
        echo
        echo "## Αναλυτική λίστα αλλαγών"
        if [[ "$commit_count" -eq 0 ]]; then
            echo "- Δεν βρέθηκαν επιπλέον λειτουργικές αλλαγές πέρα από το release bump."
        else
            local entry sha subject short_sha
            for entry in "${commits[@]}"; do
                sha="${entry%%$'\x1f'*}"
                subject="${entry#*$'\x1f'}"
                short_sha="${sha:0:7}"
                echo "- ${subject} (\`${short_sha}\`)"
            done
        fi
    } > "$notes_path"
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
write_release_notes "$PREVIOUS_TAG" "$TAG" "$RELEASE_NOTES_PATH"
echo "[release] Δημιουργήθηκαν release notes: $RELEASE_NOTES_PATH"

BRANCH="$(git branch --show-current)"
if [[ "$PUSH_CHANGES" -eq 1 ]]; then
    git push origin "$BRANCH"
    git push origin "$TAG"
    ensure_gh_release "$TAG" "$BRANCH" "$RELEASE_NOTES_PATH" "$APK_ALIAS_PATH" "$AAB_ALIAS_PATH" "$ZIP_PATH" "$CHECKSUMS_PATH"
    echo "[release] Έγινε push branch=$BRANCH και tag=$TAG"
    echo "[release] Το GitHub Action παραμένει ενεργό ως επιπλέον fallback."
else
    echo "[release] Δημιουργήθηκαν τοπικά commit+tag."
    echo "[release] Τρέξε: git push origin $BRANCH && git push origin $TAG"
fi

echo "[release] Έτοιμο. Έκδοση: $NEW_VERSION_NAME (code $NEW_VERSION_CODE)"
echo "[release] Local package folder: $RELEASE_DIR"
