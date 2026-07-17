#20260717_kpopmodder: Split from legacy multi-class module for AGENTS 29.1 file/type separation.

#20260715_kpopmodder: Centralize StarCraft2 runtime composition outside the Gradio UI module.
from __future__ import annotations

from dataclasses import dataclass
from typing import Sequence

from .sc2_ladder_proxy_launcher import SC2LadderProxyLauncher
from .sc2_local_match_command_template import _LocalMatchCommandTemplate
from .starcraft2_arg_utils import _StarCraft2ArgUtils
from .starcraft2_config import StarCraft2Config
from .starcraft2_engine_registry import StarCraft2EngineRegistry
from .starcraft2_event_bus import StarCraft2EventBus
from .starcraft2_event_service import (
    StarCraft2EngineEventService,
    StarCraft2LadderProxyEventService,
)
from .starcraft2_facade_service import StarCraft2FacadeService
from .starcraft2_local_match_service import StarCraft2LocalMatchService
from .starcraft2_match_config_service import _StarCraft2MatchConfigService
from .starcraft2_observation_tracker import SC2ObservationTracker
from .starcraft2_runtime_context import SC2RuntimeContext
from .starcraft2_runtime_downloader import StarCraft2RuntimeDownloader
from .starcraft2_state import StarCraft2RuntimeState

@dataclass(frozen=True)
class StarCraft2RuntimeBundle:
    #20260715_kpopmodder: Return the complete SC2 object graph as one typed composition result.
    config_manager: StarCraft2Config
    engine_registry: StarCraft2EngineRegistry
    state: StarCraft2RuntimeState
    runtime_context: SC2RuntimeContext
    ladder_proxy: SC2LadderProxyLauncher
    runtime_downloader: StarCraft2RuntimeDownloader
    observation_tracker: SC2ObservationTracker
    local_match_command_template: _LocalMatchCommandTemplate
    arg_utils: _StarCraft2ArgUtils
    match_config_service: _StarCraft2MatchConfigService
    event_bus: StarCraft2EventBus
    engine_event_service: StarCraft2EngineEventService
    local_match_service: StarCraft2LocalMatchService
    facade_service: StarCraft2FacadeService
    ladder_proxy_event_service: StarCraft2LadderProxyEventService
