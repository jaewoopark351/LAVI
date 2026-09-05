#20260905_kpopmodder: Sequence generic-crafting extension submission stages only.
from __future__ import annotations

from typing import Any


class GenericCraftingExtensionSubmissionPipeline:
    def __init__(
        self,
        *,
        translated_submission,
        input_decoder,
        translation_parser,
        spend_guard,
        early_result_recorder,
        action: str,
    ):
        self._translated_submission = translated_submission
        self._input_decoder = input_decoder
        self._translation_parser = translation_parser
        self._spend_guard = spend_guard
        self._early_result_recorder = early_result_recorder
        self._action = action

    def submit(
        self,
        command: Any,
        translation: Any,
        *,
        activation_receipt: object,
        korean_eligibility_proof: object,
    ) -> dict[str, Any]:
        text, rejection = self._input_decoder.decode(command)
        if rejection is not None:
            return self._early_result_recorder.record(rejection)
        translated, rejection = self._translation_parser.parse(translation)
        if rejection is not None:
            return self._early_result_recorder.record(rejection)
        return self._translated_submission.submit_translation(
            command,
            translated,
            text,
            action=self._action,
            pre_submit_guard=self._spend_guard.create(
                command,
                activation_receipt=activation_receipt,
                korean_eligibility_proof=korean_eligibility_proof,
            ),
        )


__all__ = ("GenericCraftingExtensionSubmissionPipeline",)
