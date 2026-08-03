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

        with self.assertRaises(ValueError):
            compiler.compile(
                ChatClefIntentDTO(
                    intent_type=ChatClefIntentType.FOLLOW,
                    player_name="Steve;stop",
                )
            )
        with self.assertRaises(ValueError):
            compiler.compile(
                ChatClefIntentDTO(
                    intent_type=ChatClefIntentType.GET_ITEM,
                    item_phrase="다이아 도끼",
                    quantity=1,
                ),
                target="@diamond_axe",
            )


if __name__ == "__main__":
    unittest.main()
