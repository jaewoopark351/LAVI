#20260725_kpopmodder: Added text command detector so dispatch policy does not inline type checks.
from __future__ import annotations

from typing import Any


class MinecraftTextCommandDetector:
    def is_text_command(self, command: Any) -> bool:
        return isinstance(command, str)
