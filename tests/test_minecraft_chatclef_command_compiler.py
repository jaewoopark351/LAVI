#20260803_kpopmodder: Cover safe ChatClef DSL compilation from intents.
import unittest

from plugins.Minecraft.fabric.chatclef.intent.chatclef_command_compiler import (
    ChatClefCommandCompiler,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_dto import (
    ChatClefIntentDTO,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_type import (
    ChatClefIntentType,
)


class MinecraftChatClefCommandCompilerTests(unittest.TestCase):
    def test_compiles_prefixless_commands(self):
        compiler = ChatClefCommandCompiler()

        self.assertEqual(
            "get diamond_axe 1",
            compiler.compile(
                ChatClefIntentDTO(
                    intent_type=ChatClefIntentType.GET_ITEM,
                    item_phrase="다이아 도끼",
                    quantity=1,
                ),
                target="diamond_axe",
            ),
        )
        self.assertEqual(
            "follow Steve",
            compiler.compile(
                ChatClefIntentDTO(
                    intent_type=ChatClefIntentType.FOLLOW,
                    player_name="Steve",
                )
            ),
        )

    def test_rejects_dangerous_slots(self):
        compiler = ChatClefCommandCompiler()

        for player_name in ["Steve;stop", "Steve#stop"]:
            with self.subTest(player_name=player_name):
                with self.assertRaises(ValueError):
                    compiler.compile(
                        ChatClefIntentDTO(
                            intent_type=ChatClefIntentType.FOLLOW,
                            player_name=player_name,
                        )
                    )
        for target in ["@diamond_axe", "diamond#axe"]:
            with self.subTest(target=target):
                with self.assertRaises(ValueError):
                    compiler.compile(
                        ChatClefIntentDTO(
                            intent_type=ChatClefIntentType.GET_ITEM,
                            item_phrase="다이아 도끼",
                            quantity=1,
                        ),
                        target=target,
                    )

    def test_rejects_invalid_get_counts(self):
        compiler = ChatClefCommandCompiler()

        for quantity in [0, -1, 2147483648, True, False, 1.0, 1.5, "1.5"]:
            with self.subTest(quantity=quantity):
                with self.assertRaises((TypeError, ValueError)):
                    compiler.compile(
                        ChatClefIntentDTO(
                            intent_type=ChatClefIntentType.GET_ITEM,
                            item_phrase="다이아",
                            quantity=quantity,
                        ),
                        target="diamond",
                    )

    def test_rejects_goto_coordinates_outside_java_int_contract(self):
        compiler = ChatClefCommandCompiler()

        invalid_cases = [
            {"x": True, "y": 64, "z": 0},
            {"x": 0, "y": 1.5, "z": 0},
            {"x": 0, "y": 64, "z": "3"},
            {"x": -2147483649, "y": 64, "z": 0},
            {"x": 0, "y": 64, "z": 2147483648},
        ]

        for coordinates in invalid_cases:
            with self.subTest(coordinates=coordinates):
                with self.assertRaises((TypeError, ValueError)):
                    compiler.compile(
                        ChatClefIntentDTO(
                            intent_type=ChatClefIntentType.GOTO,
                            **coordinates,
                        )
                    )


if __name__ == "__main__":
    unittest.main()
