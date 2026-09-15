#20260915_kpopmodder: Validate feedback command identity with the same typed schema and compiler as submission.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.intent.chatclef_command_compiler import ChatClefCommandCompiler
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_dto import ChatClefIntentDTO
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_schema_validator import ChatClefIntentSchemaValidator


class CommandFeedbackTrustedSlotValidator:
    def matches(self, *, command_name, command, intent, target_item, **unused):
        try:
            valid, _, _ = ChatClefIntentSchemaValidator().validate(intent)
            if not valid:
                return False
            compiled = ChatClefCommandCompiler().compile(ChatClefIntentDTO.from_mapping(intent), target_item)
            return compiled == command and compiled.split(maxsplit=1)[0] == command_name
        except (ValueError, TypeError, KeyError, OSError):
            return False
