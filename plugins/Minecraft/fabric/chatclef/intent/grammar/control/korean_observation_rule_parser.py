#20260915_kpopmodder: Keep legacy block scanning and supported structure requests separate from FIND.
from __future__ import annotations

import re

from ...chatclef_intent_dto import ChatClefIntentDTO
from ...chatclef_intent_type import ChatClefIntentType as I
from ..validation.korean_command_request_guard import KoreanCommandRequestGuard as Guard


class KoreanObservationRuleParser:
    def parse(self, text: object) -> ChatClefIntentDTO | None:
        raw = str(text or "").strip()
        structure = re.fullmatch(r"(?P<target>엔드\s*요새|사막\s*사원|요새|.+?\s*구조물)(?:로|를|을)?\s*(?:찾아가|탐색해|찾아)(?:\s*(?:줘|주세요))?[.!。！]*", raw)
        if structure:
            target = structure["target"].replace(" ", "")
            supported = {"엔드요새": "stronghold", "사막사원": "desert_temple"}
            if target not in supported:
                return Guard.reject(raw, "ambiguous_structure" if target == "요새" else "unsupported_structure")
            return ChatClefIntentDTO(intent_type=I.LOCATE_STRUCTURE, original_text=raw, source="rule", slots={"structure": supported[target]})
        scan = re.fullmatch(r"(?P<target>.*?)\s*(?:위치\s*)?스캔해(?:\s*(?:줘|주세요))?[.!。！]*", raw)
        if scan:
            phrase = scan["target"].strip()
            phrase = re.sub(r"(?:\s*블록)?\s*(?:위치|좌표)?$", "", phrase).strip()
            return ChatClefIntentDTO(intent_type=I.SCAN, item_phrase=phrase, original_text=raw, source="rule")
        return None
