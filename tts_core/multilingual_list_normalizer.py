#20260720_kpopmodder: Added a small GPT-SoVITS-inspired list prefix normalizer for TTS input.
import re


LIST_PREFIX_PATTERN = re.compile(
    r"(?m)^([ \t]*)([0-9]{1,2})\.\s+([^\r\n]*)"
)

KOREAN_ORDINALS = {
    1: "첫째",
    2: "둘째",
    3: "셋째",
    4: "넷째",
    5: "다섯째",
    6: "여섯째",
    7: "일곱째",
    8: "여덟째",
    9: "아홉째",
    10: "열째",
    11: "열한째",
    12: "열두째",
    13: "열세째",
    14: "열네째",
    15: "열다섯째",
    16: "열여섯째",
    17: "열일곱째",
    18: "열여덟째",
    19: "열아홉째",
    20: "스무째",
}

ENGLISH_ORDINALS = {
    1: "first",
    2: "second",
    3: "third",
    4: "fourth",
    5: "fifth",
    6: "sixth",
    7: "seventh",
    8: "eighth",
    9: "ninth",
    10: "tenth",
    11: "eleventh",
    12: "twelfth",
    13: "thirteenth",
    14: "fourteenth",
    15: "fifteenth",
    16: "sixteenth",
    17: "seventeenth",
    18: "eighteenth",
    19: "nineteenth",
    20: "twentieth",
}

ENGLISH_TENS = {
    20: "twenty",
    30: "thirty",
    40: "forty",
    50: "fifty",
    60: "sixty",
    70: "seventy",
    80: "eighty",
    90: "ninety",
}

JAPANESE_ORDINALS = {
    1: "一つ目",
    2: "二つ目",
    3: "三つ目",
    4: "四つ目",
    5: "五つ目",
    6: "六つ目",
    7: "七つ目",
    8: "八つ目",
    9: "九つ目",
    10: "十番目",
}

CHINESE_DIGITS = {
    0: "零",
    1: "一",
    2: "二",
    3: "三",
    4: "四",
    5: "五",
    6: "六",
    7: "七",
    8: "八",
    9: "九",
}

LANGUAGE_ALIASES = {
    "korean": "ko",
    "kor": "ko",
    "ko": "ko",
    "english": "en",
    "eng": "en",
    "en": "en",
    "chinese": "zh",
    "cn": "zh",
    "zh": "zh",
    "zh-cn": "zh",
    "zh-tw": "zh",
    "japanese": "ja",
    "jp": "ja",
    "ja": "ja",
}

URL_OR_PATH_START_PATTERN = re.compile(
    r"(?i)^(?:https?://|www\.|mailto:|[a-z]:[\\/]|\\\\)"
)
EMAIL_START_PATTERN = re.compile(
    r"(?i)^[a-z0-9._%+-]+@[a-z0-9.-]+\.[a-z]{2,}"
)
VERSION_START_PATTERN = re.compile(
    r"(?i)^(?:v|ver(?:sion)?\.?\s*)?\d+(?:\.\d+){1,}"
)
DATE_START_PATTERN = re.compile(
    r"^\d{2,4}(?:[-/.]\d{1,2}){1,2}"
)


def normalize_multilingual_list_ordinals(text, language=None):
    if not text:
        return text

    normalized_language = normalize_language_code(language)

    def replace(match):
        indent, number_text, content = match.groups()
        stripped_content = content.lstrip()

        if should_skip_list_content(stripped_content):
            return match.group(0)

        item_language = choose_list_item_language(
            stripped_content,
            normalized_language,
        )
        ordinal = get_ordinal_word(int(number_text), item_language)

        if ordinal is None:
            return match.group(0)

        separator = get_separator(item_language)
        return f"{indent}{ordinal}{separator}{stripped_content}"

    return LIST_PREFIX_PATTERN.sub(replace, text)


def normalize_language_code(language):
    if language is None:
        return None

    language = str(language).strip().lower().replace("_", "-")
    if not language or language == "auto":
        return None

    return LANGUAGE_ALIASES.get(language, language)


def should_skip_list_content(content):
    if not content:
        return True

    return bool(
        URL_OR_PATH_START_PATTERN.search(content)
        or EMAIL_START_PATTERN.search(content)
        or VERSION_START_PATTERN.search(content)
        or DATE_START_PATTERN.search(content)
    )


def detect_list_item_language(content):
    sample = content[:80]

    if re.search(r"[\uac00-\ud7a3]", sample):
        return "ko"
    if re.search(r"[\u3040-\u30ff]", sample):
        return "ja"
    if re.search(r"[\u4e00-\u9fff]", sample):
        return "cjk"
    if re.search(r"[A-Za-z]", sample):
        return "en"

    return None


def choose_list_item_language(content, language_hint=None):
    detected_language = detect_list_item_language(content)

    if detected_language == "cjk":
        if language_hint in {"ja", "zh"}:
            return language_hint
        return "zh"

    return detected_language or language_hint


def get_ordinal_word(number, language):
    if language == "ko":
        return KOREAN_ORDINALS.get(number)
    if language == "en":
        return english_ordinal(number)
    if language == "zh":
        return chinese_ordinal(number)
    if language == "ja":
        return japanese_ordinal(number)

    return None


def english_ordinal(number):
    if number in ENGLISH_ORDINALS:
        return ENGLISH_ORDINALS[number]
    if number < 1 or number > 99:
        return None

    tens = number // 10 * 10
    ones = number % 10
    if ones == 0:
        return ENGLISH_ORDINALS.get(number)
    if tens in ENGLISH_TENS and ones in ENGLISH_ORDINALS:
        return f"{ENGLISH_TENS[tens]} {ENGLISH_ORDINALS[ones]}"

    return None


def chinese_ordinal(number):
    cardinal = chinese_cardinal(number)
    if cardinal is None:
        return None
    return f"第{cardinal}"


def chinese_cardinal(number):
    if number < 1 or number > 99:
        return None
    if number < 10:
        return CHINESE_DIGITS[number]
    if number == 10:
        return "十"

    tens = number // 10
    ones = number % 10
    prefix = "" if tens == 1 else CHINESE_DIGITS[tens]
    suffix = "" if ones == 0 else CHINESE_DIGITS[ones]
    return f"{prefix}十{suffix}"


def japanese_ordinal(number):
    if number in JAPANESE_ORDINALS:
        return JAPANESE_ORDINALS[number]

    cardinal = chinese_cardinal(number)
    if cardinal is None:
        return None
    return f"{cardinal}番目"


def get_separator(language):
    if language in {"ja", "zh"}:
        return "、"
    return ", "
