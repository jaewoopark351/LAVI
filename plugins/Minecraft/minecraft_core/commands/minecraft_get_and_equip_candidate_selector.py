#20260725_kpopmodder: Added selector to isolate collect-then-equip command trigger matching.
from __future__ import annotations

import re

from .minecraft_command_aliases import GET_AND_EQUIP_PREFIXES


class MinecraftGetAndEquipCandidateSelector:
    def select(self, text: str, lowered: str) -> str:
        for prefix in GET_AND_EQUIP_PREFIXES:
            if lowered == prefix:
                return ""
            if lowered.startswith(prefix + " "):
                return text[len(prefix) :].strip()
        if lowered.startswith("get ") and " and equip" in lowered:
            return re.sub(r"^\s*get\s+", "", text, count=1, flags=re.IGNORECASE)
        return ""
