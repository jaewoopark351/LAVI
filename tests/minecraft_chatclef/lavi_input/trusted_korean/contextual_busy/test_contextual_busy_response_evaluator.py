#20260909_kpopmodder: Verify contextual busy eligibility is exact and source-bound.
from __future__ import annotations

import unittest
from dataclasses import replace

from input_core.input_event.contracts import LaviInputEvent
from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_route_decision import (
    MinecraftChatClefInputRouteDecision,
)
from plugins.Minecraft.fabric.chatclef.input.routing.trusted_korean.response.contextual_busy import (
    ContextualBusyResponseEvaluator,
)


class ContextualBusyResponseEvaluatorTests(unittest.TestCase):
    def setUp(self) -> None:
        self.evaluator = ContextualBusyResponseEvaluator()

    def test_accepts_exact_typed_busy_for_each_trusted_route_and_source(self):
        for route_kind in (
            "minecraft_command",
            "generic_crafting_defaults",
            "minecraft_chatclef",
        ):
            for event in (_chat_event(), _voice_event()):
                with self.subTest(route_kind=route_kind, source=event.source):
                    evaluation = self.evaluator.evaluate(
                        _busy_decision(route_kind=route_kind),
                        event=event,
                        proof_is_live=True,
                    )

                    self.assertTrue(evaluation.candidate)
                    self.assertTrue(evaluation.verified)

    def test_rejects_every_near_miss_without_raising(self):
        valid = _busy_decision()
        fixtures = (
            replace(valid, handled=False),
            replace(valid, reason="minecraft_command_rejected"),
            replace(valid, route_kind="command_status_query"),
            replace(valid, result={"ok": True, "error": "active_command"}),
            replace(valid, result={"ok": False, "error": "not_connected"}),
        )
        for decision in fixtures:
            with self.subTest(reason=decision.reason, route=decision.route_kind):
                evaluation = self.evaluator.evaluate(
                    decision,
                    event=_chat_event(),
                    proof_is_live=True,
                )
                self.assertFalse(evaluation.verified)

        for event in (
            replace(_chat_event(), final=False),
            replace(_chat_event(), source="butler_whisper"),
            replace(_chat_event(), provider_id="VoiceInput"),
            object(),
        ):
            with self.subTest(event_type=type(event).__name__):
                evaluation = self.evaluator.evaluate(
                    valid,
                    event=event,
                    proof_is_live=True,
                )
                self.assertTrue(evaluation.candidate)
                self.assertFalse(evaluation.verified)

        self.assertFalse(
            self.evaluator.evaluate(
                valid,
                event=_chat_event(),
                proof_is_live=False,
            ).verified
        )
        self.assertFalse(
            self.evaluator.evaluate(
                valid,
                event=_chat_event(),
                proof_is_live=1,
            ).verified
        )

    def test_non_route_decisions_and_hostile_result_mappings_fail_closed(self):
        evaluation = self.evaluator.evaluate(
            object(),
            event=_chat_event(),
            proof_is_live=True,
        )
        self.assertFalse(evaluation.candidate)
        self.assertFalse(evaluation.verified)

        decision = replace(_busy_decision(), result=_ThrowingMapping())
        evaluation = self.evaluator.evaluate(
            decision,
            event=_chat_event(),
            proof_is_live=True,
        )
        self.assertTrue(evaluation.candidate)
        self.assertFalse(evaluation.verified)


class _ThrowingMapping(dict):
    def get(self, _key, _default=None):
        raise RuntimeError("must stay private")


def _busy_decision(*, route_kind: str = "minecraft_command"):
    return MinecraftChatClefInputRouteDecision.handled_result(
        reason="minecraft_command_busy",
        response_text="generic busy",
        result={
            "ok": False,
            "error": "active_command",
            "status": {"details": {"commands": {}}},
        },
        route_kind=route_kind,
    )


def _chat_event() -> LaviInputEvent:
    return LaviInputEvent(
        text="호박 파이 만들어줘",
        source="lavi_chat_ui",
        provider_id="lavi_chat_ui",
        event_kind="chat_submit",
        final=True,
        event_id="1" * 32,
        fallback_payload="호박 파이 만들어줘",
    )


def _voice_event() -> LaviInputEvent:
    return LaviInputEvent(
        text="호박 파이 만들어줘",
        source="voice_input_final",
        provider_id="VoiceInput",
        event_kind="final_transcript",
        final=True,
        event_id="2" * 32,
        fallback_payload="호박 파이 만들어줘",
    )


if __name__ == "__main__":
    unittest.main()
