#20260908_kpopmodder: Report only bounded evaluator-failure facts without terminal payload contents.
from __future__ import annotations

import re

from plugins.Minecraft.fabric.chatclef.command_registry import (
    KoreanChatClefCommandRegistry,
)


class CommandTerminalEvidenceFailureReporter:
    _EXCEPTION_TYPE = re.compile(r"[A-Za-z_][A-Za-z0-9_]{0,63}\Z", re.ASCII)
    _TERMINAL_STATUSES = frozenset(
        {
            "completed",
            "rejected",
            "failed",
            "cancelled",
            "deadline_exceeded",
            "unknown",
        }
    )

    def __init__(self, diagnostics=None, *, command_registry=None) -> None:
        self._diagnostics = diagnostics
        registry = command_registry or KoreanChatClefCommandRegistry()
        self._command_names = frozenset(registry.command_names())

    def report(
        self,
        *,
        command_name: object,
        status: object,
        error: Exception,
    ) -> None:
        if self._diagnostics is None:
            return
        name = str(command_name or "").strip().lower()
        if name not in self._command_names:
            name = "unknown"
        terminal_status = str(
            getattr(status, "value", status) or ""
        ).strip().lower()
        if terminal_status not in self._TERMINAL_STATUSES:
            terminal_status = "unknown"
        exception_type = type(error).__name__
        if self._EXCEPTION_TYPE.fullmatch(exception_type) is None:
            exception_type = "unknown"
        try:
            self._diagnostics.warning(
                "event=command_terminal_evidence_evaluation_failed "
                f"command_name={name} "
                f"status={terminal_status} "
                f"exception_type={exception_type} "
                "fallback=unverified_cautious"
            )
        except Exception:
            pass


__all__ = ("CommandTerminalEvidenceFailureReporter",)
