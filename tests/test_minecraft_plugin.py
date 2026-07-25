#20260725_kpopmodder: Covers Minecraft plugin config smoke checks.
import json
import unittest
from pathlib import Path

from plugins.Minecraft.minecraft_core import MinecraftConfig


PROJECT_ROOT = Path(__file__).resolve().parents[1]


class MinecraftPluginTests(unittest.TestCase):
    def test_modules_json_declares_minecraft_toggle(self):
        modules = json.loads(
            (PROJECT_ROOT / "config" / "modules.example.json").read_text(
                encoding="utf-8",
            )
        )

        self.assertIn("Minecraft", modules)
        self.assertIsInstance(modules.get("Minecraft"), bool)

    def test_example_config_loads_bridge_defaults(self):
        config = MinecraftConfig(str(PROJECT_ROOT / "plugins" / "Minecraft"))
        example = config.load_example_config()

        self.assertTrue(example["enabled"])
        self.assertTrue(example["allow_actions"])
        self.assertEqual("http://127.0.0.1:4316", example["bridge"]["base_url"])
        self.assertGreater(example["bridge"]["timeout_sec"], 0)
        self.assertTrue(example["action_verification"]["enabled"])
        self.assertGreater(example["action_verification"]["timeout_sec"], 0)
        self.assertGreater(example["action_verification"]["poll_interval_sec"], 0)

    """
    Moved test bodies kept only as migration notes.
    Active command tests live in tests/test_minecraft_commands.py.
    Active extension tests live in tests/test_minecraft_game_extension.py.

    @unittest.skip("moved to tests/test_minecraft_commands.py")
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
                for key, value in parser.parse("oak_log 2개 가져와").items()
                if key in {"action", "item", "count"}
            },
        )
        self.assertEqual("stop", parser.parse("취소")["action"])

    @unittest.skip("moved to tests/test_minecraft_game_extension.py")
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

    @unittest.skip("moved to tests/test_minecraft_game_extension.py")
    def test_game_extension_passes_text_commands_to_plugin_parser(self):
        plugin = FakeMinecraftPlugin()
        extension = MinecraftGameExtension(plugin=plugin)
        extension.initialize(GameExtensionContext())
        extension.start()

        result = extension.handle_command("go to 0 64 0")

        self.assertTrue(result["ok"])
        self.assertEqual("go to 0 64 0", plugin.commands[0])
    """


if __name__ == "__main__":
    unittest.main()
