#20260905_kpopmodder: Own Korean item phrase resolution and its typed rejection conversion.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_status import (
    ChatClefIntentStatus,
)


class ChatClefItemResolutionStage:
    def __init__(self, rejection_factory: object):
        self._rejection_factory = rejection_factory

    def resolve(
        self,
        *,
        intent: object,
        resolver: object,
    ) -> tuple[dict[str, object], str, object | None]:
        resolution = resolver.resolve(intent.item_phrase)
        status = ChatClefIntentStatus(resolution["status"])
        if status is ChatClefIntentStatus.VALIDATED:
            return resolution, str(resolution["target"]), None
        #20260915_kpopmodder: Keep real ambiguity candidates in the shared Korean screen/voice message.
        from ...names.korean_name_resolution_message import KoreanNameResolutionMessage
        message = KoreanNameResolutionMessage.ambiguous(resolution)
        rejection = self._rejection_factory.create(
            status,
            str(resolution["reason_code"]),
            message or "아이템 이름 또는 이 명령의 지원 여부를 확인하지 못했어. 정확한 대상을 알려줘.",
            intent,
            {"resolution": resolution},
        )
        return resolution, "", rejection


__all__ = ("ChatClefItemResolutionStage",)
