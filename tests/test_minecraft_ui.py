#20260725_kpopmodder: Covers Minecraft UI controller callback routing.
import json
import unittest

from plugins.Minecraft.minecraft_core.ui.minecraft_ui_controller import (
    MinecraftUiController,
)


class FakeMinecraftConfig:
    def config_message(self):
        return "configured"


class FakeMinecraftFacade:
    def __init__(self):
        self.commands = []

    def handle_command(self, command):
        self.commands.append(command)
        return {"ok": True, "action": "get_item"}

    def status_json(self, payload):
        return json.dumps(payload, sort_keys=True)

    def public_config(self):
        return {}


class MinecraftUiControllerTests(unittest.TestCase):
    def test_run_command_click_routes_text_to_facade_command_handler(self):
        facade = FakeMinecraftFacade()
        controller = MinecraftUiController(FakeMinecraftConfig(), facade)

        result = json.loads(controller.on_run_command_click("get oak_log 1"))

        self.assertTrue(result["ok"])
        self.assertEqual("get_item", result["action"])
        self.assertEqual(["get oak_log 1"], facade.commands)


if __name__ == "__main__":
    unittest.main()
