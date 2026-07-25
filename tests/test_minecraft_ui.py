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

    def equip(self, item):
        self.commands.append(("equip", item))
        return {"ok": True, "action": "equip"}

    def get_and_equip(self, item, count):
        self.commands.append(("get_and_equip", item, count))
        return {"ok": True, "action": "get_and_equip"}

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

    def test_equip_click_routes_to_facade_equip(self):
        facade = FakeMinecraftFacade()
        controller = MinecraftUiController(FakeMinecraftConfig(), facade)

        result = json.loads(controller.on_equip_click("iron_pickaxe"))

        self.assertTrue(result["ok"])
        self.assertEqual("equip", result["action"])
        self.assertEqual([("equip", "iron_pickaxe")], facade.commands)

    def test_get_and_equip_click_routes_to_facade_get_and_equip(self):
        facade = FakeMinecraftFacade()
        controller = MinecraftUiController(FakeMinecraftConfig(), facade)

        result = json.loads(controller.on_get_and_equip_click("iron_pickaxe", "1"))

        self.assertTrue(result["ok"])
        self.assertEqual("get_and_equip", result["action"])
        self.assertEqual(
            [("get_and_equip", "iron_pickaxe", "1")],
            facade.commands,
        )


if __name__ == "__main__":
    unittest.main()
