#20260905_kpopmodder: Project generic-crafting submission decisions only.


class GenericCraftingSubmissionResultProjector:
    def __init__(self, decision_factory):
        self._decision_factory = decision_factory

    def project(self, translation: object, result: object):
        return self._decision_factory.submitted(translation, result)


__all__ = ("GenericCraftingSubmissionResultProjector",)
