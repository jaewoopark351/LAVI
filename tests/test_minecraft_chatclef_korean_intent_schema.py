#20260803_kpopmodder: Cover strict schema rules for Korean ChatClef intents.
import unittest

from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_dto import (
    ChatClefIntentDTO,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_schema_validator import (
    ChatClefIntentSchemaValidator,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_type import (
    ChatClefIntentType,
)


class MinecraftChatClefKoreanIntentSchemaTests(unittest.TestCase):
    def test_valid_get_item_intent_has_no_command_field(self):
        validator = ChatClefIntentSchemaValidator()

        valid, reason_code, _message = validator.validate(
            ChatClefIntentDTO(
                intent_type=ChatClefIntentType.GET_ITEM,
                item_phrase="다이아몬드 도끼",
                quantity=1,
            )
        )

        self.assertTrue(valid)
        self.assertEqual("validated_intent", reason_code)

    def test_forbidden_command_like_fields_are_rejected(self):
        validator = ChatClefIntentSchemaValidator()

        valid, reason_code, message = validator.validate(
            {
                "intent_type": "get_item",
                "item_phrase": "금괴",
                "quantity": 8,
                "command": "get gold_ingot 8",
            }
        )

        self.assertFalse(valid)
        self.assertEqual("forbidden_intent_fields", reason_code)
        self.assertIn("command", message)

    def test_extra_llm_json_fields_are_rejected(self):
        validator = ChatClefIntentSchemaValidator()

        valid, reason_code, message = validator.validate(
            {
                "intent_type": "get_item",
                "item_phrase": "철괴",
                "quantity": 4,
                "surprise": True,
            }
        )

        self.assertFalse(valid)
        self.assertEqual("unknown_intent_fields", reason_code)
        self.assertIn("surprise", message)

    def test_non_positive_quantities_are_invalid(self):
        validator = ChatClefIntentSchemaValidator()

        valid, reason_code, _message = validator.validate(
            {
                "intent_type": "get_item",
                "item_phrase": "다이아 도끼",
                "quantity": 0,
            }
        )

        self.assertFalse(valid)
        self.assertEqual("invalid_quantity", reason_code)


if __name__ == "__main__":
    unittest.main()
