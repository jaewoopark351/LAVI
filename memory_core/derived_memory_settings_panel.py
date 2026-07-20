#20260720_kpopmodder: Added Setting tab controls for manual derived-memory rebuilds.
from core.logger import log_print
from memory_core.derived_memory_rebuild_service import (
    DerivedMemoryRebuildService,
)


class DerivedMemorySettingsPanel:
    #20260720_kpopmodder: Keep manual derived-memory maintenance out of AppComposer.
    def __init__(self, memory_store=None, memory_context_builder=None):
        self.memory_store = memory_store
        self.memory_context_builder = memory_context_builder
        self.status_box = None
        self.rebuild_button = None

    def create_ui(self):
        import gradio as gr

        with gr.Tab("Memory"):
            with gr.Accordion("Derived Memory", open=True):
                self.status_box = gr.Textbox(
                    label="Status",
                    value="Idle.",
                    lines=5,
                    interactive=False,
                )
                self.rebuild_button = gr.Button(
                    "Rebuild Derived Memory",
                    variant="primary",
                )

            self.rebuild_button.click(
                self.rebuild_derived_memory,
                outputs=self.status_box,
            )

    def get_status(self):
        derived_store = self._derived_store()
        if self.memory_store is None:
            return "Memory store is not available."
        if derived_store is None:
            return "Derived memory store is not available."

        try:
            stats = DerivedMemoryRebuildService.get_refreshed_stats(
                derived_store,
                self.memory_store,
            )
        except Exception as exc:
            return f"Derived memory status check failed: {exc}"

        return self._format_status(
            "Derived memory ready.",
            stats,
            getattr(derived_store, "db_path", ""),
        )

    def rebuild_derived_memory(self):
        derived_store = self._derived_store()
        if self.memory_store is None:
            return "Memory store is not available."
        if derived_store is None:
            return "Derived memory store is not available."

        try:
            rebuild_result = DerivedMemoryRebuildService.rebuild_now_exclusive(
                derived_store,
                self.memory_store,
                reason="manual_ui",
            )
            if rebuild_result is None:
                return "Derived memory rebuild is already running."

            rebuild_stats, refreshed_stats = rebuild_result
            return self._format_status(
                "Derived memory rebuilt.",
                refreshed_stats,
                getattr(derived_store, "db_path", ""),
                rebuild_stats=rebuild_stats,
            )
        except Exception as exc:
            log_print(
                "[Memory][Warning] manual derived_memory.sqlite3 rebuild failed: "
                f"{exc}"
            )
            try:
                DerivedMemoryRebuildService.schedule_background_rebuild(
                    derived_store,
                    self.memory_store,
                    reason="manual_ui_error",
                )
                return (
                    "Manual derived memory rebuild failed. "
                    "Background rebuild was scheduled.\n"
                    f"Error: {exc}"
                )
            except Exception as background_exc:
                return (
                    "Manual derived memory rebuild failed, and background "
                    "rebuild could not be scheduled.\n"
                    f"Error: {exc}\n"
                    f"Background error: {background_exc}"
                )

    def _derived_store(self):
        retriever = getattr(
            self.memory_context_builder,
            "memory_retriever",
            None,
        )
        return getattr(retriever, "derived_store", None)

    def _format_status(self, title, stats, db_path, rebuild_stats=None):
        stats = stats or {}
        lines = [
            title,
            f"rows={int(stats.get('row_count', 0) or 0)}",
            f"stale={bool(stats.get('stale', False))}",
        ]
        if rebuild_stats:
            lines.extend([
                f"raw_events={int(rebuild_stats.get('raw_event_count', 0) or 0)}",
                f"inserted={int(rebuild_stats.get('inserted_count', 0) or 0)}",
            ])
        if db_path:
            lines.append(f"db={db_path}")
        return "\n".join(lines)
