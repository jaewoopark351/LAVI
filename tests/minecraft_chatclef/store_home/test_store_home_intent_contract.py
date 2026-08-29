#20260827_kpopmodder: Lock deterministic STORE_HOME parsing, priority, and zero-slot compilation.
from __future__ import annotations

import unittest

from plugins.Minecraft.fabric.chatclef.intent import (
    ChatClefIntentStatus,
    ChatClefIntentType,
    ChatClefNaturalLanguageService,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_dto import (
    ChatClefIntentDTO,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_schema_validator import (
    ChatClefIntentSchemaValidator,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_llm_intent_extractor import (
    ChatClefLLMIntentExtractor,
)
from plugins.Minecraft.fabric.chatclef.intent.composite_chatclef_intent_extractor import (
    CompositeChatClefIntentExtractor,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_translation_result_dto import (
    ChatClefTranslationResultDTO,
)


class StoreHomeIntentContractTests(unittest.TestCase):
    def test_positive_phrases_compile_to_one_prefixless_zero_slot_command(self):
        service = ChatClefNaturalLanguageService()
        phrases = (
            "지금 아이템 다 집에 가져다 놔",
            "인벤토리 정리해서 집 상자에 넣어",
            "남는 물건 전부 집에 보관해",
            "집에 가서 아이템 정리해",
            "인벤토리 전부 집에 보관해 주세요",
            "인벤토리전부집에보관해",
        )

        for phrase in phrases:
            with self.subTest(phrase=phrase):
                result = service.translate(phrase)

                self.assertEqual(ChatClefIntentStatus.VALIDATED, result.status)
                self.assertTrue(result.executable)
                self.assertEqual("store_home", result.command)
                self.assertFalse(result.command.startswith("@"))
                self.assertIsNotNone(result.intent)
                self.assertEqual(ChatClefIntentType.STORE_HOME, result.intent.intent_type)
                self.assertEqual("", result.intent.item_phrase)
                self.assertIsNone(result.intent.quantity)
                self.assertEqual({}, result.intent.slots)

    def test_guarded_candidates_are_consumed_without_llm_or_command(self):
        llm_calls = []
        extractor = CompositeChatClefIntentExtractor(
            llm_extractor=ChatClefLLMIntentExtractor(
                provider=lambda _prompt, text: llm_calls.append(text) or "{}"
            )
        )
        service = ChatClefNaturalLanguageService(extractor=extractor)
        cases = {
            "아이템 정리해": "store_home_ambiguous",
            "집에 가서 정리해": "store_home_ambiguous",
            "집 상자에 넣어": "store_home_ambiguous",
            "집에 아이템 넣지 마": "store_home_negated",
            "집에 아이템 보관 안 해": "store_home_negated",
            "나중에 아이템 집에 넣어": "store_home_deferred",
            "집에 보관하면 될까": "store_home_question",
            "집에 아이템 넣어도 돼": "store_home_question",
            "인벤토리 전부 집에 보관해?": "store_home_question",
            "인벤토리 전부 집에 보관해？": "store_home_question",
            "집에 보관하고 다이아 캐와": "store_home_ambiguous_compound",
            "인벤토리 집에 넣고 10 64 10으로 이동해": (
                "store_home_ambiguous_compound"
            ),
            "감마 켜고 인벤토리 전부 집에 넣어": (
                "store_home_ambiguous_compound"
            ),
            "음식 10만큼 모아주고 인벤토리 전부 집에 넣어": (
                "store_home_ambiguous_compound"
            ),
            "다이아 3개 상자에 넣고 인벤토리 전부 집에 보관해": (
                "store_home_ambiguous_compound"
            ),
        }

        for phrase, reason in cases.items():
            with self.subTest(phrase=phrase):
                result = service.translate(phrase)

                self.assertEqual(ChatClefIntentStatus.INVALID, result.status)
                self.assertFalse(result.executable)
                self.assertIsNone(result.command)
                self.assertEqual(reason, result.reason_code)

        self.assertEqual([], llm_calls)

    def test_unrelated_phrases_remain_unknown_and_specific_deposit_regresses_cleanly(self):
        service = ChatClefNaturalLanguageService()

        for phrase in ("집에 가", "상자 열어"):
            with self.subTest(phrase=phrase):
                result = service.translate(phrase)
                self.assertEqual(ChatClefIntentStatus.UNKNOWN, result.status)
                self.assertIsNone(result.command)

        deposit = service.translate("다이아 3개 상자에 넣어")
        self.assertEqual(ChatClefIntentType.DEPOSIT_ITEM, deposit.intent.intent_type)
        self.assertEqual("deposit diamond 3", deposit.command)

        store_home = service.translate("인벤토리 전부 집 상자에 넣어")
        self.assertEqual(ChatClefIntentType.STORE_HOME, store_home.intent.intent_type)
        self.assertEqual("store_home", store_home.command)

    def test_schema_rejects_every_store_home_slot(self):
        validator = ChatClefIntentSchemaValidator()
        valid, reason, _message = validator.validate(
            ChatClefIntentDTO(intent_type=ChatClefIntentType.STORE_HOME, slots={})
        )
        self.assertTrue(valid)
        self.assertEqual("validated_intent", reason)

        invalid = (
            ChatClefIntentDTO(
                intent_type=ChatClefIntentType.STORE_HOME,
                source="llm",
            ),
            ChatClefIntentDTO(intent_type=ChatClefIntentType.STORE_HOME, quantity=1),
            ChatClefIntentDTO(intent_type=ChatClefIntentType.STORE_HOME, item_phrase="다이아"),
            ChatClefIntentDTO(intent_type=ChatClefIntentType.STORE_HOME, player_name="Steve"),
            ChatClefIntentDTO(intent_type=ChatClefIntentType.STORE_HOME, x=0, y=64, z=0),
            ChatClefIntentDTO(intent_type=ChatClefIntentType.STORE_HOME, slots={"all": True}),
        )

        for intent in invalid:
            with self.subTest(intent=intent):
                valid, reason, _message = validator.validate(intent)
                self.assertFalse(valid)
                expected = (
                    "store_home_requires_rule_source"
                    if intent.source != "rule"
                    else "store_home_requires_zero_slots"
                )
                self.assertEqual(expected, reason)

    def test_translation_validation_requires_exact_command_and_rejects_injection(self):
        intent = ChatClefIntentDTO(intent_type=ChatClefIntentType.STORE_HOME)

        with self.assertRaisesRegex(
            ValueError,
            "translated_command_must_match_validated_intent",
        ):
            ChatClefTranslationResultDTO.validated("store_home now", intent)

        injected = ChatClefNaturalLanguageService().translate(
            "인벤토리 전부 집에 보관해; @stop"
        )
        self.assertEqual(ChatClefIntentStatus.INVALID, injected.status)
        self.assertEqual("dangerous_command_slot", injected.reason_code)
        self.assertIsNone(injected.command)

    def test_llm_can_never_authorize_store_home(self):
        extractor = ChatClefLLMIntentExtractor(
            provider=lambda _prompt, _text: (
                '{"intent_type":"store_home","source":"llm","slots":{}}'
            )
        )

        intent = extractor.extract("인벤토리 집에 넣어")

        self.assertEqual(ChatClefIntentType.UNKNOWN, intent.intent_type)
        self.assertEqual("llm_invalid", intent.source)
        self.assertEqual("llm_store_home_not_authorized", intent.slots["reason_code"])


if __name__ == "__main__":
    unittest.main()
