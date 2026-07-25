#20260725_kpopmodder: Covers the Minecraft ChatClef bridge client and game extension adapter.
import json
import unittest
from pathlib import Path
from unittest import mock

from app_core.extensions.game_extension_context import GameExtensionContext
from app_core.extensions.minecraft_game_extension import MinecraftGameExtension
from plugins.Minecraft.minecraft_core import (
    ChatClefBridgeClient,
    MinecraftActionService,
    MinecraftCommandParser,
    MinecraftCommandRouter,
    MinecraftConfig,
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
        if isinstance(command, dict):
            return {"ok": True, "action": command.get("action")}
        return {"ok": True, "action": command}

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

    def test_client_posts_goto_and_stop_to_v1_endpoints(self):
        calls = []

        def opener(request, timeout):
            calls.append((request, timeout))
            return FakeHttpResponse({"ok": True, "accepted": True})

        client = ChatClefBridgeClient(
            base_url="http://127.0.0.1:4316",
            timeout_sec=2,
            opener=opener,
        )

        goto_result = client.goto("0 64 0 overworld")
        stop_result = client.stop()

        self.assertTrue(goto_result["ok"])
        self.assertTrue(stop_result["ok"])
        self.assertEqual("POST", calls[0][0].get_method())
        self.assertEqual(
            "http://127.0.0.1:4316/v1/actions/goto",
            calls[0][0].full_url,
        )
        self.assertEqual(
            {"target": "0 64 0 overworld"},
            json.loads(calls[0][0].data.decode("utf-8")),
        )
        self.assertEqual(
            "http://127.0.0.1:4316/v1/actions/stop",
            calls[1][0].full_url,
        )

    def test_facade_routes_get_item_goto_stop_and_cancel(self):
        client = mock.Mock()
        client.get_item.return_value = {"ok": True, "accepted": True}
        client.goto.return_value = {"ok": True, "accepted": True}
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
        goto_result = service.handle_command(
            {"action": "goto", "target": "0 64 0 overworld"}
        )
        stop_result = service.handle_command({"action": "stop"})
        cancel_result = service.handle_command({"action": "cancel"})

        self.assertTrue(get_result["ok"])
        self.assertTrue(goto_result["ok"])
        self.assertTrue(stop_result["ok"])
        self.assertTrue(cancel_result["ok"])
        client.get_item.assert_called_once_with("oak_log", 3)
        client.goto.assert_called_once_with(
            "0 64 0 overworld",
            x=None,
            y=None,
            z=None,
            dimension=None,
        )
        self.assertEqual(2, client.stop.call_count)

    def test_facade_delegates_action_calls_to_service_and_router(self):
        client = mock.Mock()
        client.health.return_value = {"ok": True}
        config = MinecraftConfig(
            str(PROJECT_ROOT / "plugins" / "Minecraft"),
            config_path=str(PROJECT_ROOT / "missing_minecraft_config.json"),
        )
        service = MinecraftFacadeService(
            config,
            client_factory=lambda **_kwargs: client,
        )

        result = service.handle_command("health")

        self.assertTrue(result["ok"])
        self.assertIsInstance(service.action_service, MinecraftActionService)
        self.assertIsInstance(service.command_router, MinecraftCommandRouter)
        client.health.assert_called_once_with()

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


if __name__ == "__main__":
    unittest.main()
