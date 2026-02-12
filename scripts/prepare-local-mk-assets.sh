#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

MSI_PATH="${1:-/home/john/Downloads/MK_6_6.msi}"
EXE_PATH="${2:-/home/john/Downloads/MKwclient300.exe}"

ASSETS_DIR="$ROOT_DIR/app/src/main/assets"
LOCAL_FONTS_DIR="$ASSETS_DIR/local_fonts"
SYMBOLS_JSON="$ASSETS_DIR/editor_symbols_local_v1.json"

find_source_dir() {
    local msi_dir exe_dir
    msi_dir="$(dirname "$MSI_PATH")/$(basename "${MSI_PATH%.msi}")/MK"
    exe_dir="$(dirname "$EXE_PATH")/$(basename "${EXE_PATH%.exe}")/MK"

    local candidates=(
        "${MK_EXTRACTED_DIR:-}"
        "$msi_dir"
        "$exe_dir"
        "/home/john/Downloads/projects/MK_6_6/MK"
        "/home/john/Downloads/MK_6_6/MK"
    )

    for candidate in "${candidates[@]}"; do
        if [[ -n "$candidate" && -d "$candidate/files" && -d "$candidate/fonts" ]]; then
            printf '%s\n' "$candidate"
            return 0
        fi
    done
    return 1
}

copy_best_font() {
    local dest_name="$1"
    shift
    local source_name source_path
    for source_name in "$@"; do
        source_path="$SOURCE_DIR/fonts/$source_name"
        if [[ -f "$source_path" ]]; then
            cp "$source_path" "$LOCAL_FONTS_DIR/$dest_name"
            return 0
        fi
    done
    return 1
}

if [[ ! -f "$MSI_PATH" ]]; then
    echo "ERROR: Δεν βρέθηκε το MSI: $MSI_PATH" >&2
    exit 1
fi

if [[ ! -f "$EXE_PATH" ]]; then
    echo "ERROR: Δεν βρέθηκε το EXE: $EXE_PATH" >&2
    exit 1
fi

if ! SOURCE_DIR="$(find_source_dir)"; then
    cat >&2 <<'EOF'
ERROR: Δεν βρέθηκε extracted MK φάκελος (με subfolders files/ και fonts/).
Αναμενόμενα paths:
- /home/john/Downloads/MK_6_6/MK
- /home/john/Downloads/projects/MK_6_6/MK
ή δώσε custom path με MK_EXTRACTED_DIR=/path/to/MK.
EOF
    exit 1
fi

KEYBOARD_INI="$SOURCE_DIR/files/KeyBoard.ini"
if [[ ! -f "$KEYBOARD_INI" ]]; then
    echo "ERROR: Δεν βρέθηκε το KeyBoard.ini στο $SOURCE_DIR/files" >&2
    exit 1
fi

mkdir -p "$LOCAL_FONTS_DIR"

copy_best_font "mk_byzantine.ttf" "MKByzantine.ttf" "MK.ttf" || {
    echo "ERROR: Λείπει MKByzantine.ttf (ή fallback MK.ttf)." >&2
    exit 1
}
copy_best_font "mk_ison.ttf" "MKIson.ttf" "MKNewIson.ttf" "MKIson2.ttf" || {
    echo "ERROR: Λείπει MKIson.ttf (ή fallback MKNewIson.ttf/MKIson2.ttf)." >&2
    exit 1
}
copy_best_font "mk_fthores.ttf" "MKFthores.ttf" || {
    echo "ERROR: Λείπει MKFthores.ttf." >&2
    exit 1
}
copy_best_font "mk_loipa.ttf" "MKLoipa.ttf" || {
    echo "ERROR: Λείπει MKLoipa.ttf." >&2
    exit 1
}
copy_best_font "mk_xronos.ttf" "MKXronos.ttf" "MKXronos2016.ttf" || {
    echo "ERROR: Λείπει MKXronos.ttf (ή fallback MKXronos2016.ttf)." >&2
    exit 1
}

TMP_TSV="$(mktemp)"
trap 'rm -f "$TMP_TSV"' EXIT

awk '
{
  gsub(/\r/, "", $0)
}
/^\[[0-9]+\]$/ {
  key = $0
  gsub(/\[|\]/, "", key)
  next
}
/^\[/ {
  key = ""
  next
}
/^[1-5]=[A-Za-z0-9_]+$/ && key != "" {
  split($0, pair, "=")
  layer = pair[1]
  token = pair[2]
  if (!(token in seen)) {
    seen[token] = 1
    sequence += 1
    printf "%d\t%s\t%s\t%s\n", sequence, token, key, layer
  }
}
' "$KEYBOARD_INI" > "$TMP_TSV"

symbol_count="$(wc -l < "$TMP_TSV" | tr -d ' ')"
if [[ "$symbol_count" -lt 80 ]]; then
    echo "ERROR: Βρέθηκαν πολύ λίγα symbols ($symbol_count). Το mapping φαίνεται ελλιπές." >&2
    exit 1
fi

required_tokens=(a1 c1 e1 f1 g1 xr1 di1 k1 m1 o1)
missing_tokens=()
for token in "${required_tokens[@]}"; do
    if ! grep -q $'\t'"$token"$'\t' "$TMP_TSV"; then
        missing_tokens+=("$token")
    fi
done
if [[ "${#missing_tokens[@]}" -gt 0 ]]; then
    echo "ERROR: Λείπουν απαιτούμενα tokens από mapping: ${missing_tokens[*]}" >&2
    exit 1
fi

jq -Rn --rawfile rows "$TMP_TSV" '
def resolve_font($key; $layer):
  if ($key | test("^(a|b)")) then "mk_byzantine"
  elif ($key | test("^(c|d|e|p)")) then "mk_loipa"
  elif ($key | test("^(f|k)")) then "mk_fthores"
  elif ($key | test("^(g|xr)")) then "mk_xronos"
  elif ($key | test("^(m|o|di|dm|ark|sa)")) then "mk_ison"
  else
    if $layer == 5 then "mk_ison"
    elif $layer == 4 then "mk_loipa"
    elif $layer == 3 then "mk_fthores"
    else "mk_byzantine"
    end
  end;
def resolve_category($font):
  if $font == "mk_xronos" then "time"
  elif $font == "mk_fthores" then "fthores"
  elif $font == "mk_ison" then "ison"
  elif $font == "mk_loipa" then "loipa"
  else "basic"
  end;
def resolve_default_dy($font):
  if $font == "mk_xronos" then -46.0
  elif $font == "mk_fthores" then -26.0
  elif $font == "mk_ison" then -20.0
  else -24.0
  end;

($rows
  | split("\n")
  | map(select(length > 0))
  | map(split("\t"))
  | map({
      order: (.[0] | tonumber),
      key: .[1],
      keycode: (.[2] | tonumber),
      layer: (.[3] | tonumber)
    })
  | sort_by(.order)
  | map(
      . as $row
      | (resolve_font($row.key; $row.layer)) as $font
      | {
          key: $row.key,
          label: ($row.key | ascii_upcase),
          text: ([$row.keycode] | implode),
          fontId: $font,
          category: resolve_category($font),
          defaultDyDp: resolve_default_dy($font)
        }
    )
) | unique_by(.key)
' > "$SYMBOLS_JSON"

echo "OK: Local MK assets έτοιμα."
echo "- Source: $SOURCE_DIR"
echo "- Fonts: $LOCAL_FONTS_DIR"
echo "- Symbols: $SYMBOLS_JSON"
echo "- Symbols count: $(jq 'length' "$SYMBOLS_JSON")"
