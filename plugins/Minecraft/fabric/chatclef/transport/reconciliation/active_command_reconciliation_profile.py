#20260820_kpopmodder: Keep command-specific reconciliation profile diagnostics separate from lifecycle evidence.
from __future__ import annotations

from typing import Any

from .reconciliation_mapping_utils import lower_text


class ActiveCommandReconciliationProfileResolver:
    NEUTRAL_ROOT_CLASSES = {
        "adris.altoclef.tasks.movement.IdleTask",
    }

    def profile_for(self, active_command: Any) -> dict[str, Any]:
        command_name = self._command_name(active_command)
        if command_name == "deposit":
            return {
                "command_name": command_name,
                "reconciliation_profile": "deposit",
                "profile_source": "python_diagnostic_static",
                "auto_reconcile_to_unknown_allowed": True,
                "task_binding_expected": True,
                "persistent_command": False,
                "neutral_root_classes": sorted(self.NEUTRAL_ROOT_CLASSES),
                "expected_terminal_evidence": [
                    "finish_callback_received",
                    "task_finished_event_absence",
                    "stable_request_quiescence",
                    "exact_active_identity",
                ],
            }
        return {
            "command_name": command_name or "unknown",
            "reconciliation_profile": "unconfigured",
            "profile_source": "python_diagnostic_default",
            "auto_reconcile_to_unknown_allowed": False,
            "task_binding_expected": True,
            "persistent_command": False,
            "neutral_root_classes": sorted(self.NEUTRAL_ROOT_CLASSES),
            "expected_terminal_evidence": [
                "explicit_terminal_result",
            ],
        }

    def _command_name(self, active_command: Any) -> str:
        text = lower_text(active_command) or ""
        while text.startswith("@") or text.startswith("/"):
            text = text[1:].strip()
        return text.split()[0] if text else ""
