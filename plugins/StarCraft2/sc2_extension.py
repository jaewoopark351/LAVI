#20260708_kpopmodder: Added passive Changeling/ProBots observer extension for SC2 log commentary.
from __future__ import annotations

import copy
import json
import os
from typing import Any, Dict, Optional

from app_core.extensions import GameExtensionInterface
from app_core.extensions.game_extension_context import GameExtensionContext
from core.logger import log_print

from .probots_launcher import ProBotsLauncher
from .probots_log_watcher import ProBotsLogWatcher
from .sc2_event_parser import SC2EventParser
from .sc2_tts_bridge import SC2TTSBridge


STARCRAFT2_STATUS_EVENT_CALLBACK_RESOURCE = "starcraft2_status_event_callback"
STARCRAFT2_LOG_EVENT_ORIGIN = "starcraft2_log_observer"
SHARED_STATUS_EVENT_CATEGORIES = {"upgrade", "strategy"}
SHARED_LOG_ONLY_CATEGORIES = {"build", "train"}
GENERIC_GAME_END_MESSAGE = "내가 경기를 종료했어요. 결과 로그를 확인할게요."


# #20260712_kpopmodder: LAN Lobby remote-human archived code is kept commented
# # out for maintenance safety. Do not re-enable without an explicit LAN revive.
# LAN_LOBBY_ARCHIVED_ERROR = "lan_lobby_archived"
# LAN_LOBBY_ARCHIVED_MESSAGE = (
#     "LAN Lobby remote-human is archived/disabled. "
#     "Use Local Match or explicitly re-enable the experiment before testing."
# )


DEFAULT_CONFIG: Dict[str, Any] = {
    "enabled": False,
    "sc2aiapp_path": "",
    "probots_app_path": "",
    "starcraft2_exe_path": "",
    "starcraft2_support64_path": "",
    "starcraft2_base_path": "",
    "starcraft2_install_path": "C:\\Program Files (x86)\\StarCraft II",
    "maps_path": "",
    "preferred_bot": "Changeling",
    "preferred_map": "PersephoneLE.SC2Map",
    "auto_launch_probots": False,
    "kill_existing_processes_before_launch": False,
    "log_paths": [],
    "watch_stdout": True,
    "speak_events": True,
#     "lan_lobby": {
#         "enabled": False,
#         "archived": True,
#         "notes": "20260712_kpopmodder: LAN Lobby remote-human is archived/disabled; keep as reference only unless explicitly revived.",
#         "room_name": "LAV StarCraft II",
#         "player_name": "LAV",
#         "mode": "observer",
#         "discovery_port": 47624,
#         "join_port": 47625,
#         "broadcast_addresses": ["255.255.255.255"],
#         "announce_interval_sec": 2.0,
#         "room_ttl_sec": 10.0,
#         "proxy_host": "",
#         "proxy_ports": [5677, 5678],
#         "start_port": 5690,
#         "human_client_port": 5679,
#         "remote_start_port": 47626,
#         "lan_connect_mode": "relay",
#         "lan_port_layout": "s2client-api-shared",
#         "multiplayer_relay_enabled": True,
#         "multiplayer_relay_bind_host": "",
#         "multiplayer_relay_ports": [],
#         "auto_start_scan": False,
#         "auto_host_room": False,
#     },
#     "ladder_proxy": {
#         "enabled": False,
#         "executable_path": "",
#         "working_directory": "",
#         "args": [],
#         "ports": [5677, 5678],
#         "check_hosts": ["127.0.0.1"],
#         "connect_timeout_sec": 0.5,
#         "capture_output": True,
#         "auto_start_with_lan_host": False,
#         "starcraft2_exe_path": "",
#         "starcraft2_support64_path": "",
#         "starcraft2_base_path": "",
#     },
}


