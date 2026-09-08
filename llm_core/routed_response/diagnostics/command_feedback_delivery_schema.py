#20260905_kpopmodder: Keep routed-response diagnostics in the established responsibility-split boundary.
#20260907_kpopmodder: Own closed diagnostic value validation separately from formatting.
from __future__ import annotations

import re


class CommandFeedbackDeliverySchema:
    CANONICAL_FIELDS = (
        "event_id",
        "route_kind",
        "response_kind",
        "delivery_mode",
        "sink",
        "response_generation",
        "delivered",
        "reason",
    )
    DELIVERY_MODES = frozenset({"current_input", "non_preempting"})
    ROUTE_KINDS = frozenset(
        {
            "minecraft_command",
            "minecraft_chatclef",
            "generic_crafting_defaults",
            "stop_control",
            "minecraft_chatclef_external",
            "crafting_lifecycle",
            "crafting_status_query",
            "command_lifecycle",
            "command_status_query",
        }
    )
    RESPONSE_KINDS = frozenset(
        {
            "immediate",
            "external",
            "stop_terminal",
            "crafting_terminal",
            "command_coalesced",
            "command_start",
            "command_status",
            "command_terminal",
        }
    )
    ROUTE_RESPONSE_DELIVERY_IDENTITIES = frozenset(
        {
            ("crafting_lifecycle", "crafting_terminal", "non_preempting"),
            ("crafting_status_query", "immediate", "current_input"),
            ("command_lifecycle", "command_coalesced", "current_input"),
            ("command_lifecycle", "command_start", "current_input"),
            ("command_lifecycle", "command_terminal", "non_preempting"),
            ("command_status_query", "command_status", "current_input"),
            ("stop_control", "command_start", "current_input"),
            ("stop_control", "immediate", "current_input"),
            ("stop_control", "stop_terminal", "non_preempting"),
        }
    )
    LIFECYCLE_ROUTE_KINDS = frozenset(
        route_kind
        for route_kind, _response_kind, _delivery_mode in (
            ROUTE_RESPONSE_DELIVERY_IDENTITIES
        )
    )
    LIFECYCLE_RESPONSE_KINDS = frozenset(
        response_kind
        for _route_kind, response_kind, _delivery_mode in (
            ROUTE_RESPONSE_DELIVERY_IDENTITIES
        )
        if response_kind != "immediate"
    )
    SINKS = frozenset(
        {
            "output_listener",
            "full_output_listener",
            "history",
            "chat_ui",
            "ui_presentation",
            "tts_queue",
            "tts_playback",
        }
    )
    REASONS = frozenset(
        {
            "delivered",
            "yielded",
            "enqueued",
            "played",
            "duplicate_event",
            "disabled",
            "skipped_empty",
            "interrupted",
            "stale",
            "synthesis_failed",
            "playback_failed",
            "authorization_unavailable",
            "authorization_failed",
            "authorization_rejected",
            "generation_failed",
            "delivery_failed",
        }
    )
    _EVENT_ID = re.compile(r"[0-9a-f]{32}\Z", re.ASCII)

    def normalize(self, record: object) -> dict:
        fields = {
            name: getattr(record, name) for name in self.CANONICAL_FIELDS
        }
        if fields["event_id"] != "none" and (
            type(fields["event_id"]) is not str
            or self._EVENT_ID.fullmatch(fields["event_id"]) is None
        ):
            fields["event_id"] = "invalid"
        if fields["route_kind"] not in self.ROUTE_KINDS:
            fields["route_kind"] = "invalid"
        if fields["response_kind"] not in self.RESPONSE_KINDS:
            fields["response_kind"] = "invalid"
        if fields["delivery_mode"] not in self.DELIVERY_MODES:
            fields["delivery_mode"] = "invalid"
        identity = (
            fields["route_kind"],
            fields["response_kind"],
            fields["delivery_mode"],
        )
        if (
            fields["route_kind"] in self.LIFECYCLE_ROUTE_KINDS
            or fields["response_kind"] in self.LIFECYCLE_RESPONSE_KINDS
        ) and identity not in self.ROUTE_RESPONSE_DELIVERY_IDENTITIES:
            fields["route_kind"] = "invalid"
            fields["response_kind"] = "invalid"
            fields["delivery_mode"] = "invalid"
        if fields["sink"] not in self.SINKS:
            fields["sink"] = "invalid"
        if fields["response_generation"] is not None and (
            type(fields["response_generation"]) is not int
            or fields["response_generation"] < 0
            or fields["response_generation"] >= 2**63
        ):
            fields["response_generation"] = "invalid"
        if type(fields["delivered"]) is not bool:
            fields["delivered"] = "invalid"
        if fields["reason"] not in self.REASONS:
            fields["reason"] = "invalid"
        return fields


__all__ = ("CommandFeedbackDeliverySchema",)
