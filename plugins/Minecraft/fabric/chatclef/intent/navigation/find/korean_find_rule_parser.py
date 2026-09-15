#20260914_kpopmodder: Interpret one whole Korean FIND request without LLM repair or item acquisition.
from __future__ import annotations

import re
import unicodedata

from ...chatclef_intent_dto import ChatClefIntentDTO
from ...chatclef_intent_type import ChatClefIntentType
from .find_request import FindRequest


class KoreanFindRuleParser:
    GUARD_SOURCE = "find_guard"
    _RAW = re.compile(r"^@?find(?:\s|$)", re.IGNORECASE)
    _CANDIDATE = re.compile(r"찾(?:아|지)|위치\s*만?\s*알려")
    _REQUEST = re.compile(
        r"^(?P<target>.+?)\s*(?:"
        r"(?P<report>위치\s*만\s*알려\s*(?:줘|주세요)|찾아서\s*알려\s*(?:줘|주세요))"
        r"|찾아(?:서\s*(?:가\s*줘|가\s*주세요)|\s*(?:줘|주세요))?"
        r")\s*[.!。！]*$"
    )
    _NON_EXECUTABLE = re.compile(
        r"[?？]|(?:찾지|말아|말고|말자|말라고|하지\s*마|않|아니|안\s*찾|못\s*찾|"
        r"그리고|하지만|(?:찾아|가져|죽여|공격해|캐|보관해)\s*줘\s+|\b(?:and|then|not)\b)"
    )

    @classmethod
    def is_candidate(cls, text: object) -> bool:
        return bool(cls._RAW.search(str(text or "").strip()) or cls._CANDIDATE.search(str(text or "")))

    def parse(self, text: object) -> ChatClefIntentDTO | None:
        raw = str(text or "").strip()
        #20260915_kpopmodder: Explicit supported structures belong to their existing Java command.
        from ...grammar.control.korean_observation_rule_parser import KoreanObservationRuleParser
        observation = KoreanObservationRuleParser().parse(raw)
        if observation is not None and (observation.intent_type is ChatClefIntentType.LOCATE_STRUCTURE
                                        or observation.slots.get("all_commands_guard") in {"ambiguous_structure", "unsupported_structure"}):
            return None
        if not self.is_candidate(raw):
            return None
        try:
            if self._RAW.match(raw):
                request = FindRequest.parse(raw)
            else:
                if any(unicodedata.category(c)[0] == "C" or c in ";#\\\"'@" for c in raw):
                    raise ValueError("find_unsafe_utterance")
                if self._NON_EXECUTABLE.search(raw):
                    raise ValueError("find_non_executable_utterance")
                match = self._REQUEST.fullmatch(raw)
                if match is None:
                    raise ValueError("find_requires_explicit_request")
                target = match["target"].strip()
                target = re.sub(r"^(?:라비야?|LAVI)[, ]+", "", target, flags=re.IGNORECASE)
                target = re.sub(r"\s+(?:좀|제발)$", "", target).strip()
                target = re.sub(r"[을를]$", "", target).strip()
                kind = "auto"
                hints = (("떨어진 ", "item"), ("드롭된 ", "item"),
                         ("아이템 ", "item"), ("블록 ", "block"),
                         ("몹 ", "entity"), ("엔티티 ", "entity"), ("플레이어 ", "player"))
                for prefix, hint in hints:
                    if target.startswith(prefix):
                        kind, target = hint, target[len(prefix):].strip()
                        break
                #20260915_kpopmodder: Keep official names intact; Python resolves exact names before category suffix fallback.
                if kind == "auto" and target.endswith(" 블록"):
                    kind = "block"
                elif target.endswith(" 아이템"):
                    kind, target = "item", target[:-4].strip()
                elif target.endswith(" 몹"):
                    kind, target = "entity", target[:-2].strip()
                request = FindRequest(kind, " ".join(target.split()), "report" if match["report"] else "approach")
            return ChatClefIntentDTO(intent_type=ChatClefIntentType.FIND, original_text=raw,
                                     source="rule", slots=request.slots())
        except (ValueError, TypeError):
            return ChatClefIntentDTO(intent_type=ChatClefIntentType.UNKNOWN, original_text=raw,
                                     source=self.GUARD_SOURCE, slots={"find_guard": True})
