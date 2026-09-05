#20260905_kpopmodder: Isolate Feature-B unresolved-submission reconciliation.
from __future__ import annotations


class GenericCraftingReconciliationStage:
    def __init__(self, *, submission_reconciliation, decision_factory):
        self._submission_reconciliation = submission_reconciliation
        self._decision_factory = decision_factory

    def blocked_decision(self):
        unresolved = self._submission_reconciliation.blocking_result()
        if unresolved is None:
            return None
        pending_request_id = self._submission_reconciliation.pending_request_id or ""
        if self._submission_reconciliation.reconcile():
            return self._decision_factory.reconciled_without_submission(
                pending_request_id
            )
        return self._decision_factory.submitted({}, unresolved)


__all__ = ("GenericCraftingReconciliationStage",)
