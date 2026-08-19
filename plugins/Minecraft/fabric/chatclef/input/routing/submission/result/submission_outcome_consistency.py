#20260819_kpopmodder: Validate submit outcome metadata without constructing results.
from __future__ import annotations

from typing import Any, Mapping

from plugins.Minecraft.common.protocol.command_result_status import (
    CommandResultStatus,
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
