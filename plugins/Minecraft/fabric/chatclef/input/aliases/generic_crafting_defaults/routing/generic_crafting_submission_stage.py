#20260905_kpopmodder: Preserve route submission as a thin sequencing facade.
from __future__ import annotations

from .submission import (
    GenericCraftingSubmissionDelivery,
    GenericCraftingSubmissionReadinessStage,
    GenericCraftingSubmissionReconciliationObserver,
    GenericCraftingSubmissionResultProjector,
)


class GenericCraftingSubmissionStage:
    def __init__(
        self,
        *,
        extension,
        submission_precheck,
        submission_boundary,
        submission_reconciliation,
        decision_factory,
        readiness_stage=None,
        delivery=None,
        reconciliation_observer=None,
        result_projector=None,
    ):
        self._extension = extension
        self._submission_precheck = submission_precheck
        self._submission_boundary = submission_boundary
        self._submission_reconciliation = submission_reconciliation
        self._decision_factory = decision_factory
        self._readiness_stage = (
            readiness_stage
            or GenericCraftingSubmissionReadinessStage(
                extension=extension,
                submission_precheck=submission_precheck,
                decision_factory=decision_factory,
            )
        )
        self._delivery = (
            delivery
            or GenericCraftingSubmissionDelivery(
                extension=extension,
                submission_boundary=submission_boundary,
            )
        )
        self._reconciliation_observer = (
            reconciliation_observer
            or GenericCraftingSubmissionReconciliationObserver(
                submission_reconciliation
            )
        )
        self._result_projector = (
            result_projector
            or GenericCraftingSubmissionResultProjector(decision_factory)
        )

    def submit(
        self,
        *,
        event: object,
        translation: object,
        activation_receipt: object,
        korean_eligibility_proof: object,
    ):
        rejection = self._readiness_stage.rejection_if_unready()
        if rejection is not None:
            return rejection
        result = self._delivery.deliver(
            event=event,
            translation=translation,
            activation_receipt=activation_receipt,
            korean_eligibility_proof=korean_eligibility_proof,
        )
        self._reconciliation_observer.observe(result)
        return self._result_projector.project(translation, result)


__all__ = ("GenericCraftingSubmissionStage",)
