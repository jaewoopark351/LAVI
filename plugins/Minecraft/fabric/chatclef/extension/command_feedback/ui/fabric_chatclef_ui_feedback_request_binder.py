#20260907_kpopmodder: Bind one immutable GUI event projection without mutating the caller request.
from __future__ import annotations

from typing import Mapping

from input_core.input_event.contracts.lavi_input_event import LaviInputEvent


class FabricChatClefUiFeedbackRequestBinder:
    _EVENT_FIELDS = (
        "source",
        "provider_id",
        "event_kind",
        "final",
        "event_id",
    )

    def bind(
        self,
        request: object,
        *,
        input_event: object,
        text_key: str,
        expected_source: str,
    ) -> dict | None:
        if (
            not isinstance(request, Mapping)
            or type(input_event) is not LaviInputEvent
            or text_key not in {"command", "text"}
            or input_event.source != expected_source
            or input_event.final is not True
            or request.get("source") != expected_source
            or request.get(text_key) != input_event.text
        ):
            return None
        payload = dict(request)
        metadata_value = payload.get("metadata")
        metadata = dict(metadata_value) if isinstance(metadata_value, Mapping) else {}
        metadata["input_event"] = {
            field: getattr(input_event, field) for field in self._EVENT_FIELDS
        }
        payload["metadata"] = metadata
        return payload


__all__ = ("FabricChatClefUiFeedbackRequestBinder",)
