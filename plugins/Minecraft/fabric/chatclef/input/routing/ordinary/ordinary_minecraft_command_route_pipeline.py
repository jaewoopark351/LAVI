#20260905_kpopmodder: Sequence ordinary route stages without owning their policies.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.input.routing.goto import GotoInputBinding


class OrdinaryMinecraftCommandRoutePipeline:
    def __init__(
        self,
        *,
        reconciliation_stage,
        translation_stage,
        rejection_stage,
        precheck_stage,
        submission_result_stage,
        goto_binding_stage,
        find_binding_stage=None,
        confirmation_owner=None,
        single_registration_owner=None,
    ):
        self._reconciliation_stage = reconciliation_stage
        self._translation_stage = translation_stage
        self._rejection_stage = rejection_stage
        self._precheck_stage = precheck_stage
        self._submission_result_stage = submission_result_stage
        self._goto_binding_stage = goto_binding_stage
        #20260914_kpopmodder: FIND uses the same lock and final trusted input boundary.
        self._find_binding_stage = find_binding_stage
        self._confirmation_owner = confirmation_owner
        self._single_registration_owner = single_registration_owner

    def route_locked(
        self,
        event: object,
        command_text: str,
        *,
        korean_eligibility_proof: object = None,
        goto_input_binding: GotoInputBinding | None = None,
        confirmation_receipt=None,
    ):
        blocked = self._reconciliation_stage.blocked_decision()
        if blocked is not None:
            return blocked

        translation, rejection = self._translation_stage.translate(command_text)
        if rejection is not None:
            return rejection

        if confirmation_receipt is not None and not self._confirmation_owner.accepts_translation(
            confirmation_receipt, event=event, translation=translation,
        ):
            return self._confirmation_owner._decision("confirmation_request_changed", event)

        #20260913_kpopmodder: Bind the translation to the pre-normalization input XYZ.
        rejection = self._goto_binding_stage.inspect(
            event=event,
            proof=korean_eligibility_proof,
            binding=goto_input_binding,
            translation=translation,
        )
        if rejection is not None:
            return rejection

        if self._find_binding_stage is not None:
            rejection = self._find_binding_stage.inspect(
                event=event, proof=korean_eligibility_proof, translation=translation,
            )
            if rejection is not None:
                return rejection

        translation_status, rejection = self._rejection_stage.inspect(
            event=event,
            command_text=command_text,
            translation=translation,
            korean_eligibility_proof=korean_eligibility_proof,
        )
        if rejection is not None:
            return rejection

        rejection = self._precheck_stage.rejection()
        if rejection is not None:
            return rejection

        if self._confirmation_owner is not None and confirmation_receipt is None:
            pending = self._confirmation_owner.begin(
                event=event, proof=korean_eligibility_proof,
                command_text=command_text, translation=translation,
                goto_input_binding=goto_input_binding,
            )
            if pending is not None:
                return pending

        route_claim = confirmation_receipt
        if self._single_registration_owner is not None and translation.get("command") == "auto_deposit_trust":
            route_claim, rejection = self._single_registration_owner.issue(
                event=event, proof=korean_eligibility_proof, translation=translation,
            )
            if rejection is not None:
                return rejection
        try:
            #20260907_kpopmodder: Preserve trusted proof through the final submission boundary.
            return self._submission_result_stage.submit(
                event=event,
                command_text=command_text,
                translation=translation,
                translation_status=translation_status,
                korean_eligibility_proof=korean_eligibility_proof,
                **({"route_claim": route_claim} if route_claim is not None else {}),
            )
        finally:
            if route_claim is not None:
                route_claim.abandon()


__all__ = ("OrdinaryMinecraftCommandRoutePipeline",)
