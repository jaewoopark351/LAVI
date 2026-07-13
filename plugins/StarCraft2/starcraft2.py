#20260707_kpopmodder: Added optional StarCraft2 facade for Windows-first engine adapter control.
from __future__ import annotations

import json
import os
import shlex
import subprocess
from typing import Any, Dict, Optional

import gradio as gr

from core.logger import log_print
from plugins.StarCraft2.starcraft2_core.starcraft2_config import StarCraft2Config
from plugins.StarCraft2.starcraft2_core.starcraft2_engine_registry import (
    StarCraft2EngineRegistry,
)
from plugins.StarCraft2.starcraft2_core.starcraft2_state import (
    StarCraft2RuntimeState,
)
from plugins.StarCraft2.starcraft2_core.sc2_ladder_proxy_launcher import SC2LadderProxyLauncher
from plugins.StarCraft2.bot_launch_profiles import get_bot_launch_profile
from plugins.StarCraft2.starcraft2_core.starcraft2_runtime_downloader import (
    DEFAULT_RUNTIME_REPO_ID,
    DEFAULT_RUNTIME_REPO_TYPE,
    DEFAULT_RUNTIME_REVISION,
    StarCraft2RuntimeDownloader,
)
from plugins.StarCraft2.starcraft2_core.starcraft2_observation_tracker import (
    SC2ObservationTracker,
)


