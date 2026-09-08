#20260907_kpopmodder: Verify GUI feedback routing needs a one-shot typed input event.
import unittest

from plugins.Minecraft.fabric.chatclef.ui.feedback import (
    CommandFeedbackUiEventReceiptAuthority,
    FabricChatClefUiFeedbackSubmissionRouter,
)


class FabricChatClefUiFeedbackSubmissionRouterTests(unittest.TestCase):
    def setUp(self):
        self.router = FabricChatClefUiFeedbackSubmissionRouter(
            receipt_authority=CommandFeedbackUiEventReceiptAuthority(
                event_id_factory=lambda: "a" * 32,
            )
        )

    def test_raw_feedback_handler_receives_typed_bound_event(self):
        extension = _FeedbackExtension()
        request = {
            "request_id": "raw-1",
            "command": "@get oak_log 2",
            "source": "lavi_gui",
            "metadata": {"ui": "fabric_chatclef"},
        }

        result = self.router.submit_raw(extension, request)

        self.assertEqual({"ok": True}, result)
        self.assertEqual([], extension.fallback_calls)
        submitted, event = extension.feedback_calls[0]
        self.assertIs(request, submitted)
        self.assertEqual("@get oak_log 2", event.text)
        self.assertEqual("lavi_gui", event.source)
        self.assertEqual("a" * 32, event.event_id)
        self.assertTrue(event.final)

    def test_korean_feedback_handler_uses_distinct_source_and_event_kind(self):
        extension = _FeedbackExtension()
        request = {
            "request_id": "ko-1",
            "text": "참나무 원목 두 개 구해줘",
            "source": "lavi_gui_korean",
            "metadata": {"ui": "fabric_chatclef", "language": "ko"},
        }

        self.router.submit_korean(extension, request)

        _submitted, event = extension.feedback_calls[0]
        self.assertEqual("lavi_gui_korean", event.source)
        self.assertEqual("minecraft_korean_gui_submit", event.event_kind)

    def test_extension_without_feedback_api_keeps_exact_legacy_request(self):
        extension = _LegacyExtension()
        request = {
            "request_id": "legacy-1",
            "command": "@scan dirt",
            "source": "lavi_gui",
            "metadata": {"ui": "fabric_chatclef"},
        }

        result = self.router.submit_raw(extension, request)

        self.assertEqual({"legacy": True}, result)
        self.assertEqual([request], extension.calls)

    def test_missing_feedback_and_fallback_handler_is_not_submittable(self):
        self.assertFalse(self.router.can_submit_raw(object()))
        self.assertFalse(self.router.can_submit_korean(object()))


class _FeedbackExtension:
    def __init__(self):
        self.feedback_calls = []
        self.fallback_calls = []

    def handle_ui_command_with_feedback(self, request, *, input_event):
        self.feedback_calls.append((request, input_event))
        return {"ok": True}

    def handle_ui_natural_language_command_with_feedback(
        self,
        request,
        *,
        input_event,
    ):
        self.feedback_calls.append((request, input_event))
        return {"ok": True}

    def handle_command(self, request):
        self.fallback_calls.append(request)
        return {"ok": True}

    def handle_natural_language_command(self, request):
        self.fallback_calls.append(request)
        return {"ok": True}


class _LegacyExtension:
    def __init__(self):
        self.calls = []

    def handle_command(self, request):
        self.calls.append(request)
        return {"legacy": True}


if __name__ == "__main__":
    unittest.main()
