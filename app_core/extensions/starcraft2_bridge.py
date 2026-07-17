#20260717_kpopmodder: Compatibility facade for StarCraft2 bridge adapter classes.
from app_core.extensions.starcraft2_core.starcraft2_bridge import StarCraft2Bridge
from app_core.extensions.starcraft2_core.starcraft2_bridge_protocol import (
    StarCraft2BridgeProtocol,
)

__all__ = [
    "StarCraft2Bridge",
    "StarCraft2BridgeProtocol",
]
