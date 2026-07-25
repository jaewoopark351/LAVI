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


if __name__ == "__main__":
    unittest.main()
