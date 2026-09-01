#20260801_kpopmodder: Verify Fabric ChatClef GUI stays backend-owned and lifecycle-passive.
import json
import unittest
from types import SimpleNamespace
from unittest import mock

from plugins.Minecraft.fabric.chatclef.minecraft_fabric_chatclef_plugin import (
    MinecraftFabricChatClefPlugin,
)
from plugins.Minecraft.fabric.chatclef.ui import FabricChatClefPanel


class MinecraftFabricChatClefGuiTests(unittest.TestCase):
    def test_panel_uses_short_user_facing_tab_label(self):
        self.assertEqual("Minecraft Fabric", FabricChatClefPanel.TAB_LABEL)

    def test_plugin_create_ui_delegates_to_fabric_panel(self):
        plugin = MinecraftFabricChatClefPlugin()
        extension = object()

        with mock.patch(
            "plugins.Minecraft.fabric.chatclef.minecraft_fabric_chatclef_plugin."
            "FabricChatClefPanel",
        ) as panel_class:
            panel = panel_class.return_value

            plugin.create_ui(extension=extension)

        panel_class.assert_called_once_with(plugin=plugin, extension=extension)
        panel.create_ui.assert_called_once_with()

    def test_panel_refresh_reads_status_without_starting_adapter(self):
        adapter = _FakeAdapter()
        plugin = SimpleNamespace(
            config=SimpleNamespace(endpoint="ws://127.0.0.1:4316"),
            create_adapter=lambda: adapter,
            get_status=lambda: {
                "endpoint": "ws://127.0.0.1:4316",
                "bridge": {
                    "backend_id": "fabric_chatclef",
                    "enabled": True,
                    "connected": False,
                    "lifecycle_state": "disconnected",
                    "detail": "waiting",
                    "details": {"endpoint": "ws://127.0.0.1:4316"},
                },
            },
        )

        endpoint, lifecycle, connected, status_json = FabricChatClefPanel(
            plugin=plugin,
        ).on_refresh_click()

        self.assertEqual("ws://127.0.0.1:4316", endpoint)
        self.assertEqual("disconnected", lifecycle)
        self.assertEqual("false", connected)
        self.assertIn("fabric_chatclef", status_json)
        self.assertEqual(0, adapter.start_calls)

    def test_panel_submit_uses_extension_command_path(self):
        calls = []

        class FakeExtension:
            def handle_command(self, command):
                calls.append(command)
                return {
                    "ok": True,
                    "status": {
                        "request_id": command["request_id"],
                        "status": "accepted",
                    },
                    "error": None,
                    "message": "accepted",
                    "details": {},
                }

            def get_status(self):
                return {
                    "details": {
                        "backend_id": "fabric_chatclef",
                        "enabled": True,
                        "connected": True,
                        "lifecycle_state": "connected",
                        "detail": "connected",
                        "details": {"endpoint": "ws://127.0.0.1:4316"},
                    }
                }

        result_json, endpoint, lifecycle, connected, _status_json = (
            FabricChatClefPanel(
                plugin=object(),
                extension=FakeExtension(),
            ).on_submit_command_click(" @get oak_log 1 ")
        )

        result = json.loads(result_json)
        self.assertTrue(result["ok"])
        self.assertEqual("accepted", result["status"]["status"])
        self.assertEqual("@get oak_log 1", calls[0]["command"])
        self.assertEqual("lavi_gui", calls[0]["source"])
        self.assertEqual({"ui": "fabric_chatclef"}, calls[0]["metadata"])
        self.assertEqual("ws://127.0.0.1:4316", endpoint)
        self.assertEqual("connected", lifecycle)
        self.assertEqual("true", connected)

    def test_panel_preserves_exact_store_home_raw_command_identity_once(self):
        calls = []

        class FakeExtension:
            def handle_command(self, command):
                calls.append(command)
                return {
                    "ok": True,
                    "status": {
                        "request_id": command["request_id"],
                        "status": "accepted",
                    },
                    "error": None,
                    "message": "accepted",
                    "details": {},
                }

            def get_status(self):
                return {"details": {}}

        with mock.patch(
            "plugins.Minecraft.fabric.chatclef.ui."
            "fabric_chatclef_command_controller.uuid.uuid4",
            return_value=SimpleNamespace(hex="storehomeidentity"),
        ):
            result_json, *_status = FabricChatClefPanel(
                plugin=object(),
                extension=FakeExtension(),
            ).on_submit_command_click("  @store_home  ")

        expected_request = {
            "request_id": "lavi-gui-storehomeidentity",
            "command": "@store_home",
            "source": "lavi_gui",
            "metadata": {"ui": "fabric_chatclef"},
        }
        self.assertEqual([expected_request], calls)
        result = json.loads(result_json)
        self.assertEqual(
            expected_request["request_id"],
            result["status"]["request_id"],
        )
        self.assertEqual("accepted", result["status"]["status"])

    def test_panel_rejects_empty_command_locally(self):
        result_json, _endpoint, _lifecycle, _connected, _status_json = (
            FabricChatClefPanel(plugin=object()).on_submit_command_click(" ")
        )

        result = json.loads(result_json)
        self.assertFalse(result["ok"])
        self.assertEqual("rejected", result["status"]["status"])
        self.assertEqual("invalid_request", result["error"])


class _FakeAdapter:
    def __init__(self):
        self.start_calls = 0

    def start(self):
        self.start_calls += 1


if __name__ == "__main__":
    unittest.main()
