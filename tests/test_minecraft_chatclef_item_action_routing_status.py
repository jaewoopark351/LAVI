#20260815_kpopmodder: Lock routing status separation for Korean item actions.
from __future__ import annotations

import unittest

from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_router import (
    MinecraftChatClefInputRouter,
)


class MinecraftChatClefItemActionRoutingStatusTests(unittest.TestCase):
    def test_unknown_resolution_status_falls_through_without_submission(self):
        extension = _RecordingExtension(
            translation={
                "status": "unknown",
                "executable": False,
                "command": None,
                "reason_code": "unknown_item",
            },
        )
        router = MinecraftChatClefInputRouter(
            extension=extension,
            log_callback=lambda _message: None,
        )

        decision = router.route("이상한광물 가져와줘")

        self.assertFalse(decision.handled)
        self.assertEqual("unknown_intent", decision.reason)
        self.assertEqual(["이상한광물 가져와줘"], extension.translated)
        self.assertEqual([], extension.submitted)

    def test_invalid_resolution_status_is_consumed_without_submission(self):
        extension = _RecordingExtension(
            translation={
                "status": "invalid",
                "executable": False,
                "command": None,
                "reason_code": "dangerous_command_slot",
                "message": "Korean command contains invalid input.",
            },
        )
        router = MinecraftChatClefInputRouter(
            extension=extension,
            log_callback=lambda _message: None,
        )

        decision = router.route("다이아몬드 가져와줘")

        self.assertTrue(decision.handled)
        self.assertEqual("minecraft_translation_rejected", decision.reason)
        self.assertEqual([], extension.submitted)

    def test_disconnected_submission_status_is_router_owned(self):
        extension = _RecordingExtension(
            translation=_validated_get_translation(),
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

        decision = router.route("다이아몬드 가져와줘")

        self.assertTrue(decision.handled)
        self.assertEqual("minecraft_bridge_disconnected", decision.reason)
        self.assertEqual(["다이아몬드 가져와줘"], extension.translated)
        self.assertEqual([], extension.submitted)

    def test_busy_submission_status_is_router_owned(self):
        extension = _RecordingExtension(
            translation=_validated_get_translation(),
            bridge_status={
                "details": {
                    "backend_id": "fabric_chatclef",
                    "enabled": True,
                    "connected": True,
                    "lifecycle_state": "connected",
                    "details": {"commands": {"active_request_id": "active-1"}},
                }
            },
        )
        router = MinecraftChatClefInputRouter(
            extension=extension,
            log_callback=lambda _message: None,
        )

        decision = router.route("다이아몬드 가져와줘")

        self.assertTrue(decision.handled)
        self.assertEqual("minecraft_command_busy", decision.reason)
        self.assertEqual([], extension.submitted)

    def test_rejected_submission_is_not_retried_or_fallen_through(self):
        extension = _RecordingExtension(
            translation=_validated_get_translation(),
            result={
                "ok": False,
                "status": {"status": "rejected"},
                "message": "Fabric ChatClef command already pending or active.",
            },
        )
        router = MinecraftChatClefInputRouter(
            extension=extension,
            log_callback=lambda _message: None,
        )

        decision = router.route("다이아몬드 가져와줘")

        self.assertTrue(decision.handled)
        self.assertEqual("minecraft_command_rejected", decision.reason)
        self.assertIn("command rejected", decision.response_text)
        self.assertEqual(1, len(extension.submitted))

    def test_accepted_get_is_not_replayed_for_quantity_recovery(self):
        extension = _RecordingExtension(
            translation=_validated_get_translation(),
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

        decision = router.route("다이아몬드 가져와줘")

        self.assertTrue(decision.handled)
        self.assertEqual("minecraft_command_routed", decision.reason)
        self.assertEqual(1, len(extension.translated))
        self.assertEqual(1, len(extension.submitted))

    def test_malformed_validated_translation_is_consumed_without_submission(self):
        malformed_cases = [
            {"executable": "false"},
            {"command": None},
            {"command": ""},
            {"command": "   "},
            {"command": "@get diamond 1"},
            {"command": "get diamond 1\n@stop"},
            {"command": "get diamond 1; stop"},
            {"command": "get diamond 1 # stop"},
            {"command": 'get "diamond" 1'},
            {"command": "get diamond 1\u0000"},
            {"executable": False},
            {"status": "unknown", "executable": True},
            {"status": "invalid", "executable": True},
            {"intent": None},
            {"command": "get diamond 2"},
        ]

        for override in malformed_cases:
            with self.subTest(override=override):
                translation = _validated_get_translation()
                translation.update(override)
                extension = _RecordingExtension(translation=translation)
                router = MinecraftChatClefInputRouter(
                    extension=extension,
                    log_callback=lambda _message: None,
                )

                decision = router.route("다이아몬드 가져와줘")

                self.assertTrue(decision.handled)
                self.assertEqual("minecraft_translation_malformed", decision.reason)
                self.assertEqual(["다이아몬드 가져와줘"], extension.translated)
                self.assertEqual([], extension.submitted)

    def test_player_name_policy_blocks_dsl_separators(self):
        blocked_names = [
            "",
            "Steve Jobs",
            "Steve\t",
            "Steve\n",
            "@Steve",
            "Steve#comment",
            "Steve;stop",
            "Steve,Alex",
            "[Steve]",
            '"Steve"',
            "'Steve'",
            r"Steve\Alex",
        ]

        for player_name in blocked_names:
            with self.subTest(player_name=player_name):
                self.assertTrue(_has_blocked_player_name_syntax(player_name))

        self.assertFalse(_has_blocked_player_name_syntax("Steve"))
        self.assertFalse(_has_blocked_player_name_syntax("Player_123"))


def _has_blocked_player_name_syntax(player_name: object) -> bool:
    text = str(player_name or "")
    if not text:
        return True
    return any(character in text for character in " \t\r\n@#;,[]\"'\\")


def _validated_get_translation() -> dict[str, object]:
    return {
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
    }


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
        self.translated: list[str] = []
        self.submitted: list[dict[str, object]] = []

    def translate_natural_language_command(self, text):
        self.translated.append(str(text))
        return dict(self.translation)

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


if __name__ == "__main__":
    unittest.main()
