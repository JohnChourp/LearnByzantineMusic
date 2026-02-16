#!/usr/bin/env python3
"""Generate MK symbol templates and catalog from MK fonts + KeyBoard.ini."""

from __future__ import annotations

import argparse
import json
import re
from pathlib import Path
from typing import Dict, List

from PIL import Image, ImageDraw, ImageFont, ImageOps

FONT_RULES = {
    "mk_byzantine": ["MKByzantine.ttf", "MK.ttf"],
    "mk_ison": ["MKIson.ttf", "MKNewIson.ttf", "MKIson2.ttf"],
    "mk_fthores": ["MKFthores.ttf"],
    "mk_loipa": ["MKLoipa.ttf"],
    "mk_xronos": ["MKXronos.ttf", "MKXronos2016.ttf"],
}

REQUIRED_TOKENS = ["a1", "c1", "e1", "f1", "g1", "xr1", "di1", "k1", "m1", "o1"]
SECTION_RE = re.compile(r"^\[(\d+)]$")
LAYER_RE = re.compile(r"^([1-5])=([A-Za-z0-9_]+)$")


def resolve_font(token: str, layer: int) -> str:
    if re.match(r"^(a|b)", token):
        return "mk_byzantine"
    if re.match(r"^(c|d|e|p)", token):
        return "mk_loipa"
    if re.match(r"^(f|k)", token):
        return "mk_fthores"
    if re.match(r"^(g|xr)", token):
        return "mk_xronos"
    if re.match(r"^(m|o|di|dm|ark|sa)", token):
        return "mk_ison"
    if layer == 5:
        return "mk_ison"
    if layer == 4:
        return "mk_loipa"
    if layer == 3:
        return "mk_fthores"
    return "mk_byzantine"


def resolve_category(font_id: str) -> str:
    if font_id == "mk_xronos":
        return "time"
    if font_id == "mk_fthores":
        return "fthores"
    if font_id == "mk_ison":
        return "ison"
    if font_id == "mk_loipa":
        return "loipa"
    return "basic"


def resolve_default_dy(font_id: str) -> float:
    if font_id == "mk_xronos":
        return -46.0
    if font_id == "mk_fthores":
        return -26.0
    if font_id == "mk_ison":
        return -20.0
    return -24.0


def parse_keyboard_ini(path: Path) -> List[Dict[str, int | str]]:
    rows: List[Dict[str, int | str]] = []
    seen = set()
    sequence = 0
    current_keycode = None

    for raw_line in path.read_text(encoding="utf-8", errors="ignore").splitlines():
        line = raw_line.replace("\r", "").strip()
        section = SECTION_RE.match(line)
        if section:
            current_keycode = int(section.group(1))
            continue
        if line.startswith("["):
            current_keycode = None
            continue
        if current_keycode is None:
            continue

        layer_match = LAYER_RE.match(line)
        if not layer_match:
            continue
        layer = int(layer_match.group(1))
        token = layer_match.group(2)
        if token in seen:
            continue

        seen.add(token)
        sequence += 1
        rows.append(
            {
                "order": sequence,
                "token": token,
                "keycode": current_keycode,
                "layer": layer,
            }
        )

    return rows


def choose_fonts(fonts_dir: Path) -> Dict[str, Path]:
    selected = {}
    for dest_name, candidates in FONT_RULES.items():
        for candidate in candidates:
            candidate_path = fonts_dir / candidate
            if candidate_path.exists():
                selected[dest_name] = candidate_path
                break
        if dest_name not in selected:
            joined = ", ".join(candidates)
            raise FileNotFoundError(f"Missing font for {dest_name}. Expected one of: {joined}")
    return selected


