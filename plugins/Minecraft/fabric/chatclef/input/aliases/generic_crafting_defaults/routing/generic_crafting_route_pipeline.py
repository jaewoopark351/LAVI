#20260905_kpopmodder: Sequence generic-crafting route stages only.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_route_decision import (
    MinecraftChatClefInputRouteDecision,
)
from plugins.Minecraft.fabric.chatclef.input.ownership.item_command import (
    ItemCommandOwnership,
)

from plugins.Minecraft.fabric.chatclef.input.aliases.generic_crafting_defaults.routing.generic_crafting_route_failure_handler import GenericCraftingRouteFailureHandler
from .lifecycle import GenericCraftingRouteExecutionLifecycle


class GenericCraftingRoutePipeline:
    def __init__(
        self,
        *,
        candidate_ownership_stage,
        reconciliation_stage,
        activation_stage,
        translation_stage,
        submission_stage,
        dispatch_lifecycle,
        decision_factory,
        execution_lifecycle=None,
    ):
        self._candidate_ownership_stage = candidate_ownership_stage
        self._reconciliation_stage = reconciliation_stage
        self._activation_stage = activation_stage
        self._translation_stage = translation_stage
        self._submission_stage = submission_stage
        self._dispatch_lifecycle = dispatch_lifecycle
        self._decision_factory = decision_factory
        self._execution_lifecycle = (
            execution_lifecycle
            or GenericCraftingRouteExecutionLifecycle(
                failure_handler=GenericCraftingRouteFailureHandler(
                    decision_factory
                ),
                receipt_cleanup=dispatch_lifecycle,
            )
        )

    def try_route(self, event: object, korean_eligibility_proof: object):
        return self._execution_lifecycle.execute(
            lambda capture_receipt: self._sequence(
                event,
                korean_eligibility_proof,
                capture_receipt,
            )
        )

    def _sequence(
        self,
        event: object,
        korean_eligibility_proof: object,
        capture_receipt,
    ):
        candidate, ownership = self._candidate_ownership_stage.classify(
            getattr(event, "text", None)
        )
        if ownership.ownership is ItemCommandOwnership.UNRELATED:
            return None
        if ownership.ownership is ItemCommandOwnership.OWNED_INVALID:
            return self._decision_factory.generic_crafting_defaults_rejection(
                ownership.reason_code,
                ownership.message,
            )

        blocked = self._reconciliation_stage.blocked_decision()
        if blocked is not None:
            return blocked

        activation = self._activation_stage.activate(
            event,
            korean_eligibility_proof,
            candidate,
        )
        if activation is None:
            return None
        if isinstance(activation, MinecraftChatClefInputRouteDecision):
            return activation
        receipt = capture_receipt(activation)

        translation, rejection = self._translation_stage.translate(
            event=event,
            receipt=receipt,
            korean_eligibility_proof=korean_eligibility_proof,
        )
        if rejection is not None:
            return rejection
        return self._submission_stage.submit(
            event=event,
            translation=translation,
            activation_receipt=receipt,
            korean_eligibility_proof=korean_eligibility_proof,
        )


__all__ = ("GenericCraftingRoutePipeline",)