class StarCraft2Extension(GameExtensionInterface):
    """Observe ProBots/Changeling logs and write commentary; never controls SC2."""

    def __init__(self, plugin_root: Optional[str] = None, config_path: Optional[str] = None):
        self.plugin_root = plugin_root or os.path.dirname(os.path.abspath(__file__))
        self.config_path = config_path or os.path.join(
            self.plugin_root,
            "config_starcraft2.json",
        )
        self.config = self._load_config()
        self.launcher = ProBotsLauncher()
        self.log_watcher = ProBotsLogWatcher()
        self.parser = SC2EventParser(bot_name=str(self.config.get("preferred_bot") or "Changeling"))
        self.tts_bridge = SC2TTSBridge(enabled=bool(self.config.get("speak_events", True)))
        self._context: Optional[GameExtensionContext] = None
        self._is_initialized = False
        self._is_started = False
        self._last_status_message = "created"
        self._status_event_callback = None

    @property
    def name(self) -> str:
        return "starcraft2_changeling_observer"

    def initialize(self, context: GameExtensionContext) -> None:
        self._context = context
        self.tts_bridge.set_tts(getattr(context, "tts", None))
        self._refresh_status_event_callback()
        self._is_initialized = True
        self._last_status_message = "initialized"

    def start(self) -> None:
        if self._is_started:
            return
        self.config = self._load_config()
        self.parser = SC2EventParser(bot_name=str(self.config.get("preferred_bot") or "Changeling"))
        self.tts_bridge.enabled = bool(self.config.get("speak_events", True))
        self._refresh_status_event_callback()

        if not bool(self.config.get("enabled", False)):
            self._last_status_message = "disabled_in_config"
            return

        if bool(self.config.get("auto_launch_probots", False)):
            launch_result = self._launch_sc2aiapp()
            if not launch_result.get("ok"):
                self._last_status_message = str(launch_result.get("error", "probots_launch_failed"))
                log_print(f"[StarCraft2Extension] ProBots launch skipped/failed: {self._last_status_message}")

        self.log_watcher.start(
            self._resolved_log_paths(),
            self._on_log_line,
        )
        #20260712_kpopmodder: LAN Lobby auto-start is archived. Keep the old
        # implementation commented for reference so normal observer startup
        # cannot reopen discovery sockets during maintenance.
        # lan_config = self._lan_config()
        # if bool(lan_config.get("auto_start_scan", False)):
        #     self.lan_discovery.start_scan()
        # if bool(lan_config.get("auto_host_room", False)):
        #     self.lan_discovery.start_host(self._lan_room_info())
        self._is_started = True
        self._last_status_message = "watching_logs"

    def stop(self) -> None:
        self.log_watcher.stop()
        self.launcher.stop()
        self._is_started = False
        self._last_status_message = "stopped"

    def handle_command(self, command: Any) -> Any:
        action = self._action(command)
        if action in {"start", "launch"}:
            self.start()
            return {"ok": True, "action": action, "status": self.get_status()}
        if action in {"launch_sc2aiapp", "launch_probots", "start_sc2aiapp"}:
            self.config = self._load_config()
            result = self._launch_sc2aiapp()
            return {"ok": bool(result.get("ok")), "action": action, "result": result}
        if action in {"validate_paths", "validate_sc2aiapp"}:
            self.config = self._load_config()
            return {
                "ok": True,
                "action": action,
                "validation": self.launcher.validate_sc2aiapp_config(self.config),
            }
        if action == "stop":
            self.stop()
            return {"ok": True, "action": action, "status": self.get_status()}
        if action in {"status", "get_status"}:
            return {"ok": True, "action": action, "status": self.get_status()}
        if action == "reload":
            self.config = self._load_config()
            return {"ok": True, "action": action, "config": copy.deepcopy(self.config)}
#         if action in {"lan_start_scan", "scan_lan_rooms", "start_lan_scan"}:
#             #20260712_kpopmodder: LAN Lobby commands are archived and should not
#             # open sockets during routine maintenance.
#             return self._lan_lobby_archived_result(action)
#         if action in {"lan_stop_scan", "stop_lan_scan"}:
#             return self._lan_lobby_archived_result(action)
#         if action in {"lan_host_room", "host_lan_room", "announce_lan_room"}:
#             #20260712_kpopmodder: The original LAN host-room implementation is
#             # left below as reference only and is intentionally unreachable.
#             return self._lan_lobby_archived_result(action)
#         if action in {"lan_stop_host", "stop_lan_host"}:
#             return self._lan_lobby_archived_result(action)
#         if action in {"lan_rooms", "get_lan_rooms", "lan_status"}:
#             return {
#                 "ok": False,
#                 "action": action,
#                 "error": LAN_LOBBY_ARCHIVED_ERROR,
#                 "message": LAN_LOBBY_ARCHIVED_MESSAGE,
#                 "status": {"archived": True},
#             }
#         if action in {"ladder_proxy_start", "start_ladder_proxy"}:
#             #20260712_kpopmodder: This extension ladder_proxy path belongs to
#             # the archived LAN Lobby experiment, not Local Match.
#             return self._lan_lobby_archived_result(action)
#         if action in {"ladder_proxy_stop", "stop_ladder_proxy"}:
#             return self._lan_lobby_archived_result(action)
#         if action in {"ladder_proxy_status", "check_ladder_proxy", "check_proxy_ports"}:
#             self.config = self._load_config()
#             return {
#                 "ok": False,
#                 "action": action,
#                 "error": LAN_LOBBY_ARCHIVED_ERROR,
#                 "message": LAN_LOBBY_ARCHIVED_MESSAGE,
#                 "status": {"archived": True},
#             }
        return {"ok": False, "action": action, "error": "unknown_action"}

