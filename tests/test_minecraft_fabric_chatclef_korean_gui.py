#20260803_kpopmodder: Cover Korean Minecraft command UI path separately from raw ChatClef commands.
import json
import unittest
from types import SimpleNamespace

from plugins.Minecraft.fabric.chatclef.ui import FabricChatClefPanel


class MinecraftFabricChatClefKoreanGuiTests(unittest.TestCase):
    def test_panel_exposes_separate_korean_command_label(self):
        self.assertEqual(
            "Korean Minecraft Command",
            FabricChatClefPanel.KOREAN_COMMAND_LABEL,
        )

    def test_korean_command_uses_extension_natural_language_path(self):
        calls = []

        class FakeExtension:
            def handle_natural_language_command(self, command):
                calls.append(command)
                return {
                    "ok": True,
                    "status": {
                        "request_id": command["request_id"],
                        "status": "accepted",
                    },
                    "error": None,
                    "message": "accepted",
                    "details": {"command": "get diamond_axe 1"},
                }

            def get_status(self):
                return _connected_status()

        result_json, endpoint, lifecycle, connected, _status_json = (
            FabricChatClefPanel(
                plugin=object(),
                extension=FakeExtension(),
            ).on_submit_korean_command_click(" 다이아몬드 도끼 하나 가져와 ")
        )

        result = json.loads(result_json)
        self.assertTrue(result["ok"])
        self.assertEqual("accepted", result["status"]["status"])
        self.assertEqual("다이아몬드 도끼 하나 가져와", calls[0]["text"])
        self.assertEqual("lavi_gui_korean", calls[0]["source"])
        self.assertEqual(
            {"ui": "fabric_chatclef", "language": "ko"},
            calls[0]["metadata"],
        )
        self.assertEqual("ws://127.0.0.1:4316", endpoint)
        self.assertEqual("connected", lifecycle)
        self.assertEqual("true", connected)

    def test_korean_command_requires_extension_handler(self):
        plugin = SimpleNamespace(get_status=_connected_status)

        result_json, _endpoint, _lifecycle, _connected, _status_json = (
            FabricChatClefPanel(
                plugin=plugin,
                extension=object(),
            ).on_submit_korean_command_click("금괴 8개 구해")
        )

        result = json.loads(result_json)
        self.assertFalse(result["ok"])
        self.assertEqual("not_implemented", result["error"])
        self.assertIn("Korean command handler", result["message"])

    def test_panel_rejects_empty_korean_command_locally(self):
        plugin = SimpleNamespace(get_status=_connected_status)

        result_json, _endpoint, _lifecycle, _connected, _status_json = (
            FabricChatClefPanel(plugin=plugin).on_submit_korean_command_click(" ")
        )

        result = json.loads(result_json)
        self.assertFalse(result["ok"])
        self.assertEqual("rejected", result["status"]["status"])
        self.assertEqual("invalid_request", result["error"])
        self.assertEqual("Korean Minecraft command is empty.", result["message"])


def _connected_status():
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


if __name__ == "__main__":
    unittest.main()
