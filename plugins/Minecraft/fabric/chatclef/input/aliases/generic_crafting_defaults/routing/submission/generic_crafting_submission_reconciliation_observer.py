#20260905_kpopmodder: Observe generic-crafting submission results only.


class GenericCraftingSubmissionReconciliationObserver:
    def __init__(self, submission_reconciliation):
        self._submission_reconciliation = submission_reconciliation

    def observe(self, result: object) -> None:
        self._submission_reconciliation.observe_submission_result(result)


__all__ = ("GenericCraftingSubmissionReconciliationObserver",)
