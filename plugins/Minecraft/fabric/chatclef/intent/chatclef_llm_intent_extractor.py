#20260803_kpopmodder: Added optional LLM extraction isolated from ChatClef execution.
#20260827_kpopmodder: Deny LLM authority to create STORE_HOME.
#20260905_kpopmodder: Deny LLM authority to create persistent H5 registration intents.
from __future__ import annotations

import json
from typing import Callable

from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_dto import (
    ChatClefIntentDTO,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_prompt import (
    CHATCLEF_INTENT_PROMPT,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_schema_validator import (
    ChatClefIntentSchemaValidator,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_type import (
    ChatClefIntentType,
)


class ChatClefLLMIntentExtractor:
    def __init__(
        self,
        provider: Callable[[str, str], str] | None = None,
        validator: ChatClefIntentSchemaValidator | None = None,
    ):
        self._provider = provider
        self._validator = validator or ChatClefIntentSchemaValidator()

    def extract(self, text: str) -> ChatClefIntentDTO:
        if self._provider is None:
            return ChatClefIntentDTO(
                intent_type=ChatClefIntentType.UNKNOWN,
                original_text=text,
                source="llm_unavailable",
                confidence=0.0,
            )
        try:
            payload = json.loads(self._provider(CHATCLEF_INTENT_PROMPT, text))
        except Exception as error:
            return ChatClefIntentDTO(
                intent_type=ChatClefIntentType.UNKNOWN,
                original_text=text,
                source="llm_invalid",
                confidence=0.0,
                slots={
                    "reason_code": "malformed_llm_json",
                    "message": f"{type(error).__name__}: {error}",
                },
            )
        if isinstance(payload, dict):
            if payload.get("intent_type") == "store_home":
                return self._store_home_rejection(text)
            if payload.get("intent_type") == "auto_deposit_trust_area":
                return self._auto_deposit_trust_rejection(text)
        valid, reason_code, message = self._validator.validate(payload)
        if not valid:
            return ChatClefIntentDTO(
                intent_type=ChatClefIntentType.UNKNOWN,
                original_text=text,
                source="llm_invalid",
                confidence=0.0,
                slots={"reason_code": reason_code, "message": message},
            )
        intent = ChatClefIntentDTO.from_mapping(payload)
        if intent.intent_type is ChatClefIntentType.STORE_HOME:
            return self._store_home_rejection(text)
        if intent.intent_type is ChatClefIntentType.AUTO_DEPOSIT_TRUST_AREA:
            return self._auto_deposit_trust_rejection(text)
        return intent

    def _store_home_rejection(self, text: str) -> ChatClefIntentDTO:
        return ChatClefIntentDTO(
            intent_type=ChatClefIntentType.UNKNOWN,
            original_text=text,
            source="llm_invalid",
            confidence=0.0,
            slots={
                "reason_code": "llm_store_home_not_authorized",
                "message": "LLM extraction cannot authorize STORE_HOME.",
            },
        )

    def _auto_deposit_trust_rejection(self, text: str) -> ChatClefIntentDTO:
        return ChatClefIntentDTO(
            intent_type=ChatClefIntentType.UNKNOWN,
            original_text=text,
            source="llm_invalid",
            confidence=0.0,
            slots={
                "reason_code": "llm_auto_deposit_trust_area_not_authorized",
                "message": (
                    "LLM extraction cannot authorize "
                    "AUTO_DEPOSIT_TRUST_AREA."
                ),
            },
        )
