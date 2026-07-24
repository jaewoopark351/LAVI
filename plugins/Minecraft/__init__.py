#20260725_kpopmodder: Added Minecraft optional plugin package for LAVI/ChatClef bridge integration.
from __future__ import annotations

__all__ = ["Minecraft"]


def __getattr__(name):
    if name == "Minecraft":
        from .minecraft import Minecraft

        return Minecraft
    raise AttributeError(name)
