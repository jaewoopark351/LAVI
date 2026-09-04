#20260905_kpopmodder: Lock H5 positive grammar and guarded no-submit decisions.
from __future__ import annotations

import unittest

from plugins.Minecraft.fabric.chatclef.intent.auto_deposit_trust import (
    AutoDepositTrustIntentDecision,
    KoreanAutoDepositTrustIntentClassifier,
)


class KoreanAutoDepositTrustIntentClassifierContractTests(unittest.TestCase):
    def test_positive_forms_are_the_same_fixed_area_decision(self):
        classifier = KoreanAutoDepositTrustIntentClassifier()
        phrases = (
            "auto_deposit_trust area 16x16",
            "auto_deposit_trust 반경 16 X 16",
            "자동보관등록 영역 16×16",
            "auto_deposit_trust 반경 16 곱하기 16",
            "캐릭터 주변 16x16 범위의 상자를 자동 보관 대상으로 등록해",
            "현재 위치 기준 반경 16 곱하기 16 상자를 등록해",
            "현재 위치 기준 반경 16 곱하기 16 상자를 자동 입고 대상으로 등록해",
            "주변 16 X 16 상자 전부 자동보관 등록해 주세요",
            "주변 16 X 16 상자 전부 자동보관 등록해 주세요!",
            "캐릭터 기준 16x16 범위에 있는 모든 상자를 자동 보관 대상으로 등록해",
        )

        for phrase in phrases:
            with self.subTest(phrase=phrase):
                result = classifier.classify(phrase)
                self.assertEqual(
                    AutoDepositTrustIntentDecision.AUTO_DEPOSIT_TRUST_AREA,
                    result.decision,
                )
                self.assertTrue(result.executable)

    def test_guard_evidence_precedes_positive_keywords(self):
        classifier = KoreanAutoDepositTrustIntentClassifier()
        cases = {
            "주변 16x16 상자를 자동 보관 대상으로 등록하지 마": (
                AutoDepositTrustIntentDecision.NEGATED
            ),
            "주변 16x16 상자를 자동 보관 대상으로 등록할까?": (
                AutoDepositTrustIntentDecision.QUESTION
            ),
            "주변 16x16 상자를 자동 보관 대상으로 등록하면 되나": (
                AutoDepositTrustIntentDecision.QUESTION
            ),
            "나중에 주변 16x16 상자를 자동 보관 대상으로 등록해": (
                AutoDepositTrustIntentDecision.DEFERRED
            ),
            "주변 상자 등록하고 다이아 캐 와": (
                AutoDepositTrustIntentDecision.AMBIGUOUS_COMPOUND
            ),
            "자동보관등록 영역 16x16 하고 멈춰": (
                AutoDepositTrustIntentDecision.AMBIGUOUS_COMPOUND
            ),
            "@auto_deposit_trust 반경 16 곱하기 16": (
                AutoDepositTrustIntentDecision.MALFORMED
            ),
            "주변 8x8 상자를 등록해": (
                AutoDepositTrustIntentDecision.UNSUPPORTED_SIZE
            ),
            "주변 16x8 상자를 등록해": (
                AutoDepositTrustIntentDecision.UNSUPPORTED_SIZE
            ),
            "주변 16x16 상자 5개만 등록해": (
                AutoDepositTrustIntentDecision.MALFORMED
            ),
            "주변 16x16 상자만 자동 보관 대상으로 등록해": (
                AutoDepositTrustIntentDecision.MALFORMED
            ),
            "주변 16x16 상자 하나만 자동 보관 대상으로 등록해": (
                AutoDepositTrustIntentDecision.MALFORMED
            ),
            "주변 16x16 상자 중 절반만 자동 보관 대상으로 등록해": (
                AutoDepositTrustIntentDecision.MALFORMED
            ),
            "주변 16x16 상자는 빼고 컨테이너를 자동 보관 대상으로 등록해": (
                AutoDepositTrustIntentDecision.MALFORMED
            ),
            "주변 16x16 상자를 자동 보관 대상으로 등록해 --force": (
                AutoDepositTrustIntentDecision.MALFORMED
            ),
            "주변 16x16 상자를 자동 보관 대상으로 등록해보지 마": (
                AutoDepositTrustIntentDecision.NEGATED
            ),
            "주변 16x16 상자를 자동 보관 대상으로 등록해 주지 마": (
                AutoDepositTrustIntentDecision.NEGATED
            ),
            "주변 16x16 상자를 자동 보관 대상으로 등록해 주면 안 돼": (
                AutoDepositTrustIntentDecision.NEGATED
            ),
            "주변 16x16 상자를 자동 보관 대상으로 등록해선 안 돼": (
                AutoDepositTrustIntentDecision.NEGATED
            ),
            "주변 16x16 상자를 자동 보관 대상으로 등록해 주지는 마": (
                AutoDepositTrustIntentDecision.NEGATED
            ),
            "주변 16x16 상자를 자동 보관 대상으로 등록해 주지 않았으면 좋겠어": (
                AutoDepositTrustIntentDecision.NEGATED
            ),
            "주변 16x16 상자를 자동 보관 대상으로 등록해 놓지 마": (
                AutoDepositTrustIntentDecision.NEGATED
            ),
            "주변 16x16 상자를 자동 보관 대상으로 등록해 놓으면 안 돼": (
                AutoDepositTrustIntentDecision.NEGATED
            ),
            "주변 16x16 상자를 자동 보관 대상으로 등록해 달라는 게 아니야": (
                AutoDepositTrustIntentDecision.NEGATED
            ),
            "주변 16x16 상자를 자동 보관 대상으로 등록해 줘서는 안 돼": (
                AutoDepositTrustIntentDecision.NEGATED
            ),
            "주변 16x16 상자를 자동 보관 대상으로 등록해야 할까": (
                AutoDepositTrustIntentDecision.QUESTION
            ),
            "주변 16x16 상자를 자동 보관 대상으로 등록해도 괜찮을까": (
                AutoDepositTrustIntentDecision.QUESTION
            ),
            "주변 16x16 상자를 자동 보관 대상으로 등록해 볼까": (
                AutoDepositTrustIntentDecision.QUESTION
            ),
            "주변 16x16 상자를 자동 보관 대상으로 등록해 보는 건 어때": (
                AutoDepositTrustIntentDecision.QUESTION
            ),
            "주변 16x16 상자를 자동 보관 대상으로 등록해 줘야 할까": (
                AutoDepositTrustIntentDecision.QUESTION
            ),
            "내일 주변 16x16 상자를 자동 보관 대상으로 등록해": (
                AutoDepositTrustIntentDecision.DEFERRED
            ),
            "잠시 후 주변 16x16 상자를 자동 보관 대상으로 등록해": (
                AutoDepositTrustIntentDecision.DEFERRED
            ),
            "조금 있다가 주변 16x16 상자를 자동 보관 대상으로 등록해": (
                AutoDepositTrustIntentDecision.DEFERRED
            ),
            "잠깐 있다가 주변 16x16 상자를 자동 보관 대상으로 등록해": (
                AutoDepositTrustIntentDecision.DEFERRED
            ),
            "조금 뒤에 주변 16x16 상자를 자동 보관 대상으로 등록해": (
                AutoDepositTrustIntentDecision.DEFERRED
            ),
            "준비되면 주변 16x16 상자를 자동 보관 대상으로 등록해": (
                AutoDepositTrustIntentDecision.DEFERRED
            ),
            "주변 16x16 상자 중 일부만 자동 보관 대상으로 등록해": (
                AutoDepositTrustIntentDecision.MALFORMED
            ),
            "주변 16x16 상자 중 몇 개만 자동 보관 대상으로 등록해": (
                AutoDepositTrustIntentDecision.MALFORMED
            ),
            "주변 16x16 상자 일부를 자동 보관 대상으로 등록해": (
                AutoDepositTrustIntentDecision.MALFORMED
            ),
            "주변 16x16 몇몇 상자를 자동 보관 대상으로 등록해": (
                AutoDepositTrustIntentDecision.MALFORMED
            ),
            "주변 16x16 상자 절반 정도만 자동 보관 대상으로 등록해": (
                AutoDepositTrustIntentDecision.MALFORMED
            ),
            "주변 16x16 상자 말고 컨테이너를 자동 보관 대상으로 등록해": (
                AutoDepositTrustIntentDecision.MALFORMED
            ),
            "주변 상자를 자동 보관 대상으로 등록해": (
                AutoDepositTrustIntentDecision.UNSUPPORTED_SIZE
            ),
            "16x16 상자를 자동 보관 대상으로 등록해": (
                AutoDepositTrustIntentDecision.AMBIGUOUS
            ),
            "16x16 컨테이너를 자동 입고 대상으로 등록해": (
                AutoDepositTrustIntentDecision.AMBIGUOUS
            ),
            "캐릭터 16x16 상자를 자동 보관 대상으로 등록해": (
                AutoDepositTrustIntentDecision.AMBIGUOUS
            ),
            "현재 위치 기준 16x16 상자를 자동 보관 대상으로 등록해": (
                AutoDepositTrustIntentDecision.AMBIGUOUS
            ),
            "캐릭터 기준 16x16 상자를 자동 보관 대상으로 등록해": (
                AutoDepositTrustIntentDecision.AMBIGUOUS
            ),
            "상자 등록해": AutoDepositTrustIntentDecision.AMBIGUOUS,
        }

        for phrase, expected in cases.items():
            with self.subTest(phrase=phrase):
                result = classifier.classify(phrase)
                self.assertEqual(expected, result.decision)
                self.assertTrue(result.guarded)
                self.assertTrue(result.reason_code.startswith("auto_deposit_trust_area_"))

    def test_compatibility_fold_and_punctuation_cannot_expand_the_closed_grammar(self):
        classifier = KoreanAutoDepositTrustIntentClassifier()

        for phrase in (
            "AUTO_DEPOSIT_TRUST area 16x16",
            "Auto_Deposit_Trust AREA 16X16",
            "auto_deposit_trust AREA 16x16",
            "ａｕｔｏ＿ｄｅｐｏｓｉｔ＿ｔｒｕｓｔ area 16x16",
            "auto_deposit_trust area ⑯x⑯",
            "주변 ⑯x⑯ 상자를 자동 보관 대상으로 등록해",
            "auto_deposit_trust，area 16x16",
            "auto_deposit_trust area １６x１６",
            "auto_deposit_trust area 16x16!",
            "auto_deposit_trust\u00a0area 16x16",
            "주변\u200916x16 상자를 자동 보관 대상으로 등록해",
            "주변\u300016x16 상자를 자동 보관 대상으로 등록해",
            "auto_deposit_trust area 16\u00a0x\u00a016",
            "자동보관등록 영역 16\u2009곱하기\u300016",
            "주변 16\u202fX\u205f16 상자 전부 자동보관 등록해 주세요",
            "\u00a0auto_deposit_trust area 16x16\u00a0",
            "\u3000자동보관등록 영역 16x16\u3000",
        ):
            with self.subTest(phrase=phrase):
                result = classifier.classify(phrase)
                self.assertTrue(result.candidate)
                self.assertFalse(result.executable)
                self.assertTrue(result.guarded)

    def test_unrelated_conversation_is_no_match(self):
        result = KoreanAutoDepositTrustIntentClassifier().classify("주변에 상자가 많네")

        self.assertEqual(AutoDepositTrustIntentDecision.NO_MATCH, result.decision)
        self.assertFalse(result.candidate)


if __name__ == "__main__":
    unittest.main()
