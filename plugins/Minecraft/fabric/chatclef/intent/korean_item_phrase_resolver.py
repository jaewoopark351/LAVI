#20260803_kpopmodder: Added Korean item phrase resolution before ChatClef DSL compilation.
from __future__ import annotations

import re

from plugins.Minecraft.fabric.chatclef.intent.chatclef_alias_repository import (
    ChatClefKoreanAliasRepository,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_equipment_target_composer import (
    ChatClefEquipmentTargetComposer,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_status import (
    ChatClefIntentStatus,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_target_catalog import (
    ChatClefTargetCatalog,
)
from plugins.Minecraft.fabric.chatclef.intent.korean_text_normalizer import (
    KoreanTextNormalizer,
)


class KoreanItemPhraseResolver:
    _AMBIGUOUS_PHRASES = {"갑옷", "도구", "장비"}
    _UNKNOWN_PHRASES = {"아무거나"}
    _UNSUPPORTED_MATERIALS = {"구리", "동"}

    def __init__(
        self,
        aliases: ChatClefKoreanAliasRepository | None = None,
        composer: ChatClefEquipmentTargetComposer | None = None,
        normalizer: KoreanTextNormalizer | None = None,
        target_catalog: ChatClefTargetCatalog | None = None,
    ):
        self._aliases = aliases or ChatClefKoreanAliasRepository()
        self._composer = composer or ChatClefEquipmentTargetComposer()
        self._normalizer = normalizer or KoreanTextNormalizer()
        self._target_catalog = target_catalog

    def resolve(self, phrase: object) -> dict[str, object]:
        normalized = self._cleanup(self._normalizer.normalize(phrase))
        compact = normalized.replace(" ", "")
        if not normalized:
            return self._result(ChatClefIntentStatus.UNKNOWN, "empty_item_phrase")
        if compact in self._UNKNOWN_PHRASES:
            return self._result(ChatClefIntentStatus.UNKNOWN, "unknown_item_phrase")
        equipment_result = self._resolve_equipment(normalized, compact)
        if equipment_result is not None:
            return equipment_result
        fixed_result = self._resolve_fixed_alias(normalized, compact)
        if fixed_result is not None:
            return fixed_result
        catalog_result = self._resolve_direct_catalog_target(normalized, compact)
        if catalog_result is not None:
            return catalog_result
        if self._has_ambiguous_phrase(compact):
            return self._result(ChatClefIntentStatus.AMBIGUOUS, "ambiguous_item_phrase")
        if self._has_unsupported_material(compact):
            return self._result(ChatClefIntentStatus.UNSUPPORTED, "unsupported_material")
        return self._result(ChatClefIntentStatus.UNKNOWN, "unknown_item_phrase")

    def supports_equipment_target(self, target: object) -> bool:
        return str(target or "").strip() in self._composer.all_targets()

    def _resolve_equipment(
        self,
        normalized: str,
        compact: str,
    ) -> dict[str, object] | None:
        for material_alias, material in self._aliases.sorted_aliases(
            self._aliases.material_aliases
        ):
            material_compact = material_alias.replace(" ", "")
            for equipment_alias, equipment in self._aliases.sorted_aliases(
                self._aliases.equipment_aliases
            ):
                equipment_compact = equipment_alias.replace(" ", "")
                if material_compact + equipment_compact != compact:
                    continue
                target = self._composer.compose(material, equipment)
                if target is None:
                    return self._result(
                        ChatClefIntentStatus.UNSUPPORTED,
                        "unsupported_equipment_material",
                        material=material,
                        equipment=equipment,
                    )
                return self._result(
                    ChatClefIntentStatus.VALIDATED,
                    "resolved_equipment",
                    target=target,
                    material=material,
                    equipment=equipment,
                    phrase=normalized,
                )
        return None

    def _resolve_fixed_alias(
        self,
        normalized: str,
        compact: str,
    ) -> dict[str, object] | None:
        for alias, target in self._aliases.sorted_aliases(self._aliases.fixed_item_aliases):
            alias_compact = self._normalizer.normalize(alias).replace(" ", "")
            if alias_compact == compact:
                return self._result(
                    ChatClefIntentStatus.VALIDATED,
                    "resolved_fixed_alias",
                    target=target,
                    phrase=normalized,
                )
        return None

    def _resolve_direct_catalog_target(
        self,
        normalized: str,
        compact: str,
    ) -> dict[str, object] | None:
        target = normalized.replace(" ", "_")
        compact_target = compact.replace(" ", "_")
        for candidate in {target, compact_target}:
            if candidate and self._catalog().contains(candidate):
                return self._result(
                    ChatClefIntentStatus.VALIDATED,
                    "resolved_direct_catalog_target",
                    target=candidate,
                    phrase=normalized,
                )
        return None

    def _catalog(self) -> ChatClefTargetCatalog:
        if self._target_catalog is None:
            self._target_catalog = ChatClefTargetCatalog()
        return self._target_catalog

    def _cleanup(self, text: str) -> str:
        text = re.sub(r"\b(?:좀|제발|please)\b", " ", text)
        text = re.sub(r"\s+", " ", text).strip()
        text = re.sub(r"(?<=[가-힣A-Za-z0-9_])(?:을|를)$", "", text)
        return text

    def _has_ambiguous_phrase(self, compact: str) -> bool:
        return any(phrase in compact for phrase in self._AMBIGUOUS_PHRASES)

    def _has_unsupported_material(self, compact: str) -> bool:
        return any(material in compact for material in self._UNSUPPORTED_MATERIALS)

    def _result(
        self,
        status: ChatClefIntentStatus,
        reason_code: str,
        **data: object,
    ) -> dict[str, object]:
        return {
            "status": status.value,
            "target": data.pop("target", None),
            "reason_code": reason_code,
            "data": data,
        }
