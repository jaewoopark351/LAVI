#20260803_kpopmodder: Cover Korean command entry without changing raw ChatClef command flow.
import unittest

from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO
from plugins.Minecraft.common.protocol.command_result_status import CommandResultStatus
from plugins.Minecraft.fabric.chatclef.extension import (
    MinecraftFabricChatClefExtension,
)


class MinecraftChatClefKoreanCommandIntegrationTests(unittest.TestCase):
    def test_extension_translates_korean_then_uses_existing_command_handler(self):
        adapter = _RecordingAdapter()
        extension = MinecraftFabricChatClefExtension(adapter=adapter)

        result = extension.handle_natural_language_command(
            {
                "request_id": "ko-1",
                "text": "다이아몬드 도끼 하나 가져와",
                "metadata": {"origin": "test"},
            }
        )

        self.assertTrue(result["ok"])
        self.assertEqual("get diamond_axe 1", adapter.requests[0].command)
        self.assertEqual("lavi_korean_intent", adapter.requests[0].source)
        self.assertEqual("test", adapter.requests[0].metadata["origin"])
        self.assertIn("natural_language", adapter.requests[0].metadata)

    def test_extension_rejects_invalid_korean_without_adapter_submission(self):
        adapter = _RecordingAdapter()
        extension = MinecraftFabricChatClefExtension(adapter=adapter)

        result = extension.handle_natural_language_command("다이아 도끼 1; stop")

        self.assertFalse(result["ok"])
        self.assertEqual("dangerous_command_slot", result["error"])
        self.assertEqual([], adapter.requests)


class _RecordingAdapter:
    backend_id = "fabric_chatclef"

    def __init__(self):
        self.requests = []

    def submit_command(self, request):
        self.requests.append(request)
        return CommandResultDTO(
            request_id=request.request_id,
            ok=True,
            status=CommandResultStatus.ACCEPTED,
            message="accepted",
            data={"command": request.command},
        )


if __name__ == "__main__":
    unittest.main()
