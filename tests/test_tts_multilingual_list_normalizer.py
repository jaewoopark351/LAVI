import unittest

from tts_core.multilingual_list_normalizer import (
    normalize_multilingual_list_ordinals,
)
from tts_core.text_processor import TTSTextProcessor


class MultilingualListNormalizerTests(unittest.TestCase):
    def test_korean_line_start_list_numbers_become_ordinals(self):
        text = "\u0031. \uc815\uc758\n\u0032. \uad6c\uc131\n\u0033. \uc704\ud5d8\uacfc \uc218\uc775"

        self.assertEqual(
            "\uccab\uc9f8, \uc815\uc758\n\ub458\uc9f8, \uad6c\uc131\n\uc14b\uc9f8, \uc704\ud5d8\uacfc \uc218\uc775",
            normalize_multilingual_list_ordinals(text),
        )

    def test_english_line_start_list_numbers_become_ordinals(self):
        text = "1. Definition\n2. Structure\n21. Risk and return"

        self.assertEqual(
            "first, Definition\nsecond, Structure\ntwenty first, Risk and return",
            normalize_multilingual_list_ordinals(text),
        )

    def test_chinese_line_start_list_numbers_become_ordinals(self):
        text = "1. \u5b9a\u4e49\n2. \u7ed3\u6784\n12. \u98ce\u9669"

        self.assertEqual(
            "\u7b2c\u4e00\u3001\u5b9a\u4e49\n\u7b2c\u4e8c\u3001\u7ed3\u6784\n\u7b2c\u5341\u4e8c\u3001\u98ce\u9669",
            normalize_multilingual_list_ordinals(text),
        )

    def test_japanese_language_hint_handles_kanji_only_items(self):
        text = "1. \u5b9a\u7fa9\n2. \u69cb\u6210\n11. \u8a73\u7d30"

        self.assertEqual(
            "\u4e00\u3064\u76ee\u3001\u5b9a\u7fa9\n\u4e8c\u3064\u76ee\u3001\u69cb\u6210\n\u5341\u4e00\u756a\u76ee\u3001\u8a73\u7d30",
            normalize_multilingual_list_ordinals(text, language="ja"),
        )

    def test_language_hint_does_not_override_clear_latin_script(self):
        self.assertEqual(
            "first, Definition",
            normalize_multilingual_list_ordinals(
                "1. Definition",
                language="ko",
            ),
        )

    def test_auto_detects_japanese_kana_items(self):
        text = "1. \u30c6\u30fc\u30de\n2. \u307e\u3068\u3081"

        self.assertEqual(
            "\u4e00\u3064\u76ee\u3001\u30c6\u30fc\u30de\n\u4e8c\u3064\u76ee\u3001\u307e\u3068\u3081",
            normalize_multilingual_list_ordinals(text),
        )

    def test_preserves_decimal_date_version_and_url_starts(self):
        samples = [
            "31.6\ub9cc\uba85",
            "v1.2.3 release",
            "2.0 version",
            "2026-07-20",
            "2026. 7. 20.",
            "https://example.com/v1.2",
            "1. https://example.com/v2",
            "1. v1.2.3",
            "1. 2026-07-20",
        ]

        for sample in samples:
            with self.subTest(sample=sample):
                self.assertEqual(
                    sample,
                    normalize_multilingual_list_ordinals(sample),
                )

    def test_tts_processor_normalizes_before_sentence_split(self):
        processor = TTSTextProcessor()
        text = "\u0031. \uc815\uc758\n\u0032. \uad6c\uc131\n\u0033. \uc704\ud5d8\uacfc \uc218\uc775"

        normalized = processor.normalize_text_item(text)

        self.assertEqual(
            "\uccab\uc9f8, \uc815\uc758 \ub458\uc9f8, \uad6c\uc131 \uc14b\uc9f8, \uc704\ud5d8\uacfc \uc218\uc775",
            normalized,
        )
        self.assertEqual(
            [
                "\uccab\uc9f8, \uc815\uc758 \ub458\uc9f8, \uad6c\uc131 \uc14b\uc9f8, \uc704\ud5d8\uacfc \uc218\uc775",
            ],
            processor.split_tts_sentences(normalized),
        )

    def test_tts_processor_normalizes_streamed_markdown_list_item(self):
        processor = TTSTextProcessor()

        normalized = processor.normalize_text_item(
            "\u0031.\n**\uc815\uc758**: \ud569\uc131 CDO\uc57c."
        )

        self.assertEqual(
            "\uccab\uc9f8, \uc815\uc758: \ud569\uc131 CDO\uc57c.",
            normalized,
        )


if __name__ == "__main__":
    unittest.main()
