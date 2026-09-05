#20260905_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations

from typing import Mapping


class StopControlTerminalTargetValidator:
    EMPTY_IDENTITY = (None, None, None, None)

    def valid_declared_target(
        self,
        data: Mapping[str, object],
        tracker: object,
    ) -> bool:
        scope = data.get("target_scope")
        requested = self.target_tuple(data, "requested_target_")
        target = tracker.target
        if scope != tracker.target_scope:
            return False
        if scope == "tracked_command":
            return target is not None and requested == self.tracker_target(target)
        if scope == "current_global_automation":
            return target is None and requested == self.EMPTY_IDENTITY
        return False

    def valid_resolved_target(
        self,
        data: Mapping[str, object],
        tracker: object,
    ) -> bool:
        resolution = data.get("target_resolution")
        resolved = self.target_tuple(data, "resolved_target_")
        if resolution in frozenset({"not_evaluated", "none", "unknown"}):
            return resolved == self.EMPTY_IDENTITY
        if resolution == "exact":
            target = tracker.target
            return target is not None and resolved == self.tracker_target(target)
        if resolution != "captured_current" or not self.complete_identity(resolved):
            return False
        identity = tracker.identity
        if (
            resolved[2] != identity.session_id
            or resolved[3] != identity.server_connection_generation
        ):
            return False
        target = tracker.target
        return target is None or resolved != self.tracker_target(target)

    def target_tuple(
        self,
        data: Mapping[str, object],
        prefix: str,
    ) -> tuple[object, object, object, object]:
        return (
            data.get(prefix + "request_id"),
            data.get(prefix + "command_message_id"),
            data.get(prefix + "session_id"),
            data.get(prefix + "server_connection_generation"),
        )

    def tracker_target(self, target: object) -> tuple[object, ...]:
        return (
            target.request_id,
            target.command_message_id,
            target.session_id,
            target.server_connection_generation,
        )

    def identity_tuple_shape(self, value: tuple[object, ...]) -> bool:
        return value == self.EMPTY_IDENTITY or self.complete_identity(value)

    def complete_identity(self, value: tuple[object, ...]) -> bool:
        return (
            all(type(part) is str and bool(part) for part in value[:3])
            and type(value[3]) is int
            and value[3] > 0
        )


__all__ = ("StopControlTerminalTargetValidator",)
