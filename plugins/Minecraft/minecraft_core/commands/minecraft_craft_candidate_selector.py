#20260725_kpopmodder: Added selector to isolate craft command trigger matching.
from __future__ import annotations

from .minecraft_command_aliases import CRAFT_PREFIXES


class MinecraftCraftCandidateSelector:
    def select(self, text: str, lowered: str) -> str:
        for prefix in CRAFT_PREFIXES:
            if lowered == prefix:
                return ""
            if lowered.startswith(prefix + " "):
                return text[len(prefix) :].strip()
        return ""
