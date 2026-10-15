#!/usr/bin/env bash
set -euo pipefail

usage() {
    cat <<'USAGE'
Usage:
  make-store-assets.sh <raw-screenshots-dir>

Builds, from the same raw device screenshots, both the website images and the Google Play listing images:

  docs/screenshots/NN-name-360.webp, -720.webp   website gallery (720x1440, 1:2, cropped from the top)
  docs/assets/icon-192.png                        website icon
  docs/assets/feature-graphic.png                 website preview image (same as the Play one)
  play-store/phone-screenshots/NN-name.png        Play phone screenshots, 1080x1920 (9:16), no alpha
  play-store/feature-graphic.png                  Play feature graphic, 1024x500, no alpha
  play-store/icon-512.png                         Play app icon, 512x512

The raw directory must contain exactly these PNGs, as captured on the phone:
  01-eight-modes.png 02-ison.png 03-melody-trainer.png 04-anastasimatarion.png 05-calendar.png 06-lectern.png

Environment:
  CROP_TOP=<px>     rows to cut from the top of every raw screenshot (the status bar). Default 0.
  CROP_BOTTOM=<px>  rows to cut from the bottom (the navigation bar). Default 0.
  FONT_BOLD, FONT_REGULAR  fonts with Greek glyphs for the feature graphic (macOS Arial by default).

Requires ImageMagick 7 (magick).
USAGE
}

if [[ $# -ne 1 || "$1" == "-h" || "$1" == "--help" ]]; then
    usage
    [[ $# -eq 1 ]] && exit 0
    exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
RAW_DIR="$1"
CROP_TOP="${CROP_TOP:-0}"
CROP_BOTTOM="${CROP_BOTTOM:-0}"
FONT_BOLD="${FONT_BOLD:-/System/Library/Fonts/Supplemental/Arial Bold.ttf}"
FONT_REGULAR="${FONT_REGULAR:-/System/Library/Fonts/Supplemental/Arial.ttf}"

# LbmPalette.light
PAGE_BG="#F6F2EA"
HERO_START="#EADCC5"
HERO_END="#F7EFE1"
TEXT_PRIMARY="#2F251A"
BROWN="#7A4E24"

SHOTS=(01-eight-modes 02-ison 03-melody-trainer 04-anastasimatarion 05-calendar 06-lectern)

command -v magick >/dev/null || { echo "ERROR: Χρειάζεται το ImageMagick 7 (magick)." >&2; exit 1; }
for font in "$FONT_BOLD" "$FONT_REGULAR"; do
    [[ -f "$font" ]] || { echo "ERROR: Δεν βρέθηκε η γραμματοσειρά $font (FONT_BOLD/FONT_REGULAR)." >&2; exit 1; }
done
for name in "${SHOTS[@]}"; do
    [[ -f "$RAW_DIR/$name.png" ]] || { echo "ERROR: Λείπει το $RAW_DIR/$name.png" >&2; exit 1; }
done

WEB_SHOTS="$ROOT_DIR/docs/screenshots"
WEB_ASSETS="$ROOT_DIR/docs/assets"
PLAY_DIR="$ROOT_DIR/play-store"
mkdir -p "$WEB_SHOTS" "$WEB_ASSETS" "$PLAY_DIR/phone-screenshots"

for name in "${SHOTS[@]}"; do
    raw="$RAW_DIR/$name.png"
    # Cut the system bars, then strip every metadata chunk so nothing about the device is published.
    trimmed=(magick "$raw" -gravity North -chop "0x$CROP_TOP" -gravity South -chop "0x$CROP_BOTTOM" +repage -strip)

    # Website: a fixed 1:2 frame (a phone screen without its system bars), filled from the top so the
    # screen's title is always visible.
    for width in 720 360; do
        height=$((width * 2))
        "${trimmed[@]}" -resize "${width}x${height}^" -gravity North -extent "${width}x${height}" \
            -quality 82 "$WEB_SHOTS/$name-$width.webp"
    done

    # Play: the longer side may be at most twice the shorter one, so a tall phone screen is fitted
    # inside 1080x1920 on the app's page background instead of being cropped.
    "${trimmed[@]}" -resize 1080x1920 -background "$PAGE_BG" -gravity Center -extent 1080x1920 \
        -alpha off "PNG24:$PLAY_DIR/phone-screenshots/$name.png"
done

ICON_SRC="$ROOT_DIR/app/src/main/ic_launcher-playstore.png"
magick "$ICON_SRC" -strip -alpha off "PNG24:$PLAY_DIR/icon-512.png"
magick "$ICON_SRC" -strip -resize 192x192 "$WEB_ASSETS/icon-192.png"

# Feature graphic: the icon on the left, the name and the one-line pitch on the right.
magick -size 1024x500 "gradient:$HERO_START-$HERO_END" \
    \( "$ICON_SRC" -resize 260x260 \
       \( -size 260x260 xc:none -fill white -draw "roundrectangle 0,0 259,259 52,52" \) \
       -alpha set -compose DstIn -composite \) \
    -gravity West -geometry +72+0 -compose Over -composite \
    -font "$FONT_BOLD" -pointsize 58 -fill "$TEXT_PRIMARY" -gravity NorthWest \
    -annotate +380+165 "Learn Byzantine" -annotate +380+232 "Music" \
    -font "$FONT_REGULAR" -pointsize 34 -fill "$BROWN" \
    -annotate +380+318 "Βυζαντινή μουσική βήμα-βήμα" \
    -strip -alpha off "PNG24:$PLAY_DIR/feature-graphic.png"
cp "$PLAY_DIR/feature-graphic.png" "$WEB_ASSETS/feature-graphic.png"

echo "OK: εικόνες ιστοσελίδας στο docs/, εικόνες Google Play στο play-store/."
