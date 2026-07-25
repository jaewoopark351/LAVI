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

    def test_parser_maps_equip_commands(self):
        parser = MinecraftCommandParser()

        self.assertEqual(
            {"action": "equip", "item": "iron_pickaxe"},
            {
                key: value
                for key, value in parser.parse("equip iron_pickaxe").items()
                if key in {"action", "item"}
            },
        )
        self.assertEqual(
            {"action": "equip", "item": "iron_pickaxe"},
            {
                key: value
                for key, value in parser.parse("hold iron pickaxe").items()
                if key in {"action", "item"}
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

    def test_parser_maps_get_and_equip_commands_before_get_item(self):
        parser = MinecraftCommandParser()

        self.assertEqual(
            {"action": "get_and_equip", "item": "iron_pickaxe", "count": 1},
            {
                key: value
                for key, value in parser.parse("get and equip iron pickaxe").items()
                if key in {"action", "item", "count"}
            },
        )

    def test_parser_maps_craft_commands(self):
        parser = MinecraftCommandParser()

        self.assertEqual(
            {"action": "craft", "item": "crafting_table", "count": 1},
            {
                key: value
                for key, value in parser.parse("craft one crafting table").items()
                if key in {"action", "item", "count"}
            },
        )
        self.assertEqual(
            {"action": "craft", "item": "stick", "count": 4},
            {
                key: value
                for key, value in parser.parse("make 4 sticks").items()
                if key in {"action", "item", "count"}
            },
        )
        self.assertEqual(
            {"action": "get_and_equip", "item": "diamond_pickaxe", "count": 2},
            {
                key: value
                for key, value in parser.parse(
                    "get diamond_pickaxe 2 and equip"
                ).items()
                if key in {"action", "item", "count"}
            },
        )


if __name__ == "__main__":
    unittest.main()
