#20260905_kpopmodder: Keep trusted Korean route responsibilities split by module.
#20260909_kpopmodder: Project only closed pre-permit contextual-busy failure facts.
from __future__ import annotations

import re

from plugins.Minecraft.fabric.chatclef.command_registry import (
    KoreanChatClefCommandRegistry,
)

from .contextual_busy_response_failure_record import (
    ContextualBusyResponseFailureRecord,
)


class ContextualBusyResponseFailureProjector:
    _STAGES = frozenset(
        {"decision_validation", "identity_freeze", "locked_status_inspection"}
    )
    _LIFECYCLE_STATES = frozenset({"running", "pending", "unavailable", "none"})
    _TERMINAL_STATES = frozenset({"none", "unclaimed", "claimed"})
    _AVAILABILITY_REASONS = frozenset(
        {
            "none",
            "no_tracked_owner",
            "disconnected",
            "quarantined",
            "stale_owner",
            "identity_mismatch",
            "terminal_claimed",
            "evidence_unavailable",
            "unsupported_profile",
            "malformed_observation",
        }
    )
    _EXCEPTION_CLASS = re.compile(r"[A-Za-z_][A-Za-z0-9_]{0,95}\Z", re.ASCII)

    def __init__(self, *, command_names: object = None) -> None:
        names = (
            KoreanChatClefCommandRegistry().command_names()
            if command_names is None
            else command_names
        )
        self._command_names = frozenset(
            name for name in names if type(name) is str and name
        )

    def project(
        self,
        *,
        stage: object,
        snapshot: object = None,
        availability_reason: object = None,
        exception_class: object = "none",
    ) -> ContextualBusyResponseFailureRecord:
        return ContextualBusyResponseFailureRecord(
            stage=self._allowed(stage, self._STAGES),
            busy_reason="minecraft_command_busy",
            active_command_name=self._allowed(
                getattr(snapshot, "command_name", None),
                self._command_names | {"none"},
            ),
            active_lifecycle_state=self._allowed(
                getattr(snapshot, "state", None),
                self._LIFECYCLE_STATES,
            ),
            terminal_state=self._allowed(
                getattr(snapshot, "terminal_state", None),
                self._TERMINAL_STATES,
            ),
            availability_reason=self._allowed(
                availability_reason or getattr(snapshot, "availability_reason", None),
                self._AVAILABILITY_REASONS,
            ),
            exception_class=self._exception(exception_class),
            selected_fallback="generic_busy",
        )

    @staticmethod
    def _allowed(value: object, allowed: object) -> str:
        if value is None or value == "":
            return "none"
        return value if type(value) is str and value in allowed else "invalid"

    @classmethod
    def _exception(cls, value: object) -> str:
        if value == "none":
            return "none"
        if type(value) is str and cls._EXCEPTION_CLASS.fullmatch(value) is not None:
            return value
        return "invalid"


__all__ = ("ContextualBusyResponseFailureProjector",)
