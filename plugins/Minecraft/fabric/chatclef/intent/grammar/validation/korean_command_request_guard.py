#20260915_kpopmodder: Reject noncommand utterances before lossy normalization or LLM repair.
from __future__ import annotations

import re
import unicodedata

from ...chatclef_intent_dto import ChatClefIntentDTO


class KoreanCommandRequestGuard:
    SOURCE = "all_commands_guard"
    _NONCOMMAND = re.compile(
        r"[?？\"'“”‘’「」『』]|하지\s*(?:마|말)|주지\s*(?:마|말)|"
        r"(?:말아|말고|말자|않아|않을|않는|아니야|금지)|"
        r"(?:^|\s)안\s+(?:가|해|공격|보관|켜|꺼|따라|구해)|"
        r"(?:어떻게|뭐야|무엇|라고|라는|라며|말했)|"
        r"(?:^|\s)(?:설명(?:해|해줘|해\s*줘)?|예시|만약|가정)(?:\s|$)|"
        r"(?:그리고|그다음|한\s*다음|하지만)|\b(?:and|then|not)\b",
        re.IGNORECASE,
    )

    @classmethod
    def reason(cls, text: object) -> str | None:
        if not isinstance(text, str) or len(text) > 2048:
            return "invalid_command_utterance"
        if any(unicodedata.category(c)[0] == "C" for c in text):
            return "command_control_character"
        if cls._NONCOMMAND.search(text):
            return "command_non_executable_utterance"
        return None

    @classmethod
    def reject(cls, text: object, reason: str) -> ChatClefIntentDTO:
        return ChatClefIntentDTO(original_text=str(text or ""), source=cls.SOURCE,
                                slots={"all_commands_guard": reason})
