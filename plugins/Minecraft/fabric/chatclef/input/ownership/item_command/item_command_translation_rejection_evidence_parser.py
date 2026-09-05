#20260905_kpopmodder: Parse only typed item-resolution rejection evidence from a live trusted dispatch.
from __future__ import annotations

from collections.abc import Mapping

from plugins.Minecraft.fabric.chatclef.intent.korean_text_normalizer import (
    KoreanTextNormalizer,
)

from .item_command_minecraft_marker_matcher import (
    ItemCommandMinecraftMarkerMatcher,
)
from .item_command_resolver_family_matcher import (
    ItemCommandResolverFamilyMatcher,
)
from .item_command_translation_rejection_evidence import (
    ItemCommandTranslationRejectionEvidence,
)


class ItemCommandTranslationRejectionEvidenceParser:
    _REJECTION_STATUSES = frozenset({"unknown", "ambiguous", "unsupported"})
    _ITEM_INTENTS = frozenset(
        {"get_item", "equip_item", "deposit_item", "give_item"}
    )

    def __init__(
        self,
        family_matcher: ItemCommandResolverFamilyMatcher | None = None,
        marker_matcher: ItemCommandMinecraftMarkerMatcher | None = None,
        normalizer: KoreanTextNormalizer | None = None,
    ):
        self._family_matcher = family_matcher or ItemCommandResolverFamilyMatcher()
        self._marker_matcher = marker_matcher or ItemCommandMinecraftMarkerMatcher()
        self._normalizer = normalizer or KoreanTextNormalizer()

    def parse(
        self,
        *,
        command_text: object,
        translation: object,
        trusted_scope_live: bool,
    ) -> ItemCommandTranslationRejectionEvidence:
        payload = translation if isinstance(translation, Mapping) else {}
        status = self._text(payload.get("status")).lower()
        reason_code = self._text(payload.get("reason_code"))
        intent = payload.get("intent")
        intent_payload = intent if isinstance(intent, Mapping) else {}
        intent_type = self._text(intent_payload.get("intent_type")).lower()
        item_phrase = self._text(intent_payload.get("item_phrase"))
        original_text = self._text(intent_payload.get("original_text"))
        intent_source = self._text(intent_payload.get("source"))
        intent_language = self._text(intent_payload.get("language")).lower()

        data = payload.get("data")
        data_payload = data if isinstance(data, Mapping) else {}
        resolution = data_payload.get("resolution")
        resolution_payload = resolution if isinstance(resolution, Mapping) else {}
        resolver_status = self._text(resolution_payload.get("status")).lower()
        resolver_reason_code = self._text(resolution_payload.get("reason_code"))

        expected_original_text = (
            self._normalizer.normalize(command_text, lowercase_english=False)
            if type(command_text) is str
            else ""
        )
        shape_is_bound = (
            status in self._REJECTION_STATUSES
            and intent_type in self._ITEM_INTENTS
            and intent_source == "rule"
            and intent_language == "ko"
            and bool(item_phrase)
            and original_text == expected_original_text
            and resolver_status == status
            and bool(resolver_reason_code)
            and resolution_payload.get("target") is None
        )
        resolver_family = (
            self._family_matcher.match(item_phrase) if shape_is_bound else ""
        )
        return ItemCommandTranslationRejectionEvidence(
            trusted_scope_live=trusted_scope_live is True,
            status=status,
            reason_code=reason_code,
            intent_type=intent_type,
            item_phrase=item_phrase,
            resolver_status=resolver_status,
            resolver_reason_code=resolver_reason_code,
            resolver_family=resolver_family,
            explicit_minecraft_marker=(
                shape_is_bound and self._marker_matcher.matches(command_text)
            ),
        )

    def _text(self, value: object) -> str:
        return str(value or "").strip()


__all__ = ("ItemCommandTranslationRejectionEvidenceParser",)
