#20260905_kpopmodder: Sequence focused item resolution, validation, and compilation stages.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_dto import (
    ChatClefIntentDTO,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_type import (
    ChatClefIntentType,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_target_catalog import (
    ChatClefTargetCatalog,
)
from ..chatclef_translation_rejection_factory import (
    ChatClefTranslationRejectionFactory,
)
from .chatclef_item_command_compilation_stage import (
    ChatClefItemCommandCompilationStage,
)
from .chatclef_item_resolution_stage import ChatClefItemResolutionStage
from .chatclef_item_target_validator import ChatClefItemTargetValidator


class ChatClefItemActionTranslator:
    ITEM_ACTION_INTENTS = frozenset(
        {
            ChatClefIntentType.GET_ITEM,
            ChatClefIntentType.EQUIP_ITEM,
            ChatClefIntentType.DEPOSIT_ITEM,
            ChatClefIntentType.GIVE_ITEM,
        }
    )

    def __init__(
        self,
        *,
        resolver: object,
        compiler: object,
        rejection_factory: ChatClefTranslationRejectionFactory,
        target_catalog: ChatClefTargetCatalog | None = None,
    ):
        self._resolver = resolver
        self._compiler = compiler
        self._rejection_factory = rejection_factory
        self._target_catalog = target_catalog
        self._resolution_stage = ChatClefItemResolutionStage(rejection_factory)
        self._target_validator = ChatClefItemTargetValidator(
            rejection_factory=rejection_factory,
            target_catalog=target_catalog,
        )
        self._compilation_stage = ChatClefItemCommandCompilationStage()

    def translate(
        self,
        intent: ChatClefIntentDTO,
        resolver: object | None = None,
    ) -> object:
        active_resolver = resolver or self._resolver
        resolution, target, rejection = self._resolution_stage.resolve(
            intent=intent,
            resolver=active_resolver,
        )
        if rejection is not None:
            return rejection
        rejection = self._target_validator.inspect(
            intent=intent,
            resolver=active_resolver,
            target=target,
            resolution=resolution,
        )
        if rejection is not None:
            return rejection
        return self._compilation_stage.compile(
            intent=intent,
            target=target,
            resolution=resolution,
            compiler=self._compiler,
        )


__all__ = ("ChatClefItemActionTranslator",)
