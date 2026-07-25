#20260725_kpopmodder: Added selector to isolate equip command trigger matching.
from __future__ import annotations

from .minecraft_command_aliases import EQUIP_PREFIXES


class MinecraftEquipCandidateSelector:
    def select(self, text: str, lowered: str) -> str:
        for prefix in EQUIP_PREFIXES:
            if lowered == prefix:
                return ""
            if lowered.startswith(prefix + " "):
                return text[len(prefix) :].strip()
        return ""
