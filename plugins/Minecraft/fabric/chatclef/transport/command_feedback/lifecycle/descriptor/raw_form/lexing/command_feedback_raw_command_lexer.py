#20260907_kpopmodder: Isolate bounded raw-command lexical validation from form grammar.
from __future__ import annotations

import re


class CommandFeedbackRawCommandLexer:
    _MAX_COMMAND_LENGTH = 512
    _FORBIDDEN = re.compile(r"[;#\r\n\x00-\x1f\x7f]|&&|\|\||[\"\\]")

    def split(self, command: object) -> tuple[str, tuple[str, ...]] | None:
        if (
            type(command) is not str
            or not command
            or command != command.strip()
            or len(command) > self._MAX_COMMAND_LENGTH
            or self._FORBIDDEN.search(command) is not None
            or any(character.isspace() and character != " " for character in command)
        ):
            return None
        body = command
        if body.startswith("@"):
            body = body[1:].lstrip(" ")
        if not body or body.startswith("@"):
            return None
        units = tuple(unit for unit in body.split(" ") if unit)
        if not units:
            return None
        return units[0], units[1:]


__all__ = ("CommandFeedbackRawCommandLexer",)
