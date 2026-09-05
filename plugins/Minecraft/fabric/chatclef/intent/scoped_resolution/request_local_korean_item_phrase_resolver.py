#20260905_kpopmodder: Overlay one immutable exact resolver profile without mutating shared Korean aliases.
from __future__ import annotations

from typing import Mapping

from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_status import (
    ChatClefIntentStatus,
)

from .scoped_item_resolution_profile import ScopedItemResolutionProfile


class RequestLocalKoreanItemPhraseResolver:
    def __init__(
        self,
        shared_resolver: object,
        profile: ScopedItemResolutionProfile,
    ):
        if not callable(getattr(shared_resolver, "resolve", None)):
            raise TypeError("shared item resolver must provide resolve")
        if not callable(getattr(profile, "resolve_exact", None)):
            raise TypeError("scoped item profile must provide resolve_exact")
        self._shared_resolver = shared_resolver
        self._profile = profile

    def resolve(self, phrase: object) -> dict[str, object]:
        scoped = self._profile.resolve_exact(phrase)
        if scoped is None:
            return dict(self._shared_resolver.resolve(phrase))

        shared = self._shared_resolver.resolve(phrase)
        if not isinstance(shared, Mapping):
            raise TypeError("shared item resolver result must be a mapping")
        if str(shared.get("status") or "") == ChatClefIntentStatus.VALIDATED.value:
            return {
                "status": ChatClefIntentStatus.INVALID.value,
                "target": None,
                "reason_code": "scoped_item_alias_global_collision",
                "data": {
                    "profile_id": scoped.profile_id,
                    "rule_id": scoped.rule_id,
                    "item_phrase": scoped.item_phrase,
                },
            }
        return {
            "status": ChatClefIntentStatus.VALIDATED.value,
            "target": scoped.target,
            "reason_code": "resolved_request_local_exact_alias",
            "data": {
                "profile_id": scoped.profile_id,
                "rule_id": scoped.rule_id,
                "item_phrase": scoped.item_phrase,
            },
        }

    def supports_equipment_target(self, target: object) -> bool:
        method = getattr(self._shared_resolver, "supports_equipment_target", None)
        return bool(method(target)) if callable(method) else False
