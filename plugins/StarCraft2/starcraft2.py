#20260707_kpopmodder: Added optional StarCraft2 facade for Windows-first engine adapter control.
from __future__ import annotations

import json
import os
from typing import Any, Dict, Optional

import gradio as gr

from core.logger import log_print
from plugins.StarCraft2.starcraft2_core.starcraft2_arg_utils import _StarCraft2ArgUtils
from plugins.StarCraft2.starcraft2_core.starcraft2_config import StarCraft2Config
from plugins.StarCraft2.starcraft2_core.starcraft2_engine_registry import (
    StarCraft2EngineRegistry,
)
from plugins.StarCraft2.starcraft2_core.starcraft2_event_bus import _StarCraft2EventBus
from plugins.StarCraft2.starcraft2_core.starcraft2_event_service import (
    _StarCraft2EngineEventService,
    _StarCraft2LadderProxyEventService,
)
from plugins.StarCraft2.starcraft2_core.starcraft2_facade_service import (
    _StarCraft2FacadeService,
)
from plugins.StarCraft2.starcraft2_core.starcraft2_state import (
    StarCraft2RuntimeState,
)
from plugins.StarCraft2.starcraft2_core.sc2_local_match_command_template import (
    _LocalMatchCommandTemplate,
)
from plugins.StarCraft2.starcraft2_core.sc2_ladder_proxy_launcher import SC2LadderProxyLauncher
from plugins.StarCraft2.starcraft2_core.starcraft2_match_config_service import _StarCraft2MatchConfigService
from plugins.StarCraft2.starcraft2_core.starcraft2_observation_tracker import (
    SC2ObservationTracker,
)
from plugins.StarCraft2.starcraft2_core.starcraft2_local_match_service import _StarCraft2LocalMatchService
from plugins.StarCraft2.starcraft2_core.starcraft2_runtime_downloader import StarCraft2RuntimeDownloader
from plugins.StarCraft2.starcraft2_core.starcraft2_ui_sections import (
    _StarCraft2BotEngineSection,
    _StarCraft2LocalMatchSection,
)


SC2_RACE_CHOICES = ["Terran", "Zerg", "Protoss", "Random"]

# #20260712_kpopmodder: LAN Lobby remote-human archived code is kept commented
# # out for maintenance safety. Do not re-enable without an explicit LAN revive.
# LAN_LOBBY_ARCHIVED_ERROR = "lan_lobby_archived"
# LAN_LOBBY_ARCHIVED_MESSAGE = (
#     "LAN Lobby remote-human is archived/disabled. "
#     "Use Local Match or explicitly re-enable the experiment before testing."
# )
#
#
# class _NoopLock:
#     #20260712_kpopmodder: Minimal lock-shaped object for archived LAN Lobby
#     # compatibility paths; it never starts threads or sockets.
#     def __enter__(self):
#         return self
#
#     def __exit__(self, exc_type, exc, traceback):
#         return False
#
#
# class _ArchivedLanDiscoveryState:
#     #20260712_kpopmodder: Keeps old Local Match tests and defensive code from
#     # crashing after LAN Lobby source was commented out.
#     def __init__(self):
#         self._joined_lock = _NoopLock()
#         self._joined_players: Dict[str, Dict[str, Any]] = {}
#
#     def rooms(self):
#         return []
#
#     def stop(self):
#         return {"ok": False, "error": LAN_LOBBY_ARCHIVED_ERROR}
#
#     def get_status(self):
#         return {
#             "archived": True,
#             "error": LAN_LOBBY_ARCHIVED_ERROR,
#             "joined_players": list(self._joined_players.values()),
#         }


