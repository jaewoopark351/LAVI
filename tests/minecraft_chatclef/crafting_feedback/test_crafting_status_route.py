#20260907_kpopmodder: Verify STOP-first and zero-command crafting status routing.
from __future__ import annotations

import unittest
from types import SimpleNamespace

from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_route_decision import (
    MinecraftChatClefInputRouteDecision,
)
from plugins.Minecraft.fabric.chatclef.input.routing.orchestration.minecraft_input_route_sequence import (
    MinecraftInputRouteSequence,
)
from plugins.Minecraft.fabric.chatclef.input.status.crafting import (
    CraftingStatusQueryClassifier,
    CraftingStatusRouteOwner,
)
from plugins.Minecraft.fabric.chatclef.response.crafting_lifecycle import (
    CraftingLifecycleResponseRenderer,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.crafting import (
    CraftingFeedbackStatusSnapshot,
)


class CraftingStatusRouteTests(unittest.TestCase):
    def test_status_question_reads_snapshot_and_submits_zero_commands(self):
        extension = _StatusExtension(CraftingFeedbackStatusSnapshot.RUNNING)
        owner = CraftingStatusRouteOwner(
            extension=extension,
            live_proof_validator=lambda proof, event: (
                proof is _PROOF and event is _EVENT
            ),
            classifier=CraftingStatusQueryClassifier(),
            response_renderer=CraftingLifecycleResponseRenderer(),
        )

        decision = owner.try_route(_EVENT, _PROOF)

        self.assertEqual("다이아 곡괭이 만드는 중이야", decision.response_text)
        self.assertEqual(1, extension.inspections)
        self.assertEqual(0, extension.submissions)
        self.assertTrue(decision.result["read_only"])
        self.assertFalse(decision.result["command_submitted"])

    def test_uncertain_or_absent_state_uses_cautious_response(self):
        extension = _StatusExtension(CraftingFeedbackStatusSnapshot.UNAVAILABLE)
        owner = CraftingStatusRouteOwner(
            extension=extension,
            live_proof_validator=lambda _proof, _event: True,
            classifier=CraftingStatusQueryClassifier(),
            response_renderer=CraftingLifecycleResponseRenderer(),
        )

        decision = owner.try_route(_EVENT, object())

        self.assertEqual("지금 제작 상태를 확인하지 못했어", decision.response_text)
        self.assertEqual(0, extension.submissions)

    def test_stop_precedes_status_and_status_precedes_later_routes(self):
        stop = _RouteOwner(
            MinecraftChatClefInputRouteDecision.handled_result(
                reason="stop",
                response_text="stop",
            )
        )
        status = _RouteOwner(
            MinecraftChatClefInputRouteDecision.handled_result(
                reason="status",
                response_text="status",
            )
        )
        later = _FailingRouteOwner()
        sequence = _sequence(stop=stop, status=status, crafting=later)

        stop_decision = sequence.route(
            _EVENT,
            korean_eligibility_proof=_PROOF,
            optional_route_callback=_optional,
            gate_inspection_callback=_fail_gate,
        )
        status_calls_after_stop = status.calls
        stop.decision = None
        status_decision = sequence.route(
            _EVENT,
            korean_eligibility_proof=_PROOF,
            optional_route_callback=_optional,
            gate_inspection_callback=_fail_gate,
        )

        self.assertEqual("stop", stop_decision.response_text)
        self.assertEqual(0, status_calls_after_stop)
        self.assertEqual("status", status_decision.response_text)
        self.assertEqual(1, status.calls)


def _sequence(*, stop, status, crafting):
    return MinecraftInputRouteSequence(
        input_event_normalizer=_Normalizer(),
        stop_control_route_owner=stop,
        crafting_status_route_owner=status,
        generic_crafting_defaults_route_owner=crafting,
        auto_deposit_trust_route_coordinator=_FailingCoordinator(),
        ordinary_command_route_coordinator=_FailingCoordinator(),
    )


def _optional(owner, event, proof, _reason):
    return owner.try_route(event, proof)


def _fail_gate(_text):
    raise AssertionError("status must run before the general input gate")


class _Normalizer:
    def normalize(self, value):
        return value


class _RouteOwner:
    def __init__(self, decision):
        self.decision = decision
        self.calls = 0

    def try_route(self, _event, _proof):
        self.calls += 1
        return self.decision


class _FailingRouteOwner:
    def try_route(self, _event, _proof):
        raise AssertionError("later route must not run")


class _FailingCoordinator:
    def route(self, *_args, **_kwargs):
        raise AssertionError("later coordinator must not run")


class _StatusExtension:
    def __init__(self, state):
        self.state = state
        self.inspections = 0
        self.submissions = 0

    def inspect_crafting_feedback_status(self, _target_item):
        self.inspections += 1
        return CraftingFeedbackStatusSnapshot(
            state=self.state,
            target_item="diamond_pickaxe",
            requested_count=1,
            result_reason="dispatch_started",
        )

    def submit_translated_command(self, *_args, **_kwargs):
        self.submissions += 1
        raise AssertionError("status query must not submit")


_PROOF = object()
_EVENT = SimpleNamespace(
    text="마크 지금 뭐 만들고 있어?",
    source="lavi_chat_ui",
    provider_id="lavi_chat_ui",
    event_kind="chat_submit",
    final=True,
    event_id="1" * 32,
)


if __name__ == "__main__":
    unittest.main()
