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

    def test_overwrite_month_touches_only_target_month(self):
        dataset = {
            "version": "1",
            "country_scope": "GR",
            "language": "el",
            "days": {
                "2025-01-01": [
                    {
                        "title": "old jan",
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
                "2025-01-01": {"apostle": [], "gospel": []},
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

        fake_days = {
            "2025-01-01": [
                {
                    "title": "new jan",
                    "type": "normal_day",
                    "is_official_non_working": False,
                    "is_half_day": False,
                    "description": "new",
                    "priority": 1000,
                }
            ]
        }
        fake_month = MODULE.ParsedMonthReadings(
            readings_by_date={
                "2025-01-01": {
                    "apostle": [
                        {
                            "id": "2025-01-01-apostle-1",
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

        with patch.object(MODULE, "build_month_entries", return_value=fake_days), patch.object(
            MODULE, "build_month_readings", return_value=fake_month
        ):
            updated, parsed = MODULE.overwrite_month(dataset, 2025, 1)

        self.assertEqual("keep feb", updated["days"]["2025-02-01"][0]["title"])
        self.assertEqual("new jan", updated["days"]["2025-01-01"][0]["title"])
        self.assertEqual("feb-1", updated["readings"]["2025-02-01"]["apostle"][0]["id"])
        self.assertEqual(1, parsed.provider_counters["provider-1"])

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


if __name__ == "__main__":
    unittest.main()
