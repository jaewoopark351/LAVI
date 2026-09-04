#20260827_kpopmodder: Normalize natural-language request fields without owning submission policy.
from __future__ import annotations

import uuid
from typing import Any, Mapping


def natural_language_text(command: Any) -> str:
    if isinstance(command, str):
        return command.strip()
    if isinstance(command, Mapping):
        return str(command.get("text") or command.get("command") or "").strip()
    return str(command or "").strip()


def request_source(command: Any) -> str:
    if isinstance(command, Mapping):
        source = command.get("source")
        return source if type(source) is str else ""
    return ""


def request_id(command: Any) -> str:
    if isinstance(command, Mapping):
        value = command.get("request_id")
        if type(value) is str and value and value == value.strip():
            return value
    return f"lavi-ko-rejected-{uuid.uuid4().hex}"


def translated_command_name(command: object) -> str:
    text = str(command or "").strip()
    if text.startswith("@"):
        text = text[1:].strip()
    return text.split(maxsplit=1)[0].lower() if text else ""
