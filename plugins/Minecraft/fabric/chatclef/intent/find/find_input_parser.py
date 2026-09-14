#20260914_kpopmodder: Parse closed FIND requests before acquisition or LLM fallback.
from __future__ import annotations

from dataclasses import dataclass
import re
import unicodedata

from plugins.Minecraft.fabric.chatclef.transport.find_catalog.find_catalog_snapshot import safe_text


@dataclass(frozen=True, slots=True)
class FindInput:
    candidate: bool
    target_phrase: str = ""
    target_kind: str = ""
    mode: str = "report"
    reason: str = ""


class FindInputParser:
    _REQUEST = re.compile(r"(?P<target>.+?)(?:을|를)?\s*(?:찾아(?:줘|주세요|줄래)?|찾아\s*줘|위치(?:만)?\s*알려(?:줘|주세요))(?:[.!])?\Z")
    _APPROACH = re.compile(r"(?P<target>.+?)(?:을|를)?\s*찾아서\s*(?:가까이\s*)?(?:가줘|가주세요|접근해줘|다가가줘)(?:[.!])?\Z")
    _NATIVE = re.compile(r"find(?:\s|$)", re.ASCII | re.IGNORECASE)
    _KIND_PREFIX = re.compile(r"^(몹|엔티티|블록|아이템|플레이어)\s+")
    _KIND_SUFFIX = re.compile(r"\s+(몹|엔티티|블록|아이템|플레이어)$")
    _KINDS = {"몹": "entity", "엔티티": "entity", "블록": "block", "아이템": "item", "플레이어": "player"}

    def is_candidate(self, text: object) -> bool:
        return self.parse(text).candidate

    def parse(self, text: object) -> FindInput:
        if type(text) is not str:
            return FindInput(False)
        value = unicodedata.normalize("NFC", text).strip()
        if self._NATIVE.match(value):
            if not safe_text(value, 512):
                return FindInput(True, reason="invalid_find_target")
            units = value.split()
            if len(units) not in {3, 4}:
                return FindInput(True, reason="invalid_find_grammar")
            return FindInput(True, units[2], units[1], units[3] if len(units) == 4 else "report")
        match = self._APPROACH.fullmatch(value)
        mode = "approach" if match else "report"
        match = match or self._REQUEST.fullmatch(value)
        # Consume malformed FIND imperatives, but do not claim discussions about finding.
        if match is None:
            if re.search(r"찾아(?:줘|주세요|서.+(?:가줘|접근해줘))", value):
                return FindInput(True, reason="invalid_find_grammar")
            return FindInput(False)
        phrase = match.group("target").strip()
        if any(token in phrase for token in ("인벤토리", "소지품", "내용물", "보관한", "보관했", "얻는", "획득", "상자 안", "상자에 있는")):
            return FindInput(True, reason="unsupported_find_query")
        kind = ""
        prefix = self._KIND_PREFIX.search(phrase)
        suffix = self._KIND_SUFFIX.search(phrase)
        if prefix:
            kind, phrase = self._KINDS[prefix.group(1)], phrase[prefix.end():].strip()
        elif suffix:
            kind, phrase = self._KINDS[suffix.group(1)], phrase[:suffix.start()].strip()
        if phrase.startswith("떨어진 "):
            if kind and kind != "item":
                return FindInput(True, reason="conflicting_find_kind")
            kind, phrase = "item", phrase[4:].strip()
        if phrase.endswith(("을", "를")):
            phrase = phrase[:-1].strip()
        if not safe_text(phrase, 256) or not safe_text(value, 512):
            return FindInput(True, reason="invalid_find_target")
        return FindInput(True, phrase, kind, mode)
