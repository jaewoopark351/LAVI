#20260717_kpopmodder: Compatibility facade for StarCraft116 worker classes.
from app_core.extensions.starcraft116_core.starcraft116_worker import (
    StarCraft116Worker,
)
from app_core.extensions.starcraft116_core.starcraft116_worker_protocol import (
    StarCraft116WorkerProtocol,
)

__all__ = [
    "StarCraft116Worker",
    "StarCraft116WorkerProtocol",
]
