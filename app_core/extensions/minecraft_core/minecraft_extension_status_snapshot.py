#20260725_kpopmodder: Added extension status snapshot builder for Minecraft GameExtension lifecycle fields.
from __future__ import annotations

from typing import Dict


class MinecraftExtensionStatusSnapshot:
    def build(self, *, name: str, initialized: bool, started: bool) -> Dict[str, object]:
        return {
            "name": name,
            "initialized": initialized,
            "started": started,
        }
