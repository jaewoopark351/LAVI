#20260725_kpopmodder: Added dispatch result value object for Minecraft extension commands.
from __future__ import annotations

from dataclasses import dataclass
from typing import Any


@dataclass(frozen=True)
class MinecraftCommandDispatchResult:
    result: Any
    action: str = ""
