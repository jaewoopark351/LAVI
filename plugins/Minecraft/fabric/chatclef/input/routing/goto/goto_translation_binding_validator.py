#20260913_kpopmodder: Compare original XYZ with the already validated translation.
from __future__ import annotations

from typing import Any, Mapping

from plugins.Minecraft.fabric.chatclef.intent.chatclef_command_compiler import (
    ChatClefCommandCompiler,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_dto import (
    ChatClefIntentDTO,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_type import (
    ChatClefIntentType,
)

from .goto_input_binding import GotoInputBinding


class GotoTranslationBindingValidator:
    def __init__(self):
        self._compiler = ChatClefCommandCompiler()

    def validate(
        self,
        binding: GotoInputBinding,
        translation: Mapping[str, Any],
    ) -> str | None:
        if not binding.automatic_input:
            return None
        parsed = binding.parse_result
        intent = translation.get("intent")
        is_goto = isinstance(intent, Mapping) and intent.get("intent_type") == "goto"
        if not parsed.executable:
            return "goto_original_xyz_required" if is_goto else None
        if not is_goto or translation.get("executable") is not True:
            return "goto_translation_intent_changed"
        xyz = tuple(intent.get(axis) for axis in ("x", "y", "z"))
        if xyz != parsed.xyz or any(type(value) is not int for value in xyz):
            return "goto_translation_coordinates_changed"
        x, y, z = parsed.xyz
        expected = self._compiler.compile(
            ChatClefIntentDTO(intent_type=ChatClefIntentType.GOTO, x=x, y=y, z=z)
        )
        if translation.get("command") != expected:
            return "goto_translation_command_changed"
        return None