class StarCraft2:
    #20260707_kpopmodder: Facade owns UI/config; engines own frame-level game control.
    def __init__(self):
        self.plugin_root = os.path.dirname(__file__)
        self.config_manager = StarCraft2Config(self.plugin_root)
        self.engine_registry = StarCraft2EngineRegistry()
        self.state = StarCraft2RuntimeState(
            engine=str(self.config_manager.get("engine", "internal_lav_bot")),
            map_name=str(self.config_manager.get("map_name", "")),
            race=str(self.config_manager.get("race", "Terran")),
            enemy_race=str(self.config_manager.get("enemy_race", "Zerg")),
            enemy_difficulty=str(self.config_manager.get("enemy_difficulty", "Easy")),
        )
        self.status_event_callback = None
        self.tts = None
        self.current_engine = None
        self.last_start_result: Dict[str, Any] = {}
        self.last_stop_result: Dict[str, Any] = {}
        self._shutdown = False
        # self.lan_discovery = _ArchivedLanDiscoveryState()
        self.ladder_proxy = SC2LadderProxyLauncher()
        self.runtime_downloader = StarCraft2RuntimeDownloader()
        self.observation_tracker = SC2ObservationTracker()
        self._local_match_command_template = _LocalMatchCommandTemplate()
        self._arg_utils = _StarCraft2ArgUtils(SC2_RACE_CHOICES)
        self._match_config_service = _StarCraft2MatchConfigService(
            self.config_manager,
            self.plugin_root,
            self.runtime_downloader,
            self._arg_utils,
        )
        self._event_bus = _StarCraft2EventBus()
        self._engine_event_service = _StarCraft2EngineEventService(
            self.state,
            event_bus=self._event_bus,
        )
        self._facade_service = _StarCraft2FacadeService(
            self.config_manager,
            self.engine_registry,
            self.state,
            self.ladder_proxy,
            self._match_config_service,
            self._engine_event_service,
            event_bus=self._event_bus,
        )
        self._ladder_proxy_event_service = _StarCraft2LadderProxyEventService(
            self._engine_event_service,
            self.observation_tracker,
            event_bus=self._event_bus,
        )
        self._local_match_service = _StarCraft2LocalMatchService(
            self._arg_utils,
            self._match_config_service,
            self._local_match_command_template,
            self.ladder_proxy,
            line_callback=self._on_ladder_proxy_line,
        )

    def create_ui(self):
        config = self.config_manager.snapshot()
        with gr.Tab("StarCraft2"):
            self.config_status_box = gr.Textbox(
                label="Config Status",
                value=self.config_manager.config_message(),
                lines=3,
                interactive=False,
            )
            self.enabled_box = gr.Checkbox(
                label="Enabled",
                value=bool(config.get("enabled", False)),
            )
            with gr.Row():
                self.starcraft2_path_box = gr.Textbox(
                    label="StarCraft II Path",
                    value=self.config_manager.resolve_starcraft2_path(),
                    lines=1,
                )
                self.sc2path_box = gr.Textbox(
                    label="SC2PATH",
                    value=self.config_manager.sc2path_env(),
                    lines=1,
                    interactive=False,
                )
            bot_section = _StarCraft2BotEngineSection(self, SC2_RACE_CHOICES)
            local_match_section = _StarCraft2LocalMatchSection(self, SC2_RACE_CHOICES)
            bot_section.build(config)
            local_match_section.build(config)
            bot_section.bind()
            local_match_section.bind()

    def start(self, config_overrides: Optional[Dict[str, Any]] = None, launch_source="manual"):
        result = self._facade_service.start(config_overrides, launch_source)
        self.current_engine = self._facade_service.current_engine
        self.last_start_result = self._facade_service.last_start_result
        return result

    def stop(self):
        result = self._facade_service.stop()
        self.current_engine = self._facade_service.current_engine
        self.last_stop_result = self._facade_service.last_stop_result
        return result

    def shutdown(self):
        self._facade_service.shutdown()

    def get_status(self):
        return self._facade_service.get_status()

    def set_status_event_callback(self, callback):
        self.status_event_callback = callback
        self._facade_service.set_status_event_callback(callback)

    def set_tts(self, tts):
        #20260710_kpopmodder: Keep a direct TTS fallback for local-match
        # lifecycle events when the reaction callback is unavailable.
        self.tts = tts
        self._facade_service.set_tts(tts)

    def is_running(self) -> bool:
        return self._facade_service.is_running()

    def on_start_click(
        self,
        enabled,
        starcraft2_path,
        map_name,
        race,
        enemy_race,
        enemy_difficulty,
        realtime,
        engine,
        external_exe_path,
        micromachine_path,
        ares_sc2_script_path,
        external_jar_path,
    ):
        overrides = {
            "enabled": bool(enabled),
            "starcraft2_path": str(starcraft2_path or ""),
            "map_name": str(map_name or "AbyssalReefLE"),
            "race": str(race or "Terran"),
            "enemy_race": str(enemy_race or "Zerg"),
            "enemy_difficulty": str(enemy_difficulty or "Easy"),
            "realtime": bool(realtime),
            "engine": str(engine or "internal_lav_bot"),
            "external_exe": {"path": str(external_exe_path or "")},
            "micromachine": {"path": str(micromachine_path or "")},
            "ares_sc2": {"script_path": str(ares_sc2_script_path or "")},
            "external_jar": {"jar_path": str(external_jar_path or "")},
        }
        result = self.start(overrides)
        return self._ui_values(result)

    def on_stop_click(self):
        result = self.stop()
        return self._ui_values(result)

    def on_status_click(self):
        return self._ui_values()

