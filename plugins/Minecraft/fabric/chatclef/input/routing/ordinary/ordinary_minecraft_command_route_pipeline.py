#20260905_kpopmodder: Sequence ordinary route stages without owning their policies.


class OrdinaryMinecraftCommandRoutePipeline:
    def __init__(
        self,
        *,
        reconciliation_stage,
        translation_stage,
        rejection_stage,
        precheck_stage,
        submission_result_stage,
    ):
        self._reconciliation_stage = reconciliation_stage
        self._translation_stage = translation_stage
        self._rejection_stage = rejection_stage
        self._precheck_stage = precheck_stage
        self._submission_result_stage = submission_result_stage

    def route_locked(
        self,
        event: object,
        command_text: str,
        *,
        korean_eligibility_proof: object = None,
    ):
        blocked = self._reconciliation_stage.blocked_decision()
        if blocked is not None:
            return blocked

        translation, rejection = self._translation_stage.translate(command_text)
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

        return self._submission_result_stage.submit(
            event=event,
            command_text=command_text,
            translation=translation,
            translation_status=translation_status,
        )


__all__ = ("OrdinaryMinecraftCommandRoutePipeline",)
