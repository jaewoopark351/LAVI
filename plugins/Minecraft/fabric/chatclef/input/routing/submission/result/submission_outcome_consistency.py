#20260819_kpopmodder: Validate submit outcome metadata without constructing results.
#20260827_kpopmodder: Preserve only internally consistent typed STORE_HOME terminals.
#20260828_kpopmodder: Reject typed STORE_HOME success when the outer lifecycle did not complete.
from __future__ import annotations

from typing import Any, Mapping

from plugins.Minecraft.common.protocol.command_result_status import (
    CommandResultStatus,
)
from plugins.Minecraft.fabric.chatclef.result.store_home import (
    StoreHomeTerminalPayload,
)


_MISSING = object()


class SubmissionOutcomeConsistency:
    SUBMISSION_OUTCOMES = {
        "accepted",
        "submit_response_not_accepted",
        "submission_outcome_unknown",
    }

    def is_consistent(
        self,
        status: CommandResultStatus,
        data: Mapping[str, Any],
    ) -> bool:
        if StoreHomeTerminalPayload.claims_store_home(data):
            terminal = StoreHomeTerminalPayload.from_data(data)
            if terminal is None or status in {
                CommandResultStatus.ACCEPTED,
                CommandResultStatus.RUNNING,
            }:
                return False
            return (
                not terminal.goal_satisfied
                or status is CommandResultStatus.COMPLETED
            )
        outcome = data.get("submission_outcome", _MISSING)
        reconciliation = data.get("reconciliation_required", _MISSING)
        if outcome is not _MISSING:
            if type(outcome) is not str or outcome not in self.SUBMISSION_OUTCOMES:
                return False
        if reconciliation is not _MISSING and type(reconciliation) is not bool:
            return False
        if status is CommandResultStatus.UNKNOWN:
            return (
                outcome == "submission_outcome_unknown"
                and reconciliation is True
            )
        if outcome == "submission_outcome_unknown" or reconciliation is True:
            return False
        if outcome == "accepted" and not status.ok:
            return False
        if outcome == "submit_response_not_accepted" and status.ok:
            return False
        return True
