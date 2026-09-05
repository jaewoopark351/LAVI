#20260803_kpopmodder: Route recognized LAVI chat/mic input into Fabric ChatClef commands.
#20260819_kpopmodder: Block later Minecraft routes while submission reconciliation is pending.
#20260905_kpopmodder: Delegate router responsibilities to focused LAVI-owned coordinators.
from __future__ import annotations

from typing import Any

from core.logger import log_print
from input_core.input_event.normalization import LaviInputEventNormalizer
from plugins.Minecraft.fabric.chatclef.input.aliases.generic_crafting_defaults.generic_crafting_defaults_admission import (
    GenericCraftingDefaultsAdmission,
)
from plugins.Minecraft.fabric.chatclef.input.auto_deposit_trust import (
    AutoDepositTrustExactInputAdapter,
    AutoDepositTrustInputAdmission,
    AutoDepositTrustInputEventClaimRegistry,
    AutoDepositTrustRawInputSafety,
    AutoDepositTrustTranslationAdmission,
)
from plugins.Minecraft.fabric.chatclef.input.eligibility import (
    KoreanChatMicrophoneEligibilityAdmission,
)
from plugins.Minecraft.fabric.chatclef.input.gating import (
    MinecraftChatClefInputIntentGate,
)
from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_route_decision import (
    MinecraftChatClefInputRouteDecision,
)
from plugins.Minecraft.fabric.chatclef.input.routing.composition import (
    MinecraftChatClefRouterComponentGraph,
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
        stop_control_route_owner: Any = None,
        korean_eligibility_admission: (
            KoreanChatMicrophoneEligibilityAdmission | None
        ) = None,
        generic_crafting_defaults_admission: (
            GenericCraftingDefaultsAdmission | None
        ) = None,
        generic_crafting_defaults_route_owner: Any = None,
        trusted_korean_command_feedback_facade: Any = None,
        feature_admission_logger: Any = None,
        feature_admission_projector: Any = None,
        log_callback: Any = log_print,
    ):
        self._component_graph = MinecraftChatClefRouterComponentGraph(
            owner=self,
            extension=extension,
            intent_gate=intent_gate,
            input_event_normalizer=input_event_normalizer,
            auto_deposit_trust_input_admission=(
                auto_deposit_trust_input_admission
            ),
            auto_deposit_trust_raw_input_safety=(
                auto_deposit_trust_raw_input_safety
            ),
            auto_deposit_trust_exact_input_adapter=(
                auto_deposit_trust_exact_input_adapter
            ),
            auto_deposit_trust_translation_admission=(
                auto_deposit_trust_translation_admission
            ),
            auto_deposit_trust_claim_registry=(
                auto_deposit_trust_claim_registry
            ),
            stop_control_route_owner=stop_control_route_owner,
            korean_eligibility_admission=korean_eligibility_admission,
            generic_crafting_defaults_admission=(
                generic_crafting_defaults_admission
            ),
            generic_crafting_defaults_route_owner=(
                generic_crafting_defaults_route_owner
            ),
            trusted_korean_command_feedback_facade=(
                trusted_korean_command_feedback_facade
            ),
            feature_admission_logger=feature_admission_logger,
            feature_admission_projector=feature_admission_projector,
            log_callback=log_callback,
        )
        self._component_graph.install_compatibility_seams(self)

    def route_trusted_user_input(
        self,
        value: object,
        consumed_ingress_evidence: object,
    ) -> MinecraftChatClefInputRouteDecision:
        return self._trusted_input_route_coordinator.route(
            value,
            consumed_ingress_evidence,
        )

    def route(
        self,
        value: object,
        *,
        korean_eligibility_proof: object = None,
    ) -> MinecraftChatClefInputRouteDecision:
        return self._route_ordering_coordinator.route(
            value,
            korean_eligibility_proof=korean_eligibility_proof,
        )

    def _route_auto_deposit_trust(
        self,
        event: Any,
    ) -> MinecraftChatClefInputRouteDecision:
        return self._auto_deposit_trust_route_coordinator.route(event)

    def _route_h5_claimed(
        self,
        event: Any,
        receipt: object,
    ) -> MinecraftChatClefInputRouteDecision:
        return self._auto_deposit_trust_route_coordinator.route_claimed(
            event,
            receipt,
        )

    def _route_submission(
        self,
        event: Any,
        command_text: str,
        *,
        korean_eligibility_proof: object = None,
    ) -> MinecraftChatClefInputRouteDecision:
        return self._ordinary_command_route_coordinator.route_locked(
            event,
            command_text,
            korean_eligibility_proof=korean_eligibility_proof,
        )

    def _operation_failure(
        self,
        reason: str,
        error: Exception,
        translation: dict[str, Any] | None = None,
    ) -> MinecraftChatClefInputRouteDecision:
        return self._route_failure_handler.decision(
            reason,
            error,
            translation,
        )

    def _log(self, message: str) -> None:
        self._router_logger.log(message)

    def _log_feature_admission(
        self,
        event: object,
        *,
        eligibility_reason: object,
        proof_issued: bool,
        route_decision: object,
    ) -> None:
        self._trusted_input_route_coordinator.log_feature_admission(
            event,
            eligibility_reason=eligibility_reason,
            proof_issued=proof_issued,
            route_decision=route_decision,
        )

    def _log_active_command_reconciliation(
        self,
        details: dict[str, Any],
    ) -> None:
        self._router_logger.log_active_command_reconciliation(details)

    def _inspect_gate(self, text: str) -> Any:
        return self._route_ordering_coordinator.inspect_gate(text)

    def close_generic_crafting_defaults_dispatch(
        self,
        korean_eligibility_proof: object,
    ) -> None:
        self._route_ordering_coordinator.close_generic_crafting_defaults_dispatch(
            korean_eligibility_proof
        )

    def _try_optional_route_owner(
        self,
        owner: object,
        event: object,
        korean_eligibility_proof: object,
        failure_reason: str,
    ) -> MinecraftChatClefInputRouteDecision | None:
        return self._route_ordering_coordinator.try_optional_route_owner(
            owner,
            event,
            korean_eligibility_proof,
            failure_reason,
        )

    def _validate_generic_crafting_registry(
        self,
        admission: GenericCraftingDefaultsAdmission,
    ) -> None:
        self._component_graph.validate_generic_crafting_registry(admission)

    def _extension_claim_registry(self, extension: Any) -> Any:
        return self._component_graph.extension_registry(
            extension,
            "get_auto_deposit_trust_input_claim_registry",
        )

    def _extension_generic_crafting_registry(self, extension: Any) -> Any:
        return self._component_graph.extension_registry(
            extension,
            "get_generic_crafting_defaults_activation_registry",
        )

    def _extension_stop_claim_registry(self, extension: Any) -> Any:
        return self._component_graph.extension_registry(
            extension,
            "get_stop_control_claim_registry",
        )

    def _is_live_eligibility_proof(
        self,
        proof: object,
        event: object,
    ) -> bool:
        return self._trusted_input_route_coordinator.is_live_proof(proof, event)
