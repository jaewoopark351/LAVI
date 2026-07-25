#20260725_kpopmodder: Added resolver so conversation handlers do not own registry lookup details.
from __future__ import annotations

from typing import Any


class MinecraftExtensionResolver:
    def resolve(self, extension_registry: Any, name: str = "minecraft") -> Any:
        getter = getattr(extension_registry, "get", None)
        if not callable(getter):
            return None
        return getter(name)
