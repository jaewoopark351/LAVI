#20260909_kpopmodder: Decode bounded busy identity only from the canonical precheck status mapping.
from __future__ import annotations

from collections.abc import Mapping

from .command_busy_observed_identity import CommandBusyObservedIdentity


class CommandBusyObservedIdentityFactory:
    def create(self, result_mapping: object) -> CommandBusyObservedIdentity | None:
        try:
            if not isinstance(result_mapping, Mapping):
                return None
            status = result_mapping.get("status")
            if not isinstance(status, Mapping):
                return None
            details = status.get("details")
            if not isinstance(details, Mapping):
                return None
            commands = details.get("commands")
            if not isinstance(commands, Mapping):
                return None
            return CommandBusyObservedIdentity(
                active_session_id=commands.get("active_session_id"),
                active_generation=commands.get("active_generation"),
                active_request_id=commands.get("active_request_id"),
                active_command_message_id=commands.get(
                    "active_command_message_id"
                ),
            )
        except Exception:
            return None


__all__ = ("CommandBusyObservedIdentityFactory",)
