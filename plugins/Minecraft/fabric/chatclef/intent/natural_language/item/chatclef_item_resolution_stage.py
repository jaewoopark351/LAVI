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
        rejection = self._rejection_factory.create(
            status,
            str(resolution["reason_code"]),
            "Korean item phrase could not be resolved to one ChatClef target.",
            intent,
            {"resolution": resolution},
        )
        return resolution, "", rejection


__all__ = ("ChatClefItemResolutionStage",)
