#20260914_kpopmodder: Compile FIND only from closed kind/mode and canonical target slots.
from __future__ import annotations

import re

from plugins.Minecraft.fabric.chatclef.transport.find_catalog.find_catalog_snapshot import IDENTIFIER

PLAYER_NAME = re.compile(r"[A-Za-z0-9_]{3,16}\Z", re.ASCII)


class FindCommandCompiler:
    @staticmethod
    def compile_slots(kind: object, target: object, mode: object = "report") -> str:
        if (type(kind) is not str or type(mode) is not str
                or kind not in {"entity", "block", "item", "player"} or mode not in {"report", "approach"}):
            raise ValueError("invalid_find_kind_or_mode")
        if kind == "item" and mode != "report":
            raise ValueError("item_find_approach_unsupported")
        if kind == "item" and target == "minecraft:air":
            raise ValueError("invalid_find_target")
        pattern = PLAYER_NAME if kind == "player" else IDENTIFIER
        if type(target) is not str or len(target) > 128 or pattern.fullmatch(target) is None:
            raise ValueError("invalid_find_target")
        return f"find {kind} {target} {mode}"

    def compile(self, intent: object, target: object) -> str:
        slots = getattr(intent, "slots", {})
        if set(slots) != {"target_kind", "target_phrase", "mode"} or getattr(intent, "source", None) != "rule":
            raise ValueError("find_requires_closed_rule_slots")
        if any((getattr(intent, "quantity", None) is not None, bool(getattr(intent, "item_phrase", "")),
                getattr(intent, "food_units", None) is not None, getattr(intent, "x", None) is not None,
                getattr(intent, "y", None) is not None, getattr(intent, "z", None) is not None,
                bool(getattr(intent, "player_name", "")))):
            raise ValueError("find_rejects_unrelated_slots")
        return self.compile_slots(slots["target_kind"], target, slots["mode"])
