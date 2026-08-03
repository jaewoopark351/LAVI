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

        self.assertTrue(gate.should_consider("\uae08\uad34 8\uac1c \uad6c\ud574"))
        self.assertTrue(
            gate.should_consider(
                "\ub2e4\uc774\uc544\ubaac\ub4dc \ub3c4\ub07c "
                "\ud558\ub098 \uac00\uc838\uc640"
            )
        )
        self.assertTrue(
            gate.should_consider("100 64 -30\uc73c\ub85c \uc774\ub3d9\ud574")
        )
        self.assertTrue(gate.should_consider("\uba48\ucdb0"))
        self.assertFalse(
            gate.should_consider("\uc624\ub298 \ubb50 \uba39\uc9c0?")
        )
        self.assertFalse(
            gate.should_consider("\uadf8\ub0e5 \uc774\uc57c\uae30\ud558\uc790")
        )

    def test_non_minecraft_input_is_not_handled(self):
        extension = _RecordingExtension(
            translation={
                "status": "validated",
                "executable": True,
                "command": "get gold_ingot 8",
            }
        )
        router = MinecraftChatClefInputRouter(
            extension=extension,
            log_callback=lambda _message: None,
        )

        decision = router.route("\uc624\ub298 \ubb50 \uba39\uc9c0?")

        self.assertFalse(decision.handled)
        self.assertEqual("no_minecraft_trigger", decision.reason)
        self.assertEqual([], extension.translated)
        self.assertEqual([], extension.submitted)

    def test_valid_minecraft_input_submits_natural_language_command(self):
        extension = _RecordingExtension(
            translation={
                "status": "validated",
                "executable": True,
                "command": "get gold_ingot 8",
            },
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

        decision = router.route("\uae08\uad34 8\uac1c \uad6c\ud574")

        self.assertTrue(decision.handled)
        self.assertEqual("minecraft_command_routed", decision.reason)
        self.assertEqual(
            "[Minecraft] command sent: get gold_ingot 8",
            decision.response_text,
        )
        self.assertEqual(["\uae08\uad34 8\uac1c \uad6c\ud574"], extension.translated)
        self.assertEqual(
            "\uae08\uad34 8\uac1c \uad6c\ud574",
            extension.submitted[0]["text"],
        )
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

        decision = router.route("\ubb34\uc5b8\uac00 \uac00\uc838\uac08\uae4c?")

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
            "\ub2e4\uc774\uc544\ubaac\ub4dc \ub3c4\ub07c 1; "
            "stop \uac00\uc838\uc640"
        )

        self.assertTrue(decision.handled)
        self.assertIn("command rejected", decision.response_text)
        self.assertEqual(1, len(extension.submitted))

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
    def __init__(self, translation, result=None):
        self.translation = dict(translation)
        self.result = dict(result or {"ok": True, "status": {"status": "accepted"}})
        self.translated = []
        self.submitted = []

    def translate_natural_language_command(self, text):
        self.translated.append(str(text))
        return dict(self.translation)

    def handle_natural_language_command(self, request):
        self.submitted.append(dict(request))
        return dict(self.result)


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
