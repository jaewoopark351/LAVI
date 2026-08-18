#20260803_kpopmodder: Cover chat/mic routing from LAVI input into Fabric ChatClef.
import unittest

from app_core.composition_core.app_component_wiring_service import (
    AppComponentWiringService,
)
from plugins.Minecraft.fabric.chatclef.input import (
    MinecraftChatClefInputIntentGate,
    MinecraftChatClefInputRouter,
)


class MinecraftChatClefInputRouterTests(unittest.TestCase):
    def test_gate_only_accepts_minecraft_like_korean_commands(self):
        gate = MinecraftChatClefInputIntentGate()

        self.assertTrue(gate.should_consider("금괴 8개 구해"))
        self.assertTrue(gate.should_consider("다이아몬드 캐줘"))
        self.assertTrue(gate.should_consider("석탄 5개 캐와줘"))
        self.assertTrue(gate.should_consider("돌 10개 채굴해줘"))
        self.assertTrue(
            gate.should_consider(
                "다이아몬드 도끼 "
                "하나 가져와"
            )
        )
        self.assertTrue(
            gate.should_consider("100 64 -30으로 이동해")
        )
        self.assertTrue(gate.should_consider("멈춰"))
        self.assertFalse(
            gate.should_consider("오늘 뭐 먹지?")
        )
        self.assertFalse(
            gate.should_consider("그냥 이야기하자")
        )
        self.assertFalse(gate.should_consider("캐나다 여행 얘기하자"))
        self.assertFalse(gate.should_consider("캐시가 10개 남았어"))
        self.assertFalse(gate.should_consider("캐시 확인해줘"))

    def test_non_minecraft_input_is_not_handled(self):
        extension = _RecordingExtension(
            translation=_validated_get_translation("get gold_ingot 8", "금괴", 8, "gold_ingot")
        )
        router = MinecraftChatClefInputRouter(
            extension=extension,
            log_callback=lambda _message: None,
        )

        decision = router.route("오늘 뭐 먹지?")

        self.assertFalse(decision.handled)
        self.assertEqual("no_minecraft_trigger", decision.reason)
        self.assertEqual([], extension.translated)
        self.assertEqual([], extension.submitted)

    def test_valid_minecraft_input_submits_translated_command_once(self):
        extension = _RecordingExtension(
            translation=_validated_get_translation("get gold_ingot 8", "금괴", 8, "gold_ingot"),
            result={
                "ok": True,
                "status": {"status": "accepted"},
                "message": "sent",
            },
        )
        router = MinecraftChatClefInputRouter(
            extension=extension,
            log_callback=lambda _message: None,
        )

        decision = router.route("금괴 8개 구해")

        self.assertTrue(decision.handled)
        self.assertEqual("minecraft_command_routed", decision.reason)
        self.assertEqual(
            "[Minecraft] command sent: get gold_ingot 8",
            decision.response_text,
        )
        self.assertEqual(["금괴 8개 구해"], extension.translated)
        self.assertEqual(
            "금괴 8개 구해",
            extension.submitted[0]["text"],
        )
        self.assertEqual("get gold_ingot 8", extension.submitted[0]["translation"]["command"])
        self.assertEqual("lavi_chat_mic_router", extension.submitted[0]["source"])

    def test_unknown_translation_falls_through_to_llm(self):
        extension = _RecordingExtension(
            translation={
                "status": "unknown",
                "executable": False,
                "command": None,
                "message": "unknown",
            },
        )
        router = MinecraftChatClefInputRouter(
            extension=extension,
            log_callback=lambda _message: None,
        )

        decision = router.route("무언가 가져와줘")

        self.assertFalse(decision.handled)
        self.assertEqual("unknown_intent", decision.reason)
        self.assertEqual([], extension.submitted)

    def test_invalid_minecraft_like_input_is_consumed_as_rejection(self):
        extension = _RecordingExtension(
            translation={
                "status": "invalid",
                "executable": False,
                "command": None,
                "message": "invalid",
            },
            result={
                "ok": False,
                "status": {"status": "rejected"},
                "message": "Korean command contains invalid input.",
            },
        )
        router = MinecraftChatClefInputRouter(
            extension=extension,
            log_callback=lambda _message: None,
        )

        decision = router.route(
            "다이아몬드 도끼 1; "
            "stop 가져와"
        )

        self.assertTrue(decision.handled)
        self.assertIn("command rejected", decision.response_text)
        self.assertEqual([], extension.submitted)

    def test_validated_translation_is_rejected_when_bridge_is_disconnected(self):
        extension = _RecordingExtension(
            translation=_validated_get_translation("get diamond 1", "다이아몬드", 1, "diamond"),
            bridge_status={
                "details": {
                    "backend_id": "fabric_chatclef",
                    "enabled": True,
                    "connected": False,
                    "lifecycle_state": "disconnected",
                    "details": {"commands": {"active_request_id": None}},
                }
            },
        )
        router = MinecraftChatClefInputRouter(
            extension=extension,
            log_callback=lambda _message: None,
        )

        decision = router.route("다이아몬드 캐줘")

        self.assertTrue(decision.handled)
        self.assertEqual("minecraft_bridge_disconnected", decision.reason)
        self.assertIn("not connected", decision.response_text)
        self.assertEqual([], extension.submitted)

    def test_validated_translation_is_rejected_when_command_is_active(self):
        extension = _RecordingExtension(
            translation=_validated_get_translation("get diamond 1", "다이아몬드", 1, "diamond"),
            bridge_status={
                "details": {
                    "backend_id": "fabric_chatclef",
                    "enabled": True,
                    "connected": True,
                    "lifecycle_state": "connected",
                    "details": {
                        "commands": {"active_request_id": "already-active"}
                    },
                }
            },
        )
        router = MinecraftChatClefInputRouter(
            extension=extension,
            log_callback=lambda _message: None,
        )

        decision = router.route("다이아몬드 캐줘")

        self.assertTrue(decision.handled)
        self.assertEqual("minecraft_command_busy", decision.reason)
        self.assertIn("already active", decision.response_text)
        self.assertEqual([], extension.submitted)

    def test_app_wiring_injects_router_without_replacing_input_listener(self):
        service = AppComponentWiringService()
        input_component = _ListenerSource()
        llm = _FakeLLM()
        translate = _ListenerSource()
        tts = _ListenerSource()
        vtuber = _ListenerSource()
        extension = _RecordingExtension(
            translation={"status": "unknown", "executable": False}
        )

        service.wire_event_listeners(
            input_component=input_component,
            llm=llm,
            translate=translate,
            tts=tts,
            vtuber=vtuber,
            minecraft_fabric_chatclef_extension=extension,
        )

        self.assertIsInstance(llm.input_router, MinecraftChatClefInputRouter)
        self.assertIn(llm.receive_input, input_component.listeners)


