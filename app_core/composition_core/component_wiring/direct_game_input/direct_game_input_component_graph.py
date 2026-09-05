#20260905_kpopmodder: Assemble stable StarCraft and ScreenVision input wiring.
from __future__ import annotations

from .adapters import DirectInputAdapterCache
from .screen_vision import ScreenVisionDirectInputWiring
from .starcraft import StarCraftDirectInputWiring


class DirectGameInputComponentGraph:
    def __init__(self):
        self.adapter_cache = DirectInputAdapterCache()
        self.starcraft_wiring = StarCraftDirectInputWiring(self.adapter_cache)
        self.screen_vision_wiring = ScreenVisionDirectInputWiring(
            self.adapter_cache
        )


__all__ = ("DirectGameInputComponentGraph",)
