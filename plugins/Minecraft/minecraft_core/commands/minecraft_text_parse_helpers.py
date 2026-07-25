#20260725_kpopmodder: Added shared text parsing helpers for Minecraft command parsers.
from __future__ import annotations

import re
from typing import Any, Dict, Iterable, Mapping


ITEM_TOKEN_PATTERN = re.compile(r"[a-z0-9_:.\\-]+", re.IGNORECASE)
NUMBER_PATTERN = re.compile(r"-?\d+")


def build_payload(action: str, raw_text: str) -> Dict[str, Any]:
    return {
        "action": action,
        "source": "minecraft_text_parser",
        "raw_text": raw_text,
    }


def squash_spaces(value: str) -> str:
    return " ".join(str(value or "").strip().split())


def first_item_token(
    text: str,
    *,
    dimension_aliases: Mapping[str, str],
    item_prefixes: Iterable[str],
) -> str:
    for token in ITEM_TOKEN_PATTERN.findall(text.lower()):
        if token.lstrip("-").isdigit():
            continue
        if token in dimension_aliases:
            continue
        if token in item_prefixes:
            continue
        return token.removeprefix("minecraft:")
    return ""


def first_positive_number(text: str, default: int = 1) -> int:
    for match in NUMBER_PATTERN.findall(text):
        try:
            value = int(match)
        except ValueError:
            continue
        if value > 0:
            return value
    return default
