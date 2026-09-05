#20260905_kpopmodder: Classify detected route-candidate ownership only.


class GenericCraftingCandidateOwnershipClassificationStage:
    def __init__(self, ownership_classifier):
        self._ownership_classifier = ownership_classifier

    def classify(self, candidate: object):
        return self._ownership_classifier.classify(candidate)


__all__ = ("GenericCraftingCandidateOwnershipClassificationStage",)
