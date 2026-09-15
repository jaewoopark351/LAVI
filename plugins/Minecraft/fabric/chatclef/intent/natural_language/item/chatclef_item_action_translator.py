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
            ChatClefIntentType.DEPOSIT_ALL,
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
        #20260915_kpopmodder: Native bulk/armor shortcuts and item lists share the established item pipeline.
        if intent.slots.get("bulk") is True or "armor_set" in intent.slots:
            from ...chatclef_translation_result_dto import ChatClefTranslationResultDTO
            return ChatClefTranslationResultDTO.validated(self._compiler.compile(intent), intent)
        if "items" in intent.slots:
            from dataclasses import replace
            from ...chatclef_translation_result_dto import ChatClefTranslationResultDTO
            targets, resolutions = [], []
            for item in intent.slots["items"]:
                scalar = replace(intent, item_phrase=item["phrase"], quantity=item["quantity"], slots={})
                resolution, target, rejection = self._resolution_stage.resolve(intent=scalar, resolver=active_resolver)
                if rejection is not None:
                    return replace(rejection, intent=intent)
                rejection = self._target_validator.inspect(intent=scalar, resolver=active_resolver, target=target, resolution=resolution)
                if rejection is not None:
                    return rejection
                targets.append(target)
                resolutions.append(resolution)
            resolved = ",".join(targets)
            try:
                command = self._compiler.compile(intent, resolved)
            except ValueError as error:
                from ...chatclef_intent_status import ChatClefIntentStatus
                return self._rejection_factory.create(ChatClefIntentStatus.INVALID, str(error),
                    "목록의 아이템별 수량과 같은 아이템의 합산 수량을 확인해 줘.", intent)
            return ChatClefTranslationResultDTO.validated(command, intent,
                resolved_target=resolved, data={"resolutions": resolutions})
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