SC2_RACE_CHOICES = ["Terran", "Zerg", "Protoss", "Random"]
#20260710_kpopmodder: Keep Local Match AI selection explicit and deterministic; Random is UI-only until a safe selection policy exists.
LOCAL_MATCH_AI_BY_RACE = {
    "Terran": "BenBotBC",
    "Protoss": "sharkbot",
    "Zerg": "changeling",
}

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
        self.current_engine = None
        self.status_event_callback = None
        self.tts = None
        self.last_start_result: Dict[str, Any] = {}
        self.last_stop_result: Dict[str, Any] = {}
        # self.lan_discovery = _ArchivedLanDiscoveryState()
        self.ladder_proxy = SC2LadderProxyLauncher()
        self.runtime_downloader = StarCraft2RuntimeDownloader()
        self.observation_tracker = SC2ObservationTracker()
        self._shutdown = False

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
            with gr.Accordion("Bot Engine", open=False):
                with gr.Row():
                    self.map_name_box = gr.Textbox(
                        label="Map Name",
                        value=str(config.get("map_name", "AbyssalReefLE")),
                        lines=1,
                    )
                    self.engine_dropdown = gr.Dropdown(
                        label="Engine",
                        choices=self.engine_registry.names(),
                        value=str(config.get("engine", "internal_lav_bot")),
                        interactive=True,
                    )
                with gr.Row():
                    self.race_dropdown = gr.Dropdown(
                        label="Player Race",
                        choices=SC2_RACE_CHOICES,
                        value=str(config.get("race", "Terran")),
                        interactive=True,
                    )
                    self.enemy_race_dropdown = gr.Dropdown(
                        label="Enemy Race",
                        choices=SC2_RACE_CHOICES,
                        value=str(config.get("enemy_race", "Zerg")),
                        interactive=True,
                    )
                    self.enemy_difficulty_dropdown = gr.Dropdown(
                        label="Enemy Difficulty",
                        choices=["VeryEasy", "Easy", "Medium", "MediumHard", "Hard"],
                        value=str(config.get("enemy_difficulty", "Easy")),
                        interactive=True,
                    )
                self.realtime_box = gr.Checkbox(
                    label="Realtime",
                    value=bool(config.get("realtime", False)),
                )
                with gr.Row():
                    self.external_exe_path_box = gr.Textbox(
                        label="External Exe Path",
                        value=str(config.get("external_exe", {}).get("path", "")),
                        lines=1,
                    )
                    self.micromachine_path_box = gr.Textbox(
                        label="MicroMachine Exe Path",
                        value=str(config.get("micromachine", {}).get("path", "")),
                        lines=1,
                    )
                    self.ares_sc2_script_box = gr.Textbox(
                        label="Ares-sc2 Script Path",
                        value=str(config.get("ares_sc2", {}).get("script_path", "")),
                        lines=1,
                    )
                    self.external_jar_path_box = gr.Textbox(
                        label="External Jar Path",
                        value=str(config.get("external_jar", {}).get("jar_path", "")),
                        lines=1,
                    )
                with gr.Row():
                    self.start_button = gr.Button("Start")
                    self.stop_button = gr.Button("Stop")
                    self.status_button = gr.Button("Status")
                self.status_box = gr.Textbox(
                    label="Status",
                    value=self._status_json(),
                    lines=12,
                    interactive=False,
                )
                self.last_event_box = gr.Textbox(
                    label="Last Event",
                    value="",
                    lines=4,
                    interactive=False,
                )
                self.last_error_box = gr.Textbox(
                    label="Last Error",
                    value="",
                    lines=2,
                    interactive=False,
                )
            local_match_config = self.config_manager.get_section("local_match")
            ladder_config = self.config_manager.get_section("ladder_proxy")
            local_match_args = local_match_config.get("args", ladder_config.get("args", []))
            if isinstance(local_match_args, list):
                local_match_args_text = " ".join(str(item) for item in local_match_args)
            else:
                local_match_args_text = str(local_match_args or "")
            with gr.Accordion("Local Match", open=False):
                with gr.Row():
                    self.local_match_exe_path_box = gr.Textbox(
                        label="Local Match Exe Path",
                        value=str(local_match_config.get("executable_path", "")),
                        lines=1,
                    )
                    self.local_match_working_dir_box = gr.Textbox(
                        label="Local Match Working Dir",
                        value=str(local_match_config.get("working_directory", "")),
                        lines=1,
                    )
                with gr.Row():
                    #20260710_kpopmodder: Keep the human race separate from the selectable AI race.
                    self.local_match_race_dropdown = gr.Dropdown(
                        label="Local Human Race",
                        choices=SC2_RACE_CHOICES,
                        value=self._local_match_race_from_args(local_match_args),
                        interactive=True,
                    )
                    self.local_match_ai_race_dropdown = gr.Dropdown(
                        label="Local AI Race",
                        choices=SC2_RACE_CHOICES,
                        value=self._local_match_ai_race_from_args(local_match_args),
                        interactive=True,
                    )
                    self.local_match_ports_box = gr.Textbox(
                        label="Local Proxy Ports",
                        value=",".join(str(port) for port in local_match_config.get("ports", [5677, 5678]))
                        if isinstance(local_match_config.get("ports", [5677, 5678]), list)
                        else str(local_match_config.get("ports", "5677,5678")),
                        lines=1,
                    )
                self.local_match_args_box = gr.Textbox(
                    label="Local Human vs AI Args",
                    value=local_match_args_text,
                    lines=1,
                )
                with gr.Row():
                    self.local_human_vs_changeling_button = gr.Button("Local Human vs AI")
                    self.local_match_stop_button = gr.Button("Stop Local Match")
                    self.local_match_status_button = gr.Button("Local Match Status")
                self.local_match_status_box = gr.Textbox(
                    label="Local Match Status",
                    value=self._local_match_status_json(),
                    lines=8,
                    interactive=False,
                )
            #20260712_kpopmodder: LAN Lobby UI was removed from the live Gradio
            # surface. The archived implementation remains in git history only;
            # keeping fields/buttons here risks accidental execution during
            # maintenance.

            inputs = [
                self.enabled_box,
                self.starcraft2_path_box,
                self.map_name_box,
                self.race_dropdown,
                self.enemy_race_dropdown,
                self.enemy_difficulty_dropdown,
                self.realtime_box,
                self.engine_dropdown,
                self.external_exe_path_box,
                self.micromachine_path_box,
                self.ares_sc2_script_box,
                self.external_jar_path_box,
            ]
            outputs = [
                self.config_status_box,
                self.status_box,
                self.last_event_box,
                self.last_error_box,
            ]
            self.start_button.click(
                fn=self.on_start_click,
                inputs=inputs,
                outputs=outputs,
            )
            self.stop_button.click(
                fn=self.on_stop_click,
                inputs=[],
                outputs=outputs,
                queue=False,
            )
            self.status_button.click(
                fn=self.on_status_click,
                inputs=[],
                outputs=outputs,
                queue=False,
            )
            self.local_match_race_dropdown.change(
                fn=self.on_local_match_race_change,
                inputs=[self.local_match_race_dropdown, self.local_match_args_box],
                outputs=[self.local_match_args_box],
                queue=False,
            )
            self.local_match_ai_race_dropdown.change(
                fn=self.on_local_match_ai_race_change,
                inputs=[self.local_match_ai_race_dropdown, self.local_match_args_box],
                outputs=[self.local_match_args_box],
                queue=False,
            )
            local_match_inputs = [
                self.local_match_exe_path_box,
                self.local_match_working_dir_box,
                self.local_match_args_box,
                self.local_match_ports_box,
            ]
            local_match_start_inputs = local_match_inputs + [
                self.local_match_ai_race_dropdown,
            ]
            self.local_human_vs_changeling_button.click(
                fn=self.on_local_human_vs_changeling_click,
                inputs=local_match_start_inputs,
                outputs=[self.local_match_status_box],
                queue=False,
            )
            self.local_match_stop_button.click(
                fn=self.on_local_match_stop_click,
                inputs=[],
                outputs=[self.local_match_status_box],
                queue=False,
            )
            self.local_match_status_button.click(
                fn=self.on_local_match_status_click,
                inputs=local_match_inputs,
                outputs=[self.local_match_status_box],
                queue=False,
            )

    def start(self, config_overrides: Optional[Dict[str, Any]] = None, launch_source="manual"):
        runtime_config = self.config_manager.build_runtime_config(config_overrides or {})
        if not bool(runtime_config.get("enabled", False)):
            result = self._facade_result(False, "enabled_false")
            self.last_start_result = result
            return result
        if launch_source == "startup" and not bool(runtime_config.get("auto_launch", False)):
            result = self._facade_result(True, None, {"skipped": "auto_launch_false"})
            self.last_start_result = result
            return result

        engine_name = str(runtime_config.get("engine") or "internal_lav_bot")
        if self.current_engine is None or self.current_engine.engine_name != engine_name:
            self.current_engine = self.engine_registry.create(engine_name)
        result = self.current_engine.start(
            runtime_config,
            event_callback=self._handle_engine_event,
        )
        self.last_start_result = result
        self._sync_state_from_engine()
        return result

    def stop(self):
        if self.current_engine is None:
            self.state.mark_stopped("not_running")
            result = self._facade_result(True, None, {"stopped": "not_running"})
            self.last_stop_result = result
            return result
        result = self.current_engine.stop()
        self.last_stop_result = result
        self._sync_state_from_engine()
        return result

    def shutdown(self):
        if self._shutdown:
            return
        self._shutdown = True
        self.ladder_proxy.stop()
        try:
            self.stop()
        except Exception as e:
            log_print(f"[StarCraft2] shutdown failed: {e}")

    def get_status(self):
        self._sync_state_from_engine()
        return {
            "enabled": self.config_manager.get_bool("enabled", False),
            "engine": str(self.config_manager.get("engine", "internal_lav_bot")),
            "config": self.config_manager.config_message(),
            "state": self.state.to_dict(),
            "engine_status": (
                self.current_engine.get_status()
                if self.current_engine is not None
                else {}
            ),
            "last_start_result": dict(self.last_start_result or {}),
            "last_stop_result": dict(self.last_stop_result or {}),
            "ladder_proxy": self.ladder_proxy.get_status(self._ladder_proxy_config()),
        }

    def set_status_event_callback(self, callback):
        self.status_event_callback = callback

    def set_tts(self, tts):
        #20260710_kpopmodder: Keep a direct TTS fallback for local-match
        # lifecycle events when the reaction callback is unavailable.
        self.tts = tts

    def is_running(self) -> bool:
        return bool(self.current_engine and self.current_engine.is_running())

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
        selected_race = self._normalize_sc2_race(
            race,
            fallback=self._local_match_race_from_args(args),
        )
        normalized_args = self._strip_local_match_args(
            self._normalize_ladder_args(args)
        )
        normalized_args = self._strip_ladder_args(normalized_args, {"--race"})
        normalized_args.extend(["--race", selected_race])
        return subprocess.list2cmdline(normalized_args)

    def on_local_match_ai_race_change(self, ai_race, args):
        selected_race = self._normalize_sc2_race(ai_race, fallback="Zerg")
        normalized_args = self._strip_local_match_args(self._normalize_ladder_args(args))
        bot_name = LOCAL_MATCH_AI_BY_RACE.get(selected_race, "")
        replaced = False
        rewritten = []
        skip_next = False
        for arg in normalized_args:
            text = str(arg or "").strip()
            if skip_next:
                skip_next = False
                continue
            if text == "--bot":
                rewritten.extend(["--bot", bot_name] if bot_name else [])
                skip_next = True
                replaced = True
                continue
            if text.startswith("--bot="):
                if bot_name:
                    rewritten.append("--bot=" + bot_name)
                replaced = True
                continue
            rewritten.append(arg)
        if bot_name and not replaced:
            rewritten = ["--bot", bot_name] + rewritten
        normalized_args = rewritten
        return subprocess.list2cmdline(normalized_args)

    def on_local_human_vs_changeling_click(
        self,
        executable_path,
        working_directory,
        args,
        proxy_ports,
        ai_race=None,
    ):
        selected_ai_race = self._normalize_sc2_race(
            ai_race or self._local_match_ai_race_from_args(args),
            fallback="Zerg",
        )
        bot_name = LOCAL_MATCH_AI_BY_RACE.get(selected_ai_race)
        if not bot_name:
            result = {
                "ok": False,
                "error": "local_match_random_ai_not_supported",
                "message": "Random AI is disabled until deterministic bot selection is implemented.",
            }
            return self._local_match_status_json(result=result)
        args = self.on_local_match_ai_race_change(selected_ai_race, args)
        config = self._local_match_config(
            executable_path=executable_path,
            working_directory=working_directory,
            args=args,
            proxy_ports=proxy_ports,
        )
        runtime_download = self._ensure_local_match_runtime(config)
        config["runtime_download"] = runtime_download
        if not runtime_download.get("ok", False):
            result = {
                "ok": False,
                "running": False,
                "error": runtime_download.get("error", "starcraft2_runtime_download_failed"),
                "runtime_download": runtime_download,
            }
            log_print(f"[StarCraft2] Local Match runtime restore failed: {result}")
            return self._local_match_status_json(config, result)
        if runtime_download.get("downloaded"):
            #20260712_kpopmodder: Rebuild validation after restoring ignored
            # runtime files so bot profile checks see the freshly downloaded tree.
            config = self._local_match_config(
                executable_path=executable_path,
                working_directory=working_directory,
                args=args,
                proxy_ports=proxy_ports,
            )
            config["runtime_download"] = runtime_download
        bot_profile_validation = config.get("bot_profile_validation", {})
        if (
            isinstance(bot_profile_validation, dict)
            and bot_profile_validation
            and not bot_profile_validation.get("ok", False)
        ):
            #20260712_kpopmodder: Do not open SC2 when the selected bot runtime
            # is incomplete; a half-restored runtime otherwise reaches JoinGame.
            result = {
                "ok": False,
                "running": False,
                "error": bot_profile_validation.get("error", "bot_runtime_invalid"),
                "bot_profile_validation": bot_profile_validation,
            }
            log_print(f"[StarCraft2] Local Human vs AI preflight failed: {result}")
            return self._local_match_status_json(config, result)
        result = self.ladder_proxy.start(
            config,
            capture_output=bool(config.get("capture_output", True)),
            line_callback=self._on_ladder_proxy_line,
        )
        log_print(f"[StarCraft2] Start Local Human vs Changeling result: {result}")
        return self._local_match_status_json(config, result)

    def on_local_match_stop_click(self):
        result = self.ladder_proxy.stop()
        log_print(f"[StarCraft2] Stop Local Match result: {result}")
        return self._local_match_status_json(result=result)

    def on_local_match_status_click(
        self,
        executable_path,
        working_directory,
        args,
        proxy_ports,
    ):
        config = self._local_match_config(
            executable_path=executable_path,
            working_directory=working_directory,
            args=args,
            proxy_ports=proxy_ports,
        )
        return self._local_match_status_json(config)

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
        event = dict(event or {})
        self.state.update_event(event)
        if callable(self.status_event_callback):
            try:
                self.status_event_callback(event)
            except Exception as e:
                log_print(f"[StarCraft2] status event callback failed: {e}")

