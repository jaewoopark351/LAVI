#20260915_kpopmodder: Freeze bounded native observations without deriving effects from log text.
from __future__ import annotations

from collections.abc import Mapping
from dataclasses import dataclass
from types import MappingProxyType
import unicodedata

from .instant_command_values_validator import InstantCommandValuesValidator


@dataclass(frozen=True, slots=True)
class InstantCommandPayload:
    command: str
    command_name: str
    outcome: str
    reason: str
    values: Mapping

    COMMANDS = frozenset({"auto_deposit_trust", "auto_deposit_trusted_list", "auto_deposit_untrust",
                          "chatclef", "gamma", "overlay", "reload_settings", "resetmemory", "scan",
                          "give", "follow", "자동보관등록"})

    @classmethod
    def from_data(cls, data):
        value = data.get("instant_command") if isinstance(data, Mapping) else None
        if not isinstance(value, Mapping) or set(value) != {
                "schema_version", "command", "command_name", "outcome", "reason", "values"}:
            return None
        try:
            if type(value["schema_version"]) is not int or value["schema_version"] != 1:
                return None
            for key in ("command", "command_name", "outcome", "reason"):
                text = value[key]
                if (type(text) is not str or not text or len(text) > (4096 if key == "command" else 64)
                        or any(unicodedata.category(c)[0] == "C" for c in text)):
                    return None
            command, name = value["command"], value["command_name"]
            if (name not in cls.COMMANDS or command != command.strip() or cls.native_name(command.split()[0]) != name
                    or value["outcome"] not in {"completed", "failed", "unknown"}
                    or not InstantCommandValuesValidator.valid(
                        name, command, value["outcome"], value["reason"], value["values"])):
                return None
            frozen = dict(value["values"])
            if "destinations" in frozen:
                frozen["destinations"] = tuple(MappingProxyType(dict(item)) for item in frozen["destinations"])
            return cls(command, name, value["outcome"], value["reason"], MappingProxyType(frozen))
        except (ValueError, TypeError, KeyError, IndexError, OverflowError):
            return None

    @staticmethod
    def native_name(command_name):
        return "auto_deposit_trust" if command_name == "자동보관등록" else command_name
