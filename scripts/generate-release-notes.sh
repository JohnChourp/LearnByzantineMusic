#!/usr/bin/env bash
# Generates the GitHub Release description (RELEASE_NOTES.md) for a LearnByzantineMusic tag.
#
# One generator serves both release paths, so every release reads the same way:
#   - scripts/release-and-tag.sh (local release, also run by the brain's release skill)
#   - .github/workflows/android-release.yml (tag push)
#
# Content: the release summary, the merged pull requests grouped by kind (from the
# conventional-commit prefix of each PR title), install notes for apk-release.apk
# (size, SHA-256, minimum Android version) and the technical version details.
#
# Changes are read from the FIRST-PARENT history between the previous release tag and
# this tag: one line per merged pull request or direct commit on master, so a PR's
# internal review-fix commits do not flood the notes. Needs the full history and tags
# (CI: actions/checkout with fetch-depth: 0).
#
# Portable to bash 3.2 (macOS /bin/bash): no associative arrays, no mapfile.
set -euo pipefail

usage() {
    cat <<'USAGE'
Usage:
  generate-release-notes.sh --tag vX.Y.Z [--previous-tag vA.B.C] [--apk path/to/apk] [--out RELEASE_NOTES.md]

Without --previous-tag, the closest earlier vX.Y.Z tag that is an ancestor of --tag is used.
Without --out, the notes are written to stdout.
USAGE
}

TAG=""
PREVIOUS_TAG=""
APK_PATH=""
OUT_PATH=""

while (($# > 0)); do
    case "$1" in
        --tag|--previous-tag|--apk|--out)
            if [[ $# -lt 2 || -z "$2" ]]; then
                echo "ERROR: Λείπει τιμή για $1" >&2
                usage >&2
                exit 1
            fi
            case "$1" in
                --tag) TAG="$2" ;;
                --previous-tag) PREVIOUS_TAG="$2" ;;
                --apk) APK_PATH="$2" ;;
                --out) OUT_PATH="$2" ;;
            esac
            shift 2
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            echo "ERROR: Άγνωστη παράμετρος: $1" >&2
            usage >&2
            exit 1
            ;;
    esac
done

if [[ -z "$TAG" ]]; then
    echo "ERROR: Το --tag είναι υποχρεωτικό." >&2
    usage >&2
    exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$ROOT_DIR"

if ! git rev-parse --verify --quiet "${TAG}^{commit}" >/dev/null; then
    echo "ERROR: Το tag $TAG δεν υπάρχει τοπικά (χρειάζεται πλήρες history και tags)." >&2
    exit 1
fi

# Closest earlier release tag that is an ancestor of TAG, by semantic version order.
find_previous_tag() {
    local candidate
    while IFS= read -r candidate; do
        [[ -z "$candidate" || "$candidate" == "$TAG" ]] && continue
        if git merge-base --is-ancestor "$candidate" "$TAG" 2>/dev/null; then
            printf '%s\n' "$candidate"
            return 0
        fi
    done < <(git tag --list 'v[0-9]*.[0-9]*.[0-9]*' --sort=-v:refname --merged "$TAG" | grep -E '^v[0-9]+\.[0-9]+\.[0-9]+$' || true)
}

if [[ -z "$PREVIOUS_TAG" ]]; then
    PREVIOUS_TAG="$(find_previous_tag || true)"
elif ! git rev-parse --verify --quiet "${PREVIOUS_TAG}^{commit}" >/dev/null; then
    echo "ERROR: Το --previous-tag $PREVIOUS_TAG δεν υπάρχει τοπικά." >&2
    exit 1
fi

RANGE="$TAG"
if [[ -n "$PREVIOUS_TAG" ]]; then
    RANGE="${PREVIOUS_TAG}..${TAG}"
fi

repo_slug() {
    local url
    url="$(git remote get-url origin 2>/dev/null || true)"
    url="${url#git@github.com:}"
    url="${url#https://github.com/}"
    url="${url#http://github.com/}"
    url="${url%.git}"
    if [[ "$url" == */* ]]; then
        printf '%s\n' "$url"
    fi
}
SLUG="${GITHUB_REPOSITORY:-$(repo_slug)}"

gradle_value() {
    # First `name = value` assignment in the TAGGED app/build.gradle.kts, quotes stripped.
    git show "${TAG}:app/build.gradle.kts" 2>/dev/null \
        | sed -nE "s/^[[:space:]]*$1[[:space:]]*=[[:space:]]*\"?([^\"[:space:]]+)\"?.*/\1/p" | head -n1
}

android_version_for_sdk() {
    case "$1" in
        21) echo "5.0" ;; 22) echo "5.1" ;; 23) echo "6.0" ;; 24) echo "7.0" ;; 25) echo "7.1" ;;
        26) echo "8.0" ;; 27) echo "8.1" ;; 28) echo "9" ;; 29) echo "10" ;; 30) echo "11" ;;
        31) echo "12" ;; 32) echo "12L" ;; 33) echo "13" ;; 34) echo "14" ;; 35) echo "15" ;; 36) echo "16" ;;
        *) echo "" ;;
    esac
}

