#20260725_kpopmodder: Added runtime context snapshot reader for Minecraft extension status output.
from __future__ import annotations

from typing import Any


class MinecraftRuntimeContextSnapshotReader:
    def read(self, runtime_context: Any):
        snapshot = getattr(runtime_context, "snapshot", None)
        if callable(snapshot):
            return snapshot()
        return None
