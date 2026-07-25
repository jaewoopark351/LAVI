#20260725_kpopmodder: Covers Minecraft facade command routing behavior.
import unittest
from pathlib import Path
from unittest import mock

from plugins.Minecraft.minecraft_core import (
    MinecraftActionService,
    MinecraftCommandRouter,
    MinecraftConfig,
    MinecraftFacadeService,
)


PROJECT_ROOT = Path(__file__).resolve().parents[1]


class MinecraftFacadeTests(unittest.TestCase):
    def test_facade_routes_get_item_goto_stop_and_cancel(self):
        client = mock.Mock()
        client.get_item.return_value = {"ok": True, "accepted": True}
        client.get_and_equip.return_value = {"ok": True, "accepted": True}
        client.craft.return_value = {"ok": True, "accepted": True}
        client.equip.return_value = {"ok": True, "accepted": True}
        client.goto.return_value = {"ok": True, "accepted": True}
        client.stop.return_value = {"ok": True, "accepted": True}
        config = MinecraftConfig(
            str(PROJECT_ROOT / "plugins" / "Minecraft"),
            config_path=str(PROJECT_ROOT / "missing_minecraft_config.json"),
        )
        config.config["action_verification"]["enabled"] = False
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
        equip_result = service.handle_command(
            {"action": "equip", "item": "iron_pickaxe"}
        )
        get_and_equip_result = service.handle_command(
            {"action": "get-and-equip", "item": "diamond_pickaxe", "count": "1"}
        )
        craft_result = service.handle_command(
            {"action": "craft", "item": "stick", "count": "4"}
        )
        stop_result = service.handle_command({"action": "stop"})
        cancel_result = service.handle_command({"action": "cancel"})

        self.assertTrue(get_result["ok"])
        self.assertTrue(goto_result["ok"])
        self.assertTrue(equip_result["ok"])
        self.assertTrue(get_and_equip_result["ok"])
        self.assertTrue(craft_result["ok"])
        self.assertTrue(stop_result["ok"])
        self.assertTrue(cancel_result["ok"])
        client.get_item.assert_called_once_with("oak_log", 3)
        client.get_and_equip.assert_called_once_with("diamond_pickaxe", 1)
        client.craft.assert_called_once_with("stick", 4)
        client.equip.assert_called_once_with("iron_pickaxe")
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
        config.config["action_verification"]["enabled"] = False
        service = MinecraftFacadeService(
            config,
            client_factory=lambda **_kwargs: client,
        )

        result = service.handle_command("health")

        self.assertTrue(result["ok"])
        self.assertIsInstance(service.action_service, MinecraftActionService)
        self.assertIsInstance(service.command_router, MinecraftCommandRouter)
        client.health.assert_called_once_with()

    def test_facade_routes_korean_text_commands(self):
        client = mock.Mock()
        client.equip.return_value = {"ok": True, "accepted": True}
        client.craft.return_value = {"ok": True, "accepted": True}
        config = MinecraftConfig(
            str(PROJECT_ROOT / "plugins" / "Minecraft"),
            config_path=str(PROJECT_ROOT / "missing_minecraft_config.json"),
        )
        config.config["action_verification"]["enabled"] = False
        service = MinecraftFacadeService(
            config,
            client_factory=lambda **_kwargs: client,
        )

        equip_result = service.handle_command(
            "\ucca0 \uace1\uad2d\uc774 \uc7a5\ucc29\ud574"
        )
        craft_result = service.handle_command(
            "\ub9c9\ub300\uae30 4\uac1c \ub9cc\ub4e4\uc5b4"
        )

        self.assertTrue(equip_result["ok"])
        self.assertTrue(craft_result["ok"])
        client.equip.assert_called_once_with("iron_pickaxe")
        client.craft.assert_called_once_with("stick", 4)

    def test_facade_previews_command_without_calling_bridge_client(self):
        client = mock.Mock()
        config = MinecraftConfig(
            str(PROJECT_ROOT / "plugins" / "Minecraft"),
            config_path=str(PROJECT_ROOT / "missing_minecraft_config.json"),
        )
        config.config["action_verification"]["enabled"] = False
        service = MinecraftFacadeService(
            config,
            client_factory=lambda **_kwargs: client,
        )

        result = service.preview_command("\ub9c9\ub300\uae30 4\uac1c \ub9cc\ub4e4\uc5b4")

        self.assertTrue(result["ok"])
        self.assertTrue(result["accepted"])
        self.assertTrue(result["preview"])
        self.assertEqual("craft", result["action"]["type"])
        self.assertEqual({"item": "stick", "count": 4}, result["action"]["request"])
        client.assert_not_called()


if __name__ == "__main__":
    unittest.main()
