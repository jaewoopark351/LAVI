#20260725_kpopmodder: Covers the Minecraft ChatClef bridge client and game extension adapter.
import json
import unittest
from pathlib import Path
from unittest import mock

from app_core.extensions.game_extension_context import GameExtensionContext
from app_core.extensions.minecraft_game_extension import MinecraftGameExtension
from plugins.Minecraft.minecraft_core.chatclef_bridge_client import ChatClefBridgeClient
from plugins.Minecraft.minecraft_core.minecraft_config import MinecraftConfig
from plugins.Minecraft.minecraft_core.minecraft_facade_service import (
    MinecraftFacadeService,
)


PROJECT_ROOT = Path(__file__).resolve().parents[1]


class FakeHttpResponse:
    def __init__(self, payload):
        self.payload = payload

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc, traceback):
        return False

    def read(self):
        return json.dumps(self.payload).encode("utf-8")


class FakeMinecraftPlugin:
    def __init__(self):
        self.commands = []

    def handle_command(self, command):
        self.commands.append(command)
        return {"ok": True, "action": command.get("action")}

    def get_status(self):
        return {"ok": True, "bridge": {"ok": True}}


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

    def test_client_posts_get_item_to_v1_endpoint(self):
        calls = []

        def opener(request, timeout):
            calls.append((request, timeout))
            return FakeHttpResponse({"ok": True, "accepted": True})

        client = ChatClefBridgeClient(
            base_url="http://127.0.0.1:4316",
            timeout_sec=2,
            opener=opener,
        )

        result = client.get_item("oak_log", 2)

        self.assertTrue(result["ok"])
        request, timeout = calls[0]
        self.assertEqual(2, timeout)
        self.assertEqual("POST", request.get_method())
        self.assertEqual(
            "http://127.0.0.1:4316/v1/actions/get-item",
            request.full_url,
        )
        self.assertEqual(
            {"item": "oak_log", "count": 2},
            json.loads(request.data.decode("utf-8")),
        )

    def test_facade_routes_get_item_and_stop(self):
        client = mock.Mock()
        client.get_item.return_value = {"ok": True, "accepted": True}
        client.stop.return_value = {"ok": True, "accepted": True}
        config = MinecraftConfig(
            str(PROJECT_ROOT / "plugins" / "Minecraft"),
            config_path=str(PROJECT_ROOT / "missing_minecraft_config.json"),
        )
        service = MinecraftFacadeService(
            config,
            client_factory=lambda **_kwargs: client,
        )

        get_result = service.handle_command(
            {"action": "get-item", "item": "oak_log", "count": "3"}
        )
        stop_result = service.handle_command({"action": "stop"})

        self.assertTrue(get_result["ok"])
        self.assertTrue(stop_result["ok"])
        client.get_item.assert_called_once_with("oak_log", 3)
        client.stop.assert_called_once_with()

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


if __name__ == "__main__":
    unittest.main()
