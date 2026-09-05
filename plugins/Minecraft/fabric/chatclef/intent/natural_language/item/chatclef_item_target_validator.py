#20260905_kpopmodder: Own catalog membership and equipment-target validation.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_status import (
    ChatClefIntentStatus,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_type import (
    ChatClefIntentType,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_target_catalog import (
    ChatClefTargetCatalog,
)


class ChatClefItemTargetValidator:
    def __init__(self, *, rejection_factory: object, target_catalog: object = None):
        self._rejection_factory = rejection_factory
        self._target_catalog = target_catalog

    def inspect(
        self,
        *,
        intent: object,
        resolver: object,
        target: str,
        resolution: dict[str, object],
    ) -> object | None:
        if not self._catalog().contains(target):
            return self._rejection_factory.create(
                ChatClefIntentStatus.UNSUPPORTED,
                "target_not_in_chatclef_catalog",
                f"Resolved target is not obtainable by ChatClef: {target}",
                intent,
                {"resolution": resolution},
            )
        if (
            intent.intent_type is ChatClefIntentType.EQUIP_ITEM
            and not resolver.supports_equipment_target(target)
        ):
            return self._rejection_factory.create(
                ChatClefIntentStatus.UNSUPPORTED,
                "target_not_equippable",
                f"Resolved target cannot be equipped by ChatClef: {target}",
                intent,
                {"resolution": resolution},
            )
        return None

    def _catalog(self) -> object:
        if self._target_catalog is None:
            self._target_catalog = ChatClefTargetCatalog()
        return self._target_catalog


__all__ = ("ChatClefItemTargetValidator",)
