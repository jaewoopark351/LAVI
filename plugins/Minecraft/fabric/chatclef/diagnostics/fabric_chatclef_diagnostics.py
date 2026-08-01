#20260801_kpopmodder: Keep Fabric ChatClef logging backend-owned.
from __future__ import annotations

from typing import Callable

from core.logger import log_print


class FabricChatClefDiagnostics:
    PREFIX = "[MinecraftFabricChatClef]"

    def __init__(self, logger: Callable[[str], None] = log_print):
        self._logger = logger

    def info(self, message: str) -> None:
        self._logger(f"{self.PREFIX} {message}")

    def warning(self, message: str) -> None:
        self._logger(f"{self.PREFIX}[WARN] {message}")

    def error(self, message: str) -> None:
        self._logger(f"{self.PREFIX}[ERROR] {message}")
