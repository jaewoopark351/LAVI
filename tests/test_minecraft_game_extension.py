#20260725_kpopmodder: Covers Minecraft GameExtension adapter dispatch behavior.
import unittest

from app_core.extensions.game_extension_context import GameExtensionContext
from app_core.extensions.minecraft_game_extension import MinecraftGameExtension


class FakeMinecraftPlugin:
    def __init__(self):
        self.commands = []
        self.previews = []

    def handle_command(self, command):
        self.commands.append(command)
        if isinstance(command, dict):
            return {"ok": True, "action": command.get("action")}
        return {"ok": True, "action": command}

    def preview_command(self, command):
        self.previews.append(command)
        return {"ok": True, "accepted": True, "preview": True}

    def get_status(self):
        return {"ok": True, "bridge": {"ok": True}}


class MinecraftGameExtensionTests(unittest.TestCase):
    def test_game_extension_records_commands_without_lifecycle_stop_side_effect(self):
        plugin = FakeMinecraftPlugin()
        extension = MinecraftGameExtension(plugin=plugin)
        extension.initialize(GameExtensionContext())
        extension.start()

        result = extension.handle_command({"action": "health"})
        extension.stop()

        self.assertTrue(result["ok"])
        self.assertEqual("health", plugin.commands[0]["action"])
        self.assertFalse(extension.get_status()["started"])

    def test_game_extension_passes_text_commands_to_plugin_parser(self):
        plugin = FakeMinecraftPlugin()
        extension = MinecraftGameExtension(plugin=plugin)
        extension.initialize(GameExtensionContext())
        extension.start()

        result = extension.handle_command("go to 0 64 0")

        self.assertTrue(result["ok"])
        self.assertEqual("go to 0 64 0", plugin.commands[0])

    def test_game_extension_previews_without_recording_command_execution(self):
        plugin = FakeMinecraftPlugin()
        extension = MinecraftGameExtension(plugin=plugin)
        extension.initialize(GameExtensionContext())
        extension.start()

        result = extension.preview_command("get oak_log 1")

        self.assertTrue(result["ok"])
        self.assertTrue(result["preview"])
        self.assertEqual(["get oak_log 1"], plugin.previews)
        self.assertEqual([], plugin.commands)


if __name__ == "__main__":
    unittest.main()
