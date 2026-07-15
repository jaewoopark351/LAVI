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


class StarCraft2RuntimeFactory:
    #20260715_kpopmodder: Build dependencies only; runtime behavior stays in SC2 services.
    def create(
        self,
        plugin_root: str,
        race_choices: Sequence[str],
    ) -> StarCraft2RuntimeBundle:
        config_manager = StarCraft2Config(plugin_root)
        engine_registry = StarCraft2EngineRegistry()
        state = StarCraft2RuntimeState(
            engine=str(config_manager.get("engine", "internal_lav_bot")),
            map_name=str(config_manager.get("map_name", "")),
            race=str(config_manager.get("race", "Terran")),
            enemy_race=str(config_manager.get("enemy_race", "Zerg")),
            enemy_difficulty=str(config_manager.get("enemy_difficulty", "Easy")),
        )
        runtime_context = SC2RuntimeContext()
        ladder_proxy = SC2LadderProxyLauncher()
        runtime_downloader = StarCraft2RuntimeDownloader()
        observation_tracker = SC2ObservationTracker()
        local_match_command_template = _LocalMatchCommandTemplate()
        arg_utils = _StarCraft2ArgUtils(list(race_choices))
        match_config_service = _StarCraft2MatchConfigService(
            config_manager,
            plugin_root,
            runtime_downloader,
            arg_utils,
        )
        event_bus = StarCraft2EventBus()
        engine_event_service = StarCraft2EngineEventService(
            state,
            event_bus=event_bus,
        )
        ladder_proxy_event_service = StarCraft2LadderProxyEventService(
            engine_event_service,
            observation_tracker,
            event_bus=event_bus,
        )
        local_match_service = StarCraft2LocalMatchService(
            arg_utils,
            match_config_service,
            local_match_command_template,
            ladder_proxy,
            line_callback=ladder_proxy_event_service.on_ladder_proxy_line,
            event_bus=event_bus,
            runtime_context=runtime_context,
        )
        facade_service = StarCraft2FacadeService(
            config_manager,
            engine_registry,
            state,
            ladder_proxy,
            match_config_service,
            engine_event_service,
            local_match_service=local_match_service,
            event_bus=event_bus,
            runtime_context=runtime_context,
        )
        return StarCraft2RuntimeBundle(
            config_manager=config_manager,
            engine_registry=engine_registry,
            state=state,
            runtime_context=runtime_context,
            ladder_proxy=ladder_proxy,
            runtime_downloader=runtime_downloader,
            observation_tracker=observation_tracker,
            local_match_command_template=local_match_command_template,
            arg_utils=arg_utils,
            match_config_service=match_config_service,
            event_bus=event_bus,
            engine_event_service=engine_event_service,
            local_match_service=local_match_service,
            facade_service=facade_service,
            ladder_proxy_event_service=ladder_proxy_event_service,
        )