#     def on_lan_host_click(
#         self,
#         room_name,
#         player_name,
#         preferred_bot,
#         discovery_port,
#         proxy_host,
#         proxy_ports,
#         map_name,
#     ):
#         #20260712_kpopmodder: LAN Lobby source is fully archived; do not host,
#         # scan, serve maps, start relays, or launch LAN ladder proxies.
#         return self._lan_lobby_archived_ui("host_lan_room")
#
#     def on_lan_scan_click(
#         self,
#         room_name,
#         player_name,
#         preferred_bot,
#         discovery_port,
#         proxy_host,
#         proxy_ports,
#         map_name,
#     ):
#         #20260712_kpopmodder: LAN Lobby source is archived; scan is disabled.
#         return self._lan_lobby_archived_ui("scan_lan_rooms")
#
#     def on_lan_stop_click(self):
#         #20260712_kpopmodder: LAN Lobby source is archived; this intentionally
#         # does not stop the shared Local Match ladder proxy.
#         return self._lan_lobby_archived_ui("stop_lan")
#
#     def on_lan_status_click(self):
#         return self._lan_rooms_json(), self._lan_status_json()

    def on_local_match_race_change(self, race, args):
        return self._local_match_service.on_local_match_race_change(race, args)

    def on_local_match_ai_race_change(self, ai_race, args):
        return self._local_match_service.on_local_match_ai_race_change(ai_race, args)

    def on_local_human_vs_changeling_click(
        self,
        executable_path,
        working_directory,
        args,
        proxy_ports,
        ai_race=None,
    ):
        return self._local_match_service.on_local_human_vs_changeling_click(
            executable_path,
            working_directory,
            args,
            proxy_ports,
            ai_race=ai_race,
        )

    def on_local_match_stop_click(self):
        return self._local_match_service.on_local_match_stop_click()

    def on_local_match_status_click(
        self,
        executable_path,
        working_directory,
        args,
        proxy_ports,
    ):
        return self._local_match_service.on_local_match_status_click(
            executable_path,
            working_directory,
            args,
            proxy_ports,
        )

#     def on_ladder_proxy_start_click(
#         self,
#         executable_path,
#         working_directory,
#         args,
#         proxy_host,
#         proxy_ports,
#     ):
#         #20260712_kpopmodder: LAN Lobby ladder/proxy launch source is archived.
#         return self._lan_lobby_archived_ui("start_ladder_proxy")
#
#     def on_ladder_proxy_stop_click(self):
#         #20260712_kpopmodder: LAN Lobby source is archived; this intentionally
#         # does not stop the shared Local Match ladder proxy.
#         return self._lan_lobby_archived_ui("stop_ladder_proxy")
#
#     def on_ladder_proxy_check_click(
#         self,
#         executable_path,
#         working_directory,
#         args,
#         proxy_host,
#         proxy_ports,
#     ):
#         #20260712_kpopmodder: LAN Lobby proxy checks are archived.
#         return self._lan_lobby_archived_ui("check_proxy_ports")

    def _handle_engine_event(self, event):
        self._facade_service.handle_engine_event(event)

#     def _on_lan_ladder_proxy_exit(self, event: Dict[str, Any]) -> None:
#         #20260712_kpopmodder: LAN Lobby exit handling is archived.
#         log_print("[StarCraft2] LAN Lobby exit callback ignored because LAN Lobby is archived.")
#
#     def _summarize_lan_relay_snapshot(self, snapshot: Dict[str, Any]) -> Dict[str, Any]:
#         #20260712_kpopmodder: LAN relay snapshot summarization is archived.
#         return {"archived": True, "exit": dict(snapshot or {})}

    def _on_ladder_proxy_line(self, stream_name: str, line: str) -> None:
        self._ladder_proxy_event_service.on_ladder_proxy_line(
            stream_name,
            line,
            status_event_callback=self.status_event_callback,
            tts=self.tts,
        )

    def _is_ladder_proxy_error_line(self, lower_line: str) -> bool:
        return self._ladder_proxy_event_service.is_ladder_proxy_error_line(lower_line)

