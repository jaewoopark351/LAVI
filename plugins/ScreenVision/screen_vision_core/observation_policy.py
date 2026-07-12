#20260621_kpopmodder: ScreenVision 관찰 결과 정규화/깨진 출력 필터 강화.
from collections import Counter
from difflib import SequenceMatcher
from pathlib import Path
import re

from .observation_policy_text import (
    canonical_decision_text,
    compact_decision_text,
    load_broken_exact_texts,
    normalize_observation_text,
    strip_screen_prefix,
)


class ObservationPolicy:#20260621_kpopmodder
    """ScreenVision 관찰 결과를 LLM/TTS에 전달할지 판단한다."""

    MAX_ACCEPTED_OBSERVATION_CHARS = 700
    MAX_SUMMARY_SEGMENTS = 5

    NO_IMPORTANT_CHANGE_PHRASES = (
        "현재 화면에 중요한 변화가 없습니다",
        "현재 화면에는 중요한 변화가 없습니다",
        "중요한 변화가 없습니다",
        "특별한 변화가 없습니다",
        "새로운 프로그램, 문서, 코드, 오류, 게임 상태 등이 나타나지 않았습니다",
    )

    NO_CHANGE_EXACT_TEXTS = {
        "중요한 변화 없음",
        "변화 없음",
        "특별한 변화 없음",
        "없음",
        "없습니다",
        "해당 없음",
    }

    #20260621_kpopmodder: 기본 차단값은 안전망으로 유지하고, 실제 운영 차단값은 txt에서 추가 로드한다.
    DEFAULT_BROKEN_EXACT_TEXTS = {
        "n/a",
        "na",
        "none",
        "null",
        "undefined",
        "unknown",
        "모름",
        "모르겠습니다",
        "화면에는",
        "현재 화면에는",
        "pc 화면에는",
        "화면에",
        "현재 화면에",
    }

    UNCERTAIN_EXACT_TEXTS = {
        "\ud655\uc2e4\ud558\uc9c0 \uc54a\uc74c",
        "\uc54c \uc218 \uc5c6\uc74c",
    }

    BROKEN_TEXT_FILE_NAME = "broken_observation_texts.txt"

    SCREEN_PREFIXES = (
        "현재 화면에는 ",
        "PC 화면에는 ",
        "pc 화면에는 ",
        "화면에는 ",
        "현재 화면에 ",
        "PC 화면에 ",
        "pc 화면에 ",
        "화면에 ",
    )

    DECISION_PUNCTUATION = ".,!?:;~…。！？、\"'`“”‘’()[]{}<>"

    _EXIT_CODE_NOISE_PATTERNS = (
        re.compile(r"(?is)```.*?(?:\b(?:r?_?exit_code|exit code)\b).*?```"),
        re.compile(
            r"(?im)^\s*\d+[.)]\s*.*(?:-\s+|\*\s+|•\s+).*(?:r?_?exit_code|exit code)\b",
        ),
        re.compile(r"(?i)(?:^|\n)\s*[-*•]\s*.*(?:r?_?exit_code|exit code)\b"),
        re.compile(r"(?i)\b(r?_?exit_code)\b\s*[:=]\s*\d+"),
    )

    DUPLICATE_SUFFIXES = (
        "이 보입니다",
        "가 보입니다",
        "이 보임",
        "가 보임",
        "이 표시됩니다",
        "가 표시됩니다",
        "이 나타납니다",
        "가 나타납니다",
    )

    DUPLICATE_NOISE_WORDS = (
        "알림",
        "문구",
        "텍스트",
        "자막",
        "문장",
        "글자",
        "글씨",
        "단어",
        "표시",
    )

    def __init__(self, duplicate_similarity=0.96, broken_texts_path=None):
        self.duplicate_similarity = duplicate_similarity

        #20260621_kpopmodder: 코드 수정 없이 broken_observation_texts.txt로 깨진 관찰값을 추가 차단한다.
        broken_texts = set(self.DEFAULT_BROKEN_EXACT_TEXTS)
        broken_texts.update(self._load_broken_exact_texts(broken_texts_path))

        self.broken_exact_texts = {
            self._canonical_decision_text(text)
            for text in broken_texts
            if self._canonical_decision_text(text)
        }
        self.broken_compact_texts = {
            self._compact_decision_text(text)
            for text in self.broken_exact_texts
            if self._compact_decision_text(text)
        }
        self.uncertain_exact_texts = {
            self._canonical_decision_text(text)
            for text in self.UNCERTAIN_EXACT_TEXTS
            if self._canonical_decision_text(text)
        }
        self.uncertain_compact_texts = {
            self._compact_decision_text(text)
            for text in self.uncertain_exact_texts
            if self._compact_decision_text(text)
        }

    def _load_broken_exact_texts(self, broken_texts_path=None):
        #20260706_kpopmodder: Keep the policy facade stable while helper owns optional file loading.
        return load_broken_exact_texts(
            broken_texts_path=broken_texts_path,
            default_config_dir=Path(__file__).parents[1] / "config",
            file_name=self.BROKEN_TEXT_FILE_NAME,
        )

    def normalize(self, observation):
        return normalize_observation_text(observation, self.SCREEN_PREFIXES)

    def strip_screen_prefix(self, text):
        return strip_screen_prefix(text, self.SCREEN_PREFIXES)

    def normalize_for_decision(self, observation):
        return self._canonical_decision_text(observation)

    def _canonical_decision_text(self, observation):
        return canonical_decision_text(
            observation,
            self.SCREEN_PREFIXES,
            self.DECISION_PUNCTUATION,
        )

    def _compact_decision_text(self, observation):
        return compact_decision_text(observation)

    def _is_uncertain_observation(self, decision_text):#20260623_kpopmodder
        decision_compact = self._compact_decision_text(decision_text)
        return (
            decision_text in self.uncertain_exact_texts
            or decision_compact in self.uncertain_compact_texts
        )

    def _has_repeated_laughter(self, text):#20260623_kpopmodder
        #20260623_kpopmodder: Korean chat laughter can be real screen text, so allow it by itself.
        # compact = self._compact_decision_text(text)#20260623_kpopmodder
        # return bool(re.search(r"[\u314b\u314e]{3,}", compact))#20260623_kpopmodder
        return False

    def _trim_observation_summary(self, text):#20260623_kpopmodder
        text = self.normalize(text)
        if len(text) <= self.MAX_ACCEPTED_OBSERVATION_CHARS:
            return text

        trimmed = text[: self.MAX_ACCEPTED_OBSERVATION_CHARS].rsplit(" ", 1)[0]
        return trimmed.strip() or text[: self.MAX_ACCEPTED_OBSERVATION_CHARS].strip()

    def summarize_if_long(self, observation):#20260623_kpopmodder
        text = self.normalize(observation)
        if len(text) <= self.MAX_ACCEPTED_OBSERVATION_CHARS:
            return text

        #20260623_kpopmodder: Long code/list/URL-heavy observations should be remembered as summaries, not rejected.
        raw_source = str(observation or "")
        summary_source = re.sub(r"```.*?```", " ", raw_source, flags=re.DOTALL)
        #20260705_kpopmodder: Preserve line/list boundaries before normalize() collapses them so repeated ScreenVision bullets are deduped.
        line_parts = [
            self.normalize(part.strip(" -\t"))
            for part in re.split(r"[\r\n]+", summary_source)
        ]
        line_parts = [part for part in line_parts if part]
        if len(line_parts) >= 2:
            parts = line_parts
        else:
            summary_source = self.normalize(summary_source) or text
            parts = re.split(
                r"(?<=[.!?])\s+|\s+-\s+|\s+\d+\.\s+",
                summary_source,
            )

        selected = []
        selected_keys = set()
        total_chars = 0
        for part in parts:
            part = self.normalize(part.strip(" -"))
            if len(part) < 8:
                continue

            key = self.normalize_for_decision(part)
            if not key or key in selected_keys:
                continue

            if (
                selected
                and total_chars + len(part) > self.MAX_ACCEPTED_OBSERVATION_CHARS
            ):
                break

            selected.append(part)
            selected_keys.add(key)
            total_chars += len(part) + 1

            if len(selected) >= self.MAX_SUMMARY_SEGMENTS:
                break

        summary = " ".join(selected) if selected else summary_source
        return self._trim_observation_summary(summary)

    def describe_summary(self, original_observation, summarized_observation):#20260623_kpopmodder
        original = self.normalize(original_observation)
        summarized = self.normalize(summarized_observation)
        if not original or len(summarized) >= len(original):
            return ""
        return f"summarized={len(original)}->{len(summarized)}"

    def normalize_for_duplicate(self, observation):
        text = self.normalize_for_decision(observation)

        for suffix in self.DUPLICATE_SUFFIXES:
            if text.endswith(suffix):
                text = text[: -len(suffix)].strip()
                break

        for word in self.DUPLICATE_NOISE_WORDS:
            text = re.sub(
                rf"(?<![가-힣A-Za-z0-9]){re.escape(word)}(?![가-힣A-Za-z0-9])",
                " ",
                text,
            )

        text = re.sub(r"\s+", " ", text).strip()
        return text

    def is_no_important_change(self, observation):
        text = str(observation or "")
        decision_text = self.normalize_for_decision(text)
        return (
            any(phrase in text for phrase in self.NO_IMPORTANT_CHANGE_PHRASES)
            or decision_text in self.NO_CHANGE_EXACT_TEXTS
            or "중요한 변화 없음" in decision_text
        )

    #20260623_kpopmodder: Explain current filter decisions without changing thresholds.
    def describe_no_important_change(self, observation):#20260623_kpopmodder
        text = str(observation or "")
        decision_text = self.normalize_for_decision(text)

        for phrase in self.NO_IMPORTANT_CHANGE_PHRASES:
            if phrase in text:
                return f"matched_phrase={phrase!r}"

        if decision_text in self.NO_CHANGE_EXACT_TEXTS:
            return f"exact_no_change={decision_text!r}"

        if "중요한 변화 없음" in decision_text:
            return "contains_no_change_phrase"

        return ""

    def describe_broken(self, observation):#20260623_kpopmodder
        if not self.is_broken(observation):
            return ""

        raw_text = str(observation or "").strip()
        text = self.normalize(raw_text)
        if not text:
            return "empty_after_normalize"

        if self._looks_like_exit_code_ui_noise(raw_text, text):
            return "exit_code_ui_snippet"

        decision_text = self.normalize_for_decision(text)
        if self._is_uncertain_observation(decision_text):
            return f"uncertain_observation={decision_text!r}"
        if decision_text in self.broken_exact_texts:
            return f"exact_blocklist={decision_text!r}"
        if decision_text in self.NO_CHANGE_EXACT_TEXTS:
            return f"exact_no_change={decision_text!r}"

        duplicate_text = self.normalize_for_duplicate(text)
        duplicate_compact = self._compact_decision_text(duplicate_text)
        if duplicate_text in self.broken_exact_texts:
            return f"duplicate_normalized_blocklist={duplicate_text!r}"
        if duplicate_compact in self.broken_compact_texts:
            return "duplicate_compact_blocklist"

        compact = "".join(text.split())
        lower_compact = compact.lower()
        decision_compact = self._compact_decision_text(decision_text)
        if lower_compact in self.broken_compact_texts:
            return "raw_compact_blocklist"
        if decision_compact in self.broken_compact_texts:
            return "decision_compact_blocklist"

        raw_lower = raw_text.strip().lower()
        raw_compact = "".join(raw_lower.split())
        if raw_compact in {"n/a", "n.a", "na"}:
            return f"raw_na={raw_compact!r}"

        if not any(ch.isalnum() for ch in compact):
            return "no_letters_or_digits"

        #20260623_kpopmodder: Absolute punctuation counts are too noisy for URLs, lists, and chat text.
        # if text.count("!") >= 4:
        #     return f"too_many_exclamation_marks={text.count('!')}"
        # if text.count("?") >= 6:
        #     return f"too_many_question_marks={text.count('?')}"
        #20260623_kpopmodder: Many periods can be normal in code, URLs, lists, timestamps, or YouTube text.
        # if text.count(".") >= 12:
        #     return f"too_many_periods={text.count('.')}"
        if self._has_repeated_laughter(text):
            return "repeated_laughter"
        if re.search(r"([!?.~])\1{3,}", compact):
            return "repeated_punctuation_run"

        if len(compact) >= 8:
            counter = Counter(compact)
            most_common_char, most_common_count = counter.most_common(1)[0]
            if most_common_count / max(len(compact), 1) >= 0.50:
                return (
                    "dominant_character="
                    f"{most_common_char!r}:{most_common_count}/{len(compact)}"
                )
            if len(set(compact)) <= 3:
                return f"low_unique_characters={len(set(compact))}"

        if len(text) < 8 and not re.search(r"[가-힣A-Za-z0-9]{3,}", text):
            return f"too_short={len(text)}"

        return "matched_broken_policy"

    def _looks_like_exit_code_ui_noise(self, raw_text, normalized_text):
        raw_text = str(raw_text or "")
        normalized_text = str(normalized_text or "")
        lowered = raw_text.lower()
        if "exit_code" not in lowered and "exit code" not in lowered:
            return False

        if any(pattern.search(raw_text) for pattern in self._EXIT_CODE_NOISE_PATTERNS):
            return True

        for line in [line.strip() for line in raw_text.splitlines() if line.strip()]:
            lower_line = line.lower()
            if "exit_code" not in lower_line and "exit code" not in lower_line:
                continue

            if re.search(r"^\s*(?:\d+[.)]\s*)?[-*•]\s+.*", line):
                return True

            if (
                re.search(r"['\"]r?_?exit_code['\"]", line)
                and len(normalized_text) < 280
            ):
                return True

        return False

    def is_broken(self, observation):
        raw_text = str(observation or "").strip()
        text = self.normalize(raw_text)
        if not text:
            return True

        decision_text = self.normalize_for_decision(text)
        if self._is_uncertain_observation(decision_text):
            return True
        if decision_text in self.broken_exact_texts:
            return True
        if decision_text in self.NO_CHANGE_EXACT_TEXTS:
            return True
        if self._looks_like_exit_code_ui_noise(raw_text, text):
            return True

        #20260621_kpopmodder: txt에는 핵심 대상만 적어도 "'대상'이 보입니다" 형태의 깨진 관찰값을 차단한다.
        duplicate_text = self.normalize_for_duplicate(text)#20260621_kpopmodder
        duplicate_compact = self._compact_decision_text(duplicate_text)#20260621_kpopmodder

        if duplicate_text in self.broken_exact_texts:#20260621_kpopmodder
            return True
        if duplicate_compact in self.broken_compact_texts:#20260621_kpopmodder
            return True

        compact = "".join(text.split())
        lower_compact = compact.lower()
        decision_compact = self._compact_decision_text(decision_text)

        if lower_compact in self.broken_compact_texts:
            return True
        if decision_compact in self.broken_compact_texts:
            return True
        # if "n/a" in raw_text.lower() or "n.a" in raw_text.lower():#20260622_kpopmodder
        #     return True

        raw_lower = raw_text.strip().lower()#20260622_kpopmodder
        raw_compact = "".join(raw_lower.split())#20260622_kpopmodder

        #20260622_kpopmodder: FPS N/A, GPU N/A 같은 정상 HUD 문장은 허용한다.
        # 단독 "N/A"처럼 관찰값 전체가 깨진 값일 때만 차단한다.
        if raw_compact in {"n/a", "n.a", "na"}:
            return True

        # 문장 내용 없이 구두점/감탄만 반복되는 출력 차단.
        if not any(ch.isalnum() or ("가" <= ch <= "힣") for ch in compact):
            return True
        #20260623_kpopmodder: Let no_letters_or_digits and repeated_punctuation_run catch real punctuation noise.
        # if text.count("!") >= 4 or text.count("?") >= 6:
        #     return True
        #20260623_kpopmodder: Do not reject long/list-like text only because it has many periods; summarize accepted text later.
        # if text.count(".") >= 12:
        #     return True
        if self._has_repeated_laughter(text):
            return True
        #20260623_kpopmodder: Keep punctuation-run blocking, but allow Korean chat laughter like ㅋㅋㅋ/ㅎㅎㅎ.
        if re.search(r"([!?.~])\1{3,}", compact):
            return True

        # 같은 글자만 과하게 반복되는 출력 차단.
        if len(compact) >= 8:
            counter = Counter(compact)
            _, most_common_count = counter.most_common(1)[0]
            if most_common_count / max(len(compact), 1) >= 0.50:
                return True
            if len(set(compact)) <= 3:
                return True

        # 너무 짧고 일반적인 조각은 TTS/LLM으로 보내지 않는다.
        if len(text) < 8 and not re.search(r"[가-힣A-Za-z0-9]{3,}", text):
            return True

        return False

    #20260623_kpopmodder: Log duplicate similarity so the cutoff can be judged from evidence.
    def describe_duplicate(self, previous_observation, current_observation):#20260623_kpopmodder
        previous = self.normalize_for_duplicate(previous_observation)
        current = self.normalize_for_duplicate(current_observation)

        if not previous or not current:
            return "missing_normalized_text"
        if previous == current:
            return "normalized_exact_match"

        similarity = SequenceMatcher(None, previous, current).ratio()
        return (
            f"similarity={similarity:.3f} "
            f"threshold={self.duplicate_similarity:.3f}"
        )

    def is_duplicate(self, previous_observation, current_observation):
        previous = self.normalize_for_duplicate(previous_observation)
        current = self.normalize_for_duplicate(current_observation)

        if not previous or not current:
            return False
        if previous == current:
            return True

        similarity = SequenceMatcher(None, previous, current).ratio()
        return similarity >= self.duplicate_similarity
    

