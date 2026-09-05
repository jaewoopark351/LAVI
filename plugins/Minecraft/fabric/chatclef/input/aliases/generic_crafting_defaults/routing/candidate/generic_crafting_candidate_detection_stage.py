#20260905_kpopmodder: Detect generic-crafting route candidates only.


class GenericCraftingCandidateDetectionStage:
    def __init__(self, candidate_detector):
        self._candidate_detector = candidate_detector

    def detect(self, text: object):
        return self._candidate_detector.inspect(text)


__all__ = ("GenericCraftingCandidateDetectionStage",)
