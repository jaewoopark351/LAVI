#20260905_kpopmodder: Lock parser, schema, compiler, LLM denial, and guard consumption for H5.
from __future__ import annotations

import unittest

from plugins.Minecraft.fabric.chatclef.intent import (
    ChatClefIntentStatus,
    ChatClefIntentType,
    ChatClefNaturalLanguageService,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_command_compiler import (
    ChatClefCommandCompiler,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_dto import ChatClefIntentDTO
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_schema_validator import (
    ChatClefIntentSchemaValidator,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_llm_intent_extractor import (
    ChatClefLLMIntentExtractor,
)
from plugins.Minecraft.fabric.chatclef.intent.composite_chatclef_intent_extractor import (
    CompositeChatClefIntentExtractor,
)


class AutoDepositTrustIntentContractTests(unittest.TestCase):
    def test_executable_intent_is_rule_only_zero_slot_and_compiles_exactly(self):
        result = ChatClefNaturalLanguageService().translate(
            "캐릭터 주변 16x16 범위의 상자를 자동 보관 대상으로 등록해"
        )

        self.assertEqual(ChatClefIntentStatus.VALIDATED, result.status)
        self.assertEqual("auto_deposit_trust area 16x16", result.command)
        self.assertIsNotNone(result.intent)
        intent = result.intent
        self.assertEqual(ChatClefIntentType.AUTO_DEPOSIT_TRUST_AREA, intent.intent_type)
        self.assertEqual("rule", intent.source)
        self.assertEqual("", intent.item_phrase)
        self.assertIsNone(intent.quantity)
        self.assertIsNone(intent.food_units)
        self.assertEqual("", intent.player_name)
        self.assertEqual((None, None, None), (intent.x, intent.y, intent.z))
        self.assertEqual({}, intent.slots)
        self.assertFalse(result.command.startswith("@"))

    def test_schema_rejects_non_rule_source_and_every_user_slot(self):
        validator = ChatClefIntentSchemaValidator()
        base = ChatClefIntentDTO(
            intent_type=ChatClefIntentType.AUTO_DEPOSIT_TRUST_AREA
        )
        self.assertEqual((True, "validated_intent"), validator.validate(base)[:2])

        invalid = (
            ChatClefIntentDTO(
                intent_type=ChatClefIntentType.AUTO_DEPOSIT_TRUST_AREA,
                source="llm",
            ),
            ChatClefIntentDTO(
                intent_type=ChatClefIntentType.AUTO_DEPOSIT_TRUST_AREA,
                quantity=16,
            ),
            ChatClefIntentDTO(
                intent_type=ChatClefIntentType.AUTO_DEPOSIT_TRUST_AREA,
                item_phrase="chest",
            ),
            ChatClefIntentDTO(
                intent_type=ChatClefIntentType.AUTO_DEPOSIT_TRUST_AREA,
                player_name="Steve",
            ),
            ChatClefIntentDTO(
                intent_type=ChatClefIntentType.AUTO_DEPOSIT_TRUST_AREA,
                x=0,
                y=64,
                z=0,
            ),
            ChatClefIntentDTO(
                intent_type=ChatClefIntentType.AUTO_DEPOSIT_TRUST_AREA,
                slots={"size": "16x16"},
            ),
        )
        for intent in invalid:
            with self.subTest(intent=intent):
                valid, reason, _message = validator.validate(intent)
                self.assertFalse(valid)
                self.assertIn(
                    reason,
                    {
                        "auto_deposit_trust_area_requires_rule_source",
                        "auto_deposit_trust_area_requires_zero_slots",
                    },
                )

    def test_guarded_candidate_is_invalid_without_llm_or_other_intent_fallback(self):
        llm_calls: list[str] = []
        service = ChatClefNaturalLanguageService(
            extractor=CompositeChatClefIntentExtractor(
                llm_extractor=ChatClefLLMIntentExtractor(
                    provider=lambda _prompt, text: llm_calls.append(text) or "{}"
                )
            )
        )

        result = service.translate("주변 16x16 상자를 등록할까?")

        self.assertEqual(ChatClefIntentStatus.INVALID, result.status)
        self.assertEqual("auto_deposit_trust_area_question", result.reason_code)
        self.assertEqual(ChatClefIntentType.UNKNOWN, result.intent.intent_type)
        self.assertIsNone(result.command)
        self.assertEqual([], llm_calls)

        stop_compound = service.translate(
            "자동보관등록 영역 16x16 하고 멈춰"
        )
        self.assertEqual(ChatClefIntentStatus.INVALID, stop_compound.status)
        self.assertEqual(
            "auto_deposit_trust_area_ambiguous_compound",
            stop_compound.reason_code,
        )
        self.assertIsNone(stop_compound.command)
        self.assertEqual([], llm_calls)

    def test_llm_is_denied_before_and_after_validation(self):
        pre = ChatClefLLMIntentExtractor(
            provider=lambda _prompt, _text: (
                '{"intent_type":"auto_deposit_trust_area","source":"rule","slots":{}}'
            )
        ).extract("등록해")
        self.assertEqual(ChatClefIntentType.UNKNOWN, pre.intent_type)
        self.assertEqual(
            "llm_auto_deposit_trust_area_not_authorized",
            pre.slots["reason_code"],
        )

        class PermissiveValidator:
            def validate(self, _value):
                return True, "validated_intent", "ok"

        post = ChatClefLLMIntentExtractor(
            provider=lambda _prompt, _text: (
                '{"intent_type":"auto_deposit_trust_area","source":"rule","slots":{}}'
            ),
            validator=PermissiveValidator(),
        ).extract("등록해")
        self.assertEqual(ChatClefIntentType.UNKNOWN, post.intent_type)
        self.assertEqual(
            "llm_auto_deposit_trust_area_not_authorized",
            post.slots["reason_code"],
        )

    def test_translation_result_rejects_command_mismatch(self):
        intent = ChatClefIntentDTO(
            intent_type=ChatClefIntentType.AUTO_DEPOSIT_TRUST_AREA
        )
        self.assertEqual(
            "auto_deposit_trust area 16x16",
            ChatClefCommandCompiler().compile(intent),
        )


if __name__ == "__main__":
    unittest.main()