# from collections import Counter
# from difflib import SequenceMatcher


# class ObservationPolicy:#20260621_kpopmodder
#     """ScreenVision 관찰 결과를 LLM/TTS에 전달할지 판단한다."""

#     #20260621_kpopmodder: 비전 모델의 출력 품질과 자동 전달 여부 판단을 UI 코드에서 분리한다.
#     NO_IMPORTANT_CHANGE_PHRASES = (
#         "현재 화면에 중요한 변화가 없습니다",
#         "현재 화면에는 중요한 변화가 없습니다",
#         "중요한 변화가 없습니다",
#         "특별한 변화가 없습니다",
#         "새로운 프로그램, 문서, 코드, 오류, 게임 상태 등이 나타나지 않았습니다",
#     )
#     NO_CHANGE_EXACT_TEXTS = {
#         "중요한 변화 없음",
#         "변화 없음",
#         "특별한 변화 없음",
#         "없음",
#         "없습니다",
#         "해당 없음",
#     }
#     DECISION_PUNCTUATION = ".,!?:;~…。！？、\"'`“”‘’()[]{}<>"

#     def __init__(self, duplicate_similarity=0.96):
#         self.duplicate_similarity = duplicate_similarity

#     def normalize(self, observation):
#         text = str(observation or "").strip()
#         return " ".join(text.split())

