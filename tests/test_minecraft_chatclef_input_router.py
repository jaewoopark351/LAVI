#20260803_kpopmodder: Cover chat/mic routing from LAVI input into Fabric ChatClef.
#20260819_kpopmodder: Keep router fakes aligned with the canonical mirrored result contract.
#20260905_kpopmodder: Lock legacy feedback outside trusted Korean ingress.
import unittest

from app_core.composition_core.app_component_wiring_service import (
    AppComponentWiringService,
)
from input_core.input_event.contracts import LaviInputEvent
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
            result=_submission_result(message="sent"),
        )
        router = MinecraftChatClefInputRouter(
            extension=extension,
            log_callback=lambda _message: None,
        )

        decision = router.route(_chat_event("금괴 8개 구해"))

        self.assertTrue(decision.handled)
        self.assertEqual("minecraft_command_routed", decision.reason)
        self.assertEqual(
            "[Minecraft] 금 주괴 8개 수집 명령을 제출했어요.",
            decision.response_text,
        )
        self.assertEqual(["금괴 8개 구해"], extension.translated)
        self.assertEqual(
            "금괴 8개 구해",
            extension.submitted[0]["text"],
        )
        self.assertEqual("get gold_ingot 8", extension.submitted[0]["translation"]["command"])
        self.assertEqual("lavi_chat_ui", extension.submitted[0]["source"])

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

    def test_direct_and_forged_proof_item_rejections_fall_through(self):
        text = "구리 검 하나 만들어"
        extension = _RecordingExtension(
            translation=_item_translation_rejection(
                text=text,
                status="unsupported",
                reason_code="unsupported_material",
                item_phrase="구리 검",
            ),
        )
        router = MinecraftChatClefInputRouter(
            extension=extension,
            log_callback=lambda _message: None,
        )

        direct = router.route(_chat_event(text))
        forged = router.route(
            _chat_event(text),
            korean_eligibility_proof=object(),
        )

        self.assertFalse(direct.handled)
        self.assertEqual("unsupported_intent", direct.reason)
        self.assertFalse(forged.handled)
        self.assertEqual("unsupported_intent", forged.reason)
        self.assertEqual([], extension.submitted)

    def test_untrusted_ingress_evidence_cannot_receive_scoped_feedback(self):
        extension = _RecordingExtension(
            translation=_validated_get_translation(
                "get diamond 1",
                "다이아몬드",
                1,
                "diamond",
            ),
        )
        router = MinecraftChatClefInputRouter(
            extension=extension,
            log_callback=lambda _message: None,
        )

        decision = router.route_trusted_user_input(
            _chat_event("다이아몬드 캐줘"),
            object(),
        )

        self.assertTrue(decision.handled)
        self.assertEqual("consumed_ingress_evidence_invalid", decision.reason)
        self.assertEqual("", decision.response_text)
        self.assertFalse(decision.publish_external_response)
        self.assertIsNone(decision.response_emission_capability)
        self.assertTrue(decision.suppress_response)
        self.assertEqual([], extension.translated)
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
        self.assertIn("명령을 이해하지 못했어요", decision.response_text)
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
        self.assertEqual(
            "[Minecraft] 마인크래프트 연결이 끊겨 있어요.",
            decision.response_text,
        )
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
        self.assertEqual(
            "[Minecraft] 지금 다른 마인크래프트 작업을 하고 있어요.",
            decision.response_text,
        )
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
        self.result = dict(result or _submission_result())
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


def _chat_event(text: str) -> LaviInputEvent:
    return LaviInputEvent(
        text=text,
        source="lavi_chat_ui",
        event_id="1" * 32,
        event_kind="chat_submit",
        final=True,
        provider_id="lavi_chat_ui",
        fallback_payload=text,
    )


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


def _submission_result(
    *,
    ok: bool = True,
    status: str = "accepted",
    error_code: str | None = None,
    message: str = "accepted",
) -> dict[str, object]:
    data: dict[str, object] = {}
    return {
        "ok": ok,
        "status": {
            "ok": ok,
            "status": status,
            "error_code": error_code,
            "message": message,
            "data": dict(data),
        },
        "error": error_code,
        "message": message,
        "details": dict(data),
    }


def _item_translation_rejection(
    *,
    text: str,
    status: str,
    reason_code: str,
    item_phrase: str,
) -> dict[str, object]:
    return {
        "status": status,
        "executable": False,
        "command": None,
        "intent": {
            "intent_type": "get_item",
            "quantity": 1,
            "item_phrase": item_phrase,
            "original_text": text,
            "source": "rule",
            "language": "ko",
        },
        "resolved_target": None,
        "reason_code": reason_code,
        "message": "Korean item phrase could not be resolved.",
        "data": {
            "resolution": {
                "status": status,
                "target": None,
                "reason_code": reason_code,
                "data": {},
            }
        },
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
