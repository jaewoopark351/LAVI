#20260905_kpopmodder: Inspect generic-crafting submission readiness only.


class GenericCraftingSubmissionReadinessStage:
    def __init__(self, *, extension, submission_precheck, decision_factory):
        self._extension = extension
        self._submission_precheck = submission_precheck
        self._decision_factory = decision_factory

    def rejection_if_unready(self):
        readiness = self._submission_precheck.inspect(self._extension)
        if readiness.ready:
            return None
        return self._decision_factory.precheck_rejection(readiness)


__all__ = ("GenericCraftingSubmissionReadinessStage",)
