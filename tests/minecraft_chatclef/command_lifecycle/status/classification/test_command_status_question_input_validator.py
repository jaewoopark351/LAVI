#20260908_kpopmodder: Verify the raw gate for newly admitted STATUS questions.
from __future__ import annotations

import unittest
import unicodedata

from plugins.Minecraft.fabric.chatclef.input.status.command_lifecycle.classification.validation import (
    CommandStatusQuestionInputValidator,
)


class CommandStatusQuestionInputValidatorTests(unittest.TestCase):
    def setUp(self) -> None:
        self.validator = CommandStatusQuestionInputValidator()

    def test_accepts_exact_str_length_boundaries_before_normalization(self):
        self.assertTrue(self.validator.accepts("가"))
        self.assertTrue(self.validator.accepts("가" * 128))
        self.assertTrue(self.validator.accepts(" " + ("가" * 128) + " "))
        self.assertFalse(self.validator.accepts("가" * 129))
        derived = type("DerivedStatusText", (str,), {})
        self.assertFalse(self.validator.accepts(derived("뭐 해")))
        self.assertFalse(self.validator.accepts(None))

    def test_accepts_only_terminal_soft_punctuation_and_ordinary_spaces(self):
        for text in (
            "뭐 해",
            "  뭐   해  ",
            "뭐 해?",
            "뭐 해,.!?，。！？",
            "뭐 해?!   ",
        ):
            with self.subTest(text=text):
                self.assertTrue(self.validator.accepts(text))
        for text in (
            "뭐, 해?",
            '"뭐 해"',
            "뭐; 해",
            "뭐|해",
            "뭐&해",
            "뭐\t해",
            "뭐\n해",
            "뭐\r해",
        ):
            with self.subTest(text=text):
                self.assertFalse(self.validator.accepts(text))

    def test_rejects_unicode_controls_formats_and_non_ascii_spaces(self):
        for separator in ("\u0000", "\u200b", "\u2028", "\u2029", "\u00a0", "\u2003", "\u3000"):
            with self.subTest(separator=repr(separator)):
                self.assertFalse(self.validator.accepts(f"뭐{separator}해"))

    def test_nfkc_compatibility_punctuation_cannot_bypass_exact_raw_policy(self):
        compatibility_punctuation = (
            "\ufe50",  # SMALL COMMA -> COMMA
            "\ufe52",  # SMALL FULL STOP -> FULL STOP
            "\ufe56",  # SMALL QUESTION MARK -> QUESTION MARK
            "\uff02",  # FULLWIDTH QUOTATION MARK -> QUOTATION MARK
            "\ufe54",  # SMALL SEMICOLON -> SEMICOLON
            "\ufe60",  # SMALL AMPERSAND -> AMPERSAND
        )
        for punctuation in compatibility_punctuation:
            with self.subTest(punctuation=f"U+{ord(punctuation):04X}"):
                self.assertFalse(self.validator.accepts(f"뭐 해{punctuation}"))
                self.assertFalse(self.validator.accepts(f"뭐{punctuation} 해"))

    def test_exact_terminal_fullwidth_soft_punctuation_remains_allowed(self):
        for punctuation in ",.!?，。！？":
            with self.subTest(punctuation=punctuation):
                self.assertTrue(self.validator.accepts(f"뭐 해{punctuation}"))
        self.assertTrue(self.validator.accepts("뭐 해，。！？"))

    def test_rejects_every_non_soft_punctuation_position(self):
        for punctuation in ("…", ":", "/", "-", "(", ")", "[", "]", "{", "}"):
            with self.subTest(punctuation=punctuation):
                self.assertFalse(self.validator.accepts(f"뭐 해{punctuation}"))
                self.assertFalse(self.validator.accepts(f"뭐{punctuation} 해"))

    def test_rejects_unicode_initial_final_and_bracketed_quotation_marks(self):
        quotation_marks = (
            "\u00ab",
            "\u00bb",
            "\u2018",
            "\u2019",
            "\u201c",
            "\u201d",
            "\u2e02",
            "\u2e03",
            "\u301d",
            "\u301e",
            "\u301f",
            "\u2e42",
        )
        for quotation_mark in quotation_marks:
            with self.subTest(quotation_mark=f"U+{ord(quotation_mark):04X}"):
                self.assertIn(
                    unicodedata.category(quotation_mark),
                    {"Pi", "Pf", "Ps", "Pe"},
                )
                self.assertFalse(
                    self.validator.accepts(f"{quotation_mark}뭐 해")
                )
                self.assertFalse(
                    self.validator.accepts(f"뭐 해{quotation_mark}")
                )


if __name__ == "__main__":
    unittest.main()