#     def _maybe_request_remote_native_joiner(self, line: str) -> None:
#         #20260712_kpopmodder: Remote-native JoinGame source is archived.
#         if "REMOTE_NATIVE_JOINER_READY" in str(line or ""):
#             log_print("[StarCraft2] remote native joiner ignored because LAN Lobby is archived.")
#
#     def _remote_native_joiner_start_worker(self, config: Dict[str, Any]) -> None:
#         #20260712_kpopmodder: Remote-native JoinGame source is archived.
#         log_print("[StarCraft2] remote native joiner worker ignored because LAN Lobby is archived.")
#
#     def _request_remote_native_joiner_start(self, config: Dict[str, Any]) -> Dict[str, Any]:
#         #20260712_kpopmodder: Remote-native JoinGame source is archived.
#         return self._lan_lobby_archived_result("remote_native_joiner_start")

    def _sync_state_from_engine(self):
        self._facade_service.sync_state_from_engine()

    def _facade_result(self, ok: bool, error=None, status=None):
        return self._facade_service._facade_result(ok, error, status)

    def _ui_values(self, result=None):
        status = self.get_status()
        last_event = status.get("state", {}).get("last_event") or {}
        last_error = (
            (result or {}).get("error")
            if isinstance(result, dict)
            else None
        ) or status.get("state", {}).get("last_error") or ""
        return (
            self.config_manager.config_message(),
            self._status_json(status),
            json.dumps(last_event, ensure_ascii=False, indent=2, default=str),
            str(last_error or ""),
        )

    def _status_json(self, status=None):
        return self._facade_service.status_json(status)

#     def _configure_lan_discovery(self, discovery_port):
#         #20260712_kpopmodder: LAN Lobby discovery source is archived.
#         return self._lan_lobby_archived_result("configure_lan_discovery")

    def _ladder_proxy_config(
        self,
        executable_path=None,
        working_directory=None,
        args=None,
        proxy_host=None,
        proxy_ports=None,
    ):
        return self._match_config_service.ladder_proxy_config(
            executable_path=executable_path,
            working_directory=working_directory,
            args=args,
            proxy_host=proxy_host,
            proxy_ports=proxy_ports,
        )

    def _local_match_config(
        self,
        executable_path=None,
        working_directory=None,
        args=None,
        proxy_ports=None,
        keep_local_match_identity_args: bool = False,
    ) -> Dict[str, Any]:
        return self._match_config_service.local_match_config(
            executable_path=executable_path,
            working_directory=working_directory,
            args=args,
            proxy_ports=proxy_ports,
            keep_local_match_identity_args=keep_local_match_identity_args,
        )

    def _ensure_local_match_runtime(self, config: Dict[str, Any]) -> Dict[str, Any]:
        return self._match_config_service.ensure_local_match_runtime(config)

    def _same_path(self, left: str, right: str) -> bool:
        return self._arg_utils.same_path(left, right)

    def _config_bool(self, value: Any, default: bool = False) -> bool:
        return self._arg_utils.config_bool(value, default=default)

#     def _lan_room_info(
#         self,
#         room_name="",
#         player_name="",
#         preferred_bot="",
#         proxy_host="",
#         proxy_ports="",
#         map_name="",
#     ):
#         #20260712_kpopmodder: LAN Lobby room metadata source is archived.
#         return self._lan_lobby_archived_result("lan_room_info")
#
#     def _prepare_lan_map_download(
#         self,
#         room_info: Dict[str, Any],
#         ladder_proxy_config: Dict[str, Any],
#     ) -> Dict[str, Any]:
#         #20260712_kpopmodder: LAN map serving source is archived.
#         return self._lan_lobby_archived_result("prepare_lan_map_download")
#
#     def _resolve_sc2_map_file_path(self, map_name: str) -> str:
#         value = str(map_name or "").strip().strip('"')
#         if not value:
#             return ""
#         if os.path.isabs(value):
#             return value
#         return os.path.join(self.config_manager.resolve_starcraft2_path(), "Maps", value)
#
#     def _map_name_from_ladder_args(self, args) -> str:
#         normalized = self._normalize_ladder_args(args)
#         for index, arg in enumerate(normalized):
#             text = str(arg or "").strip()
#             if text == "--map" and index + 1 < len(normalized):
#                 return str(normalized[index + 1] or "").strip()
#             if text.startswith("--map="):
#                 return text.split("=", 1)[1].strip()
#         return ""
#
#     def _request_remote_human_start(self, config: Dict[str, Any]) -> Dict[str, Any]:
#         #20260712_kpopmodder: Remote human startup source is archived.
#         return self._lan_lobby_archived_result("remote_human_start")
#
#     def _start_lan_multiplayer_relay(self, config: Dict[str, Any]) -> Dict[str, Any]:
#         #20260712_kpopmodder: LAN multiplayer relay source is archived.
#         return self._lan_lobby_archived_result("start_lan_multiplayer_relay")
#
#     def _apply_remote_human_args(self, config: Dict[str, Any]) -> None:
#         #20260712_kpopmodder: Remote human arg injection source is archived.
#         config["remote_human_enabled"] = False
#
#     def _select_remote_human_player(self) -> Optional[Dict[str, Any]]:
#         #20260712_kpopmodder: Remote human selection source is archived.
#         return None
#
#     def _remote_human_client_port(
#         self,
#         config: Dict[str, Any],
#         player: Optional[Dict[str, Any]] = None,
#     ) -> int:
#         #20260712_kpopmodder: Remote human client port source is archived.
#         return 0

    def _float_config_value(self, value: Any, default: float) -> float:
        return self._arg_utils.float_config_value(value, default)

    def _normalize_ladder_args(self, value: Any) -> list[str]:
        return self._arg_utils.normalize_ladder_args(value)

    def _has_arg(self, args: list[str], name: str) -> bool:
        return self._arg_utils.has_arg(args, name)

    def _ladder_arg_value(self, args: list[str], name: str, fallback: str = "") -> str:
        return self._arg_utils.ladder_arg_value(args, name, fallback=fallback)

    def _normalize_sc2_race(self, value: Any, fallback: str = "Random") -> str:
        return self._arg_utils.normalize_sc2_race(value, fallback=fallback)

    def _local_match_race_from_args(
        self,
        args: Any,
        fallback: str = "Terran",
    ) -> str:
        return self._local_match_service.local_match_race_from_args(
            args,
            fallback=fallback,
        )

    def _local_match_ai_race_from_args(self, args, fallback="Zerg"):
        return self._local_match_service.local_match_ai_race_from_args(
            args,
            fallback=fallback,
        )

