#20260905_kpopmodder: Sequence existing H5 admission, translation, and submission owners.
from __future__ import annotations


class AutoDepositTrustRouteSequence:
    def __init__(
        self,
        *,
        extension: object,
        input_admission: object,
        raw_input_safety: object,
        exact_input_adapter: object,
        translation_admission: object,
        translation_boundary: object,
        submission_precheck: object,
        submission_boundary: object,
        submission_reconciliation: object,
        decision_factory: object,
        command_feedback: object = None,
        descriptor_factory: object = None,
        start_decision_decorator: object = None,
    ):
        self._extension = extension
        self._input_admission = input_admission
        self._raw_input_safety = raw_input_safety
        self._exact_input_adapter = exact_input_adapter
        self._translation_admission = translation_admission
        self._translation_boundary = translation_boundary
        self._submission_precheck = submission_precheck
        self._submission_boundary = submission_boundary
        self._submission_reconciliation = submission_reconciliation
        self._decision_factory = decision_factory
        self._command_feedback = command_feedback
        self._descriptor_factory = descriptor_factory
        self._start_decision_decorator = start_decision_decorator

    def inspect_input(self, event: object) -> object | None:
        admission = self._input_admission.inspect(event)
        if not admission.allowed:
            return self._decision_factory.input_rejection(
                admission.reason_code,
                admission.message,
            )
        if not self._raw_input_safety.is_safe(event.text):
            return self._decision_factory.input_rejection(
                "auto_deposit_trust_input_raw_control_not_allowed",
                "Control characters or non-ASCII spacing are not allowed in this command.",
            )
        return None

    def inspect_availability(self) -> object | None:
        if self._extension is None:
            return self._decision_factory.input_rejection(
                "auto_deposit_trust_extension_unavailable",
                "The Fabric ChatClef extension is unavailable.",
            )
        if not self._translation_boundary.is_available(
            self._extension
        ) or not self._submission_boundary.is_available(self._extension):
            return self._decision_factory.input_rejection(
                "auto_deposit_trust_handler_unavailable",
                "The Fabric ChatClef command handler is unavailable.",
            )
        return None

    def route_claimed(self, event: object, receipt: object) -> object:
        unresolved = self._submission_reconciliation.blocking_result()
        if unresolved is not None:
            pending_request_id = (
                self._submission_reconciliation.pending_request_id or ""
            )
            if self._submission_reconciliation.reconcile():
                return self._decision_factory.reconciled_without_submission(
                    pending_request_id
                )
            return self._decision_factory.submitted({}, unresolved)

        readiness = self._submission_precheck.inspect(self._extension)
        if not readiness.ready:
            return self._decision_factory.precheck_rejection(readiness)

        adaptation = self._exact_input_adapter.adapt(event.text)
        raw_translation = self._translation_boundary.translate_once(
            self._extension,
            adaptation.translation_input_text,
        )
        translation = self._translation_boundary.validate(raw_translation)
        if self._translation_boundary.status(translation) != "validated":
            return self._decision_factory.translation_rejection(translation)
        translation_admission = self._translation_admission.inspect(translation)
        if not translation_admission.allowed:
            return self._decision_factory.input_rejection(
                translation_admission.reason_code,
                translation_admission.message,
            )

        descriptor = self._descriptor(event=event, translation=translation)
        grant = (
            self._command_feedback.prepare_descriptor(descriptor)
            if self._command_feedback is not None
            else None
        )
        try:
            result = self._submission_boundary.submit_once(
                self._extension,
                event,
                translation,
                route_claim=receipt,
                original_text=adaptation.original_text,
                translation_input_text=adaptation.translation_input_text,
            )
            self._submission_reconciliation.observe_submission_result(result)
            decision = self._decision_factory.submitted(translation, result)
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


__all__ = ("AutoDepositTrustRouteSequence",)
