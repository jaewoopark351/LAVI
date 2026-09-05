#20260905_kpopmodder: Sequence focused intent admission, branch, and execution stages.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_dto import (
    ChatClefIntentDTO,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_translation_result_dto import (
    ChatClefTranslationResultDTO,
)

from ..chatclef_translation_rejection_factory import (
    ChatClefTranslationRejectionFactory,
)
from ..item import ChatClefItemActionTranslator
from .chatclef_intent_admission_stage import ChatClefIntentAdmissionStage
from .chatclef_non_item_translation_stage import ChatClefNonItemTranslationStage
from .chatclef_translation_branch_selector import ChatClefTranslationBranchSelector


class ChatClefIntentTranslationPolicy:
    def __init__(
        self,
        *,
        schema_validator: object,
        guard_decoder: object,
        compiler: object,
        item_translator: ChatClefItemActionTranslator,
        rejection_factory: ChatClefTranslationRejectionFactory,
    ):
        self._schema_validator = schema_validator
        self._guard_decoder = guard_decoder
        self._compiler = compiler
        self._item_translator = item_translator
        self._rejection_factory = rejection_factory
        self._admission_stage = ChatClefIntentAdmissionStage(
            schema_validator=schema_validator,
            guard_decoder=guard_decoder,
            rejection_factory=rejection_factory,
        )
        self._branch_selector = ChatClefTranslationBranchSelector(
            item_action_intents=ChatClefItemActionTranslator.ITEM_ACTION_INTENTS
        )
        self._non_item_translation_stage = ChatClefNonItemTranslationStage()

    def translate(
        self,
        intent: ChatClefIntentDTO,
        resolver: object,
    ) -> ChatClefTranslationResultDTO:
        rejection = self._admission_stage.inspect(intent)
        if rejection is not None:
            return rejection
        if self._branch_selector.is_item_action(intent):
            return self._item_translator.translate(intent, resolver)
        return self._non_item_translation_stage.translate(intent, self._compiler)


__all__ = ("ChatClefIntentTranslationPolicy",)