#     def _strip_remote_human_args(self, args: list[str]) -> list[str]:
#         return self._strip_ladder_args(
#             args,
#             {
#                 "--remote-human-host",
#                 "--remote-human-client-port",
#                 "--lan-game-host-ip",
#                 "--remote-human-join-mode",
#             },
#         )

    def _strip_local_match_args(self, args: list[str]) -> list[str]:
        return self._arg_utils.strip_local_match_args(args)

    def _strip_ladder_args(self, args: list[str], names: set[str]) -> list[str]:
        return self._arg_utils.strip_ladder_args(args, names)

#     def _lan_rooms_json(self):
#         return json.dumps(
#             [],
#             ensure_ascii=False,
#             indent=2,
#             default=str,
#         )
#
#     def _lan_lobby_archived_result(self, action: str) -> Dict[str, Any]:
#         result = {
#             "ok": False,
#             "action": str(action or "lan_lobby"),
#             "error": LAN_LOBBY_ARCHIVED_ERROR,
#             "message": LAN_LOBBY_ARCHIVED_MESSAGE,
#         }
#         log_print(f"[StarCraft2] LAN Lobby archived action blocked: {result}")
#         return result
#
#     def _lan_lobby_archived_ui(self, action: str):
#         config = self._ladder_proxy_config()
#         config["lan_lobby_archived"] = self._lan_lobby_archived_result(action)
#         return self._lan_rooms_json(), self._lan_status_json(config)

    def _local_match_status_json(self, ladder_proxy_config=None, result=None):
        return self._local_match_service.local_match_status_json(
            ladder_proxy_config=ladder_proxy_config,
            result=result,
        )

#     def _lan_status_json(self, ladder_proxy_config=None):
#         proxy_config = (
#             ladder_proxy_config
#             if isinstance(ladder_proxy_config, dict)
#             else self._ladder_proxy_config()
#         )
#         status = {
#             "lan_lobby_archived": {
#                 "disabled": True,
#                 "error": LAN_LOBBY_ARCHIVED_ERROR,
#                 "message": LAN_LOBBY_ARCHIVED_MESSAGE,
#             },
#             "ladder_proxy": self.ladder_proxy.get_status(proxy_config),
#         }
#         if isinstance(proxy_config, dict):
#             if "remote_human_start" in proxy_config:
#                 status["remote_human_start"] = proxy_config.get("remote_human_start")
#             if "remote_human_start_player" in proxy_config:
#                 status["remote_human_start_player"] = proxy_config.get("remote_human_start_player")
#             if "multiplayer_relay" in proxy_config:
#                 status["multiplayer_relay_start"] = proxy_config.get("multiplayer_relay")
#             if "lan_lobby_archived" in proxy_config:
#                 status["lan_lobby_archived"] = proxy_config.get("lan_lobby_archived")
#         return json.dumps(
#             status,
#             ensure_ascii=False,
#             indent=2,
#             default=str,
#         )