def render_token_png(token: str, keycode: int, font_path: Path, output_path: Path) -> None:
    text = chr(keycode)
    canvas_size = 360
    glyph_size = 210

    image = Image.new("L", (canvas_size, canvas_size), color=255)
    draw = ImageDraw.Draw(image)
    font = ImageFont.truetype(str(font_path), glyph_size)

    bbox = draw.textbbox((0, 0), text, font=font)
    text_width = bbox[2] - bbox[0]
    text_height = bbox[3] - bbox[1]
    x = (canvas_size - text_width) // 2 - bbox[0]
    y = (canvas_size - text_height) // 2 - bbox[1]
    draw.text((x, y), text, fill=0, font=font)

    inverted = ImageOps.invert(image)
    glyph_box = inverted.getbbox()
    if glyph_box is None:
        glyph_box = (0, 0, canvas_size, canvas_size)

    cropped = image.crop(glyph_box)
    target = Image.new("L", (96, 96), color=255)
    max_inner = 80
    scale = min(max_inner / max(1, cropped.width), max_inner / max(1, cropped.height))
    resized = cropped.resize(
        (max(1, int(cropped.width * scale)), max(1, int(cropped.height * scale))),
        Image.Resampling.LANCZOS,
    )
    paste_x = (target.width - resized.width) // 2
    paste_y = (target.height - resized.height) // 2
    target.paste(resized, (paste_x, paste_y))

    output_path.parent.mkdir(parents=True, exist_ok=True)
    target.save(output_path, format="PNG", optimize=True)


def main() -> int:
    parser = argparse.ArgumentParser(description="Generate MK symbol templates and catalog")
    parser.add_argument(
        "--mk-dir",
        default="/home/john/Downloads/MK_6_6/MK",
        help="Directory that contains fonts/ and files/KeyBoard.ini",
    )
    parser.add_argument(
        "--project-root",
        default=str(Path(__file__).resolve().parent.parent),
        help="Project root where app/src/main/assets exists",
    )
    args = parser.parse_args()

    mk_dir = Path(args.mk_dir)
    project_root = Path(args.project_root)
    fonts_dir = mk_dir / "fonts"
    keyboard_ini = mk_dir / "files" / "KeyBoard.ini"

    if not fonts_dir.exists():
        raise FileNotFoundError(f"Missing fonts directory: {fonts_dir}")
    if not keyboard_ini.exists():
        raise FileNotFoundError(f"Missing KeyBoard.ini: {keyboard_ini}")

    assets_dir = project_root / "app" / "src" / "main" / "assets"
    local_fonts_dir = assets_dir / "local_fonts"
    templates_dir = assets_dir / "mk_symbol_templates_v1"
    catalog_path = assets_dir / "byzantine_symbols_catalog_v1.json"
    mapping_path = assets_dir / "byzantine_interval_mapping_v1.json"

    rows = parse_keyboard_ini(keyboard_ini)
    if len(rows) < 80:
        raise RuntimeError(f"Too few symbols parsed from KeyBoard.ini ({len(rows)})")

    parsed_tokens = {row["token"] for row in rows}
    missing_required = [token for token in REQUIRED_TOKENS if token not in parsed_tokens]
    if missing_required:
        missing_str = ", ".join(missing_required)
        raise RuntimeError(f"Missing required tokens from KeyBoard.ini: {missing_str}")

    selected_fonts = choose_fonts(fonts_dir)
    local_fonts_dir.mkdir(parents=True, exist_ok=True)
    for font_id, source_path in selected_fonts.items():
        ext = source_path.suffix.lower()
        target_name = f"{font_id}{ext}"
        (local_fonts_dir / target_name).write_bytes(source_path.read_bytes())

    token_deltas: Dict[str, int] = {}
    if mapping_path.exists():
        mapping_data = json.loads(mapping_path.read_text(encoding="utf-8"))
        token_deltas = {k: int(v) for k, v in mapping_data.get("tokenDeltas", {}).items()}

    catalog = []
    for row in rows:
        token = str(row["token"])
        layer = int(row["layer"])
        keycode = int(row["keycode"])
        font_id = resolve_font(token, layer)
        font_path = selected_fonts[font_id]
        template_path = templates_dir / f"{token}.png"
        render_token_png(token, keycode, font_path, template_path)

        catalog.append(
            {
                "order": int(row["order"]),
                "token": token,
                "key": token,
                "label": token.upper(),
                "text": chr(keycode),
                "fontId": font_id,
                "category": resolve_category(font_id),
                "defaultDyDp": resolve_default_dy(font_id),
                "keycode": keycode,
                "layer": layer,
                "templateAssetPath": f"mk_symbol_templates_v1/{token}.png",
                "activeForMelody": token in token_deltas,
            }
        )

    catalog = sorted(catalog, key=lambda item: item["order"])
    catalog_path.write_text(json.dumps(catalog, ensure_ascii=False, indent=2), encoding="utf-8")

    print("OK: MK symbol dataset generated")
    print(f"- templates: {templates_dir}")
    print(f"- catalog: {catalog_path}")
    print(f"- total symbols: {len(catalog)}")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
