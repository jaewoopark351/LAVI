#20260803_kpopmodder: Cover Korean item aliases and equipment composition.
import unittest

from plugins.Minecraft.fabric.chatclef.intent.chatclef_alias_repository import (
    ChatClefKoreanAliasRepository,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_equipment_target_composer import (
    ChatClefEquipmentTargetComposer,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_status import (
    ChatClefIntentStatus,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_target_catalog import (
    ChatClefTargetCatalog,
)
from plugins.Minecraft.fabric.chatclef.intent.korean_item_phrase_resolver import (
    KoreanItemPhraseResolver,
)


class MinecraftChatClefItemPhraseResolverTests(unittest.TestCase):
    def test_resolves_equipment_before_fixed_aliases(self):
        resolver = KoreanItemPhraseResolver()

        self.assertEqual("wooden_axe", resolver.resolve("나무 도끼")["target"])
        self.assertEqual("log", resolver.resolve("나무")["target"])
        self.assertEqual("golden_axe", resolver.resolve("금 도끼")["target"])
        self.assertEqual("gold_ingot", resolver.resolve("금괴")["target"])

    def test_resolves_core_fixed_aliases(self):
        resolver = KoreanItemPhraseResolver()

        self.assertEqual("iron_ingot", resolver.resolve("철 주괴")["target"])
        self.assertEqual("cooked_beef", resolver.resolve("구운 소고기")["target"])

    def test_resolves_standalone_resource_aliases(self):
        resolver = KoreanItemPhraseResolver()
        cases = {
            "다이아몬드": "diamond",
            "다이아": "diamond",
            "돌": "stone",
            "조약돌": "cobblestone",
            "레드스톤": "redstone",
            "석탄": "coal",
            "철": "iron_ingot",
            "금": "gold_ingot",
        }

        for phrase, expected_target in cases.items():
            with self.subTest(phrase=phrase):
                self.assertEqual(expected_target, resolver.resolve(phrase)["target"])

    def test_reports_ambiguous_unknown_and_unsupported_phrases(self):
        resolver = KoreanItemPhraseResolver()

        self.assertEqual(
            ChatClefIntentStatus.AMBIGUOUS.value,
            resolver.resolve("다이아몬드 갑옷")["status"],
        )
        self.assertEqual(
            ChatClefIntentStatus.UNKNOWN.value,
            resolver.resolve("아무거나")["status"],
        )
        self.assertEqual(
            ChatClefIntentStatus.UNSUPPORTED.value,
            resolver.resolve("구리 검")["status"],
        )

    def test_all_equipment_matrix_targets_exist_in_chatclef_catalog(self):
        catalog = ChatClefTargetCatalog()
        composer = ChatClefEquipmentTargetComposer()

        missing = sorted(
            target for target in composer.all_targets() if not catalog.contains(target)
        )

        self.assertEqual([], missing)

    def test_all_fixed_alias_targets_exist_in_chatclef_catalog(self):
        catalog = ChatClefTargetCatalog()
        aliases = ChatClefKoreanAliasRepository()

        missing = sorted(
            target
            for target in set(aliases.fixed_item_aliases.values())
            if not catalog.contains(target)
        )

        self.assertEqual([], missing)


if __name__ == "__main__":
    unittest.main()
