#20260905_kpopmodder: Preserve combined candidate ownership as a thin facade.
from __future__ import annotations

from .candidate import (
    GenericCraftingCandidateDetectionStage,
    GenericCraftingCandidateOwnershipClassificationStage,
)


class GenericCraftingCandidateOwnershipStage:
    def __init__(
        self,
        *,
        candidate_detector,
        ownership_classifier,
        detection_stage=None,
        ownership_stage=None,
    ):
        self._candidate_detector = candidate_detector
        self._ownership_classifier = ownership_classifier
        self._detection_stage = (
            detection_stage
            or GenericCraftingCandidateDetectionStage(candidate_detector)
        )
        self._ownership_stage = (
            ownership_stage
            or GenericCraftingCandidateOwnershipClassificationStage(
                ownership_classifier
            )
        )

    def classify(self, text: object):
        candidate = self._detection_stage.detect(text)
        return candidate, self._ownership_stage.classify(candidate)


__all__ = ("GenericCraftingCandidateOwnershipStage",)
