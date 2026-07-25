#20260725_kpopmodder: Added goto bridge action handler.
from __future__ import annotations

from typing import Any, Dict

from ..bridge.minecraft_bridge_client_provider import MinecraftBridgeClientProvider
from ..minecraft_config import MinecraftConfig


class MinecraftGotoAction:
    def __init__(
        self,
        config_manager: MinecraftConfig,
        client_provider: MinecraftBridgeClientProvider,
    ):
        self.config_manager = config_manager
        self.client_provider = client_provider

    def run(
        self,
        target: Any = None,
        *,
        x: Any = None,
        y: Any = None,
        z: Any = None,
        dimension: Any = None,
    ) -> Dict[str, object]:
        if not self._actions_allowed():
            return {
                "ok": False,
                "action": "goto",
                "error": "actions_disabled",
                "message": "Minecraft actions are disabled in config.",
            }
        target_text = str(target or "").strip()
        if not target_text and not any(
            value is not None and str(value).strip()
            for value in (x, y, z, dimension)
        ):
            return {
                "ok": False,
                "action": "goto",
                "error": "missing_target",
                "message": "target, coordinates, or dimension is required.",
            }
        return self.client_provider.client.goto(
            target_text or None,
            x=x,
            y=y,
            z=z,
            dimension=dimension,
        )

    def _actions_allowed(self) -> bool:
        return (
            self.config_manager.get_bool("enabled", True)
            and self.config_manager.get_bool("allow_actions", True)
        )
