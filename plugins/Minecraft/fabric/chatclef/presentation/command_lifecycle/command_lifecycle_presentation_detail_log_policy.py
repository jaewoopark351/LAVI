#20260907_kpopmodder: Keep command detail bounded, ASCII, and UI-only.
from __future__ import annotations

import json
import re


class CommandLifecyclePresentationDetailLogPolicy:
    MAX_LENGTH = 1024
    _MAX_INTEGER = 2**31 - 1
    _COMMAND_NAME = re.compile(
        r"(?:[a-z][a-z0-9_]*|자동보관등록)\Z",
        re.ASCII,
    )
    _FORM_KIND = re.compile(r"[a-z][a-z0-9_]{0,63}\Z", re.ASCII)
    _CANONICAL_SLOT = re.compile(r"[a-z0-9_.:/+-]{1,128}\Z", re.ASCII)
    _PLAYER = re.compile(r"[A-Za-z0-9_]{1,16}\Z", re.ASCII)
    _OPTIONAL_CANONICAL_KEYS = frozenset(
        {
            "destination",
            "dimension",
            "operation_target",
            "setting_value",
            "structure",
            "target",
        }
    )
    _ALLOWED_KEYS = frozenset(
        {
            "command_name",
            "coordinates",
            "form_kind",
            "player",
            "requested_count",
            "target_entry_count",
            "targets",
            "targets_truncated",
        }
    ) | _OPTIONAL_CANONICAL_KEYS

    @classmethod
    def validate(cls, value: object) -> str:
        if type(value) is not str:
            raise TypeError("command presentation detail log must be an exact str")
        if len(value) > cls.MAX_LENGTH or any(
            ord(character) < 32 or ord(character) > 126 for character in value
        ):
            raise ValueError("command presentation detail log must be bounded ASCII")
        if not value:
            return value
        try:
            detail = json.loads(value)
        except (TypeError, ValueError) as exc:
            raise ValueError(
                "command presentation detail log must be canonical JSON"
            ) from exc
        if type(detail) is not dict or set(detail) - cls._ALLOWED_KEYS:
            raise ValueError("command presentation detail log schema is invalid")
        if not cls._matches(detail.get("command_name"), cls._COMMAND_NAME):
            raise ValueError("command presentation command_name is invalid")
        if not cls._matches(detail.get("form_kind"), cls._FORM_KIND):
            raise ValueError("command presentation form_kind is invalid")
        for key in cls._OPTIONAL_CANONICAL_KEYS:
            if key in detail and not cls._matches(
                detail[key],
                cls._CANONICAL_SLOT,
            ):
                raise ValueError(f"command presentation {key} is invalid")
        if "player" in detail and not cls._matches(
            detail["player"],
            cls._PLAYER,
        ):
            raise ValueError("command presentation player is invalid")
        cls._validate_coordinates(detail)
        cls._validate_scalar_target(detail)
        cls._validate_targets(detail)
        if cls._encode(detail) != value:
            raise ValueError(
                "command presentation detail log must use canonical encoding"
            )
        return value

    @classmethod
    def _validate_coordinates(cls, detail: dict) -> None:
        if "coordinates" not in detail:
            return
        values = detail["coordinates"]
        if (
            type(values) is not list
            or not 1 <= len(values) <= 3
            or any(not cls._valid_signed_integer(value) for value in values)
        ):
            raise ValueError("command presentation coordinates are invalid")

    @classmethod
    def _validate_scalar_target(cls, detail: dict) -> None:
        if "requested_count" not in detail:
            return
        if "target" not in detail or not cls._valid_integer(
            detail["requested_count"]
        ):
            raise ValueError("command presentation requested_count is invalid")

    @classmethod
    def _validate_targets(cls, detail: dict) -> None:
        target_keys = {
            "target_entry_count",
            "targets",
            "targets_truncated",
        }
        present = target_keys.intersection(detail)
        if not present:
            return
        if "target" in detail or "requested_count" in detail:
            raise ValueError("scalar and multiple command targets cannot mix")
        targets = detail.get("targets")
        entry_count = detail.get("target_entry_count")
        if (
            type(targets) is not list
            or not targets
            or not cls._valid_integer(entry_count)
            or entry_count < len(targets)
        ):
            raise ValueError("command presentation targets are invalid")
        for target in targets:
            if (
                type(target) is not dict
                or set(target) != {"count", "id"}
                or not cls._valid_integer(target.get("count"))
                or not cls._matches(target.get("id"), cls._CANONICAL_SLOT)
            ):
                raise ValueError("command presentation target entry is invalid")
        truncated = detail.get("targets_truncated")
        if entry_count == len(targets):
            if "targets_truncated" in detail:
                raise ValueError("complete command targets cannot be truncated")
        elif truncated is not True:
            raise ValueError("partial command targets must be marked truncated")

    @classmethod
    def _valid_integer(cls, value: object) -> bool:
        return type(value) is int and 1 <= value <= cls._MAX_INTEGER

    @classmethod
    def _valid_signed_integer(cls, value: object) -> bool:
        return (
            type(value) is int
            and -cls._MAX_INTEGER - 1 <= value <= cls._MAX_INTEGER
        )

    @staticmethod
    def _matches(value: object, pattern) -> bool:
        return type(value) is str and pattern.fullmatch(value) is not None

    @staticmethod
    def _encode(detail: dict) -> str:
        return json.dumps(
            detail,
            ensure_ascii=True,
            separators=(",", ":"),
            sort_keys=True,
        )


__all__ = ("CommandLifecyclePresentationDetailLogPolicy",)
