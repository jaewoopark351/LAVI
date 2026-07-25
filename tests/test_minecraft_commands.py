#20260725_kpopmodder: Covers Minecraft natural command parsing behavior.
import unittest

from plugins.Minecraft.minecraft_core import MinecraftCommandParser


class MinecraftCommandParserTests(unittest.TestCase):
    def test_parser_maps_text_commands_for_llm_bridge(self):
        parser = MinecraftCommandParser()

        self.assertEqual(
            {"action": "goto", "target": "0 64 0 overworld"},
            {
                key: value
                for key, value in parser.parse("go to 0 64 0 overworld").items()
                if key in {"action", "target"}
            },
        )
        self.assertEqual(
            {"action": "get_item", "item": "oak_log", "count": 2},
            {
                key: value
                for key, value in parser.parse("oak_log 2媛?媛?몄?").items()
                if key in {"action", "item", "count"}
            },
        )
        self.assertEqual("stop", parser.parse("취소")["action"])

    def test_parser_normalizes_english_get_item_phrases(self):
        parser = MinecraftCommandParser()

        self.assertEqual(
            {"action": "get_item", "item": "oak_log", "count": 1},
            {
                key: value
                for key, value in parser.parse("get one oak log").items()
                if key in {"action", "item", "count"}
            },
        )
        self.assertEqual(
            {"action": "get_item", "item": "iron_pickaxe", "count": 2},
            {
                key: value
                for key, value in parser.parse("bring two iron pickaxes").items()
                if key in {"action", "item", "count"}
            },
        )


if __name__ == "__main__":
    unittest.main()
