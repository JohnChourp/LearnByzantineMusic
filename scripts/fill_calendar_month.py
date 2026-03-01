#!/usr/bin/env python3
"""
Fill one calendar month inside app/src/main/assets/calendar_celebrations_v1.json.

Usage:
  ./scripts/fill_calendar_month.py 2025-01
  ./scripts/fill_calendar_month.py 01-01-2025
"""

from __future__ import annotations

import html
import json
import re
import subprocess
import sys
import unicodedata
from dataclasses import asdict, dataclass
from datetime import date, timedelta
from pathlib import Path
from typing import Dict, List, Tuple
from urllib.parse import urlencode


DATASET_PATH = (
    Path(__file__).resolve().parents[1]
    / "app"
    / "src"
    / "main"
    / "assets"
    / "calendar_celebrations_v1.json"
)

READINGS_BASE_URL = "https://www.saint.gr/readings.aspx"
READING_DAY_URL_TEMPLATE = "https://www.saint.gr/{reading_day_id}/readingsday.aspx"

VALID_TYPES = {"public_holiday", "half_holiday", "religious_observance", "normal_day"}
FORBIDDEN_READING_KEYS = {"source", "source_url", "source_label", "url", "domain"}

# Provider-3 επιτρέπεται μόνο όταν δοθεί ρητά policy opt-in.
PROVIDER_3_ALLOWED = False


@dataclass(frozen=True)
class CelebrationEntry:
    title: str
    type: str
    is_official_non_working: bool
    is_half_day: bool
    description: str
    priority: int


@dataclass(frozen=True)
class ReadingEntry:
    id: str
    reference: str
    text_ancient: str
    text_modern: str


@dataclass(frozen=True)
class ParsedMonthReadings:
    readings_by_date: Dict[str, dict]
    provider_counters: Dict[str, int]
    days_parsed: int
    apostle_count: int
    gospel_count: int
    days_with_feast_extra_readings: int


def parse_target_arg(raw: str) -> Tuple[int, int]:
    if re.fullmatch(r"\d{4}-\d{2}", raw):
        year = int(raw[:4])
        month = int(raw[5:7])
    elif re.fullmatch(r"\d{2}-\d{2}-\d{4}", raw):
        day = int(raw[:2])
        month = int(raw[3:5])
        year = int(raw[6:10])
        _ = date(year, month, day)
    else:
        raise ValueError("Expected YYYY-MM or DD-MM-YYYY format.")

    if year < 1900 or year > 2100:
        raise ValueError("Year must be within 1900..2100")
    if month < 1 or month > 12:
        raise ValueError("Month must be within 01..12")
    return year, month


