#20260905_kpopmodder: Preserve the ordinary route lock as a thin pipeline facade.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_route_decision import (
    MinecraftChatClefInputRouteDecision,
)
from plugins.Minecraft.fabric.chatclef.input.routing.goto import GotoInputBinding

from .composition import OrdinaryMinecraftCommandRouteComponentGraph


class OrdinaryMinecraftCommandRouteCoordinator:
    def __init__(
        self,
        *,
        extension,
        translation_boundary,
        submission_precheck,
        submission_boundary,
        submission_route_lock,
        submission_reconciliation,
        decision_factory,
        item_command_ownership_classifier,
        item_command_rejection_evidence_parser,
        live_proof_validator,
        failure_handler,
        router_logger,
    ):
        self._component_graph = OrdinaryMinecraftCommandRouteComponentGraph(
            extension=extension,
            translation_boundary=translation_boundary,
            submission_precheck=submission_precheck,
            submission_boundary=submission_boundary,
            submission_route_lock=submission_route_lock,
            submission_reconciliation=submission_reconciliation,
            decision_factory=decision_factory,
            item_command_ownership_classifier=(
                item_command_ownership_classifier
            ),
            item_command_rejection_evidence_parser=(
                item_command_rejection_evidence_parser
            ),
            live_proof_validator=live_proof_validator,
            failure_handler=failure_handler,
            router_logger=router_logger,
        )
        self._component_graph.install_compatibility_seams(self)

    def route(
        self,
        event: object,
        command_text: str,
        *,
        korean_eligibility_proof: object = None,
        goto_input_binding: GotoInputBinding | None = None,
    ) -> MinecraftChatClefInputRouteDecision:
        rejection = self._availability_stage.rejection()
        if rejection is not None:
            return rejection
        with self._submission_route_lock:
            return self.route_locked(
                event,
                command_text,
                korean_eligibility_proof=korean_eligibility_proof,
                goto_input_binding=goto_input_binding,
            )

    def route_locked(
        self,
        event: object,
        command_text: str,
        *,
        korean_eligibility_proof: object = None,
        goto_input_binding: GotoInputBinding | None = None,
    ) -> MinecraftChatClefInputRouteDecision:
        #20260915_openai: Newly recognized native syntax cannot be executed with forged source strings,
        # partial transcripts, spent proofs or direct legacy router calls. Keep one existing transport.
        from plugins.Minecraft.fabric.chatclef.intent.grammar.item.korean_item_list_rule_parser import KoreanItemListRuleParser
        native = KoreanItemListRuleParser().parse_native_deposit_all(command_text)
        if native is not None:
            live_proof = self._live_proof_validator(korean_eligibility_proof, event) is True
            self._router_logger.log(
                f"native deposit_all admission: live_proof={live_proof} parsed_intent={native.intent_type.value}"
            )
            if not live_proof:
                from plugins.Minecraft.fabric.chatclef.intent.chatclef_translation_result_dto import ChatClefTranslationResultDTO
                from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_status import ChatClefIntentStatus
                return self._decision_factory.translation_rejection(
                    ChatClefTranslationResultDTO.rejected(
                        ChatClefIntentStatus.INVALID, "deposit_all_live_proof_required",
                        "입력 확인이 끝난 채팅 또는 최종 마이크 문장으로 다시 말해 줘.",
                    ).to_dict()
                )
        #20260913_kpopmodder: No binding means a legacy caller, never inferred trusted GOTO.
        goto_arguments = (
            {} if goto_input_binding is None
            else {"goto_input_binding": goto_input_binding}
        )
        return self._pipeline.route_locked(
            event,
            command_text,
            korean_eligibility_proof=korean_eligibility_proof,
            **goto_arguments,
        )

    def route_confirmation(self, event, korean_eligibility_proof):
        owner = self._component_graph.confirmation_owner
        with self._submission_route_lock:
            receipt, decision = owner.reply(event=event, proof=korean_eligibility_proof)
            if receipt is None:
                return decision
            try:
                rejection = self._availability_stage.rejection()
                if rejection is not None:
                    return rejection
                # Re-translate the frozen original request with current validators.
                # The receipt binds the current confirmation proof to that exact request.
                return self._pipeline.route_locked(
                    receipt.pending.event, receipt.pending.command_text,
                    korean_eligibility_proof=receipt,
                    confirmation_receipt=receipt,
                    goto_input_binding=receipt.pending.goto_input_binding,
                )
            finally:
                owner.abandon(receipt)

    def cancel_pending_confirmation(self):
        with self._submission_route_lock:
            self._component_graph.confirmation_owner.cancel_all()


__all__ = ("OrdinaryMinecraftCommandRouteCoordinator",)
