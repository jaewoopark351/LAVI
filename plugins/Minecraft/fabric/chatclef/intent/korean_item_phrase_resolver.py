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
    ):
        self._aliases = aliases or ChatClefKoreanAliasRepository()
        self._composer = composer or ChatClefEquipmentTargetComposer()
        self._normalizer = normalizer or KoreanTextNormalizer()

    def resolve(self, phrase: object) -> dict[str, object]:
        normalized = self._cleanup(self._normalizer.normalize(phrase))
        compact = normalized.replace(" ", "")
        if not normalized:
            return self._result(ChatClefIntentStatus.UNKNOWN, "empty_item_phrase")
        if compact in self._UNKNOWN_PHRASES:
            return self._result(ChatClefIntentStatus.UNKNOWN, "unknown_item_phrase")
        if self._has_ambiguous_phrase(compact):
            return self._result(ChatClefIntentStatus.AMBIGUOUS, "ambiguous_item_phrase")
        equipment_result = self._resolve_equipment(normalized, compact)
        if equipment_result is not None:
            return equipment_result
        fixed_result = self._resolve_fixed_alias(normalized, compact)
        if fixed_result is not None:
            return fixed_result
        if self._has_unsupported_material(compact):
            return self._result(ChatClefIntentStatus.UNSUPPORTED, "unsupported_material")
        return self._result(ChatClefIntentStatus.UNKNOWN, "unknown_item_phrase")

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
            if alias.replace(" ", "") == compact:
                return self._result(
                    ChatClefIntentStatus.VALIDATED,
                    "resolved_fixed_alias",
                    target=target,
                    phrase=normalized,
                )
        return None

    def _cleanup(self, text: str) -> str:
        text = re.sub(r"\b(?:좀|제발|please)\b", " ", text)
        text = re.sub(r"\s+", " ", text).strip()
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
