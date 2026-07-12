# safety_filter.py
# 20260613_kpopmodder
import os
import re
import json
from typing import List, Tuple, Optional


try:
    from core.logger import log_print
except Exception:
    def log_print(message):
        print(message)


BASE_DIR = os.path.dirname(os.path.abspath(__file__))

CONFIG_DIR = os.path.join(
    BASE_DIR,
    "config",
    "safety_filter"
)

CONFIG_PATH = os.path.join(
    CONFIG_DIR,
    "safety_filter.json"
)

BANNED_WORDS_PATH = os.path.join(
    CONFIG_DIR,
    "banned_words.txt"
)

ALLOW_WORDS_PATH = os.path.join(#20260615_kpopmodder
    CONFIG_DIR,
    "allow_words.txt"
)

#20260613_kpopmodder#"mode": "replace" OR "mode": "Block" 일 때, 서로 다르게 처리함
DEFAULT_CONFIG = {
    "enabled": True,
    "replacement": "검열됨",
    "block_message": "그 말은 방송에서 말할 수 없습니다.",
    "mode": "replace",
    "max_text_length": 2000,
    "log_enabled": True,
    "log_clean_text": True,
    "allow_words_file": "allow_words.txt"#20260615_kpopmodder
}


class SafetyFilter:#20260613_kpopmodder
    def __init__(self):
        self.config = self.load_config()
        self.banned_words = self.load_banned_words()
        self.allow_words = self.load_allow_words()
        self.patterns = self.build_patterns(self.banned_words)

        self.log_info(
            f"[SafetyFilter] loaded: enabled={self.config.get('enabled')}, "
            f"mode={self.config.get('mode')}, "
            f"banned_words={len(self.banned_words)}, "
            f"allow_words={len(self.allow_words)}, "
            f"patterns={len(self.patterns)}"
        )

    def log_info(self, message: str):
        if self.config.get("log_enabled", True):
            log_print(message)

    def load_config(self) -> dict:
        if not os.path.exists(CONFIG_PATH):
            os.makedirs(CONFIG_DIR, exist_ok=True)
            with open(CONFIG_PATH, "w", encoding="utf-8") as f:
                json.dump(DEFAULT_CONFIG, f, ensure_ascii=False, indent=4)
            return DEFAULT_CONFIG.copy()

        try:
            with open(CONFIG_PATH, "r", encoding="utf-8") as f:
                data = json.load(f)

            config = DEFAULT_CONFIG.copy()
            config.update(data)
            return config

        except Exception as e:
            print(f"[SafetyFilter] config load failed: {e}")
            return DEFAULT_CONFIG.copy()

    def load_banned_words(self) -> List[str]:
        if not os.path.exists(BANNED_WORDS_PATH):
            os.makedirs(CONFIG_DIR, exist_ok=True)
            with open(BANNED_WORDS_PATH, "w", encoding="utf-8") as f:
                f.write("시발\n씨발\nㅅㅂ\n병신\n좆\nfuck\nshit\n")

        words = []

        try:
            with open(BANNED_WORDS_PATH, "r", encoding="utf-8") as f:
                for line in f:
                    word = line.strip()
                    if word and not word.startswith("#"):
                        words.append(word)

        except Exception as e:
            print(f"[SafetyFilter] banned_words load failed: {e}")

        return words

    def load_allow_words(self) -> List[str]:#20260615_kpopmodder
        allow_file = self.config.get("allow_words_file", "allow_words.txt")

        allow_path = os.path.join(
            CONFIG_DIR,
            allow_file
        )

        if not os.path.exists(allow_path):
            os.makedirs(CONFIG_DIR, exist_ok=True)

            default_allow_words = [
                "감염병",
                "전염병",
                "질병",
                "병원균",
                "바이러스",
                "생물학",
                "생물학적",
                "유전자",
                "유전자 조작",
                "삽입",
                "음모",
                "음모론",
            ]

            with open(allow_path, "w", encoding="utf-8") as f:
                for word in default_allow_words:
                    f.write(word + "\n")

        words = []

        try:
            with open(allow_path, "r", encoding="utf-8") as f:
                for line in f:
                    word = line.strip()
                    if word and not word.startswith("#"):
                        words.append(word)

        except Exception as e:
            print(f"[SafetyFilter] allow_words load failed: {e}")

        return words

    def normalize_text(self, text: str) -> str:
        if not text:
            return ""

        text = str(text)

        max_len = int(self.config.get("max_text_length", 2000))
        if len(text) > max_len:
            self.log_info(
                f"[SafetyFilter] text truncated: original_len={len(text)}, max_len={max_len}"
            )
            text = text[:max_len]

        return text

    def word_to_loose_regex(self, word: str) -> str:
        escaped_chars = [re.escape(ch) for ch in word]
        gap = r"[\s\.\-_\*\~\!]*"
        return gap.join(escaped_chars)

    def build_patterns(self, words: List[str]) -> List[Tuple[re.Pattern, str]]:
        patterns = []

        for word in words:
            if not word:
                continue

            loose = self.word_to_loose_regex(word)

            try:
                patterns.append((re.compile(loose, re.IGNORECASE), word))
            except re.error:
                patterns.append((re.compile(re.escape(word), re.IGNORECASE), word))

        extra_patterns = [
            (r"시+발+", "extra:시발"),
            (r"씨+발+", "extra:씨발"),
            (r"ㅅ+ㅂ+", "extra:ㅅㅂ"),
            (r"ㅆ+ㅂ+", "extra:ㅆㅂ"),
            (r"병+신+", "extra:병신"),
            (r"븅+신+", "extra:븅신"),
            (r"좆+", "extra:좆"),
            (r"f+u+c+k+", "extra:fuck"),
            (r"s+h+i+t+", "extra:shit")
        ]

        for pattern, label in extra_patterns:
            try:
                patterns.append((re.compile(pattern, re.IGNORECASE), label))
            except re.error as e:
                self.log_info(f"[SafetyFilter] regex compile failed: {label}, error={e}")

        return patterns

    def protect_allow_words(self, text: str) -> Tuple[str, dict]:#20260615_kpopmodder
        protected = {}

        if not text:
            return text, protected

        for i, word in enumerate(self.allow_words):
            if not word:
                continue

            token = f"__SAFETY_ALLOW_{i}__"

            if word in text:
                protected[token] = word
                text = text.replace(word, token)

        return text, protected


    def restore_allow_words(self, text: str, protected: dict) -> str:#20260615_kpopmodder
        for token, word in protected.items():
            text = text.replace(token, word)

        return text

    def find_first_bad_word(self, text: str) -> Optional[Tuple[str, str]]:
        text = self.normalize_text(text)

        for pattern, label in self.patterns:
            match = pattern.search(text)
            if match:
                return label, match.group(0)

        return None

    def contains_bad_word(self, text: str) -> bool:
        return self.find_first_bad_word(text) is not None

    def filter_text(self, text: str) -> str:
        original_text = self.normalize_text(text)

        if not self.config.get("enabled", True):#20260615_kpopmodder
            return original_text

        protected_text, protected_words = self.protect_allow_words(original_text)#20260615_kpopmodder

        mode = self.config.get("mode", "replace")
        replacement = self.config.get("replacement", "검열됨")
        block_message = self.config.get("block_message", "그 말은 방송에서 말할 수 없습니다.")
        log_clean_text = self.config.get("log_clean_text", True)

        #first_detected = self.find_first_bad_word(original_text)#20260615_kpopmodder
        first_detected = self.find_first_bad_word(protected_text)#20260615_kpopmodder

        if mode == "block":
            if first_detected:
                label, matched = first_detected
                self.log_info(
                    f"[SafetyFilter] BLOCK detected={label}, matched='{matched}', "
                    f"original='{original_text}'"
                )
                return block_message

            return original_text

        #filtered_text = original_text#20260615_kpopmodder
        filtered_text = protected_text#20260615_kpopmodder
        detected_items = []

        for pattern, label in self.patterns:
            matches = pattern.findall(filtered_text)
            if matches:
                detected_items.append((label, matches))
                filtered_text = pattern.sub(replacement, filtered_text)

        filtered_text = self.restore_allow_words(filtered_text, protected_words)#20260615_kpopmodder

        if detected_items:
            detected_summary = ", ".join(
                [f"{label}:{matches}" for label, matches in detected_items]
            )

            if log_clean_text:
                self.log_info(
                    f"[SafetyFilter] REPLACE detected={detected_summary}, "
                    f"original='{original_text}', filtered='{filtered_text}'"
                )
            else:
                self.log_info(
                    f"[SafetyFilter] REPLACE detected={detected_summary}"
                )

        
        return filtered_text


_safety_filter = SafetyFilter()


def clean_text(text: str) -> str:
    return _safety_filter.filter_text(text)


def is_unsafe(text: str) -> bool:
    return _safety_filter.contains_bad_word(text)
