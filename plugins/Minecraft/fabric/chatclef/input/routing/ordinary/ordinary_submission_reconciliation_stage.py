#20260905_kpopmodder: Isolate ordinary prior-submission reconciliation decisions.


class OrdinarySubmissionReconciliationStage:
    def __init__(self, *, submission_reconciliation, decision_factory, router_logger):
        self._submission_reconciliation = submission_reconciliation
        self._decision_factory = decision_factory
        self._router_logger = router_logger

    def blocked_decision(self):
        unresolved = self._submission_reconciliation.blocking_result()
        if unresolved is None:
            return None
        pending_request_id = self._submission_reconciliation.pending_request_id or ""
        if self._submission_reconciliation.reconcile():
            self._router_logger.log(
                "route blocked after matching terminal reconciliation: "
                f"request_id={pending_request_id}"
            )
            return self._decision_factory.reconciled_without_submission(
                pending_request_id
            )
        self._router_logger.log(
            "route blocked: a previous submission requires reconciliation"
        )
        return self._decision_factory.submitted({}, unresolved)


__all__ = ("OrdinarySubmissionReconciliationStage",)
