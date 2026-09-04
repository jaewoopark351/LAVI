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
                "source": "direct_typed",
                "metadata": {"origin": "test"},
            }
        )

        self.assertTrue(result["ok"])
        self.assertEqual("get diamond_axe 1", adapter.requests[0].command)
        self.assertEqual("direct_typed", adapter.requests[0].source)
        self.assertEqual("test", adapter.requests[0].metadata["origin"])
        self.assertIn("natural_language", adapter.requests[0].metadata)

    def test_extension_rejects_invalid_korean_without_adapter_submission(self):
        adapter = _RecordingAdapter()
        extension = MinecraftFabricChatClefExtension(adapter=adapter)

        result = extension.handle_natural_language_command("다이아 도끼 1; stop")

        self.assertFalse(result["ok"])
        self.assertEqual("dangerous_command_slot", result["error"])
        self.assertEqual([], adapter.requests)

    def test_extension_submits_validated_translation_without_retranslating(self):
        adapter = _RecordingAdapter()
        service = _ExplodingNaturalLanguageService()
        extension = MinecraftFabricChatClefExtension(
            adapter=adapter,
            natural_language_service=service,
        )

        result = extension.submit_translated_command(
            {
                "request_id": "ko-translated-1",
                "text": "다이아몬드 캐줘",
                "source": "direct_typed",
                "metadata": {"origin": "router"},
            },
            {
                "status": "validated",
                "executable": True,
                "command": "get diamond 1",
                "intent": {
                    "intent_type": "get_item",
                    "item_phrase": "다이아몬드",
                    "quantity": 1,
                },
                "resolved_target": "diamond",
                "reason_code": "validated",
                "message": "Korean command was translated to ChatClef DSL.",
                "data": {},
            },
        )

        self.assertTrue(result["ok"])
        self.assertEqual(0, service.translate_calls)
        self.assertEqual("get diamond 1", adapter.requests[0].command)
        self.assertEqual("router", adapter.requests[0].metadata["origin"])

    def test_extension_rejects_malformed_validated_translation_without_adapter_submission(self):
        adapter = _RecordingAdapter()
        extension = MinecraftFabricChatClefExtension(adapter=adapter)

        result = extension.submit_translated_command(
            {
                "request_id": "ko-translated-bad",
                "text": "다이아몬드 캐줘",
            },
            {
                "status": "validated",
                "executable": "false",
                "command": "get diamond 1",
                "intent": {
                    "intent_type": "get_item",
                    "item_phrase": "다이아몬드",
                    "quantity": 1,
                },
                "resolved_target": "diamond",
            },
        )

        self.assertFalse(result["ok"])
        self.assertEqual("malformed_translation_result", result["error"])
        self.assertEqual([], adapter.requests)

    def test_generic_admission_does_not_trim_or_coerce_source_identity(self):
        translation = {
            "status": "validated",
            "executable": True,
            "command": "get diamond 1",
            "intent": {
                "intent_type": "get_item",
                "item_phrase": "다이아몬드",
                "quantity": 1,
            },
            "resolved_target": "diamond",
        }
        for source in (" lavi_chat_ui ", "\tlavi_chat_ui\n", ["lavi_chat_ui"]):
            with self.subTest(source=source):
                adapter = _RecordingAdapter()
                extension = MinecraftFabricChatClefExtension(adapter=adapter)

                result = extension.submit_translated_command(
                    {"text": "다이아몬드 캐줘", "source": source},
                    translation,
                )

                self.assertFalse(result["ok"])
                self.assertEqual(
                    "korean_command_source_not_allowed",
                    result["details"]["admission_reason"],
                )
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


class _ExplodingNaturalLanguageService:
    def __init__(self):
        self.translate_calls = 0

    def translate(self, _text):
        self.translate_calls += 1
        raise AssertionError("translation should not run for validated submissions")


if __name__ == "__main__":
    unittest.main()