def month_days(year: int, month: int) -> List[date]:
    start = date(year, month, 1)
    end = date(year + (month // 12), ((month % 12) + 1), 1)
    days = []
    current = start
    while current < end:
        days.append(current)
        current += timedelta(days=1)
    return days


def orthodox_easter_gregorian(year: int) -> date:
    a = year % 4
    b = year % 7
    c = year % 19
    d = (19 * c + 15) % 30
    e = (2 * a + 4 * b - d + 34) % 7
    julian_month = (d + e + 114) // 31
    julian_day = ((d + e + 114) % 31) + 1
    julian_easter = date(year, julian_month, julian_day)
    return julian_easter + timedelta(days=13)


def build_public_holidays_for_year(year: int) -> Dict[date, List[CelebrationEntry]]:
    easter = orthodox_easter_gregorian(year)
    clean_monday = easter - timedelta(days=48)
    good_friday = easter - timedelta(days=2)
    easter_monday = easter + timedelta(days=1)
    holy_spirit_monday = easter + timedelta(days=50)

    fixed = {
        date(year, 1, 1): "Πρωτοχρονιά",
        date(year, 1, 6): "Θεοφάνεια",
        date(year, 3, 25): "Ευαγγελισμός της Θεοτόκου και Εθνική Εορτή",
        date(year, 5, 1): "Πρωτομαγιά",
        date(year, 8, 15): "Κοίμηση της Θεοτόκου",
        date(year, 10, 28): "Επέτειος 28ης Οκτωβρίου",
        date(year, 12, 25): "Χριστούγεννα",
        date(year, 12, 26): "Σύναξη Υπεραγίας Θεοτόκου",
    }
    movable = {
        clean_monday: "Καθαρά Δευτέρα",
        good_friday: "Μεγάλη Παρασκευή",
        easter_monday: "Δευτέρα του Πάσχα",
        holy_spirit_monday: "Αγίου Πνεύματος",
    }

    result: Dict[date, List[CelebrationEntry]] = {}
    for holiday_date, title in {**fixed, **movable}.items():
        result[holiday_date] = [
            CelebrationEntry(
                title=title,
                type="public_holiday",
                is_official_non_working=True,
                is_half_day=False,
                description="Επίσημη δημόσια αργία.",
                priority=10,
            )
        ]
    return result


def build_basic_religious_for_year(year: int) -> Dict[date, List[CelebrationEntry]]:
    observances = {
        date(year, 1, 1): (
            "Περιτομή του Κυρίου και Άγιος Βασίλειος",
            "Μεγάλη δεσποτική και αγιολογική εορτή της ημέρας.",
        ),
        date(year, 1, 5): (
            "Παραμονή Θεοφανείων",
            "Εκκλησιαστική παραμονή της εορτής των Θεοφανείων.",
        ),
        date(year, 1, 6): (
            "Βάπτιση του Κυρίου",
            "Μεγάλη δεσποτική εορτή της Ορθόδοξης Εκκλησίας.",
        ),
        date(year, 1, 7): (
            "Σύναξη Αγίου Ιωάννη του Προδρόμου",
            "Εκκλησιαστική εορτή την επόμενη ημέρα των Θεοφανείων.",
        ),
        date(year, 1, 17): (
            "Άγιος Αντώνιος ο Μέγας",
            "Σημαντική αγιολογική εορτή στην Ορθόδοξη παράδοση.",
        ),
        date(year, 1, 18): (
            "Άγιοι Αθανάσιος και Κύριλλος",
            "Εορτή μεγάλων Ιεραρχών της Εκκλησίας.",
        ),
        date(year, 1, 25): (
            "Άγιος Γρηγόριος ο Θεολόγος",
            "Εορτή του Αγίου Γρηγορίου του Ναζιανζηνού.",
        ),
        date(year, 1, 27): (
            "Ανακομιδή Λειψάνων Αγίου Ιωάννη Χρυσοστόμου",
            "Εκκλησιαστική μνήμη του Αγίου Ιωάννη Χρυσοστόμου.",
        ),
        date(year, 1, 30): (
            "Τρεις Ιεράρχες",
            "Εορτή των Τριών Ιεραρχών (Βασίλειος, Γρηγόριος, Χρυσόστομος).",
        ),
    }
    result: Dict[date, List[CelebrationEntry]] = {}
    for observance_date, (title, description) in observances.items():
        result[observance_date] = [
            CelebrationEntry(
                title=title,
                type="religious_observance",
                is_official_non_working=False,
                is_half_day=False,
                description=description,
                priority=20,
            )
        ]
    return result


def normal_day_entry() -> CelebrationEntry:
    return CelebrationEntry(
        title="Δεν υπάρχει καταχωρημένη εορτή",
        type="normal_day",
        is_official_non_working=False,
        is_half_day=False,
        description="Δεν βρέθηκε ειδική αργία ή βασική θρησκευτική εορτή για αυτή την ημέρα.",
        priority=1000,
    )


def build_month_entries(year: int, month: int) -> Dict[str, List[dict]]:
    public_holidays = build_public_holidays_for_year(year)
    religious = build_basic_religious_for_year(year)
    result: Dict[str, List[dict]] = {}

    for day in month_days(year, month):
        entries: List[CelebrationEntry] = []
        entries.extend(public_holidays.get(day, []))
        entries.extend(religious.get(day, []))
        if not entries:
            entries = [normal_day_entry()]
        result[day.isoformat()] = [asdict(entry) for entry in sorted(entries, key=lambda item: item.priority)]

    return result


def fetch_text_via_curl(url: str, post_data: str | None = None) -> str:
    command = ["curl", "-sS", "-L", "--fail", "--max-time", "60", url]
    if post_data is not None:
        command = [
            "curl",
            "-sS",
            "-L",
            "--fail",
            "--max-time",
            "60",
            url,
            "-H",
            "Content-Type: application/x-www-form-urlencoded",
            "--data-raw",
            post_data,
        ]

    try:
        result = subprocess.run(command, check=True, capture_output=True, text=True)
    except subprocess.CalledProcessError as ex:
        stderr = (ex.stderr or "").strip()
        raise RuntimeError(f"Network retrieval failed for {url}. {stderr}".strip()) from ex

    return result.stdout


def extract_hidden_field(html_text: str, field_name: str) -> str:
    match = re.search(
        rf'name="{re.escape(field_name)}"[^>]*value="([^"]*)"',
        html_text,
        flags=re.IGNORECASE,
    )
    if not match:
        raise ValueError(f"Missing hidden field '{field_name}' in year page.")
    return html.unescape(match.group(1))


def fetch_readings_year_page(year: int) -> str:
    first_page = fetch_text_via_curl(READINGS_BASE_URL)

    post_payload = {
        "__VIEWSTATE": extract_hidden_field(first_page, "__VIEWSTATE"),
        "__VIEWSTATEGENERATOR": extract_hidden_field(first_page, "__VIEWSTATEGENERATOR"),
        "__EVENTVALIDATION": extract_hidden_field(first_page, "__EVENTVALIDATION"),
        "__EVENTTARGET": "",
        "__EVENTARGUMENT": "",
        "ctl00$contentPlaceHolder$ddlYear": str(year),
        "ctl00$contentPlaceHolder$btnGo": "Μετάβαση",
    }

    return fetch_text_via_curl(READINGS_BASE_URL, post_data=urlencode(post_payload))


def extract_day_id_mapping(year_page_html: str) -> Dict[str, str]:
    pattern = re.compile(
        r'<div class="w3-fifth w3-container">\s*&nbsp;\s*(?:<span[^>]*>)?(\d{2}/\d{2}/\d{4})(?:</span>)?\s*&nbsp;-&nbsp;\s*</div>\s*<div class="w3-fourfifth w3-container"><a href="(\d+)/readingsday\.aspx"',
        flags=re.DOTALL,
    )
    rows = pattern.findall(year_page_html)
    if not rows:
        raise ValueError("Could not extract any day->readingId mapping from year page.")

    result: Dict[str, str] = {}
    for dd_mm_yyyy, reading_day_id in rows:
        day = date(int(dd_mm_yyyy[6:10]), int(dd_mm_yyyy[3:5]), int(dd_mm_yyyy[0:2]))
        result[day.isoformat()] = reading_day_id
    return result


def fetch_readings_day_page(reading_day_id: str) -> str:
    return fetch_text_via_curl(READING_DAY_URL_TEMPLATE.format(reading_day_id=reading_day_id))


def strip_accents(value: str) -> str:
    normalized = unicodedata.normalize("NFD", value)
    without_accents = "".join(ch for ch in normalized if unicodedata.category(ch) != "Mn")
    return unicodedata.normalize("NFC", without_accents)


def normalize_space(value: str) -> str:
    return re.sub(r"\s+", " ", value).strip()


def normalize_reference(value: str) -> str:
    cleaned = html.unescape(value).replace("\xa0", " ")
    cleaned = normalize_space(cleaned)
    cleaned = re.sub(r"\s+([,.;:])", r"\1", cleaned)
    return cleaned


def to_search_key(value: str) -> str:
    return normalize_space(strip_accents(value).upper())


def classify_reference(reference: str) -> str:
    key = to_search_key(reference)
    if key.startswith("ΚΑΤΑ "):
        return "gospel"

    apostle_markers = [
        "ΠΡΟΣ ",
        "ΠΡΑΞ",
        "ΙΑΚ",
        "ΠΕΤΡ",
        "ΙΩΑΝ",
        "ΙΟΥΔ",
        "ΡΩΜ",
        "ΚΟΡΙΝ",
        "ΓΑΛ",
        "ΕΦΕΣ",
        "ΦΙΛΙΠ",
        "ΚΟΛΟΣ",
        "ΘΕΣ",
        "ΤΙΜ",
        "ΤΙΤ",
        "ΦΙΛΗΜ",
        "ΕΒΡ",
    ]
    if any(marker in key for marker in apostle_markers):
        return "apostle"

    return "unknown"


def strip_tags_basic(value: str) -> str:
    text = re.sub(r"<[^>]+>", " ", value)
    text = html.unescape(text).replace("\xa0", " ")
    return normalize_space(text)


def extract_group_tabs(day_html: str) -> List[Tuple[str, str]]:
    pattern = re.compile(
        r"onclick=\"displayTab\(event,\s*'gospel',\s*'tablink',\s*'gID(\d+)',\s*'orange'\);\">\s*<div[^>]*>(.*?)</div>",
        flags=re.DOTALL,
    )
    return [(group_id, strip_tags_basic(label_html)) for group_id, label_html in pattern.findall(day_html)]


def classify_group_label(label: str) -> str:
    key = to_search_key(label)
    if "ΑΠΟΣΤΟΛ" in key:
        return "apostle"
    if "ΕΥΑΓΓΕΛ" in key:
        return "gospel"
    return "unknown"


def extract_variant_label_map(day_html: str, group_id: str) -> Dict[str, str]:
    pattern = re.compile(
        rf"onclick=\"displayTab\(event,\s*'verse\d+',\s*'tablink\d+',\s*'gID{group_id}vID(\d+)',\s*'cyan'\);\">\s*<div[^>]*>(.*?)</div>",
        flags=re.DOTALL,
    )

    mapping: Dict[str, str] = {}
    for variant_id, label_html in pattern.findall(day_html):
        label = to_search_key(strip_tags_basic(label_html))
        if "ΑΡΧΑΙ" in label:
            mapping[variant_id] = "ancient"
        elif "ΚΟΛΙΤΣ" in label:
            mapping[variant_id] = "provider-1"
        elif "ΤΡΕΜΠ" in label:
            mapping[variant_id] = "provider-2"
        elif "ΕΡΜΗΝ" in label:
            mapping[variant_id] = "provider-3"

    # deterministic fallback σε περίπτωση που λείπουν labels.
    mapping.setdefault("3", "ancient")
    mapping.setdefault("2", "provider-1")
    mapping.setdefault("4", "provider-2")
    return mapping


def extract_variant_content_map(day_html: str, group_id: str) -> Dict[str, str]:
    pattern = re.compile(
        rf"<div id=\"gID{group_id}vID(\d+)\"[^>]*>(.*?)</div>\s*(?=<div id=\"gID{group_id}vID\d+\"|<a href=\"javascript:void\(0\)\" onclick=\"displayTab\(event,\s*'verse\d+',\s*'tablink\d+',\s*'gID{group_id}vID\d+',\s*'cyan'\);\"|</div>\s*</div>)",
        flags=re.DOTALL,
    )
    return {variant_id: content_html for variant_id, content_html in pattern.findall(day_html)}


def normalize_line_breaks(value: str) -> str:
    lines = [line.strip() for line in value.splitlines()]
    compact: List[str] = []
    previous_blank = False
    for line in lines:
        if not line:
            if not previous_blank:
                compact.append("")
            previous_blank = True
            continue
        compact.append(line)
        previous_blank = False
    return "\n".join(compact).strip()


def extract_reference_text_pairs(variant_content_html: str) -> List[Tuple[str, str]]:
    marked = re.sub(
        r'<div class="w3-center"><strong>(.*?)</strong></div>',
        lambda match: f"\n@@REF@@{normalize_reference(strip_tags_basic(match.group(1)))}\n",
        variant_content_html,
        flags=re.DOTALL,
    )
    marked = re.sub(r"<br\s*/?>", "\n", marked, flags=re.IGNORECASE)
    marked = re.sub(r"</(p|div|li|tr|td|h\d)>", "\n", marked, flags=re.IGNORECASE)
    marked = re.sub(r"<sup[^>]*>(.*?)</sup>", r"\1", marked, flags=re.DOTALL | re.IGNORECASE)
    marked = re.sub(r"<[^>]+>", "", marked)
    marked = html.unescape(marked).replace("\xa0", " ")
    marked = normalize_line_breaks(marked)

    if "@@REF@@" not in marked:
        return []

    result: List[Tuple[str, str]] = []
    for block in marked.split("@@REF@@"):
        if not block.strip():
            continue
        lines = [line.strip() for line in block.splitlines() if line.strip()]
        if not lines:
            continue
        reference = normalize_reference(lines[0])
        text_body = normalize_line_breaks("\n".join(lines[1:]))
        if not reference or not text_body:
            continue
        result.append((reference, text_body))
    return result


def generate_modern_from_ancient(reference: str, ancient_text: str) -> str:
    summary_first_line = ancient_text.splitlines()[0].strip() if ancient_text.strip() else ""
    return normalize_line_breaks(
        f"Νεοελληνική απόδοση για το ανάγνωσμα: {reference}.\n\n"
        f"{summary_first_line}\n\n"
        "Το κείμενο αποδίδεται απλοποιημένα όταν δεν βρεθεί επιτρεπτή έτοιμη νεοελληνική εκδοχή."
    )


def resolve_modern_text(
    reference: str,
    ancient_text: str,
    modern_provider_1: str | None = None,
    modern_provider_2: str | None = None,
    modern_provider_3: str | None = None,
) -> Tuple[str, str]:
    if modern_provider_1 and modern_provider_1.strip():
        return normalize_line_breaks(modern_provider_1), "provider-1"
    if modern_provider_2 and modern_provider_2.strip():
        return normalize_line_breaks(modern_provider_2), "provider-2"
    if PROVIDER_3_ALLOWED and modern_provider_3 and modern_provider_3.strip():
        return normalize_line_breaks(modern_provider_3), "provider-3"
    return generate_modern_from_ancient(reference, ancient_text), "generated"


def parse_day_readings(day_iso: str, day_html: str) -> Tuple[Dict[str, List[ReadingEntry]], Dict[str, int], bool]:
    group_tabs = extract_group_tabs(day_html)
    provider_counters = {"provider-1": 0, "provider-2": 0, "provider-3": 0, "generated": 0}

    if not group_tabs:
        raise ValueError(f"{day_iso}: no tab groups found in readings day page.")

    entries_by_kind: Dict[str, List[Tuple[str, str, str]]] = {"apostle": [], "gospel": []}
    has_liturgy = False

    for group_id, group_label in group_tabs:
        kind_from_tab = classify_group_label(group_label)
        if kind_from_tab not in {"apostle", "gospel"}:
            # Π.χ. tab "Σχόλιο" σε ημέρα χωρίς θεία λειτουργία.
            continue

        variant_label_map = extract_variant_label_map(day_html, group_id)
        variant_content_map = extract_variant_content_map(day_html, group_id)

        ancient_variant_id = next((vid for vid, vtype in variant_label_map.items() if vtype == "ancient"), None)
        if not ancient_variant_id or ancient_variant_id not in variant_content_map:
            raise ValueError(f"{day_iso}: missing ancient text variant for group gID{group_id}.")

        provider1_variant_id = next((vid for vid, vtype in variant_label_map.items() if vtype == "provider-1"), None)
        provider2_variant_id = next((vid for vid, vtype in variant_label_map.items() if vtype == "provider-2"), None)
        provider3_variant_id = next((vid for vid, vtype in variant_label_map.items() if vtype == "provider-3"), None)

        ancient_pairs = extract_reference_text_pairs(variant_content_map[ancient_variant_id])
        provider1_pairs = (
            extract_reference_text_pairs(variant_content_map[provider1_variant_id])
            if provider1_variant_id and provider1_variant_id in variant_content_map
            else []
        )
        provider2_pairs = (
            extract_reference_text_pairs(variant_content_map[provider2_variant_id])
            if provider2_variant_id and provider2_variant_id in variant_content_map
            else []
        )
        provider3_pairs = (
            extract_reference_text_pairs(variant_content_map[provider3_variant_id])
            if provider3_variant_id and provider3_variant_id in variant_content_map
            else []
        )

        provider1_map = {to_search_key(reference): text for reference, text in provider1_pairs}
        provider2_map = {to_search_key(reference): text for reference, text in provider2_pairs}
        provider3_map = {to_search_key(reference): text for reference, text in provider3_pairs}

        for reference, ancient_text in ancient_pairs:
            has_liturgy = True
            normalized_reference = normalize_reference(reference)
            ref_key = to_search_key(normalized_reference)

            modern_text, provider_name = resolve_modern_text(
                normalized_reference,
                ancient_text,
                modern_provider_1=provider1_map.get(ref_key),
                modern_provider_2=provider2_map.get(ref_key),
                modern_provider_3=provider3_map.get(ref_key),
            )
            provider_counters[provider_name] += 1

            reading_kind = kind_from_tab
            if reading_kind not in {"apostle", "gospel"}:
                reading_kind = classify_reference(normalized_reference)
                if reading_kind == "unknown":
                    raise ValueError(
                        f"{day_iso}: could not classify reading reference '{normalized_reference}'."
                    )

            entries_by_kind[reading_kind].append(
                (normalized_reference, normalize_line_breaks(ancient_text), normalize_line_breaks(modern_text))
            )

    # deterministic IDs και ordering ανά kind.
    result = {"apostle": [], "gospel": []}
    for kind in ("apostle", "gospel"):
        for index, (reference, text_ancient, text_modern) in enumerate(entries_by_kind[kind], start=1):
            result[kind].append(
                ReadingEntry(
                    id=f"{day_iso}-{kind}-{index}",
                    reference=reference,
                    text_ancient=text_ancient,
                    text_modern=text_modern,
                )
            )

    return result, provider_counters, has_liturgy


def build_month_readings(year: int, month: int) -> ParsedMonthReadings:
    year_page_html = fetch_readings_year_page(year)
    day_id_map = extract_day_id_mapping(year_page_html)

    expected_days = [day.isoformat() for day in month_days(year, month)]
    missing_days = [day_iso for day_iso in expected_days if day_iso not in day_id_map]
    if missing_days:
        missing_indexes = [expected_days.index(day_iso) for day_iso in missing_days]
        first_missing_index = min(missing_indexes)
        expected_missing_indexes = list(range(first_missing_index, len(expected_days)))
        if sorted(missing_indexes) != expected_missing_indexes:
            raise ValueError(
                f"Month mapping is incomplete for {year:04d}-{month:02d}. Missing: {', '.join(missing_days)}"
            )

    readings_result: Dict[str, dict] = {}
    provider_counters = {"provider-1": 0, "provider-2": 0, "provider-3": 0, "generated": 0}
    apostle_total = 0
    gospel_total = 0
    days_with_feast_extra = 0

    for day_iso in expected_days:
        reading_day_id = day_id_map.get(day_iso)
        if not reading_day_id:
            readings_result[day_iso] = {"apostle": [], "gospel": []}
            continue

        day_html = fetch_readings_day_page(reading_day_id)
        day_readings, day_counters, has_liturgy = parse_day_readings(day_iso, day_html)

        if has_liturgy and not day_readings["apostle"] and not day_readings["gospel"]:
            raise ValueError(f"{day_iso}: liturgical day without parsed readings.")

        for provider in provider_counters.keys():
            provider_counters[provider] += day_counters[provider]

        apostle_total += len(day_readings["apostle"])
        gospel_total += len(day_readings["gospel"])
        if len(day_readings["apostle"]) > 1 or len(day_readings["gospel"]) > 1:
            days_with_feast_extra += 1

        readings_result[day_iso] = {
            "apostle": [asdict(entry) for entry in day_readings["apostle"]],
            "gospel": [asdict(entry) for entry in day_readings["gospel"]],
        }

    return ParsedMonthReadings(
        readings_by_date=readings_result,
        provider_counters=provider_counters,
        days_parsed=len(expected_days) - len(missing_days),
        apostle_count=apostle_total,
        gospel_count=gospel_total,
        days_with_feast_extra_readings=days_with_feast_extra,
    )


def load_or_initialize_dataset() -> dict:
    if DATASET_PATH.exists():
        with DATASET_PATH.open("r", encoding="utf-8") as handle:
            return json.load(handle)
    return {
        "version": "1",
        "country_scope": "GR",
        "language": "el",
        "days": {},
        "readings": {},
    }


def overwrite_month(dataset: dict, year: int, month: int) -> Tuple[dict, ParsedMonthReadings]:
    days = dict(dataset.get("days", {}))
    readings = dict(dataset.get("readings", {}))
    month_prefix = f"{year:04d}-{month:02d}-"

    for key in list(days.keys()):
        if key.startswith(month_prefix):
            del days[key]
    for key in list(readings.keys()):
        if key.startswith(month_prefix):
            del readings[key]

    days.update(build_month_entries(year, month))
    parsed_month = build_month_readings(year, month)
    readings.update(parsed_month.readings_by_date)

    updated_dataset = {
        "version": "1",
        "country_scope": "GR",
        "language": "el",
        "days": dict(sorted(days.items(), key=lambda pair: pair[0])),
        "readings": dict(sorted(readings.items(), key=lambda pair: pair[0])),
    }
    return updated_dataset, parsed_month


def validate_dataset(dataset: dict, year: int, month: int) -> None:
    if dataset.get("version") != "1":
        raise ValueError("Dataset version must be '1'.")
    if dataset.get("country_scope") != "GR":
        raise ValueError("country_scope must be 'GR'.")
    if dataset.get("language") != "el":
        raise ValueError("language must be 'el'.")

    days = dataset.get("days")
    readings = dataset.get("readings")
    if not isinstance(days, dict):
        raise ValueError("days must be an object.")
    if not isinstance(readings, dict):
        raise ValueError("readings must be an object.")

    month_prefix = f"{year:04d}-{month:02d}-"
    month_day_keys = sorted(key for key in days.keys() if key.startswith(month_prefix))
    month_reading_keys = sorted(key for key in readings.keys() if key.startswith(month_prefix))

    expected_keys = [day.isoformat() for day in month_days(year, month)]
    if month_day_keys != expected_keys:
        raise ValueError("days month keys are incomplete or not deterministic.")
    if month_reading_keys != expected_keys:
        raise ValueError("readings month keys are incomplete or not deterministic.")

    for key in expected_keys:
        _ = date.fromisoformat(key)

        day_entries = days[key]
        if not isinstance(day_entries, list) or not day_entries:
            raise ValueError(f"{key}: entries must be non-empty list.")
        for entry in day_entries:
            if entry.get("type") not in VALID_TYPES:
                raise ValueError(f"{key}: invalid celebration type '{entry.get('type')}'.")
            if not isinstance(entry.get("title"), str) or not entry["title"].strip():
                raise ValueError(f"{key}: celebration title is required.")
            if not isinstance(entry.get("description"), str):
                raise ValueError(f"{key}: celebration description must be string.")

        day_readings = readings[key]
        if not isinstance(day_readings, dict):
            raise ValueError(f"{key}: readings entry must be object.")
        if FORBIDDEN_READING_KEYS.intersection(day_readings.keys()):
            raise ValueError(f"{key}: forbidden source/url fields found in readings.")

        for reading_kind in ("apostle", "gospel"):
            items = day_readings.get(reading_kind, [])
            if not isinstance(items, list):
                raise ValueError(f"{key}: {reading_kind} must be list.")
            for item in items:
                if FORBIDDEN_READING_KEYS.intersection(item.keys()):
                    raise ValueError(f"{key}: forbidden source/url fields found in reading item.")
                for required_key in ("id", "reference", "text_ancient", "text_modern"):
                    value = item.get(required_key)
                    if not isinstance(value, str) or not value.strip():
                        raise ValueError(f"{key}: {reading_kind}.{required_key} is required.")


def write_dataset(dataset: dict) -> None:
    DATASET_PATH.parent.mkdir(parents=True, exist_ok=True)
    with DATASET_PATH.open("w", encoding="utf-8") as handle:
        json.dump(dataset, handle, ensure_ascii=False, indent=2)
        handle.write("\n")


def print_summary(dataset: dict, year: int, month: int, parsed_month: ParsedMonthReadings) -> None:
    month_prefix = f"{year:04d}-{month:02d}-"
    month_days_data = {k: v for k, v in dataset["days"].items() if k.startswith(month_prefix)}

    celebration_counts = {
        "public_holiday": 0,
        "half_holiday": 0,
        "religious_observance": 0,
        "normal_day": 0,
    }
    for entries in month_days_data.values():
        for entry in entries:
            celebration_counts[entry["type"]] += 1

    print(f"Month updated: {year:04d}-{month:02d}")
    print(f"Days parsed: {parsed_month.days_parsed}")
    print(f"Days updated: {len(month_days_data)}")
    print(f"public_holiday entries: {celebration_counts['public_holiday']}")
    print(f"half_holiday entries: {celebration_counts['half_holiday']}")
    print(f"religious_observance entries: {celebration_counts['religious_observance']}")
    print(f"normal_day entries: {celebration_counts['normal_day']}")
    print(f"apostle count: {parsed_month.apostle_count}")
    print(f"gospel count: {parsed_month.gospel_count}")
    print(f"provider-1 hits: {parsed_month.provider_counters['provider-1']}")
    print(f"provider-2 hits: {parsed_month.provider_counters['provider-2']}")
    print(f"provider-3 hits: {parsed_month.provider_counters['provider-3']}")
    print(f"generated hits: {parsed_month.provider_counters['generated']}")
    print(f"days with feast-extra readings: {parsed_month.days_with_feast_extra_readings}")
    print(f"Dataset: {DATASET_PATH}")


def main(argv: List[str]) -> int:
    if len(argv) != 2:
        print("Usage: fill_calendar_month.py YYYY-MM|DD-MM-YYYY")
        return 1

    try:
        year, month = parse_target_arg(argv[1])
        dataset = load_or_initialize_dataset()
        updated_dataset, parsed_month = overwrite_month(dataset, year, month)
        validate_dataset(updated_dataset, year, month)
        write_dataset(updated_dataset)
        print_summary(updated_dataset, year, month, parsed_month)
    except Exception as ex:  # noqa: BLE001
        print(f"ERROR: {ex}")
        return 1

    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
