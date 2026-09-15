#20260915_kpopmodder: Resolve bounded Korean presentation labels separately from descriptor admission.
from __future__ import annotations

import re
from collections.abc import Mapping
import unicodedata


class CommandFeedbackSpokenLabelResolver:
    _SAFE_KOREAN_LABEL = re.compile(r"[가-힣0-9 ]{1,64}\Z")

    def __init__(self, display_names, item_phrases):
        self._display_names = display_names
        self._item_phrases = item_phrases

    def resolve(self, target, item_phrase, labels=None):
        # Only validated translation metadata supplies runtime/mod labels; no command text is echoed.
        label = labels.get(target) if isinstance(labels, Mapping) else None
        if (type(label) is str and 1 <= len(label) <= 256
                and any("가" <= c <= "힣" for c in label)
                and not any(unicodedata.category(c)[0] == "C" for c in label)):
            return label
        phrase = str(item_phrase or "").strip()
        if target and self._SAFE_KOREAN_LABEL.fullmatch(phrase):
            try:
                resolution = self._item_phrases.resolve(phrase)
            except Exception:
                resolution = {}
            if resolution.get("status") == "validated" and resolution.get("target") == target:
                return phrase
        if target and target in self._display_names.display_names:
            return self._display_names.display_names[target]
        return "요청한 아이템" if target else ""
