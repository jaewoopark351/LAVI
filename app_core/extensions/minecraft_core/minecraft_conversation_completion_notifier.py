#20260725_kpopmodder: Added notifier that sends delayed Minecraft completion replies back to LAVI.
from __future__ import annotations

from typing import Callable

from core.logger import log_print


class MinecraftConversationCompletionNotifier:
    def __init__(self, callback: Callable[[str], object] | None = None):
        self.callback = callback

    @property
    def enabled(self) -> bool:
        return callable(self.callback)

    def notify(self, text: str) -> None:
        clean_text = str(text or "").strip()
        if not clean_text or not self.enabled:
            return
        try:
            self.callback(clean_text)
        except Exception as error:
            log_print(f"[MinecraftConversation] completion notify failed: {error}")
