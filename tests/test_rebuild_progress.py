#20260720_kpopmodder: Verify terminal download-style rebuild progress formatting.
import unittest

from memory_core.rebuild_progress import format_rebuild_progress_bar


class RebuildProgressTests(unittest.TestCase):
    def test_format_rebuild_progress_bar_shows_percent_and_counts(self):
        line = format_rebuild_progress_bar(
            "manual_ui",
            "upsert_items",
            {"current": 5, "total": 10, "inserted": 4},
            width=10,
        )

        self.assertIn("[#####-----]", line)
        self.assertIn("50.00%", line)
        self.assertIn("5/10", line)
        self.assertIn("stage=upsert_items", line)
        self.assertIn("reason=manual_ui", line)
        self.assertIn("inserted=4", line)

    def test_format_rebuild_progress_bar_handles_unknown_total(self):
        line = format_rebuild_progress_bar(
            "startup_error",
            "collect_raw_events",
            {"current": 1000},
            width=10,
        )

        self.assertIn("[----------]", line)
        self.assertIn("--.--%", line)
        self.assertIn("1000/?", line)


if __name__ == "__main__":
    unittest.main()
