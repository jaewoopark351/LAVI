#20260815_kpopmodder: Verify every runtime Korean item alias translates for GET.
from __future__ import annotations

import unittest

from plugins.Minecraft.fabric.chatclef.intent import (
    ChatClefIntentStatus,
    ChatClefNaturalLanguageService,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_alias_repository import (
    ChatClefKoreanAliasRepository,
)


class AllRuntimeKoreanAliasesTranslateTests(unittest.TestCase):
    def test_every_runtime_alias_translates_to_prefixless_get_command(self):
        service = ChatClefNaturalLanguageService()
        aliases = ChatClefKoreanAliasRepository()

        for alias, expected_target in sorted(aliases.fixed_item_aliases.items()):
            with self.subTest(alias=alias, expected_target=expected_target):
                result = service.translate(f"{alias} 가져와줘")

                self.assertTrue(result.executable)
                self.assertEqual(ChatClefIntentStatus.VALIDATED, result.status)
                self.assertEqual(expected_target, result.resolved_target)
                self.assertEqual(f"get {expected_target} 1", result.command)
                self.assertFalse(str(result.command).startswith("@"))

    def test_every_runtime_alias_preserves_explicit_quantity(self):
        service = ChatClefNaturalLanguageService()
        aliases = ChatClefKoreanAliasRepository()

        for alias, expected_target in sorted(aliases.fixed_item_aliases.items()):
            with self.subTest(alias=alias, expected_target=expected_target):
                result = service.translate(f"{alias} 3개 가져와줘")

                self.assertTrue(result.executable)
                self.assertEqual(ChatClefIntentStatus.VALIDATED, result.status)
                self.assertEqual(expected_target, result.resolved_target)
                self.assertEqual(f"get {expected_target} 3", result.command)


if __name__ == "__main__":
    unittest.main()
