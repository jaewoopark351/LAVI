#20260905_kpopmodder: Isolate extension translation capability inspection.
from __future__ import annotations

from typing import Any


class MinecraftChatClefTranslationCapabilityInspector:
    def is_available(self, extension: Any) -> bool:
        return callable(
            getattr(extension, "translate_natural_language_command", None)
        )

    def is_generic_crafting_defaults_available(self, extension: Any) -> bool:
        return callable(
            getattr(
                extension,
                "translate_generic_crafting_defaults_command",
                None,
            )
        )


__all__ = ("MinecraftChatClefTranslationCapabilityInspector",)
