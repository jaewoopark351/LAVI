#20260816_kpopmodder: Validate translated ChatClef commands before adapter submission.
from __future__ import annotations

from typing import Any

from plugins.Minecraft.fabric.chatclef.intent.chatclef_command_compiler import (
    ChatClefCommandCompiler,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_command_safety import (
    ChatClefCommandSafetyValidator,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_dto import (
    ChatClefIntentDTO,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_status import (
    ChatClefIntentStatus,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_type import (
    ChatClefIntentType,
)


class ChatClefTranslationResultValidator:
    def __init__(self, compiler: ChatClefCommandCompiler | None = None):
        self._compiler = compiler or ChatClefCommandCompiler()

    def validate(
        self,
        *,
        status: ChatClefIntentStatus,
        executable: Any,
        command: Any,
        intent: ChatClefIntentDTO | None,
        resolved_target: str | None,
    ) -> str | None:
        if type(executable) is not bool:
            raise TypeError("executable_must_be_bool")
        if executable != status.executable:
            raise ValueError("executable_must_match_status")
        if not executable:
            if command is not None:
                raise ValueError("non_executable_translation_must_not_include_command")
            return None

        safe_command = ChatClefCommandSafetyValidator.validate_prefixless_command(
            command
        )
        if intent is None:
            raise ValueError("executable_translation_requires_intent")

        expected_command = self._compile_expected_command(intent, resolved_target)
        if safe_command != expected_command:
            raise ValueError("translated_command_must_match_validated_intent")
        return safe_command

    def _compile_expected_command(
        self,
        intent: ChatClefIntentDTO,
        resolved_target: str | None,
    ) -> str:
        if intent.intent_type in {
            ChatClefIntentType.GET_ITEM,
            ChatClefIntentType.EQUIP_ITEM,
            ChatClefIntentType.DEPOSIT_ITEM,
            ChatClefIntentType.GIVE_ITEM,
        }:
            if not isinstance(resolved_target, str) or not resolved_target.strip():
                raise ValueError("item_action_translation_requires_resolved_target")
            return self._compiler.compile(intent, target=resolved_target)
        return self._compiler.compile(intent)
