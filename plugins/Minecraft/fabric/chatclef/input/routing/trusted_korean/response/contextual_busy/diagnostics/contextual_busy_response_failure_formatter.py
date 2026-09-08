#20260905_kpopmodder: Keep trusted Korean route responsibilities split by module.
#20260909_kpopmodder: Format one already projected contextual-busy failure record.
from __future__ import annotations

from .contextual_busy_response_failure_record import (
    ContextualBusyResponseFailureRecord,
)


class ContextualBusyResponseFailureFormatter:
    EVENT_NAME = "contextual_busy_response_failure"

    def format(self, record: object) -> str:
        if type(record) is not ContextualBusyResponseFailureRecord:
            raise TypeError("contextual busy failure record must be exact")
        values = (
            ("stage", record.stage),
            ("busy_reason", record.busy_reason),
            ("active_command_name", record.active_command_name),
            ("active_lifecycle_state", record.active_lifecycle_state),
            ("terminal_state", record.terminal_state),
            ("availability_reason", record.availability_reason),
            ("exception_class", record.exception_class),
            ("selected_fallback", record.selected_fallback),
        )
        return f"event={self.EVENT_NAME} " + " ".join(
            f"{name}={value}" for name, value in values
        )


__all__ = ("ContextualBusyResponseFailureFormatter",)
