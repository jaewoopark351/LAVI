#20260717_kpopmodder: Split from legacy multi-class module for AGENTS 29.1 file/type separation.

from .starcraft2_engine_event_service import StarCraft2EngineEventService
from .starcraft2_ladder_proxy_event_service import StarCraft2LadderProxyEventService

_StarCraft2EngineEventService = StarCraft2EngineEventService
_StarCraft2LadderProxyEventService = StarCraft2LadderProxyEventService

__all__ = [
    'StarCraft2EngineEventService',
    'StarCraft2LadderProxyEventService',
    '_StarCraft2EngineEventService',
    '_StarCraft2LadderProxyEventService',
]
