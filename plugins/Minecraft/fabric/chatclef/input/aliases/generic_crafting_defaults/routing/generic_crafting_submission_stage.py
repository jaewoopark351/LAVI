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
        command_feedback=None,
        descriptor_factory=None,
        start_decision_decorator=None,
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
        self._command_feedback = command_feedback
        self._descriptor_factory = descriptor_factory
        self._start_decision_decorator = start_decision_decorator

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
        descriptor = self._descriptor(
            event=event,
            translation=translation,
        )
        grant = (
            self._command_feedback.prepare_descriptor(descriptor)
            if self._command_feedback is not None
            else None
        )
        try:
            result = self._delivery.deliver(
                event=event,
                translation=translation,
                activation_receipt=activation_receipt,
                korean_eligibility_proof=korean_eligibility_proof,
            )
            self._reconciliation_observer.observe(result)
            decision = self._result_projector.project(translation, result)
            if self._command_feedback is None or self._start_decision_decorator is None:
                return decision
            acknowledgement = self._command_feedback.claim_start(grant, result)
            return self._start_decision_decorator.decorate(
                decision,
                descriptor=getattr(grant, "descriptor", None),
                start_claimed=acknowledgement not in (None, False),
                publication_acknowledgement=(
                    None if acknowledgement is True else acknowledgement
                ),
            )
        finally:
            if self._command_feedback is not None:
                self._command_feedback.abandon(grant)

    def _descriptor(self, *, event: object, translation: object):
        if self._descriptor_factory is None:
            return None
        try:
            return self._descriptor_factory.from_trusted_translation(
                event=event,
                translation=translation,
            )
        except Exception:
            return None


__all__ = ("GenericCraftingSubmissionStage",)
