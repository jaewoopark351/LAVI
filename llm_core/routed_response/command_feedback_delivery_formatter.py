#20260905_kpopmodder: Format the exact canonical bounded feedback-delivery fields.
from __future__ import annotations

import re

from .command_feedback_delivery_record import CommandFeedbackDeliveryRecord


class CommandFeedbackDeliveryFormatter:
    EVENT_NAME = "command_feedback_delivery"
    CANONICAL_FIELDS = (
        "event_id",
        "route_kind",
        "response_kind",
        "sink",
        "response_generation",
        "delivered",
        "reason",
    )
    _EVENT_ID = re.compile(r"[0-9a-f]{32}\Z", re.ASCII)
    _ROUTE_KINDS = frozenset(
        {
            "minecraft_command",
            "minecraft_chatclef",
            "generic_crafting_defaults",
            "stop_control",
            "minecraft_chatclef_external",
        }
    )
    _RESPONSE_KINDS = frozenset({"immediate", "external", "stop_terminal"})
    _SINKS = frozenset(
        {"output_listener", "full_output_listener", "history", "chat_ui"}
    )
    _REASONS = frozenset(
        {
            "delivered",
            "yielded",
            "authorization_unavailable",
            "authorization_failed",
            "authorization_rejected",
            "generation_failed",
            "delivery_failed",
        }
    )

    def format(self, record: object) -> str:
        if type(record) is not CommandFeedbackDeliveryRecord:
            raise TypeError("feedback delivery record must be exact")
        fields = {name: getattr(record, name) for name in self.CANONICAL_FIELDS}
        if fields["event_id"] != "none" and (
            type(fields["event_id"]) is not str
            or self._EVENT_ID.fullmatch(fields["event_id"]) is None
        ):
            fields["event_id"] = "invalid"
        if fields["route_kind"] not in self._ROUTE_KINDS:
            fields["route_kind"] = "invalid"
        if fields["response_kind"] not in self._RESPONSE_KINDS:
            fields["response_kind"] = "invalid"
        if fields["sink"] not in self._SINKS:
            fields["sink"] = "invalid"
        if fields["response_generation"] is not None and (
            type(fields["response_generation"]) is not int
            or fields["response_generation"] < 0
            or fields["response_generation"] >= 2**63
        ):
            fields["response_generation"] = "invalid"
        if type(fields["delivered"]) is not bool:
            fields["delivered"] = "invalid"
        if fields["reason"] not in self._REASONS:
            fields["reason"] = "invalid"
        return f"event={self.EVENT_NAME} " + " ".join(
            f"{name}={_bounded_atom(fields[name])}" for name in self.CANONICAL_FIELDS
        )


def _bounded_atom(value: object) -> str:
    if value is None or value == "":
        return "none"
    if type(value) is bool:
        return "true" if value else "false"
    if type(value) is int:
        return str(value) if 0 <= value < 2**63 else "invalid"
    if type(value) is not str or len(value) > 160:
        return "invalid"
    if any(character.isspace() or character == "=" for character in value):
        return "invalid"
    return value


__all__ = ("CommandFeedbackDeliveryFormatter",)