#     def _lan_lobby_archived_result(self, action: str) -> Dict[str, Any]:
#         result = {
#             "ok": False,
#             "action": str(action or "lan_lobby"),
#             "error": LAN_LOBBY_ARCHIVED_ERROR,
#             "message": LAN_LOBBY_ARCHIVED_MESSAGE,
#         }
#         log_print(f"[StarCraft2Extension] LAN Lobby archived action blocked: {result}")
#         return result

    def get_status(self) -> Dict[str, Any]:
        return {
            "name": self.name,
            "initialized": self._is_initialized,
            "started": self._is_started,
            "enabled": bool(self.config.get("enabled", False)),
            "preferred_bot": str(self.config.get("preferred_bot") or "Changeling"),
            "last_status_message": self._last_status_message,
            "path_validation": self.launcher.validate_sc2aiapp_config(self.config),
            "probots": self.launcher.get_status(),
            "log_watcher": self.log_watcher.get_status(),
            "tts_bridge": self.tts_bridge.get_status(),
#             "lan_lobby_archived": {
#                 "disabled": True,
#                 "error": LAN_LOBBY_ARCHIVED_ERROR,
#                 "message": LAN_LOBBY_ARCHIVED_MESSAGE,
#             },
        }

    def _on_process_line(self, stream_name: str, line: str) -> None:
        if not bool(self.config.get("watch_stdout", True)):
            return
        self._handle_raw_line(f"probots:{stream_name}", line)

    def _on_ladder_proxy_line(self, stream_name: str, line: str) -> None:
        text = str(line or "").strip()
        if text:
            log_print(f"[StarCraft2Extension] ladder_proxy {stream_name}: {text[:1000]}")
            #20260710_kpopmodder: Feed direct local-match proxy output into
            # the same parser/TTS path as Changeling and ProBots log files.
            self._handle_raw_line(f"ladder_proxy:{stream_name}", text)

    def _on_log_line(self, log_path: str, line: str) -> None:
        self._handle_raw_line(log_path, line)

    def _handle_raw_line(self, source: str, line: str) -> None:
        #20260710_kpopmodder: Use the active bot name in spoken commentary
        # so Terran, Protoss/Sharky, and Zerg/Changeling all reach TTS with
        # the correct speaker identity.
        source_text = f"{source} {line}".lower()
        for bot_name in ("BenBotBC", "SharkyLAVBot", "sharkbot", "changeling"):
            if bot_name.lower() in source_text:
                self.parser.bot_name = bot_name
                break
        event = self.parser.parse_event(line)
        if event is None:
            return
        log_print(
            "[StarCraft2LogCommentary] "
            f"source={source} category={event.category} message={event.message}"
        )
        category = str(event.category or "").strip().lower()
        status_event_callback = self._refresh_status_event_callback()
        if callable(status_event_callback):
            if category in SHARED_STATUS_EVENT_CATEGORIES:
                if self._publish_status_event(status_event_callback, source, event):
                    return
            elif category in SHARED_LOG_ONLY_CATEGORIES:
                #20260711_kpopmodder: Unit/build log lines overlap the main
                # ResponseObservation telemetry, so keep them diagnostic-only
                # whenever the shared status-event path is active.
                return
        if str(event.message or "").strip() == GENERIC_GAME_END_MESSAGE:
            self.tts_bridge.cancel_pending(reason="starcraft2_log_game_ended")
        if self._should_log_only_event(event):
            return
        result = self.tts_bridge.speak(event.message)
        if not result.get("ok"):
            log_print(
                "[StarCraft2Extension] TTS bridge did not speak event "
                f"source={source} error={result.get('error', '')}"
            )

    def _should_log_only_event(self, event) -> bool:
        #20260710_kpopmodder: Only startup/log-intro notices stay silent; live SC2 commentary should speak.
        category = str(getattr(event, "category", "") or "").strip().lower()
        message = str(getattr(event, "message", "") or "")
        if category == "game_started":
            return True
        if message.strip() == GENERIC_GAME_END_MESSAGE:
            return True
        return "로그 해설" in message

    def _refresh_status_event_callback(self):
        get_shared = getattr(self._context, "get_shared", None)
        callback = (
            get_shared(STARCRAFT2_STATUS_EVENT_CALLBACK_RESOURCE)
            if callable(get_shared)
            else None
        )
        self._status_event_callback = callback if callable(callback) else None
        return self._status_event_callback

    def _publish_status_event(self, callback, source: str, event) -> bool:
        category = str(getattr(event, "category", "") or "").strip().lower()
        payload = {
            "event_type": category,
            "details": {
                "origin": STARCRAFT2_LOG_EVENT_ORIGIN,
                "category": category,
                "message": str(getattr(event, "message", "") or ""),
                "source": str(source or ""),
                "raw_line": str(getattr(event, "raw_line", "") or ""),
                "speak": bool(self.tts_bridge.enabled),
            },
        }
        try:
            #20260711_kpopmodder: Route strategy/upgrade commentary through
            # the main StarCraft2 callback so memory and TTS share one event.
            callback(payload)
            return True
        except Exception as e:
            log_print(
                "[StarCraft2Extension] shared status callback failed "
                f"category={category}: {e}"
            )
            return False

    def _launch_sc2aiapp(self) -> Dict[str, Any]:
        return self.launcher.start_sc2aiapp(
            self.config,
            capture_output=bool(self.config.get("watch_stdout", True)),
            line_callback=self._on_process_line,
        )

    def _load_config(self) -> Dict[str, Any]:
        config = copy.deepcopy(DEFAULT_CONFIG)
        if not os.path.isfile(self.config_path):
            return config
        try:
            with open(self.config_path, "r", encoding="utf-8") as file:
                loaded = json.load(file)
        except Exception as e:
            log_print(f"[StarCraft2Extension] config load failed: {e}")
            return config
        if isinstance(loaded, dict):
            config.update(copy.deepcopy(loaded))
        return config

    def _resolved_log_paths(self):
        resolved = []
        seen = set()

        def append_path(path):
            normalized = os.path.normpath(path)
            key = os.path.normcase(normalized)
            if key in seen:
                return
            seen.add(key)
            resolved.append(normalized)

        for value in self.config.get("log_paths", []):
            text = str(value or "").strip().strip("\"'")
            if not text:
                continue
            text = os.path.expandvars(os.path.expanduser(text))
            if not os.path.isabs(text):
                text = os.path.join(self.plugin_root, text)
            append_path(text)
        #20260711_kpopmodder: Always observe the bot logs produced by the
        # repo-local Ladder Proxy runtime while preserving configured paths.
        preferred_bot = str(self.config.get("preferred_bot") or "Changeling").strip()
        runtime_data_path = os.path.join(
            self.plugin_root,
            "runtime",
            "Bots",
            preferred_bot,
            "data",
        )
        append_path(os.path.join(runtime_data_path, "stdout.log"))
        append_path(os.path.join(runtime_data_path, "stderr.log"))
        return resolved

#     def _lan_config(self) -> Dict[str, Any]:
#         value = self.config.get("lan_lobby", {})
#         return copy.deepcopy(value) if isinstance(value, dict) else {}

    def _ladder_proxy_config(self) -> Dict[str, Any]:
        value = self.config.get("ladder_proxy", {})
        config = copy.deepcopy(value) if isinstance(value, dict) else {}
        for source_key, target_key in (
            ("starcraft2_exe_path", "starcraft2_exe_path"),
            ("starcraft2_support64_path", "starcraft2_support64_path"),
            ("starcraft2_base_path", "starcraft2_base_path"),
        ):
            if not config.get(target_key) and self.config.get(source_key):
                config[target_key] = self.config.get(source_key)
        return config

#     def _lan_room_info(self, command: Any = None) -> Dict[str, Any]:
#         #20260712_kpopmodder: LAN Lobby room metadata source is archived.
#         return self._lan_lobby_archived_result("lan_room_info")

    def _action(self, command: Any) -> str:
        if isinstance(command, dict):
            return str(command.get("action") or command.get("type") or "").strip().lower()
        return str(command or "").strip().lower()
