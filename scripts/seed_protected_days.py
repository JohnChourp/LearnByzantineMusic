#!/usr/bin/env python3
"""
Seed protected celebrations/public holidays for an inclusive year range.

Usage:
  ./scripts/seed_protected_days.py 2025 2026
"""

from __future__ import annotations

from dataclasses import asdict
import sys
from typing import List, Tuple

from fill_calendar_month import build_protected_days_for_year, load_or_initialize_dataset, write_dataset


def parse_year_range_args(argv: List[str]) -> Tuple[int, int]:
    if len(argv) != 3:
        raise ValueError("Usage: seed_protected_days.py START_YEAR END_YEAR")

    start_year = int(argv[1])
    end_year = int(argv[2])
    if start_year < 1900 or end_year > 2100:
        raise ValueError("Years must be within 1900..2100")
    if start_year > end_year:
        raise ValueError("START_YEAR must be <= END_YEAR")
    return start_year, end_year


def seed_protected_days(dataset: dict, start_year: int, end_year: int) -> Tuple[dict, int]:
    days = dict(dataset.get("days", {}))
    readings = dict(dataset.get("readings", {}))
    inserted_count = 0

    for year in range(start_year, end_year + 1):
        protected_days = build_protected_days_for_year(year)
        for day_iso, entries in protected_days.items():
            if day_iso in days:
                continue
            days[day_iso] = [asdict(entry) for entry in entries]
            inserted_count += 1

    updated_dataset = {
        "version": "1",
        "country_scope": "GR",
        "language": "el",
        "days": dict(sorted(days.items(), key=lambda pair: pair[0])),
        "readings": dict(sorted(readings.items(), key=lambda pair: pair[0])),
    }
    return updated_dataset, inserted_count


def main(argv: List[str]) -> int:
    try:
        start_year, end_year = parse_year_range_args(argv)
        dataset = load_or_initialize_dataset()
        updated_dataset, inserted_count = seed_protected_days(dataset, start_year, end_year)
        write_dataset(updated_dataset)
        print(f"Protected days seeded: {start_year}-{end_year}")
        print(f"Inserted days: {inserted_count}")
    except Exception as ex:  # noqa: BLE001
        print(f"ERROR: {ex}")
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
