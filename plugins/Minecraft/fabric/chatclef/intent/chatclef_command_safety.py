#20260816_kpopmodder: Keep translated ChatClef DSL command safety checks in one boundary.
from __future__ import annotations

import re
from typing import Any


class ChatClefCommandSafetyValidator:
    _DANGEROUS_RE = re.compile(r"[;#\r\n\"'@]|[\x00-\x1f\x7f]")

    @classmethod
    def validate_prefixless_command(cls, command: Any) -> str:
        if not isinstance(command, str):
            raise TypeError("command_must_be_string")
        if command != command.strip():
            raise ValueError("command_must_not_have_surrounding_whitespace")
        if not command:
            raise ValueError("command_must_not_be_blank")
        if cls._DANGEROUS_RE.search(command) is not None:
            raise ValueError("unsafe_command_characters")
        return command

    @classmethod
    def has_dangerous_text(cls, text: object) -> bool:
        return cls._DANGEROUS_RE.search(str(text or "")) is not None
