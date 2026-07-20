import re


class LLMStreamingChunker:#20260617_kpopmodder
    def __init__(self):
        self.sentence_end_punctuation = {
            ".",
            "?",
            "!",
            "。",
            "？",
            "！",
            "\n",
        }

    def is_sentence_end(self, word):
        if not word:
            return True

        if word[-1] == ".":
            return self.is_sentence_end_dot(word, len(word) - 1, allow_end=True)

        return word[-1] in self.sentence_end_punctuation

    def get_streaming_tts_chunk(self, full_text, processed_idx):
        if not full_text:
            return None, processed_idx

        cut_idx = -1

        for i in range(processed_idx, len(full_text)):
            if self.is_sentence_end_at(full_text, i):
                cut_idx = i + 1
                break

        if cut_idx <= processed_idx:
            return None, processed_idx

        chunk = full_text[processed_idx:cut_idx].strip()

        if not chunk:
            return None, cut_idx

        return chunk, cut_idx

    def is_sentence_end_at(self, text, index):
        char = text[index]

        if char not in self.sentence_end_punctuation:
            return False

        if char == ".":
            #20260628_kpopmodder: Avoid cutting streamed TTS on decimal dots or dotted initialisms.
            return self.is_sentence_end_dot(text, index, allow_end=False)

        if char == "\n" and self.is_numbered_list_marker_line_break(text, index):
            return False

        return True

    def is_sentence_end_dot(self, text, index, allow_end=False):
        if self.is_decimal_dot(text, index):
            return False

        if self.is_initialism_dot(text, index):
            return False

        if self.is_line_start_numbered_list_dot(text, index):
            return False

        next_index = self.next_meaningful_index(text, index + 1)

        if next_index >= len(text):
            previous_char = text[index - 1] if index > 0 else ""
            if previous_char.isdigit():
                return bool(allow_end)
            if previous_char.isascii() and previous_char.isalpha():
                return bool(allow_end)
            return True

        return text[next_index].isspace()

    def is_line_start_numbered_list_dot(self, text, index):
        previous_char = text[index - 1] if index > 0 else ""
        if not previous_char.isdigit():
            return False

        line_start = max(
            text.rfind("\n", 0, index) + 1,
            text.rfind("\r", 0, index) + 1,
        )
        token = text[line_start:index]

        #20260720_kpopmodder: Keep streamed markdown lists together so TTS can
        # read "1. **정의**" as one item instead of a standalone "1.".
        return bool(re.fullmatch(r"[ \t]*[0-9]{1,2}", token))

    def is_numbered_list_marker_line_break(self, text, index):
        line_start = max(
            text.rfind("\n", 0, index) + 1,
            text.rfind("\r", 0, index) + 1,
        )
        line = text[line_start:index]

        return bool(re.fullmatch(r"[ \t]*[0-9]{1,2}\.[ \t]*", line))

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

    def next_meaningful_index(self, text, index):
        closing_chars = {'"', "'", ")", "]", "}", "”", "’"}

        while index < len(text) and text[index] in closing_chars:
            index += 1

        return index
