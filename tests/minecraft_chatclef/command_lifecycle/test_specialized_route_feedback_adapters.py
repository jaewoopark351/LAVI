#20260907_kpopmodder: Prove Feature-B and H5 accepted submissions enter common feedback.
from __future__ import annotations

import unittest
from types import SimpleNamespace

from plugins.Minecraft.fabric.chatclef.input.aliases.generic_crafting_defaults.routing.generic_crafting_submission_stage import (
    GenericCraftingSubmissionStage,
)
from plugins.Minecraft.fabric.chatclef.input.auto_deposit_trust.routing.auto_deposit_trust_route_sequence import (
    AutoDepositTrustRouteSequence,
)
from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_route_decision import (
    MinecraftChatClefInputRouteDecision,
)
from plugins.Minecraft.fabric.chatclef.response.command_lifecycle import (
    CommandFeedbackStartDecisionDecorator,
    CommandLifecycleResponseRenderer,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle import (
    CommandFeedbackDescriptorFactory,
)


class SpecializedRouteFeedbackAdapterTests(unittest.TestCase):
    def test_generic_crafting_reserves_before_submit_and_claims_natural_start(self):
        calls = []
        feedback = _Feedback(calls)
        acknowledgement = feedback.acknowledgement
        translation = _get_translation()
        stage = GenericCraftingSubmissionStage(
            extension=object(),
            submission_precheck=object(),
            submission_boundary=object(),
            submission_reconciliation=object(),
            decision_factory=object(),
            readiness_stage=SimpleNamespace(rejection_if_unready=lambda: None),
            delivery=SimpleNamespace(
                deliver=lambda **_values: calls.append("submit") or _accepted_result()
            ),
            reconciliation_observer=SimpleNamespace(
                observe=lambda _result: calls.append("reconcile")
            ),
            result_projector=SimpleNamespace(
                project=lambda _translation, _result: _technical_decision()
            ),
            command_feedback=feedback,
            descriptor_factory=CommandFeedbackDescriptorFactory(),
            start_decision_decorator=CommandFeedbackStartDecisionDecorator(
                CommandLifecycleResponseRenderer()
            ),
        )

        decision = stage.submit(
            event=_event("철 곡괭이 만들어줘"),
            translation=translation,
            activation_receipt=object(),
            korean_eligibility_proof=object(),
        )

        self.assertEqual(
            ["reserve", "submit", "reconcile", "claim", "abandon"],
            calls,
        )
        self.assertEqual("철 곡괭이 만들어 줄게", decision.response_text)
        self.assertEqual("command_lifecycle", decision.route_kind)
        self.assertEqual("command_start", decision.response_kind)
        self.assertIs(
            acknowledgement,
            decision.response_publication_acknowledgement,
        )

    def test_h5_claimed_route_uses_same_feedback_without_changing_admission(self):
        calls = []
        feedback = _Feedback(calls)
        translation = _h5_translation()
        sequence = AutoDepositTrustRouteSequence(
            extension=object(),
            input_admission=object(),
            raw_input_safety=object(),
            exact_input_adapter=SimpleNamespace(
                adapt=lambda _text: SimpleNamespace(
                    original_text="자동보관등록 영역 16x16",
                    translation_input_text="auto_deposit_trust area 16x16",
                )
            ),
            translation_admission=SimpleNamespace(
                inspect=lambda _translation: SimpleNamespace(allowed=True)
            ),
            translation_boundary=SimpleNamespace(
                translate_once=lambda _extension, _text: translation,
                validate=lambda value: value,
                status=lambda _value: "validated",
            ),
            submission_precheck=SimpleNamespace(
                inspect=lambda _extension: SimpleNamespace(ready=True)
            ),
            submission_boundary=SimpleNamespace(
                submit_once=lambda *_args, **_kwargs: calls.append("submit")
                or _accepted_result()
            ),
            submission_reconciliation=SimpleNamespace(
                blocking_result=lambda: None,
                observe_submission_result=lambda _result: calls.append("reconcile"),
            ),
            decision_factory=SimpleNamespace(
                submitted=lambda _translation, _result: _technical_decision()
            ),
            command_feedback=feedback,
            descriptor_factory=CommandFeedbackDescriptorFactory(),
            start_decision_decorator=CommandFeedbackStartDecisionDecorator(
                CommandLifecycleResponseRenderer()
            ),
        )

        decision = sequence.route_claimed(
            _event("자동보관등록 영역 16x16"),
            receipt=object(),
        )

        self.assertEqual(
            ["reserve", "submit", "reconcile", "claim", "abandon"],
            calls,
        )
        self.assertEqual(
            "주변 16×16 범위에 있는 보관함을 자동 보관 대상으로 등록할게",
            decision.response_text,
        )
        self.assertEqual("command_lifecycle", decision.route_kind)
        self.assertEqual("command_start", decision.response_kind)


class _Feedback:
    def __init__(self, calls) -> None:
        self.calls = calls
        self.acknowledgement = object()

    def prepare_descriptor(self, descriptor):
        self.calls.append("reserve")
        return SimpleNamespace(descriptor=descriptor)

    def claim_start(self, _grant, _result):
        self.calls.append("claim")
        return self.acknowledgement

    def abandon(self, _grant):
        self.calls.append("abandon")


def _event(text: str):
    return SimpleNamespace(
        text=text,
        source="lavi_chat_ui",
        provider_id="lavi_chat_ui",
        event_kind="chat_submit",
        final=True,
        event_id="d" * 32,
    )


def _get_translation():
    return {
        "status": "validated",
        "executable": True,
        "command": "get iron_pickaxe 1",
        "resolved_target": "iron_pickaxe",
        "intent": {
            "intent_type": "get_item",
            "quantity": 1,
            "item_phrase": "철 곡괭이",
            "language": "ko",
            "original_text": "철 곡괭이 만들어줘",
        },
    }


def _h5_translation():
    return {
        "status": "validated",
        "executable": True,
        "command": "auto_deposit_trust area 16x16",
        "resolved_target": None,
        "intent": {
            "intent_type": "auto_deposit_trust_area",
            "language": "ko",
            "original_text": "자동보관등록 영역 16x16",
        },
    }


def _accepted_result():
    return {"ok": True, "status": {"ok": True, "status": "accepted"}}


def _technical_decision():
    return MinecraftChatClefInputRouteDecision.handled_result(
        reason="minecraft_command_submitted",
        response_text="[Minecraft] 명령을 제출했어요.",
    )


if __name__ == "__main__":
    unittest.main()