# Section buckets, filled in display order below.
FEATURES=""
FIXES=""
SECURITY=""
DEPENDENCIES=""
BUILD_CI=""
DOCS=""
OTHER=""
RELEASE_SUMMARY=""
ENTRY_COUNT=0

append_entry() {
    # $1 = bucket name, $2 = markdown line
    local line="$2"$'\n'
    case "$1" in
        features) FEATURES+="$line" ;;
        fixes) FIXES+="$line" ;;
        security) SECURITY+="$line" ;;
        dependencies) DEPENDENCIES+="$line" ;;
        build_ci) BUILD_CI+="$line" ;;
        docs) DOCS+="$line" ;;
        *) OTHER+="$line" ;;
    esac
    ENTRY_COUNT=$((ENTRY_COUNT + 1))
}

# Picks the bucket from the conventional-commit prefix, with a few keyword fallbacks
# for titles written without one (e.g. "Redesign …", "Fix …", "Harden …").
classify() {
    local title="$1" lower
    lower="$(printf '%s' "$title" | tr '[:upper:]' '[:lower:]')"
    if [[ "$lower" =~ ^([a-z]+)(\(([^\)]*)\))?!?:[[:space:]] ]]; then
        local type="${BASH_REMATCH[1]}" scope="${BASH_REMATCH[3]}"
        if [[ "$type" == "docs" ]]; then
            echo "docs"
            return
        fi
        if [[ "$scope" == "deps" || "$scope" == "deps-dev" ]]; then
            echo "dependencies"
            return
        fi
        if [[ "$scope" == "security" || "$type" == "security" ]]; then
            echo "security"
            return
        fi
        case "$type" in
            feat|perf) echo "features" ;;
            fix) echo "fixes" ;;
            deps) echo "dependencies" ;;
            docs) echo "docs" ;;
            build|ci|chore|refactor|test|style|revert) echo "build_ci" ;;
            *) echo "other" ;;
        esac
        return
    fi
    case "$lower" in
        document*|docs*|readme*) echo "docs" ;;
        *security*|*cve-*|*vulnerab*|harden*) echo "security" ;;
        bump*) echo "dependencies" ;;
        *codeql*|*workflow*|ignore\ *|*gradle*) echo "build_ci" ;;
        add*|redesign*|new\ *|improve*) echo "features" ;;
        fix*|correct*|repair*) echo "fixes" ;;
        *) echo "other" ;;
    esac
}

# Drops the conventional-commit prefix ("feat(scope): ") and capitalizes the rest.
display_title() {
    local title="$1" rest first
    if [[ "$title" =~ ^[A-Za-z]+(\([^\)]*\))?!?:[[:space:]]+(.*)$ ]]; then
        rest="${BASH_REMATCH[2]}"
    else
        rest="$title"
    fi
    first="${rest:0:1}"
    printf '%s%s' "$(printf '%s' "$first" | tr '[:lower:]' '[:upper:]')" "${rest:1}"
}

pr_link() {
    if [[ -n "$SLUG" ]]; then
        printf '[#%s](https://github.com/%s/pull/%s)' "$1" "$SLUG" "$1"
    else
        printf '#%s' "$1"
    fi
}

commit_link() {
    local short="${1:0:7}"
    if [[ -n "$SLUG" ]]; then
        printf '[`%s`](https://github.com/%s/commit/%s)' "$short" "$SLUG" "$1"
    else
        printf '`%s`' "$short"
    fi
}

