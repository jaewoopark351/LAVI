#20260717_kpopmodder: Compatibility facade for StarCraft116 bridge adapter classes.
from app_core.extensions.starcraft116_core.starcraft116_bridge import (
    StarCraft116Bridge,
)
from app_core.extensions.starcraft116_core.starcraft116_bridge_protocol import (
    StarCraft116BridgeProtocol,
)

__all__ = [
    "StarCraft116Bridge",
    "StarCraft116BridgeProtocol",
]