#     def normalize_for_decision(self, observation):
#         text = self.normalize(observation)
#         for char in self.DECISION_PUNCTUATION:
#             text = text.replace(char, "")
#         return " ".join(text.split())

#     def is_no_important_change(self, observation):
#         text = str(observation or "")
#         return any(
#             phrase in text
#             for phrase in self.NO_IMPORTANT_CHANGE_PHRASES
#         )

#     def is_broken(self, observation):
#         text = self.normalize(observation)
#         if not text:
#             return True

#         decision_text = self.normalize_for_decision(text)
#         if (
#             decision_text in self.NO_CHANGE_EXACT_TEXTS
#             or "중요한 변화 없음" in decision_text
#         ):
#             return True

#         compact = "".join(text.split())
#         if not any(
#             ch.isalnum() or ("가" <= ch <= "힣")
#             for ch in compact
#         ):
#             return True

#         if (
#             text.count("!") >= 8
#             or text.count("?") >= 8
#             or text.count(".") >= 20
#         ):
#             return True

#         if len(compact) >= 12:
#             counter = Counter(compact)
#             _, most_common_count = counter.most_common(1)[0]
#             if most_common_count / max(len(compact), 1) >= 0.55:
#                 return True
#             if len(set(compact)) <= 3:
#                 return True

#         return len(text) < 8

#     def is_duplicate(self, previous_observation, current_observation):
#         previous = self.normalize(previous_observation)
#         current = self.normalize(current_observation)
#         if not previous or not current:
#             return False
#         if previous == current:
#             return True

#         similarity = SequenceMatcher(None, previous, current).ratio()
#         return similarity >= self.duplicate_similarity
