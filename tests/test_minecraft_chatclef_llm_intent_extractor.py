#20260803_kpopmodder: Cover optional LLM extractor rejection without execution.
import unittest

from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_type import (
    ChatClefIntentType,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_llm_intent_extractor import (
    ChatClefLLMIntentExtractor,
)


class MinecraftChatClefLLMIntentExtractorTests(unittest.TestCase):
    def test_rejects_llm_json_that_contains_command_field(self):
        extractor = ChatClefLLMIntentExtractor(
            provider=lambda _prompt, _text: (
                '{"intent_type":"get_item","item_phrase":"금괴",'
                '"quantity":8,"command":"get gold_ingot 8"}'
            )
        )

        intent = extractor.extract("금괴 8개 구해")

        self.assertEqual(ChatClefIntentType.UNKNOWN, intent.intent_type)
        self.assertEqual("llm_invalid", intent.source)
        self.assertEqual("forbidden_intent_fields", intent.slots["reason_code"])

    def test_rejects_malformed_llm_json(self):
        extractor = ChatClefLLMIntentExtractor(
            provider=lambda _prompt, _text: "{not json"
        )

        intent = extractor.extract("금괴 8개 구해")

        self.assertEqual(ChatClefIntentType.UNKNOWN, intent.intent_type)
        self.assertEqual("llm_invalid", intent.source)
        self.assertEqual("malformed_llm_json", intent.slots["reason_code"])


if __name__ == "__main__":
    unittest.main()
