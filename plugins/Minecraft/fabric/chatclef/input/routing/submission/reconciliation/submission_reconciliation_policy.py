#20260819_kpopmodder: Decide UNKNOWN retention and matching terminal reconciliation.
#20260827_kpopmodder: Accept a matching typed STORE_HOME refusal as terminal evidence.
from __future__ import annotations

from typing import Any, Mapping

from plugins.Minecraft.common.protocol.command_result_status import (
    CommandResultStatus,
)
from plugins.Minecraft.fabric.chatclef.result.store_home import (
    StoreHomeTerminalPayload,
)


class MinecraftChatClefSubmissionReconciliationPolicy:
    TERMINAL_STATUSES = {
        CommandResultStatus.COMPLETED.value,
        CommandResultStatus.REJECTED.value,
        CommandResultStatus.FAILED.value,
        CommandResultStatus.CANCELLED.value,
        CommandResultStatus.DEADLINE_EXCEEDED.value,
    }

    def requires_reconciliation(self, result: Mapping[str, Any]) -> bool:
        status = result.get("status")
        details = result.get("details")
        observed_status = (
            status.get("status") if isinstance(status, Mapping) else None
        )
        reconciliation = (
            details.get("reconciliation_required")
            if isinstance(details, Mapping)
            else None
        )
        return (
            observed_status == CommandResultStatus.UNKNOWN.value
            or reconciliation is True
        )

    def result_request_id(self, result: Mapping[str, Any]) -> str | None:
        return self.request_id(result.get("request_id"))

    def is_matching_terminal(
        self,
        result: Mapping[str, Any],
        expected_request_id: str,
    ) -> bool:
        status = result.get("status")
        if not isinstance(status, Mapping):
            return False
        outer_request_id = self.request_id(result.get("request_id"))
        nested_request_id = self.request_id(status.get("request_id"))
        if (
            outer_request_id != expected_request_id
            or nested_request_id != expected_request_id
        ):
            return False
        details = result.get("details")
        if not isinstance(details, Mapping):
            return False
        if details.get("reconciliation_required") is True:
            return False
        observed_status = status.get("status")
        if observed_status == CommandResultStatus.UNKNOWN.value:
            return StoreHomeTerminalPayload.from_data(details) is not None
        return (
            type(observed_status) is str
            and observed_status in self.TERMINAL_STATUSES
        )

    def request_id(self, value: Any) -> str | None:
        if type(value) is not str or not value or value != value.strip():
            return None
        return value
