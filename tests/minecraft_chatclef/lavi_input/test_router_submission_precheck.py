#20260818_kpopmodder: Lock fail-closed router status checks and the single-pass submit boundary.
#20260819_kpopmodder: Prove contradictory result mirrors remain unknown without resubmission.
from __future__ import annotations

import unittest

from plugins.Minecraft.fabric.chatclef.input import MinecraftChatClefInputRouter


class RouterSubmissionPrecheckTests(unittest.TestCase):
    def test_unambiguous_direct_and_nested_bridge_shapes_still_submit(self):
        cases = {
            "direct": _direct_bridge_status(),
            "nested": _bridge_status(),
        }

        for name, status_behavior in cases.items():
            with self.subTest(name=name):
                extension = _StatusExtension(status_behavior=status_behavior)
                router = MinecraftChatClefInputRouter(
                    extension=extension,
                    log_callback=lambda _message: None,
                )

                router.route("다이아몬드 캐줘")

                self.assertEqual(1, len(extension.submitted))

    def test_conflicting_direct_and_nested_bridge_objects_never_submit(self):
        cases = {
            "backend_conflict": _conflicting_bridge_status(
                nested_backend_id="forge_minemind",
            ),
            "connection_conflict": _conflicting_bridge_status(
                nested_connected=False,
                nested_lifecycle_state="disconnected",
            ),
        }

        for name, status_behavior in cases.items():
            with self.subTest(name=name):
                extension = _StatusExtension(status_behavior=status_behavior)
                router = MinecraftChatClefInputRouter(
                    extension=extension,
                    log_callback=lambda _message: None,
                )

                decision = router.route("다이아몬드 캐줘")

                self.assertTrue(decision.handled)
                self.assertEqual(
                    "minecraft_bridge_status_unavailable",
                    decision.reason,
                )
                self.assertIn("ambiguous bridge objects", decision.response_text)
                self.assertEqual([], extension.submitted)

    def test_unreadable_or_incomplete_status_never_submits(self):
        cases = {
            "missing_status_reader": _NO_STATUS_READER,
            "status_reader_error": RuntimeError("status failed"),
            "non_mapping_status": [],
            "missing_bridge_details": {},
            "wrong_backend": _bridge_status(backend_id="forge_minemind"),
            "disabled": _bridge_status(enabled=False),
            "string_enabled": _bridge_status(enabled="true"),
            "missing_connected": _bridge_status(include_connected=False),
            "string_connected": _bridge_status(connected="true"),
            "missing_lifecycle": _bridge_status(include_lifecycle=False),
            "lifecycle_not_connected": _bridge_status(
                lifecycle_state="disconnected"
            ),
            "missing_command_status": _bridge_status(commands=None),
            "missing_active_request_id": _bridge_status(commands={}),
            "invalid_active_request_id": _bridge_status(
                commands={"active_request_id": 1}
            ),
            "blank_active_request_id": _bridge_status(
                commands={"active_request_id": ""}
            ),
            "whitespace_active_request_id": _bridge_status(
                commands={"active_request_id": "   "}
            ),
            "boolean_active_request_id": _bridge_status(
                commands={"active_request_id": False}
            ),
        }

        for name, status_behavior in cases.items():
            with self.subTest(name=name):
                extension = _StatusExtension(status_behavior=status_behavior)
                router = MinecraftChatClefInputRouter(
                    extension=extension,
                    log_callback=lambda _message: None,
                )

                decision = router.route("다이아몬드 캐줘")

                self.assertTrue(decision.handled)
                self.assertIn(
                    decision.reason,
                    {
                        "minecraft_bridge_status_unavailable",
                        "minecraft_bridge_disabled",
                        "minecraft_bridge_disconnected",
                    },
                )
                self.assertEqual([], extension.submitted)

    def test_legacy_retranslating_handler_is_not_used_as_submit_fallback(self):
        extension = _LegacyHandlerOnlyExtension()
        router = MinecraftChatClefInputRouter(
            extension=extension,
            log_callback=lambda _message: None,
        )

        decision = router.route("다이아몬드 캐줘")

        self.assertFalse(decision.handled)
        self.assertEqual("handler_unavailable", decision.reason)
        self.assertEqual(0, extension.translate_calls)
        self.assertEqual(0, extension.handler_calls)

    def test_malformed_submit_result_is_unknown_not_routed(self):
        extension = _MalformedSubmitResultExtension(
            status_behavior=_bridge_status()
        )
        router = MinecraftChatClefInputRouter(
            extension=extension,
            log_callback=lambda _message: None,
        )

        decision = router.route("다이아몬드 캐줘")

        self.assertTrue(decision.handled)
        self.assertEqual("minecraft_submission_outcome_unknown", decision.reason)
        self.assertEqual("unknown", decision.result["status"]["status"])
        self.assertTrue(decision.result["details"]["reconciliation_required"])

    def test_contradictory_submit_result_is_unknown_and_submitted_once(self):
        extension = _ContradictorySubmitResultExtension(
            status_behavior=_bridge_status()
        )
        router = MinecraftChatClefInputRouter(
            extension=extension,
            log_callback=lambda _message: None,
        )

        decision = router.route("다이아몬드 캐줘")

        self.assertTrue(decision.handled)
        self.assertEqual("minecraft_submission_outcome_unknown", decision.reason)
        self.assertEqual("unknown", decision.result["status"]["status"])
        self.assertTrue(decision.result["details"]["reconciliation_required"])
        self.assertEqual(1, len(extension.submitted))


