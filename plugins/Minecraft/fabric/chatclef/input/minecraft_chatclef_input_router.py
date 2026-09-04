#20260803_kpopmodder: Route recognized LAVI chat/mic input into Fabric ChatClef commands.
#20260819_kpopmodder: Block later Minecraft routes while submission reconciliation is pending.
from __future__ import annotations

import json
from typing import Any

from core.logger import log_print
from input_core.input_event.normalization import LaviInputEventNormalizer
from plugins.Minecraft.fabric.chatclef.input.auto_deposit_trust import (
    AutoDepositTrustExactInputAdapter,
    AutoDepositTrustInputAdmission,
    AutoDepositTrustInputEventClaimRegistry,
    AutoDepositTrustRawInputSafety,
    AutoDepositTrustTranslationAdmission,
)
from plugins.Minecraft.fabric.chatclef.input.gating import (
    MinecraftChatClefInputIntentGate,
    MinecraftChatClefInputRouteKind,
)
from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_route_decision import (
    MinecraftChatClefInputRouteDecision,
)
from plugins.Minecraft.fabric.chatclef.input.routing import (
    MinecraftChatClefRouteDecisionFactory,
    MinecraftChatClefSubmissionBoundary,
    MinecraftChatClefSubmissionPrecheck,
    MinecraftChatClefSubmissionReconciliationCoordinator,
    MinecraftChatClefSubmissionRouteLock,
    MinecraftChatClefTranslationBoundary,
)


