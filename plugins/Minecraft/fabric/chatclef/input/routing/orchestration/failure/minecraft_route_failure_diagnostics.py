#20260905_kpopmodder: Isolate Minecraft route failure diagnostics.
from __future__ import annotations


class MinecraftRouteFailureDiagnostics:
    def __init__(self, router_logger):
        self._router_logger = router_logger

    def report(self, reason: str, error: Exception) -> None:
        message = f"{type(error).__name__}: {error}"
        self._router_logger.log(
            f"route failed: reason={reason} error={message}"
        )


__all__ = ("MinecraftRouteFailureDiagnostics",)