_NO_STATUS_READER = object()
_DEFAULT_COMMANDS = object()


class _StatusExtension:
    def __init__(self, *, status_behavior):
        self.status_behavior = status_behavior
        self.submitted = []

    def translate_natural_language_command(self, _text):
        return _validated_translation()

    def submit_translated_command(self, request, translation):
        self.submitted.append((dict(request), dict(translation)))
        return {"ok": True, "status": {"status": "accepted"}}

    def __getattribute__(self, name):
        if name == "get_status":
            behavior = object.__getattribute__(self, "status_behavior")
            if behavior is _NO_STATUS_READER:
                raise AttributeError(name)
        return object.__getattribute__(self, name)

    def get_status(self):
        if isinstance(self.status_behavior, Exception):
            raise self.status_behavior
        return self.status_behavior


class _LegacyHandlerOnlyExtension:
    def __init__(self):
        self.translate_calls = 0
        self.handler_calls = 0

    def translate_natural_language_command(self, _text):
        self.translate_calls += 1
        return _validated_translation()

    def handle_natural_language_command(self, _request):
        self.handler_calls += 1
        return {"ok": True, "status": {"status": "accepted"}}

    def get_status(self):
        return _bridge_status()


class _MalformedSubmitResultExtension(_StatusExtension):
    def submit_translated_command(self, request, translation):
        self.submitted.append((dict(request), dict(translation)))
        return None


class _ContradictorySubmitResultExtension(_StatusExtension):
    def submit_translated_command(self, request, translation):
        self.submitted.append((dict(request), dict(translation)))
        request_id = str(request["request_id"])
        return {
            "ok": True,
            "status": {
                "request_id": request_id,
                "ok": False,
                "status": "accepted",
                "error_code": "internal_error",
                "message": "contradictory result",
                "data": {},
            },
            "error": "internal_error",
            "message": "contradictory result",
            "details": {},
        }


def _validated_translation() -> dict[str, object]:
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
        "message": "translated",
        "data": {},
    }


def _bridge_status(
    *,
    backend_id: str = "fabric_chatclef",
    enabled: object = True,
    connected: object = True,
    lifecycle_state: str = "connected",
    commands: object = _DEFAULT_COMMANDS,
    include_connected: bool = True,
    include_lifecycle: bool = True,
) -> dict[str, object]:
    bridge: dict[str, object] = {
        "backend_id": backend_id,
        "enabled": enabled,
        "details": {
            "commands": (
                {"active_request_id": None}
                if commands is _DEFAULT_COMMANDS
                else commands
            )
        },
    }
    if include_connected:
        bridge["connected"] = connected
    if include_lifecycle:
        bridge["lifecycle_state"] = lifecycle_state
    return {"details": bridge}


def _conflicting_bridge_status(
    *,
    nested_backend_id: str = "fabric_chatclef",
    nested_connected: bool = True,
    nested_lifecycle_state: str = "connected",
) -> dict[str, object]:
    nested_bridge = _bridge_status(
        backend_id=nested_backend_id,
        connected=nested_connected,
        lifecycle_state=nested_lifecycle_state,
    )["details"]
    assert isinstance(nested_bridge, dict)
    nested_bridge["commands"] = {"active_request_id": None}
    return {
        "backend_id": "fabric_chatclef",
        "enabled": True,
        "connected": True,
        "lifecycle_state": "connected",
        "details": nested_bridge,
    }


def _direct_bridge_status() -> dict[str, object]:
    bridge = _bridge_status()["details"]
    assert isinstance(bridge, dict)
    return bridge


if __name__ == "__main__":
    unittest.main()
