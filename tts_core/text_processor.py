import re

from safety_filter import clean_text
from tts_core.multilingual_list_normalizer import (
    normalize_multilingual_list_ordinals,
)


class TTSTextProcessor:#20260616_kpopmodder
    def normalize_text_item(self, text, language=None):
        if text is None:
            return ""

        text = str(text).strip()
        text = clean_text(text)
        text = normalize_multilingual_list_ordinals(text, language=language)
        text = self.normalize_tts_markdown(text)
        #20260628_kpopmodder: Normalize readable dot cases before sentence splitting.
        text = self.normalize_tts_punctuation(text)
        text = text.strip()

        return text

    def is_tts_skippable(self, text):
        text = str(text).strip()

        if not text:
            return True

        if not re.search(r"[가-힣a-zA-Z0-9]", text):
            return True

        return False

    def split_tts_sentences(self, text, max_len=80):
        text = text.replace("\n", " ")
        text = re.sub(r"\s+", " ", text).strip()

        if not text:
            return []

        result = []
        sentence_start = 0

        for index, _char in enumerate(text):
            if not self.is_sentence_boundary(text, index):
                continue

            sentence = text[sentence_start:index + 1].strip()

            if not sentence:
                continue

            result.extend(
                self.split_long_sentence(sentence, max_len=max_len)
            )
            sentence_start = self.next_sentence_start(text, index + 1)

        remaining = text[sentence_start:].strip()
        if remaining:
            result.extend(
                self.split_long_sentence(remaining, max_len=max_len)
            )

        return result

    def normalize_tts_punctuation(self, text):
        text = re.sub(r"(?<=\d)\.(?=\d)", "점", text)
        text = self.normalize_initialism_dots(text)
        return re.sub(r"\s+", " ", text).strip()

    def normalize_initialism_dots(self, text):
        pattern = re.compile(
            r"(?<![A-Za-z])(?:[A-Za-z]\.){2,}[A-Za-z]?(?:\.)?"
            r"(?![A-Za-z])"
        )

        def replace(match):
            letters = re.findall(r"[A-Za-z]", match.group(0))
            replacement = " ".join(letters)
            next_index = match.end()

            if (
                next_index < len(text)
                and re.match(r"[가-힣]", text[next_index])
            ):
                replacement += " "

            return replacement

        return pattern.sub(replace, text)

    def normalize_tts_markdown(self, text):
        text = re.sub(r"(?<!\*)\*\*([^*\r\n]+)\*\*(?!\*)", r"\1", text)
        text = re.sub(r"(?<!_)__([^_\r\n]+)__(?!_)", r"\1", text)
        text = re.sub(r"`([^`\r\n]+)`", r"\1", text)
        return text

    def is_sentence_boundary(self, text, index):
        char = text[index]

        if char not in ".!?。！？…":
            return False

        if char == ".":
            if self.is_decimal_dot(text, index):
                return False
            if self.is_initialism_dot(text, index):
                return False

        if index + 1 >= len(text):
            return True

        return text[index + 1].isspace()

    def is_decimal_dot(self, text, index):
        previous_char = text[index - 1] if index > 0 else ""
        next_char = text[index + 1] if index + 1 < len(text) else ""

        return previous_char.isdigit() and next_char.isdigit()

    def is_initialism_dot(self, text, index):
        previous_char = text[index - 1] if index > 0 else ""
        next_char = text[index + 1] if index + 1 < len(text) else ""

        if previous_char.isascii() and previous_char.isalpha():
            if next_char.isascii() and next_char.isalpha():
                return True

        token_start = max(
            text.rfind(" ", 0, index) + 1,
            text.rfind("\n", 0, index) + 1,
            text.rfind("\t", 0, index) + 1,
        )
        token = text[token_start:index + 1]

        return bool(re.fullmatch(r"(?:[A-Za-z]\.){2,}", token))

    def next_sentence_start(self, text, index):
        while index < len(text) and text[index].isspace():
            index += 1

        return index

    def split_long_sentence(self, sentence, max_len=80):
        result = []

        while len(sentence) > max_len:
            cut = sentence[:max_len]

            split_pos = max(
                cut.rfind(","),
                cut.rfind(" "),
                cut.rfind("，"),
            )

            if split_pos <= 0:
                split_pos = max_len

            result.append(sentence[:split_pos].strip())
            sentence = sentence[split_pos:].strip()

        if sentence:
            result.append(sentence)

        return result
