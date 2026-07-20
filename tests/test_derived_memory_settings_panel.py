#20260720_kpopmodder: Cover manual derived-memory Setting panel actions.
import unittest
from unittest import mock

from memory_core.derived_memory_settings_panel import DerivedMemorySettingsPanel


class DerivedMemorySettingsPanelTests(unittest.TestCase):
    def test_rebuild_derived_memory_reports_manual_rebuild_result(self):
        panel = self._panel_with_store()

        with mock.patch(
            "memory_core.derived_memory_settings_panel."
            "DerivedMemoryRebuildService.rebuild_now_exclusive",
            return_value=(
                {"raw_event_count": 7, "inserted_count": 3},
                {"row_count": 3, "stale": False},
            ),
        ) as rebuild_mock:
            status = panel.rebuild_derived_memory()

        rebuild_mock.assert_called_once_with(
            panel._derived_store(),
            panel.memory_store,
            reason="manual_ui",
        )
        self.assertIn("Derived memory rebuilt.", status)
        self.assertIn("rows=3", status)
        self.assertIn("stale=False", status)
        self.assertIn("raw_events=7", status)
        self.assertIn("inserted=3", status)
        self.assertIn("memory/derived_memory.sqlite3", status)

    def test_rebuild_derived_memory_reports_already_running(self):
        panel = self._panel_with_store()

        with mock.patch(
            "memory_core.derived_memory_settings_panel."
            "DerivedMemoryRebuildService.rebuild_now_exclusive",
            return_value=None,
        ):
            status = panel.rebuild_derived_memory()

        self.assertEqual("Derived memory rebuild is already running.", status)

    def test_rebuild_derived_memory_schedules_background_after_manual_failure(self):
        panel = self._panel_with_store()

        with mock.patch(
            "memory_core.derived_memory_settings_panel."
            "DerivedMemoryRebuildService.rebuild_now_exclusive",
            side_effect=RuntimeError("boom"),
        ):
            with mock.patch(
                "memory_core.derived_memory_settings_panel."
                "DerivedMemoryRebuildService.schedule_background_rebuild",
            ) as background_mock:
                status = panel.rebuild_derived_memory()

        background_mock.assert_called_once_with(
            panel._derived_store(),
            panel.memory_store,
            reason="manual_ui_error",
        )
        self.assertIn("Background rebuild was scheduled.", status)
        self.assertIn("boom", status)

    def test_get_status_reports_missing_derived_store(self):
        panel = DerivedMemorySettingsPanel(
            memory_store=object(),
            memory_context_builder=object(),
        )

        self.assertEqual(
            "Derived memory store is not available.",
            panel.get_status(),
        )

    def test_get_status_formats_current_stats(self):
        panel = self._panel_with_store()

        with mock.patch(
            "memory_core.derived_memory_settings_panel."
            "DerivedMemoryRebuildService.get_refreshed_stats",
            return_value={"row_count": 5, "stale": True},
        ) as stats_mock:
            status = panel.get_status()

        stats_mock.assert_called_once_with(
            panel._derived_store(),
            panel.memory_store,
        )
        self.assertIn("Derived memory ready.", status)
        self.assertIn("rows=5", status)
        self.assertIn("stale=True", status)

    def _panel_with_store(self):
        derived_store = _FakeDerivedStore()
        retriever = _FakeRetriever(derived_store)
        memory_context_builder = _FakeMemoryContextBuilder(retriever)
        return DerivedMemorySettingsPanel(
            memory_store=object(),
            memory_context_builder=memory_context_builder,
        )


class _FakeDerivedStore:
    db_path = "memory/derived_memory.sqlite3"


class _FakeRetriever:
    def __init__(self, derived_store):
        self.derived_store = derived_store


class _FakeMemoryContextBuilder:
    def __init__(self, memory_retriever):
        self.memory_retriever = memory_retriever


if __name__ == "__main__":
    unittest.main()