# Walk master's first-parent chain: merges are pull requests, the rest are direct commits.
while IFS=$'\x1f' read -r sha parents subject; do
    [[ -z "$sha" ]] && continue
    pr=""
    title="$subject"
    if [[ "$parents" == *" "* ]]; then
        if [[ "$subject" =~ ^Merge\ pull\ request\ \#([0-9]+)\ from ]]; then
            pr="${BASH_REMATCH[1]}"
            # GitHub puts the PR title on the first body line of its merge commit.
            title="$(git log -1 --format=%b "$sha" | sed '/^[[:space:]]*$/d' | head -n1)"
        elif [[ "$subject" =~ /pr/([0-9]+)\'?$ ]]; then
            pr="${BASH_REMATCH[1]}"
            title=""
        fi
        if [[ -z "$title" ]]; then
            title="$(git log -1 --format=%s "${sha}^2")"
        fi
    elif [[ "$subject" =~ \(\#([0-9]+)\)$ ]]; then
        pr="${BASH_REMATCH[1]}"
        title="${subject% (#${pr})}"
    fi

    # The version bump itself is not a change; keep a hand-written summary from it.
    if [[ "$title" =~ ^release:\ v[0-9]+\.[0-9]+\.[0-9]+(\ -\ (.*))?$ ]]; then
        summary="${BASH_REMATCH[2]:-}"
        if [[ -n "$summary" && ! "$summary" =~ ^(updates\ in\ |version\ bump) && -z "$RELEASE_SUMMARY" ]]; then
            RELEASE_SUMMARY="$summary"
        fi
        continue
    fi

    if [[ -n "$pr" ]]; then
        ref="$(pr_link "$pr")"
    else
        ref="$(commit_link "$sha")"
    fi
    append_entry "$(classify "$title")" "- $(display_title "$title") (${ref})"
done < <(git log --first-parent --reverse --format='%H%x1f%P%x1f%s' "$RANGE")

VERSION_NAME="${TAG#v}"
VERSION_CODE="$(gradle_value versionCode)"
MIN_SDK="$(gradle_value minSdk)"
TARGET_SDK="$(gradle_value targetSdk)"
MIN_ANDROID="$(android_version_for_sdk "$MIN_SDK")"

APK_SIZE_MB=""
APK_SHA256=""
if [[ -n "$APK_PATH" ]]; then
    if [[ ! -f "$APK_PATH" ]]; then
        echo "ERROR: Δεν βρέθηκε το APK στο $APK_PATH" >&2
        exit 1
    fi
    APK_BYTES="$(wc -c < "$APK_PATH" | tr -d '[:space:]')"
    APK_SIZE_MB="$(awk -v b="$APK_BYTES" 'BEGIN { printf "%.1f", b / 1048576 }')"
    if command -v sha256sum >/dev/null 2>&1; then
        APK_SHA256="$(sha256sum "$APK_PATH" | awk '{print $1}')"
    else
        APK_SHA256="$(shasum -a 256 "$APK_PATH" | awk '{print $1}')"
    fi
fi

print_section() {
    # $1 = heading, $2 = bucket contents
    if [[ -n "$2" ]]; then
        printf '### %s\n%s\n' "$1" "$2"
    fi
}

render() {
    echo "## Τι αλλάζει στην έκδοση ${VERSION_NAME}"
    if [[ -n "$RELEASE_SUMMARY" ]]; then
        echo "${RELEASE_SUMMARY}"
        echo
    fi
    if [[ "$ENTRY_COUNT" -eq 0 ]]; then
        echo "- Δεν υπάρχουν αλλαγές πέρα από την αλλαγή έκδοσης."
        echo
    else
        print_section "✨ Νέα & βελτιώσεις" "$FEATURES"
        print_section "🐛 Διορθώσεις" "$FIXES"
        print_section "🔒 Ασφάλεια" "$SECURITY"
        print_section "⬆️ Εξαρτήσεις" "$DEPENDENCIES"
        print_section "🧰 Build & CI" "$BUILD_CI"
        print_section "📝 Τεκμηρίωση" "$DOCS"
        print_section "🔹 Άλλες αλλαγές" "$OTHER"
    fi

    echo "## 📦 Εγκατάσταση"
    if [[ -n "$APK_SIZE_MB" ]]; then
        echo "- Κατέβασε το **apk-release.apk** (${APK_SIZE_MB} MB) από τα Assets και άνοιξέ το στο κινητό."
    else
        echo "- Κατέβασε το **apk-release.apk** από τα Assets και άνοιξέ το στο κινητό."
    fi
    if [[ -n "$MIN_ANDROID" ]]; then
        echo "- Απαιτεί **Android ${MIN_ANDROID}** ή νεότερο."
    fi
    echo "- Αν το κινητό το ζητήσει, επίτρεψε την εγκατάσταση από αυτή την πηγή· εγκαθίσταται ως ενημέρωση πάνω από την προηγούμενη έκδοση."
    if [[ -n "$APK_SHA256" ]]; then
        echo "- SHA-256: \`${APK_SHA256}\`"
    fi
    echo

    echo "## ℹ️ Τεχνικά στοιχεία"
    local line="- Έκδοση **${VERSION_NAME}**"
    [[ -n "$VERSION_CODE" ]] && line+=" · versionCode ${VERSION_CODE}"
    [[ -n "$MIN_SDK" ]] && line+=" · minSdk ${MIN_SDK}"
    [[ -n "$TARGET_SDK" ]] && line+=" · targetSdk ${TARGET_SDK}"
    echo "$line"
    if [[ -n "$PREVIOUS_TAG" ]]; then
        if [[ -n "$SLUG" ]]; then
            echo "- Όλες οι αλλαγές από την προηγούμενη έκδοση: [${PREVIOUS_TAG}...${TAG}](https://github.com/${SLUG}/compare/${PREVIOUS_TAG}...${TAG})"
        else
            echo "- Όλες οι αλλαγές από την προηγούμενη έκδοση: \`${PREVIOUS_TAG}...${TAG}\`"
        fi
    else
        echo "- Πρώτη καταγεγραμμένη έκδοση."
    fi
}

if [[ -n "$OUT_PATH" ]]; then
    render > "$OUT_PATH"
else
    render
fi
