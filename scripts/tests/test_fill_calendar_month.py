import importlib.util
import sys
import unittest
from pathlib import Path
from unittest.mock import patch

SCRIPT_PATH = Path(__file__).resolve().parents[1] / "fill_calendar_month.py"
SPEC = importlib.util.spec_from_file_location("fill_calendar_month", SCRIPT_PATH)
MODULE = importlib.util.module_from_spec(SPEC)
assert SPEC and SPEC.loader
sys.modules[SPEC.name] = MODULE
SPEC.loader.exec_module(MODULE)

SEED_SCRIPT_PATH = Path(__file__).resolve().parents[1] / "seed_protected_days.py"
SEED_SPEC = importlib.util.spec_from_file_location("seed_protected_days", SEED_SCRIPT_PATH)
SEED_MODULE = importlib.util.module_from_spec(SEED_SPEC)
assert SEED_SPEC and SEED_SPEC.loader
sys.modules[SEED_SPEC.name] = SEED_MODULE
SEED_SPEC.loader.exec_module(SEED_MODULE)


class FillCalendarMonthTest(unittest.TestCase):
    def test_parse_target_arg_valid_formats(self):
        self.assertEqual((2025, 1), MODULE.parse_target_arg("2025-01"))
        self.assertEqual((2025, 1), MODULE.parse_target_arg("01-01-2025"))

    def test_parse_target_arg_invalid_month(self):
        with self.assertRaises(ValueError):
            MODULE.parse_target_arg("2025-13")

    def test_extract_day_id_mapping_parses_rows(self):
        html = """
        <div class="w3-fifth w3-container">&nbsp;01/01/2025&nbsp;-&nbsp;</div>
        <div class="w3-fourfifth w3-container"><a href="16077/readingsday.aspx">x</a></div>
        <div class="w3-fifth w3-container">&nbsp;<span class="w3-text-red">02/01/2025</span>&nbsp;-&nbsp;</div>
        <div class="w3-fourfifth w3-container"><a href="16078/readingsday.aspx">y</a></div>
        """
        mapping = MODULE.extract_day_id_mapping(html)
        self.assertEqual("16077", mapping["2025-01-01"])
        self.assertEqual("16078", mapping["2025-01-02"])

    def test_extract_reference_text_pairs_splits_multiple_readings(self):
        content = """
        <div class="w3-justify">
          <div class="w3-center"><strong>ΠΡΟΣ ΡΩΜΑΙΟΥΣ Α´ 1 - 2</strong></div><br /><br />
          Κείμενο πρώτο.<br /><br />
          <div class="w3-center"><strong>ΠΡΟΣ ΚΟΡΙΝΘΙΟΥΣ Β´ 3 - 4</strong></div><br /><br />
          Κείμενο δεύτερο.
        </div>
        """
        pairs = MODULE.extract_reference_text_pairs(content)
        self.assertEqual(2, len(pairs))
        self.assertEqual("ΠΡΟΣ ΡΩΜΑΙΟΥΣ Α´ 1 - 2", pairs[0][0])
        self.assertIn("Κείμενο πρώτο", pairs[0][1])
        self.assertEqual("ΠΡΟΣ ΚΟΡΙΝΘΙΟΥΣ Β´ 3 - 4", pairs[1][0])

    def test_resolve_modern_text_chain(self):
        modern, provider = MODULE.resolve_modern_text(
            reference="Κατά Ιωάννην 1:1-17",
            ancient_text="ancient",
            modern_provider_1=" provider1 text ",
            modern_provider_2="provider2 text",
        )
        self.assertEqual("provider-1", provider)
        self.assertEqual("provider1 text", modern)

        modern2, provider2 = MODULE.resolve_modern_text(
            reference="Κατά Ιωάννην 1:1-17",
            ancient_text="ancient",
            modern_provider_1=None,
            modern_provider_2="provider2 text",
        )
        self.assertEqual("provider-2", provider2)
        self.assertEqual("provider2 text", modern2)

        modern3, provider3 = MODULE.resolve_modern_text(
            reference="Προς Εβραίους 1:1-2",
            ancient_text="Αρχαίο κείμενο",
        )
        self.assertEqual("generated", provider3)
        self.assertIn("Νεοελληνική απόδοση", modern3)

    def test_parse_day_readings_comment_only_day(self):
        html = """
        <div class="w3-row-padding">
          <a href="javascript:void(0)" onclick="displayTab(event, 'gospel', 'tablink', 'gID5', 'orange');">
            <div class="tablink">Σχόλιο</div>
          </a>
        </div>
        <div id="gID5" class="w3-container gospel" style="display:block">
          <div class="w3-justify"><strong>Σήμερον δὲν τελεῖται θεία λειτουργία.</strong></div>
        </div>
        """
        day_readings, counters, has_liturgy = MODULE.parse_day_readings("2025-01-03", html)
        self.assertFalse(has_liturgy)
        self.assertEqual([], day_readings["apostle"])
        self.assertEqual([], day_readings["gospel"])
        self.assertEqual(0, sum(counters.values()))

    def test_build_protected_days_for_year_includes_movable_holidays(self):
        protected_2025 = MODULE.build_protected_days_for_year(2025)
        protected_2026 = MODULE.build_protected_days_for_year(2026)

        self.assertEqual("Δευτέρα του Πάσχα", protected_2025["2025-04-21"][0].title)
        self.assertEqual("Δευτέρα του Πάσχα", protected_2026["2026-04-13"][0].title)
        self.assertIn(
            "Ευαγγελισμός της Θεοτόκου",
            [entry.title for entry in protected_2026["2026-03-25"]],
        )

    def test_overwrite_month_touches_only_target_month(self):
        dataset = {
            "version": "1",
            "country_scope": "GR",
            "language": "el",
            "days": {
                "2025-01-02": [
                    {
                        "title": "old jan 2",
                        "type": "normal_day",
                        "is_official_non_working": False,
                        "is_half_day": False,
                        "description": "old",
                        "priority": 1000,
                    }
                ],
                "2025-02-01": [
                    {
                        "title": "keep feb",
                        "type": "normal_day",
                        "is_official_non_working": False,
                        "is_half_day": False,
                        "description": "keep",
                        "priority": 1000,
                    }
                ],
            },
            "readings": {
                "2025-01-02": {"apostle": [], "gospel": []},
                "2025-02-01": {
                    "apostle": [
                        {
                            "id": "feb-1",
                            "reference": "ref",
                            "text_ancient": "anc",
                            "text_modern": "mod",
                        }
                    ],
                    "gospel": [],
                },
            },
        }
        fake_month = MODULE.ParsedMonthReadings(
            readings_by_date={
                "2025-01-02": {
                    "apostle": [
                        {
                            "id": "2025-01-02-apostle-1",
                            "reference": "ΠΡΟΣ ΡΩΜΑΙΟΥΣ Α´ 1 - 2",
                            "text_ancient": "Ancient",
                            "text_modern": "Modern",
                        }
                    ],
                    "gospel": [],
                }
            },
            provider_counters={"provider-1": 1, "provider-2": 0, "provider-3": 0, "generated": 0},
            days_parsed=1,
            apostle_count=1,
            gospel_count=0,
            days_with_feast_extra_readings=0,
        )

        with patch.object(MODULE, "build_month_readings", return_value=fake_month):
            updated, parsed = MODULE.overwrite_month(dataset, 2025, 1)

        self.assertEqual("keep feb", updated["days"]["2025-02-01"][0]["title"])
        self.assertEqual("Δεν υπάρχει καταχωρημένη εορτή", updated["days"]["2025-01-02"][0]["title"])
        self.assertEqual("feb-1", updated["readings"]["2025-02-01"]["apostle"][0]["id"])
        self.assertEqual(1, parsed.provider_counters["provider-1"])

    def test_overwrite_month_keeps_existing_protected_day_without_overwrite(self):
        dataset = {
            "version": "1",
            "country_scope": "GR",
            "language": "el",
            "days": {
                "2025-03-25": [
                    {
                        "title": "LOCKED CUSTOM",
                        "type": "religious_observance",
                        "is_official_non_working": False,
                        "is_half_day": False,
                        "description": "custom",
                        "priority": 99,
                    }
                ]
            },
            "readings": {},
        }
        fake_month = MODULE.ParsedMonthReadings(
            readings_by_date={},
            provider_counters={"provider-1": 0, "provider-2": 0, "provider-3": 0, "generated": 0},
            days_parsed=0,
            apostle_count=0,
            gospel_count=0,
            days_with_feast_extra_readings=0,
        )

        with patch.object(MODULE, "build_month_readings", return_value=fake_month):
            updated, _ = MODULE.overwrite_month(dataset, 2025, 3)

        self.assertEqual("LOCKED CUSTOM", updated["days"]["2025-03-25"][0]["title"])

    def test_overwrite_month_inserts_missing_protected_day(self):
        dataset = {
            "version": "1",
            "country_scope": "GR",
            "language": "el",
            "days": {},
            "readings": {},
        }
        fake_month = MODULE.ParsedMonthReadings(
            readings_by_date={},
            provider_counters={"provider-1": 0, "provider-2": 0, "provider-3": 0, "generated": 0},
            days_parsed=0,
            apostle_count=0,
            gospel_count=0,
            days_with_feast_extra_readings=0,
        )

        with patch.object(MODULE, "build_month_readings", return_value=fake_month):
            updated, _ = MODULE.overwrite_month(dataset, 2026, 4)

        public_titles = [entry["title"] for entry in updated["days"]["2026-04-13"] if entry["type"] == "public_holiday"]
        self.assertIn("Δευτέρα του Πάσχα", public_titles)

    def test_validate_dataset_rejects_forbidden_source_fields(self):
        dataset = {
            "version": "1",
            "country_scope": "GR",
            "language": "el",
            "days": {
                "2025-01-01": [
                    {
                        "title": "x",
                        "type": "normal_day",
                        "is_official_non_working": False,
                        "is_half_day": False,
                        "description": "d",
                        "priority": 1,
                    }
                ]
            },
            "readings": {
                "2025-01-01": {
                    "apostle": [
                        {
                            "id": "1",
                            "reference": "ΠΡΟΣ ΡΩΜΑΙΟΥΣ Α´ 1 - 2",
                            "text_ancient": "a",
                            "text_modern": "m",
                            "source_url": "https://example.com",
                        }
                    ],
                    "gospel": [],
                }
            },
        }

        with self.assertRaises(ValueError):
            MODULE.validate_dataset(dataset, 2025, 1)

    def test_build_month_readings_allows_trailing_missing_day_mapping(self):
        year_html = """
        <div class="w3-fifth w3-container">&nbsp;01/01/2025&nbsp;-&nbsp;</div>
        <div class="w3-fourfifth w3-container"><a href="16077/readingsday.aspx">x</a></div>
        <div class="w3-fifth w3-container">&nbsp;02/01/2025&nbsp;-&nbsp;</div>
        <div class="w3-fourfifth w3-container"><a href="16078/readingsday.aspx">y</a></div>
        """

        with patch.object(MODULE, "fetch_readings_year_page", return_value=year_html), patch.object(
            MODULE, "fetch_readings_day_page", return_value="<html></html>"
        ), patch.object(
            MODULE,
            "parse_day_readings",
            return_value=({"apostle": [], "gospel": []}, {"provider-1": 0, "provider-2": 0, "provider-3": 0, "generated": 0}, False),
        ):
            parsed = MODULE.build_month_readings(2025, 1)

        self.assertEqual(31, len(parsed.readings_by_date))
        self.assertEqual(2, parsed.days_parsed)
        self.assertEqual({"apostle": [], "gospel": []}, parsed.readings_by_date["2025-01-03"])
        self.assertEqual({"apostle": [], "gospel": []}, parsed.readings_by_date["2025-01-31"])

    def test_build_month_readings_rejects_non_trailing_missing_day_mapping(self):
        year_html = """
        <div class="w3-fifth w3-container">&nbsp;01/01/2025&nbsp;-&nbsp;</div>
        <div class="w3-fourfifth w3-container"><a href="16077/readingsday.aspx">x</a></div>
        <div class="w3-fifth w3-container">&nbsp;03/01/2025&nbsp;-&nbsp;</div>
        <div class="w3-fourfifth w3-container"><a href="16079/readingsday.aspx">z</a></div>
        """

        with patch.object(MODULE, "fetch_readings_year_page", return_value=year_html):
            with self.assertRaises(ValueError):
                MODULE.build_month_readings(2025, 1)

    def test_seed_protected_days_insert_only_without_overwrite(self):
        dataset = {
            "version": "1",
            "country_scope": "GR",
            "language": "el",
            "days": {
                "2025-01-01": [
                    {
                        "title": "LOCKED EXISTING",
                        "type": "religious_observance",
                        "is_official_non_working": False,
                        "is_half_day": False,
                        "description": "custom",
                        "priority": 999,
                    }
                ]
            },
            "readings": {},
        }

        updated, inserted = SEED_MODULE.seed_protected_days(dataset, 2025, 2026)

        self.assertEqual("LOCKED EXISTING", updated["days"]["2025-01-01"][0]["title"])
        self.assertGreater(inserted, 0)
        self.assertIn("2026-04-13", updated["days"])
        self.assertIn("Δευτέρα του Πάσχα", [entry["title"] for entry in updated["days"]["2026-04-13"]])


if __name__ == "__main__":
    unittest.main()