class MinecraftChatClefInputRouter:
    def __init__(
        self,
        extension: Any = None,
        intent_gate: MinecraftChatClefInputIntentGate | None = None,
        input_event_normalizer: LaviInputEventNormalizer | None = None,
        auto_deposit_trust_input_admission: (
            AutoDepositTrustInputAdmission | None
        ) = None,
        auto_deposit_trust_raw_input_safety: (
            AutoDepositTrustRawInputSafety | None
        ) = None,
        auto_deposit_trust_exact_input_adapter: (
            AutoDepositTrustExactInputAdapter | None
        ) = None,
        auto_deposit_trust_translation_admission: (
            AutoDepositTrustTranslationAdmission | None
        ) = None,
        auto_deposit_trust_claim_registry: (
            AutoDepositTrustInputEventClaimRegistry | None
        ) = None,
        log_callback: Any = log_print,
    ):
        self.extension = extension
        self.intent_gate = intent_gate or MinecraftChatClefInputIntentGate()
        self._input_event_normalizer = (
            input_event_normalizer or LaviInputEventNormalizer()
        )
        self._auto_deposit_trust_input_admission = (
            auto_deposit_trust_input_admission
            or AutoDepositTrustInputAdmission()
        )
        self._auto_deposit_trust_raw_input_safety = (
            auto_deposit_trust_raw_input_safety
            or AutoDepositTrustRawInputSafety()
        )
        self._auto_deposit_trust_exact_input_adapter = (
            auto_deposit_trust_exact_input_adapter
            or AutoDepositTrustExactInputAdapter()
        )
        self._auto_deposit_trust_translation_admission = (
            auto_deposit_trust_translation_admission
            or AutoDepositTrustTranslationAdmission()
        )
        extension_claim_registry = self._extension_claim_registry(extension)
        if (
            auto_deposit_trust_claim_registry is not None
            and extension_claim_registry is not None
            and auto_deposit_trust_claim_registry is not extension_claim_registry
        ):
            raise ValueError(
                "H5 router and extension must share one claim registry"
            )
        if auto_deposit_trust_claim_registry is not None:
            claim_registry = auto_deposit_trust_claim_registry
        elif extension_claim_registry is not None:
            claim_registry = extension_claim_registry
        else:
            claim_registry = AutoDepositTrustInputEventClaimRegistry()
        claim_owner_binder = getattr(claim_registry, "_bind_claim_owner", None)
        if not callable(claim_owner_binder):
            raise TypeError("H5 claim registry cannot bind its router owner")
        claim_owner_binder(self)
        self.auto_deposit_trust_claim_registry = claim_registry
        self.log_callback = log_callback
        self._translation_boundary = MinecraftChatClefTranslationBoundary()
        self._submission_precheck = MinecraftChatClefSubmissionPrecheck()
        self._submission_boundary = MinecraftChatClefSubmissionBoundary()
        self._submission_route_lock = MinecraftChatClefSubmissionRouteLock()
        self.submission_reconciliation = (
            MinecraftChatClefSubmissionReconciliationCoordinator(
                extension,
                route_lock=self._submission_route_lock,
            )
        )
        self._decision_factory = MinecraftChatClefRouteDecisionFactory()

    def route(self, value: object) -> MinecraftChatClefInputRouteDecision:
        event = self._input_event_normalizer.normalize(value)
        command_text = event.text
        if not command_text:
            return MinecraftChatClefInputRouteDecision.not_handled("empty_input")
        gate_decision = self._inspect_gate(command_text)
        if not gate_decision.consider:
            return MinecraftChatClefInputRouteDecision.not_handled(
                "no_minecraft_trigger"
            )
        if (
            gate_decision.route_kind
            is MinecraftChatClefInputRouteKind.H5_AUTO_DEPOSIT_TRUST
        ):
            return self._route_auto_deposit_trust(event)
        command_text = command_text.strip()
        if self.extension is None:
            self._log("route skipped: extension unavailable")
            return MinecraftChatClefInputRouteDecision.not_handled(
                "extension_unavailable"
            )
        if not self._translation_boundary.is_available(
            self.extension
        ) or not self._submission_boundary.is_available(self.extension):
            self._log("route skipped: single-pass submission boundary unavailable")
            return MinecraftChatClefInputRouteDecision.not_handled(
                "handler_unavailable"
            )
        with self._submission_route_lock:
            return self._route_submission(event, command_text)

    def _route_auto_deposit_trust(
        self,
        event: Any,
    ) -> MinecraftChatClefInputRouteDecision:
        receipt = None
        try:
            admission = self._auto_deposit_trust_input_admission.inspect(event)
            if not admission.allowed:
                return self._decision_factory.input_rejection(
                    admission.reason_code,
                    admission.message,
                )
            if not self._auto_deposit_trust_raw_input_safety.is_safe(event.text):
                return self._decision_factory.input_rejection(
                    "auto_deposit_trust_input_raw_control_not_allowed",
                    "Control characters or non-ASCII spacing are not allowed in this command.",
                )

            with self._submission_route_lock:
                receipt, claim_error = (
                    self.auto_deposit_trust_claim_registry.claim(
                        event,
                        claim_owner=self,
                    )
                )
                if receipt is None:
                    return self._decision_factory.input_rejection(
                        claim_error,
                        "This input event cannot be claimed for submission.",
                    )
                if self.extension is None:
                    return self._decision_factory.input_rejection(
                        "auto_deposit_trust_extension_unavailable",
                        "The Fabric ChatClef extension is unavailable.",
                    )
                if not self._translation_boundary.is_available(
                    self.extension
                ) or not self._submission_boundary.is_available(self.extension):
                    return self._decision_factory.input_rejection(
                        "auto_deposit_trust_handler_unavailable",
                        "The Fabric ChatClef command handler is unavailable.",
                    )
                return self._route_h5_claimed(event, receipt)
        except Exception as error:
            self._log(
                "H5 route failed closed: "
                f"error={type(error).__name__}: {error}"
            )
            return self._decision_factory.input_rejection(
                "auto_deposit_trust_input_internal_error",
                "The automatic-deposit registration request failed safely.",
            )
        finally:
            if receipt is not None:
                try:
                    self.auto_deposit_trust_claim_registry.abandon_if_issued(
                        receipt
                    )
                except Exception as error:
                    self._log(
                        "H5 receipt abandonment failed safely: "
                        f"error={type(error).__name__}: {error}"
                    )

    def _route_h5_claimed(
        self,
        event: Any,
        receipt: object,
    ) -> MinecraftChatClefInputRouteDecision:
        unresolved = self.submission_reconciliation.blocking_result()
        if unresolved is not None:
            pending_request_id = (
                self.submission_reconciliation.pending_request_id or ""
            )
            if self.submission_reconciliation.reconcile():
                return self._decision_factory.reconciled_without_submission(
                    pending_request_id
                )
            return self._decision_factory.submitted({}, unresolved)

        readiness = self._submission_precheck.inspect(self.extension)
        if not readiness.ready:
            return self._decision_factory.precheck_rejection(readiness)

        adaptation = self._auto_deposit_trust_exact_input_adapter.adapt(
            event.text
        )
        raw_translation = self._translation_boundary.translate_once(
            self.extension,
            adaptation.translation_input_text,
        )
        translation = self._translation_boundary.validate(raw_translation)
        if self._translation_boundary.status(translation) != "validated":
            return self._decision_factory.translation_rejection(translation)
        translation_admission = (
            self._auto_deposit_trust_translation_admission.inspect(translation)
        )
        if not translation_admission.allowed:
            return self._decision_factory.input_rejection(
                translation_admission.reason_code,
                translation_admission.message,
            )

        result = self._submission_boundary.submit_once(
            self.extension,
            event,
            translation,
            route_claim=receipt,
            original_text=adaptation.original_text,
            translation_input_text=adaptation.translation_input_text,
        )
        self.submission_reconciliation.observe_submission_result(result)
        return self._decision_factory.submitted(translation, result)

    def _route_submission(
        self,
        event: Any,
        command_text: str,
    ) -> MinecraftChatClefInputRouteDecision:
        unresolved = self.submission_reconciliation.blocking_result()
        if unresolved is not None:
            pending_request_id = (
                self.submission_reconciliation.pending_request_id or ""
            )
            if self.submission_reconciliation.reconcile():
                self._log(
                    "route blocked after matching terminal reconciliation: "
                    f"request_id={pending_request_id}"
                )
                return self._decision_factory.reconciled_without_submission(
                    pending_request_id
                )
            self._log(
                "route blocked: a previous submission requires reconciliation"
            )
            return self._decision_factory.submitted({}, unresolved)

        try:
            raw_translation = self._translation_boundary.translate_once(
                self.extension,
                command_text,
            )
        except Exception as error:
            return self._operation_failure("translation_failed", error)
        try:
            translation = self._translation_boundary.validate(raw_translation)
        except Exception as error:
            self._log(
                "route rejected malformed translation: "
                f"error={type(error).__name__}: {error}"
            )
            return self._decision_factory.malformed_translation(error)

        translation_status = self._translation_boundary.status(translation)
        if translation_status in {"unknown", "ambiguous", "unsupported"}:
            return MinecraftChatClefInputRouteDecision.not_handled(
                f"{translation_status}_intent"
            )
        if translation_status != "validated":
            return self._decision_factory.translation_rejection(translation)

        readiness = self._submission_precheck.inspect(self.extension)
        if not readiness.ready:
            self._log(
                "route rejected by submission precheck: "
                f"reason={readiness.reason} error={readiness.error} "
                f"message={readiness.message}"
            )
            self._log_active_command_reconciliation(readiness.details)
            return self._decision_factory.precheck_rejection(readiness)

        try:
            result = self._submission_boundary.submit_once(
                self.extension,
                event,
                translation,
                original_text=command_text,
                translation_input_text=command_text,
            )
        except Exception as error:
            return self._operation_failure(
                "submission_failed",
                error,
                translation,
            )

        self.submission_reconciliation.observe_submission_result(result)

        self._log(
            "route handled: "
            f"translation_status={translation_status} "
            f"command={translation.get('command')} "
            f"result_status={self._decision_factory.result_status(result)} "
            f"ok={result.get('ok')}"
        )
        return self._decision_factory.submitted(translation, result)

    def _operation_failure(
        self,
        reason: str,
        error: Exception,
        translation: dict[str, Any] | None = None,
    ) -> MinecraftChatClefInputRouteDecision:
        message = f"{type(error).__name__}: {error}"
        self._log(f"route failed: reason={reason} error={message}")
        return self._decision_factory.operation_failure(
            reason,
            error,
            translation,
        )

    def _log(self, message: str) -> None:
        try:
            self.log_callback(f"[MinecraftChatClefInputRouter] {message}")
        except Exception:
            pass

    def _log_active_command_reconciliation(
        self,
        details: dict[str, Any],
    ) -> None:
        diagnostic = details.get("active_command_reconciliation")
        if not isinstance(diagnostic, dict):
            return
        self._log(
            "active command reconciliation diagnostic "
            f"{_compact_json(diagnostic)}"
        )

    def _inspect_gate(self, text: str) -> Any:
        inspect = getattr(self.intent_gate, "inspect", None)
        if callable(inspect):
            return inspect(text)
        if self.intent_gate.should_consider(text):
            from plugins.Minecraft.fabric.chatclef.input.gating.contracts import (
                MinecraftChatClefInputGateDecision,
            )

            return MinecraftChatClefInputGateDecision.generic()
        from plugins.Minecraft.fabric.chatclef.input.gating.contracts import (
            MinecraftChatClefInputGateDecision,
        )

        return MinecraftChatClefInputGateDecision.none()

    def _extension_claim_registry(self, extension: Any) -> Any:
        getter = getattr(
            extension,
            "get_auto_deposit_trust_input_claim_registry",
            None,
        )
        if callable(getter):
            return getter()
        return None


def _compact_json(payload: Any) -> str:
    try:
        return json.dumps(
            payload,
            ensure_ascii=False,
            sort_keys=True,
            separators=(",", ":"),
        )
    except Exception as error:
        return f"<json failed {type(error).__name__}: {error}>"
