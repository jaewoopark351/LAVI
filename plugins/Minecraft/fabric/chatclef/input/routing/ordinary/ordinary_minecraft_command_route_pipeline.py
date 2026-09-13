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
    ):
        self._reconciliation_stage = reconciliation_stage
        self._translation_stage = translation_stage
        self._rejection_stage = rejection_stage
        self._precheck_stage = precheck_stage
        self._submission_result_stage = submission_result_stage
        self._goto_binding_stage = goto_binding_stage

    def route_locked(
        self,
        event: object,
        command_text: str,
        *,
        korean_eligibility_proof: object = None,
        goto_input_binding: GotoInputBinding | None = None,
    ):
        blocked = self._reconciliation_stage.blocked_decision()
        if blocked is not None:
            return blocked

        translation, rejection = self._translation_stage.translate(command_text)
        if rejection is not None:
            return rejection

        #20260913_kpopmodder: Bind the translation to the pre-normalization input XYZ.
        rejection = self._goto_binding_stage.inspect(
            event=event,
            proof=korean_eligibility_proof,
            binding=goto_input_binding,
            translation=translation,
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

        #20260907_kpopmodder: Preserve trusted proof through the final submission boundary.
        return self._submission_result_stage.submit(
            event=event,
            command_text=command_text,
            translation=translation,
            translation_status=translation_status,
            korean_eligibility_proof=korean_eligibility_proof,
        )


__all__ = ("OrdinaryMinecraftCommandRoutePipeline",)
