#20260908_kpopmodder: Format the exact bounded status-route failure field set.
from __future__ import annotations

from .command_status_route_failure_record import CommandStatusRouteFailureRecord


class CommandStatusRouteFailureFormatter:
    EVENT_NAME = "command_status_route_failure"
    CANONICAL_FIELDS = (
        "stage",
        "query_kind",
        "addressed",
        "requested_family",
        "active_command_name",
        "lifecycle_state",
        "terminal_state",
        "availability_reason",
        "exception_class",
    )

    def format(self, record: object) -> str:
        if type(record) is not CommandStatusRouteFailureRecord:
            raise TypeError("command status failure record must be exact")
        return f"event={self.EVENT_NAME} " + " ".join(
            f"{field}={self._atom(getattr(record, field))}"
            for field in self.CANONICAL_FIELDS
        )

    @staticmethod
    def _atom(value: object) -> str:
        if type(value) is bool:
            return "true" if value else "false"
        if type(value) is str and value and len(value) <= 160:
            if not any(character.isspace() or character == "=" for character in value):
                return value
        return "invalid"


__all__ = ("CommandStatusRouteFailureFormatter",)
