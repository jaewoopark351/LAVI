#20260901_kpopmodder: Validate Carry On option binding and JSON configuration content before hashing it as evidence.
from __future__ import annotations

import json
from collections.abc import Mapping

from .carry_on_configuration_paths import (
    CARRY_ON_CLIENT_CONFIG_ROLE,
    CARRY_ON_COMMON_CONFIG_ROLE,
    CARRY_ON_OPTIONS_ROLE,
)


_CARRY_ON_BINDING_PREFIX = "key_key.carry.desc:"


def verify_automatic_deposit_carry_on_configuration_content(
    role: str,
    payload: bytes,
) -> str:
    if role == CARRY_ON_OPTIONS_ROLE:
        return _verify_options_binding(payload)
    if role in (CARRY_ON_CLIENT_CONFIG_ROLE, CARRY_ON_COMMON_CONFIG_ROLE):
        return _verify_json_object(payload)
    return "CARRY_ON_CONFIG_ROLE_INVALID"


def _verify_options_binding(payload: bytes) -> str:
    try:
        text = payload.decode("utf-8", errors="strict")
    except UnicodeError:
        return "CARRY_ON_OPTIONS_ENCODING_INVALID"
    matches = tuple(
        line
        for line in text.splitlines()
        if line.startswith(_CARRY_ON_BINDING_PREFIX)
    )
    if len(matches) != 1 or not matches[0][len(_CARRY_ON_BINDING_PREFIX) :].strip():
        return "CARRY_ON_OPTIONS_BINDING_MISSING_OR_AMBIGUOUS"
    return ""


def _verify_json_object(payload: bytes) -> str:
    try:
        parsed = json.loads(payload.decode("utf-8", errors="strict"))
    except (UnicodeError, ValueError):
        return "CARRY_ON_CONFIG_JSON_INVALID"
    if not isinstance(parsed, Mapping):
        return "CARRY_ON_CONFIG_JSON_NOT_OBJECT"
    return ""
