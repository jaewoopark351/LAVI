#20260905_kpopmodder: Lock the single-pass typed gate boundary for H5 and generic Minecraft input.
from __future__ import annotations

import unittest
from unittest import mock

from plugins.Minecraft.fabric.chatclef.input.gating import (
    MinecraftChatClefInputIntentGate,
    MinecraftChatClefInputRouteKind,
)


class MinecraftChatClefTypedGateDecisionContractTests(unittest.TestCase):
    def test_exact_natural_and_guarded_h5_candidates_use_the_h5_route(self):
        gate = MinecraftChatClefInputIntentGate()

        for text in (
            "@auto_deposit_trust area 16x16",
            "현재 위치 기준 반경 16 곱하기 16 상자를 등록해",
            "자동보관등록 영역 16x16; stop",
        ):
            with self.subTest(text=text):
                decision = gate.inspect(text)
                self.assertTrue(decision.consider)
                self.assertIs(
                    MinecraftChatClefInputRouteKind.H5_AUTO_DEPOSIT_TRUST,
                    decision.route_kind,
                )

    def test_generic_minecraft_and_unrelated_input_remain_distinct(self):
        gate = MinecraftChatClefInputIntentGate()

        generic = gate.inspect("다이아몬드 3개 캐줘")
        unrelated = gate.inspect("오늘 저녁 뭐 먹을까?")

        self.assertTrue(generic.consider)
        self.assertIs(MinecraftChatClefInputRouteKind.GENERIC, generic.route_kind)
        self.assertFalse(unrelated.consider)
        self.assertIs(MinecraftChatClefInputRouteKind.NONE, unrelated.route_kind)

    def test_candidate_detector_is_evaluated_once_per_gate_inspection(self):
        detector = mock.Mock()
        detector.is_candidate.return_value = False
        gate = MinecraftChatClefInputIntentGate(auto_deposit_trust=detector)

        decision = gate.inspect("금괴 하나 구해")

        self.assertIs(MinecraftChatClefInputRouteKind.GENERIC, decision.route_kind)
        detector.is_candidate.assert_called_once_with("금괴 하나 구해")


if __name__ == "__main__":
    unittest.main()
