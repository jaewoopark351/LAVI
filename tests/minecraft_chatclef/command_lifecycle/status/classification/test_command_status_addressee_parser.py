#20260908_kpopmodder: Verify longest exact normalized STATUS addressee parsing.
from __future__ import annotations

import unittest

from plugins.Minecraft.fabric.chatclef.input.status.command_lifecycle.classification.addressing import (
    CommandStatusAddresseeParser,
)


class CommandStatusAddresseeParserTests(unittest.TestCase):
    def setUp(self) -> None:
        self.parser = CommandStatusAddresseeParser()

    def test_removes_the_longest_supported_addressee_once(self):
        self.assertEqual(
            ("마크 ai ", "지금 뭐 해"),
            self.parser.parse("마크 ai 지금 뭐 해"),
        )
        self.assertEqual(
            ("마인크래프트 ", "지금 뭐 해"),
            self.parser.parse("마인크래프트 지금 뭐 해"),
        )
        self.assertEqual(("마크 ", "지금 뭐 해"), self.parser.parse("마크 지금 뭐 해"))

    def test_compact_vocatives_and_repeated_prefixes_are_not_widened(self):
        self.assertEqual(("", "마크ai 지금 뭐 해"), self.parser.parse("마크ai 지금 뭐 해"))
        self.assertEqual(("", "마크야 지금 뭐 해"), self.parser.parse("마크야 지금 뭐 해"))
        self.assertEqual(
            ("마크 ", "마크 지금 뭐 해"),
            self.parser.parse("마크 마크 지금 뭐 해"),
        )

    def test_rejects_non_exact_normalized_text_type(self):
        with self.assertRaises(TypeError):
            self.parser.parse(None)


if __name__ == "__main__":
    unittest.main()