class _RecordingExtension:
    def __init__(self, translation, result=None, bridge_status=None):
        self.translation = dict(translation)
        self.result = dict(result or {"ok": True, "status": {"status": "accepted"}})
        self.bridge_status = dict(
            bridge_status
            or {
                "details": {
                    "backend_id": "fabric_chatclef",
                    "enabled": True,
                    "connected": True,
                    "lifecycle_state": "connected",
                    "details": {"commands": {"active_request_id": None}},
                }
            }
        )
        self.translated = []
        self.submitted = []

    def translate_natural_language_command(self, text):
        self.translated.append(str(text))
        return dict(self.translation)

    def handle_natural_language_command(self, request):
        self.submitted.append(dict(request))
        return dict(self.result)

    def submit_translated_command(self, request, translation):
        payload = dict(request)
        payload["translation"] = dict(translation)
        self.submitted.append(payload)
        result = dict(self.result)
        status = result.get("status")
        if isinstance(status, dict):
            status = dict(status)
            status.setdefault("request_id", payload["request_id"])
            result["status"] = status
        return result

    def get_status(self):
        return dict(self.bridge_status)


def _validated_get_translation(
    command: str,
    item_phrase: str,
    quantity: int,
    target: str,
) -> dict[str, object]:
    return {
        "status": "validated",
        "executable": True,
        "command": command,
        "intent": {
            "intent_type": "get_item",
            "item_phrase": item_phrase,
            "quantity": quantity,
        },
        "resolved_target": target,
        "reason_code": "validated",
        "message": "Korean command was translated to ChatClef DSL.",
        "data": {},
    }


class _ListenerSource:
    def __init__(self):
        self.listeners = []

    def add_output_event_listener(self, listener, *args, **kwargs):
        self.listeners.append(listener)

    def receive_input(self, _value):
        pass


class _FakeLLM(_ListenerSource):
    def __init__(self):
        super().__init__()
        self.input_router = None

    def set_input_router(self, router):
        self.input_router = router


if __name__ == "__main__":
    unittest.main()
