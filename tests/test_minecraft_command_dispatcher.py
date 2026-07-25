#20260725_kpopmodder: Covers Minecraft structured command dispatch for bridge actions.
import unittest

from app_core.extensions.game_extension_contracts import GameCommandDTO
from app_core.extensions.minecraft_core import MinecraftCommandDispatcher


class FakeMinecraftPlugin:
    def __init__(self):
        self.commands = []

    def handle_command(self, command):
        self.commands.append(command)
        return {"ok": True, "action": command["action"]}


class MinecraftCommandDispatcherTests(unittest.TestCase):
    def test_dispatcher_allows_structured_craft_equip_and_get_and_equip(self):
        plugin = FakeMinecraftPlugin()
        dispatcher = MinecraftCommandDispatcher()

        cases = (
            ({"action": "craft", "item": "stick", "count": 4}, "craft"),
            ({"action": "equip", "item": "iron_pickaxe"}, "equip"),
            (
                {"action": "get-and-equip", "item": "iron_pickaxe", "count": 1},
                "get_and_equip",
            ),
        )

        for command, expected_action in cases:
            result = dispatcher.dispatch(
                plugin,
                command,
                GameCommandDTO.from_mapping(command),
            )
            self.assertTrue(result.result["ok"])
            self.assertEqual(expected_action, result.action)
            self.assertEqual(expected_action, plugin.commands[-1]["action"])


if __name__ == "__main__":
    unittest.main()
