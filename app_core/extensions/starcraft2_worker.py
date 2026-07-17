#20260717_kpopmodder: Compatibility facade for StarCraft2 worker classes.
from app_core.extensions.starcraft2_core.starcraft2_worker import StarCraft2Worker
from app_core.extensions.starcraft2_core.starcraft2_worker_protocol import (
    StarCraft2WorkerProtocol,
)

__all__ = [
    "StarCraft2Worker",
    "StarCraft2WorkerProtocol",
]
