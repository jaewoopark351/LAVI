#20260725_kpopmodder: Added route DTO for explicit Minecraft conversation commands.
from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class MinecraftConversationCommandRoute:
    game: str
    command: str
    raw_text: str
    trigger: str