#     def _on_lan_ladder_proxy_exit(self, event: Dict[str, Any]) -> None:
#         #20260712_kpopmodder: LAN Lobby exit handling is archived.
#         log_print("[StarCraft2] LAN Lobby exit callback ignored because LAN Lobby is archived.")
#
#     def _summarize_lan_relay_snapshot(self, snapshot: Dict[str, Any]) -> Dict[str, Any]:
#         #20260712_kpopmodder: LAN relay snapshot summarization is archived.
#         return {"archived": True, "exit": dict(snapshot or {})}

    def _on_ladder_proxy_line(self, stream_name: str, line: str) -> None:
        text = str(line or "").strip()
        if text:
            log_print(f"[StarCraft2] ladder_proxy {stream_name}: {text[:1000]}")
            telemetry_prefix = "LAV_OBSERVATION "
            #20260710_kpopmodder: Ladder Proxy prepends its own timestamp
            # to stdout lines, so telemetry may not begin at position zero.
            telemetry_index = text.find(telemetry_prefix)
            if telemetry_index >= 0:
                try:
                    snapshot = json.loads(text[telemetry_index + len(telemetry_prefix):])
                except (TypeError, ValueError, json.JSONDecodeError):
                    snapshot = None
                for event in self.observation_tracker.update(snapshot):
                    self._handle_engine_event(event)
                return
            #20260710_kpopmodder: Convert local-match lifecycle lines into
            # the existing LAV reaction/TTS callback path.
            lower = text.lower()
            event_type = ""
            if "starting the match" in lower:
                event_type = "game_started"
            elif "client changed status from in_game to ended" in lower:
                event_type = "game_ended"
            elif "finished with result:" in lower:
                # LavHumanVsBot assigns Player1 to LAVHuman and Player2 to
                # the AI. Report the result from the AI/TTS perspective;
                # checking only for the word "win" reverses Player1Win.
                if "initializationerror" in lower or "initialization error" in lower:
                    #20260711_kpopmodder: Startup failures are diagnostics, not
                    # match losses; engine_error stays log-only in the SC2 TTS policy.
                    event_type = "engine_error"
                elif "player2win" in lower or "player2 win" in lower:
                    event_type = "game_won"
                elif "player1win" in lower or "player1 win" in lower:
                    event_type = "game_lost"
                elif "player2loss" in lower or "player2 loss" in lower:
                    event_type = "game_lost"
                elif "player1loss" in lower or "player1 loss" in lower:
                    event_type = "game_won"
                else:
                    event_type = "game_won" if "win" in lower else "game_lost"
            elif self._is_ladder_proxy_error_line(lower):
                event_type = "engine_error"
            if event_type and callable(self.status_event_callback):
                if event_type == "game_started":
                    self.observation_tracker.reset()
                try:
                    self.status_event_callback({
                        "event_type": event_type,
                        "details": {"result": text, "source": stream_name},
                    })
                except Exception as e:
                    log_print(f"[StarCraft2] ladder proxy TTS callback failed: {e}")
            elif event_type and self.tts is not None:
                receive_input = getattr(self.tts, "receive_input", None)
                if callable(receive_input):
                    receive_input(f"StarCraft2 {event_type}")

    def _is_ladder_proxy_error_line(self, lower_line: str) -> bool:
        lower = str(lower_line or "").lower()
        if not lower:
            return False
        #20260712_kpopmodder: Native diagnostics include harmless fields like
        # error_count=0 in successful CreateGame/JoinGame summaries. Do not
        # convert those normal summaries into engine_error events.
        if "error_count=0" in lower and "error:" not in lower:
            blocked_terms = (
                " failed",
                "failed ",
                " timeout",
                "timeout/",
                "closed/error",
                " crashed",
                "crashed ",
                "exception",
            )
            if not any(term in lower for term in blocked_terms):
                return False
        return (
            "error:" in lower
            or "timeout/closed/error" in lower
            or "waiting for a response had a timeout" in lower
            or " failed" in lower
            or "failed " in lower
            or " crashed" in lower
            or "crashed " in lower
            or "exception" in lower
        )

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
        if self.current_engine is None:
            return
        try:
            status = self.current_engine.get_status()
        except Exception as e:
            self.state.mark_error(e)
            return
        if isinstance(status, dict):
            self.state.running = bool(status.get("running", self.current_engine.is_running()))
            self.state.last_error = status.get("last_error") or self.state.last_error
            self.state.last_event = status.get("last_event") or self.state.last_event
            self.state.process_pid = status.get("process_pid")
            self.state.stdout_tail = list(status.get("stdout_tail") or [])[-20:]
            self.state.stderr_tail = list(status.get("stderr_tail") or [])[-20:]

    def _facade_result(self, ok: bool, error=None, status=None):
        return {
            "ok": bool(ok),
            "engine": str(self.config_manager.get("engine", "internal_lav_bot")),
            "running": self.is_running(),
            "status": status or {},
            "error": None if error is None else str(error),
        }

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
        return json.dumps(
            status or self.get_status(),
            ensure_ascii=False,
            indent=2,
            default=str,
        )

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
        ladder_config = self.config_manager.get_section("ladder_proxy")
        config = dict(ladder_config)
        if executable_path is not None:
            config["executable_path"] = str(
                executable_path or ladder_config.get("executable_path", "")
            )
        if working_directory is not None:
            config["working_directory"] = str(
                working_directory or ladder_config.get("working_directory", "")
            )
        if args is not None:
            config["args"] = args if isinstance(args, list) else str(args or "")
        if proxy_host is not None:
            config["proxy_host"] = str(proxy_host or "")
        if proxy_ports is not None and str(proxy_ports or "").strip():
            config["ports"] = proxy_ports
        return config

    def _local_match_config(
        self,
        executable_path=None,
        working_directory=None,
        args=None,
        proxy_ports=None,
    ) -> Dict[str, Any]:
        #20260711_kpopmodder: Local Match intentionally no longer reads the LAN
        # Lobby launcher defaults; this protects local play while remote-human
        # native code was archived in the LAN-only native copy.
        ladder_config = self.config_manager.get_section("ladder_proxy")
        local_config = self.config_manager.get_section("local_match")
        config = dict(ladder_config)
        config.update(local_config)
        if executable_path is not None:
            config["executable_path"] = str(
                executable_path
                or local_config.get("executable_path", "")
                or ladder_config.get("executable_path", "")
            )
        if working_directory is not None:
            config["working_directory"] = str(
                working_directory
                or local_config.get("working_directory", "")
                or ladder_config.get("working_directory", "")
            )
        if args is not None:
            config["args"] = args if isinstance(args, list) else str(args or "")
        if proxy_ports is not None and str(proxy_ports or "").strip():
            config["ports"] = proxy_ports
        config["args"] = self._strip_local_match_args(
            self._normalize_ladder_args(config.get("args", []))
        )
        config["proxy_host"] = ""
        config["check_hosts"] = ["127.0.0.1"]
        config["remote_human_enabled"] = False
        config["mode"] = "local_human_vs_changeling"
        normalized_args = config["args"]
        bot_name = ""
        for index, arg in enumerate(normalized_args):
            if str(arg).strip() == "--bot" and index + 1 < len(normalized_args):
                bot_name = str(normalized_args[index + 1]).strip()
                break
        profile = get_bot_launch_profile(bot_name)
        config["bot_profile"] = {
            "name": profile.name,
            "type": profile.bot_type,
            "file_name": profile.file_name,
            "required_runtime": profile.required_runtime,
        } if profile else {"name": bot_name, "error": "unknown_bot_profile"}
        if profile:
            bot_root = os.path.join(config.get("working_directory", ""), "Bots")
            config["bot_profile_validation"] = profile.validate(bot_root)
        return config

    def _ensure_local_match_runtime(self, config: Dict[str, Any]) -> Dict[str, Any]:
        runtime_dir = self.config_manager.resolve_path_value(
            str(config.get("working_directory", "") or "")
        )
        repo_runtime_dir = os.path.normpath(os.path.join(self.plugin_root, "runtime"))
        if not self._same_path(runtime_dir, repo_runtime_dir):
            return {
                "ok": True,
                "downloaded": False,
                "skipped": "non_repo_runtime",
                "runtime_dir": runtime_dir,
            }
        download_config = self.config_manager.get_section("runtime_download")
        return self.runtime_downloader.ensure_runtime(
            repo_runtime_dir,
            enabled=self._config_bool(download_config.get("enabled", True), True),
            repo_id=str(download_config.get("repo_id") or DEFAULT_RUNTIME_REPO_ID),
            repo_type=str(download_config.get("repo_type") or DEFAULT_RUNTIME_REPO_TYPE),
            revision=str(download_config.get("revision") or DEFAULT_RUNTIME_REVISION),
            local_archive_path=os.path.join(self.plugin_root, "runtime.Zip"),
        )

    def _same_path(self, left: str, right: str) -> bool:
        left_path = os.path.normcase(os.path.abspath(os.path.normpath(str(left or ""))))
        right_path = os.path.normcase(os.path.abspath(os.path.normpath(str(right or ""))))
        return bool(left_path and right_path and left_path == right_path)

    def _config_bool(self, value: Any, default: bool = False) -> bool:
        if isinstance(value, bool):
            return value
        text = str(value if value is not None else default).strip().lower()
        return text in {"1", "true", "yes", "on"}

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
        try:
            parsed = float(value)
        except (TypeError, ValueError):
            return default
        return parsed if parsed > 0 else default

    def _normalize_ladder_args(self, value: Any) -> list[str]:
        if isinstance(value, str):
            try:
                parts = shlex.split(value, posix=False)
            except ValueError:
                parts = value.split()
            return [str(part).strip().strip("\"'") for part in parts if str(part).strip()]
        if isinstance(value, (list, tuple)):
            return [str(item) for item in value if str(item)]
        return []

    def _has_arg(self, args: list[str], name: str) -> bool:
        prefix = name + "="
        return any(str(arg or "").strip() == name or str(arg or "").strip().startswith(prefix) for arg in args)

    def _ladder_arg_value(self, args: list[str], name: str, fallback: str = "") -> str:
        prefix = name + "="
        for index, arg in enumerate(args):
            text = str(arg or "").strip()
            if text == name and index + 1 < len(args):
                return str(args[index + 1] or "").strip()
            if text.startswith(prefix):
                return text[len(prefix):].strip()
        return str(fallback or "")

    def _normalize_sc2_race(self, value: Any, fallback: str = "Random") -> str:
        races = {race.lower(): race for race in SC2_RACE_CHOICES}
        text = str(value or "").strip().lower()
        if text in races:
            return races[text]
        fallback_text = str(fallback or "").strip().lower()
        return races.get(fallback_text, "Random")

    def _local_match_race_from_args(
        self,
        args: Any,
        fallback: str = "Terran",
    ) -> str:
        normalized_args = self._normalize_ladder_args(args)
        prefix = "--race="
        for index, arg in enumerate(normalized_args):
            text = str(arg or "").strip()
            if text == "--race" and index + 1 < len(normalized_args):
                return self._normalize_sc2_race(
                    normalized_args[index + 1],
                    fallback=fallback,
                )
            if text.startswith(prefix):
                return self._normalize_sc2_race(
                    text[len(prefix):],
                    fallback=fallback,
                )
        return self._normalize_sc2_race(fallback, fallback="Terran")

    def _local_match_ai_race_from_args(self, args, fallback="Zerg"):
        normalized_args = self._normalize_ladder_args(args)
        for index, arg in enumerate(normalized_args):
            text = str(arg or "").strip()
            if text == "--bot" and index + 1 < len(normalized_args):
                bot_name = normalized_args[index + 1].lower()
                for race, mapped_bot in LOCAL_MATCH_AI_BY_RACE.items():
                    if bot_name == mapped_bot.lower():
                        return race
        return self._normalize_sc2_race(fallback, fallback="Zerg")

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
        return self._strip_ladder_args(
            args,
            {
                "--remote-human-host",
                "--remote-human-client-port",
                "--lan-game-host-ip",
                "--bot-race",
                #20260710_kpopmodder: LavHumanVsBot uses its built-in
                # human name in the current binary; passing this legacy
                # option prevents all local bot types from progressing.
                "--human-name",
            },
        )

    def _strip_ladder_args(self, args: list[str], names: set[str]) -> list[str]:
        stripped = []
        skip_next = False
        for arg in args:
            text = str(arg or "").strip()
            if skip_next:
                skip_next = False
                continue
            if text in names:
                skip_next = True
                continue
            if any(text.startswith(name + "=") for name in names):
                continue
            stripped.append(arg)
        return stripped

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
        proxy_config = (
            ladder_proxy_config
            if isinstance(ladder_proxy_config, dict)
            else self._local_match_config()
        )
        return json.dumps(
            {
                "mode": "local_human_vs_changeling",
                "result": result or {},
                "ladder_proxy": self.ladder_proxy.get_status(proxy_config),
            },
            ensure_ascii=False,
            indent=2,
            default=str,
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
