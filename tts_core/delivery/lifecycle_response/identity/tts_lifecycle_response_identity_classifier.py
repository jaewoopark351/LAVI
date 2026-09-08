#20260907_kpopmodder: Classify only the closed lifecycle TTS payload matrix.
from __future__ import annotations

from .tts_lifecycle_response_identity import TtsLifecycleResponseIdentity


class TtsLifecycleResponseIdentityClassifier:
    _ALLOWED_IDENTITIES = frozenset(
        {
            (
                "crafting_lifecycle",
                "crafting_terminal",
                "non_preempting",
            ),
            ("crafting_status_query", "immediate", "current_input"),
            (
                "command_lifecycle",
                "command_coalesced",
                "current_input",
            ),
            ("command_lifecycle", "command_start", "current_input"),
            (
                "command_lifecycle",
                "command_terminal",
                "non_preempting",
            ),
            ("command_status_query", "command_status", "current_input"),
            (
                "command_busy_current_work",
                "command_status",
                "current_input",
            ),
            ("stop_control", "command_start", "current_input"),
            ("stop_control", "stop_terminal", "non_preempting"),
        }
    )

    def classify(self, payload: object) -> TtsLifecycleResponseIdentity | None:
        if not isinstance(payload, dict):
            return None
        if payload.get("source") != "minecraft_chatclef":
            return None
        values = (
            payload.get("event_id"),
            payload.get("route_kind"),
            payload.get("response_kind"),
            payload.get("delivery_mode"),
        )
        if any(type(value) is not str or not value for value in values):
            return None
        event_id, route_kind, response_kind, delivery_mode = values
        if (
            route_kind,
            response_kind,
            delivery_mode,
        ) not in self._ALLOWED_IDENTITIES:
            return None
        return TtsLifecycleResponseIdentity(
            event_id=event_id,
            route_kind=route_kind,
            response_kind=response_kind,
            delivery_mode=delivery_mode,
        )


__all__ = ("TtsLifecycleResponseIdentityClassifier",)
