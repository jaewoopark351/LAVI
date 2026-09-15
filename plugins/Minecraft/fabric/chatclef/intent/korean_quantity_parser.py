#20260803_kpopmodder: Added quantity parsing separate from Korean intent rules.
from __future__ import annotations

import re
import unicodedata


class KoreanQuantityParser:
    #20260915_kpopmodder: Parse one complete quantity; malformed or repeated values never become defaults.
    _DIGIT_RE = re.compile(r"(?<![A-Za-z0-9_가-힣.])([+-]?[0-9]+(?:[.,][0-9]+)?)\s*(개|마리|포인트|점|만큼)(?:만큼)?")
    _KOREAN_NUMBERS = {
        "하나": 1,
        "한": 1,
        "둘": 2,
        "두": 2,
        "셋": 3,
        "세": 3,
        "넷": 4,
        "네": 4,
        "다섯": 5, "여섯": 6, "일곱": 7, "여덟": 8, "아홉": 9,
        "열": 10, "스물": 20, "스무": 20, "서른": 30, "마흔": 40,
        "쉰": 50, "예순": 60, "일흔": 70, "여든": 80, "아흔": 90,
    }
    _SINO_DIGITS = {"영": 0, "공": 0, "일": 1, "이": 2, "삼": 3, "사": 4,
                    "오": 5, "육": 6, "칠": 7, "팔": 8, "구": 9}

    def parse(self, text: str, default: int = 1) -> int:
        parsed = self.parse_optional(text)
        if parsed is None:
            return default
        return parsed

    def parse_optional(self, text: str) -> int | None:
        text = self._normalized(text)
        matches = self._matches(text)
        if not matches:
            return None
        if len(matches) != 1:
            raise ValueError("duplicate_quantity")
        return self._number(matches[0].group(1))

    def strip_quantity(self, text: str) -> str:
        text = self._normalized(text)
        matches = self._matches(text)
        if len(matches) > 1:
            raise ValueError("duplicate_quantity")
        if not matches:
            return text.strip()
        match = matches[0]
        self._number(match.group(1))
        return re.sub(r"\s+", " ", text[:match.start()] + " " + text[match.end():]).strip()

    def _korean_match(self, text: str) -> re.Match[str] | None:
        return self._korean_pattern().search(text)

    @staticmethod
    def _normalized(text):
        # Preserve decimal punctuation while retaining the existing fullwidth input contract.
        normalized = unicodedata.normalize("NFKC", text)
        if any(unicodedata.category(c) == "Nd" and c not in "0123456789" for c in normalized):
            raise ValueError("unsupported_decimal_quantity")
        return normalized

    def _matches(self, text):
        result = list(self._DIGIT_RE.finditer(text))
        result.extend(self._korean_pattern().finditer(text))
        result.sort(key=lambda match: match.start())
        return result

    def _korean_pattern(self):
        units = {k for k, v in self._KOREAN_NUMBERS.items() if v < 10}
        tens = {k for k, v in self._KOREAN_NUMBERS.items() if v >= 10}
        words = set(self._KOREAN_NUMBERS)
        words.update(t + u for t in tens if t != "스무" for u in units)
        vocabulary = "|".join(sorted(words, key=len, reverse=True))
        return re.compile(rf"(?<![가-힣A-Za-z0-9_])({vocabulary}|[영공일이삼사오육칠팔구십백천만억]+)"
                          r"(?:\s*(?:개|마리|포인트|점|만큼)(?:만큼)?|(?![가-힣]))")

    def _number(self, token):
        if re.fullmatch(r"[+-]?[0-9]+", token):
            if len(token.lstrip("+-0")) > 10:
                raise ValueError("quantity_out_of_range")
            return int(token)
        if any(c in token for c in ".,+-"):
            raise ValueError("invalid_quantity")
        if token in self._KOREAN_NUMBERS:
            return self._KOREAN_NUMBERS[token]
        for word, value in self._KOREAN_NUMBERS.items():
            if value >= 10 and token.startswith(word):
                unit = self._KOREAN_NUMBERS.get(token[len(word):])
                if unit is not None and unit < 10:
                    return value + unit
        if len(token) > 32:
            raise ValueError("quantity_out_of_range")
        total, group, digit, previous, previous_large = 0, 0, None, 10000, 1000000000000
        for character in token:
            if character in self._SINO_DIGITS:
                if digit is not None:
                    raise ValueError("invalid_quantity")
                digit = self._SINO_DIGITS[character]
            elif character in "십백천":
                multiplier = {"십": 10, "백": 100, "천": 1000}[character]
                if multiplier >= previous:
                    raise ValueError("invalid_quantity")
                group += (digit if digit is not None else 1) * multiplier
                digit, previous = None, multiplier
            elif character in "만억":
                multiplier = {"만": 10000, "억": 100000000}[character]
                if multiplier >= previous_large:
                    raise ValueError("invalid_quantity")
                group += digit or 0
                total += (group or 1) * multiplier
                group, digit, previous = 0, None, 10000
                previous_large = multiplier
            else:
                raise ValueError("invalid_quantity")
        return total + group + (digit or 0)
