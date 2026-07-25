#20260725_kpopmodder: Added Minecraft plugin loader so extension lifecycle does not own import details.
from __future__ import annotations


class MinecraftPluginLoader:
    def build(self):
        try:
            from plugins.Minecraft.minecraft import Minecraft
        except Exception as error:
            raise RuntimeError("Minecraft plugin could not be imported") from error
        return Minecraft()
