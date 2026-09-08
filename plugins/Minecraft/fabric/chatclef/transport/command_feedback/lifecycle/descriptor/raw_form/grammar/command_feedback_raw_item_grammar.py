#20260907_kpopmodder: Isolate registered item-list and GIVE raw form grammar.
from __future__ import annotations

import re

from .command_feedback_raw_java_number_parser import (
    CommandFeedbackRawJavaNumberParser,
)


class CommandFeedbackRawItemGrammar:
    GRAMMAR_IDS = frozenset(
        {
            "give_item",
            "optional_item_list",
            "required_equipment_list",
            "required_item_list",
        }
    )
    _ITEM = re.compile(r"[a-z0-9_]+\Z", re.ASCII)
    _PLAYER = re.compile(r"[A-Za-z0-9_]{1,16}\Z", re.ASCII)
    _ARMOR_SETS = frozenset({"leather", "iron", "gold", "diamond", "netherite"})

    def __init__(self, *, number_parser=None) -> None:
        self._numbers = number_parser or CommandFeedbackRawJavaNumberParser()

    def decode(
        self,
        grammar_id: str,
        _command_name: str,
        arguments: tuple[str, ...],
    ) -> str | None:
        if grammar_id == "give_item":
            return self._give(arguments)
        if grammar_id == "optional_item_list":
            return "inventory_default" if not arguments else self._item_list_form(arguments)
        if grammar_id == "required_equipment_list":
            if len(arguments) == 1 and arguments[0].lower() in self._ARMOR_SETS:
                return "equipment_material_set"
            form_kind = self._item_list_form(arguments)
            return None if form_kind is None else f"equipment_{form_kind}"
        if grammar_id == "required_item_list":
            return self._item_list_form(arguments)
        return None

    def _item_list_form(self, arguments: tuple[str, ...]) -> str | None:
        if not arguments:
            return None
        value = " ".join(arguments)
        if value.startswith("[") or value.endswith("]"):
            if not (value.startswith("[") and value.endswith("]")):
                return None
            body = value[1:-1].strip()
            entries = tuple(part.strip() for part in body.split(","))
            entry_kinds = tuple(self._item_entry_kind(entry) for entry in entries)
            if not entries or any(kind is None for kind in entry_kinds):
                return None
            return (
                "item_list"
                if all(kind == "typed" for kind in entry_kinds)
                else "item_list_unprojected"
            )
        units = tuple(unit for unit in value.split(" ") if unit)
        if len(units) not in {1, 2} or self._ITEM.fullmatch(units[0]) is None:
            return None
        if len(units) == 1:
            return "single_item"
        parsed_count = self._numbers.integer(units[1], positive=False)
        if parsed_count is None:
            return None
        return "single_item" if parsed_count > 0 else "single_item_unprojected"

    def _item_entry_kind(self, entry: str) -> str | None:
        units = tuple(unit for unit in entry.split(" ") if unit)
        if (
            len(units) not in {1, 2}
            or self._ITEM.fullmatch(units[0]) is None
        ):
            return None
        if len(units) == 1:
            return "typed"
        parsed_count = self._numbers.integer(units[1], positive=False)
        if parsed_count is None:
            return None
        return "typed" if parsed_count > 0 else "unprojected"

    def _give(self, arguments: tuple[str, ...]) -> str | None:
        if len(arguments) == 1:
            return (
                "butler_item_default_count"
                if self._ITEM.fullmatch(arguments[0])
                else "butler_item_unprojected"
            )
        if len(arguments) == 2:
            count = self._numbers.integer(arguments[1], positive=False)
            if count is None:
                return None
            if self._ITEM.fullmatch(arguments[0]) and count > 0:
                return "butler_item_count"
            return "butler_item_count_unprojected"
        if len(arguments) == 3:
            count = self._numbers.integer(arguments[2], positive=False)
            if count is None:
                return None
            if (
                self._PLAYER.fullmatch(arguments[0])
                and self._ITEM.fullmatch(arguments[1])
                and count > 0
            ):
                return "explicit_player_item_count"
            return "explicit_player_item_count_unprojected"
        return None


__all__ = ("CommandFeedbackRawItemGrammar",)
