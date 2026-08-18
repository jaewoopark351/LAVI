#20260819_kpopmodder: Verify Gradio submission exceptions stay unknown without replay.
import unittest

from plugins.Minecraft.common.dto.command_request_dto import CommandRequestDTO
from plugins.Minecraft.fabric.chatclef.ui.fabric_chatclef_command_controller import (
    FabricChatClefCommandController,
)


class MinecraftFabricChatClefControllerSubmissionExceptionTests(unittest.TestCase):
    def test_korean_handler_exception_is_unknown_and_called_once(self):
        extension = _RaisingExtension("handle_natural_language_command")
        controller = FabricChatClefCommandController(
            plugin=object(),
            extension=extension,
        )

        result = controller.submit_korean_command("다이아몬드 캐줘")

        self.assertEqual(1, extension.call_count)
        self.assert_unknown_reconciliation_result(result, "lavi-ko-gui-")

    def test_raw_handler_exception_is_unknown_and_called_once(self):
        extension = _RaisingExtension("handle_command")
        controller = FabricChatClefCommandController(
            plugin=object(),
            extension=extension,
        )

        result = controller.submit_command("get diamond 1")

        self.assertEqual(1, extension.call_count)
        self.assert_unknown_reconciliation_result(result, "lavi-gui-")

    def test_adapter_submit_exception_is_unknown_and_called_once(self):
        adapter = _RaisingAdapter()
        controller = FabricChatClefCommandController(
            plugin=_AdapterPlugin(adapter),
        )
        request = CommandRequestDTO(
            request_id="adapter-request-1",
            command="get diamond 1",
            source="test",
        )

        result = controller.submit_with_plugin_adapter(request)

        self.assertEqual(1, adapter.call_count)
        self.assert_unknown_reconciliation_result(result, "adapter-request-1")

    def test_missing_korean_handler_remains_rejected_before_invocation(self):
        controller = FabricChatClefCommandController(
            plugin=object(),
            extension=object(),
        )

        result = controller.submit_korean_command("다이아몬드 캐줘")

        self.assertEqual("rejected", result["status"]["status"])
        self.assertEqual("not_implemented", result["error"])
        self.assertEqual({}, result["details"])

    def assert_unknown_reconciliation_result(self, result, request_id_prefix):
        self.assertFalse(result["ok"])
        self.assertEqual("unknown", result["status"]["status"])
        self.assertEqual("internal_error", result["status"]["error_code"])
        self.assertEqual("internal_error", result["error"])
        self.assertTrue(result["status"]["request_id"].startswith(request_id_prefix))
        expected_details = {
            "submission_outcome": "submission_outcome_unknown",
            "reconciliation_required": True,
        }
        self.assertEqual(expected_details, result["status"]["data"])
        self.assertEqual(expected_details, result["details"])


class _RaisingExtension:
    def __init__(self, handler_name):
        self.call_count = 0
        setattr(self, handler_name, self._raise_after_invocation)

    def _raise_after_invocation(self, _request):
        self.call_count += 1
        raise ConnectionResetError("response lost")


class _RaisingAdapter:
    def __init__(self):
        self.call_count = 0

    def submit_command(self, _request):
        self.call_count += 1
        raise TimeoutError("submit result unavailable")


class _AdapterPlugin:
    def __init__(self, adapter):
        self._adapter = adapter

    def create_adapter(self):
        return self._adapter


if __name__ == "__main__":
    unittest.main()
