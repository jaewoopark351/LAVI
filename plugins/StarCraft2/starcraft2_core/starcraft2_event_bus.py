#20260717_kpopmodder: Split from legacy multi-class module for AGENTS 29.1 file/type separation.

from .starcraft2_event_bus_subscription import StarCraft2EventBusSubscription
from .starcraft2_event_bus_impl import StarCraft2EventBus

_StarCraft2EventBusSubscription = StarCraft2EventBusSubscription
_StarCraft2EventBus = StarCraft2EventBus

__all__ = [
    'StarCraft2EventBusSubscription',
    'StarCraft2EventBus',
    '_StarCraft2EventBusSubscription',
    '_StarCraft2EventBus',
]
