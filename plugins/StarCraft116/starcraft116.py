#20260702_kpopmodder: Added optional StarCraft 1.16 BWAPI bot launcher plugin.
import json
import os
import threading
import time

import gradio as gr

from core.logger import log_print
from plugins.StarCraft116.starcraft116_core.starcraft116_config import StarCraft116Config
from plugins.StarCraft116.starcraft116_core.starcraft116_exporter import (
    StarCraft116ExporterManager,
)
from plugins.StarCraft116.starcraft116_core.starcraft116_game_events import (
    StarCraft116GameEventTailer,
)
from plugins.StarCraft116.starcraft116_core.starcraft116_launcher import (
    StarCraft116Launcher,
)
from plugins.StarCraft116.starcraft116_core.starcraft116_launch_coordinator import (
    StarCraft116LaunchCoordinator,
)
from plugins.StarCraft116.starcraft116_core.starcraft116_launch_config_sync import (
    StarCraft116LaunchConfigSync,
)
from plugins.StarCraft116.starcraft116_core.starcraft116_process_manager import (
    StarCraft116ProcessManager,
)
from plugins.StarCraft116.starcraft116_core.starcraft116_event_poller import (
    StarCraft116EventPoller,
)
from plugins.StarCraft116.starcraft116_core.starcraft116_monster_log_events import (
    StarCraft116MonsterLogTailer,
)
from plugins.StarCraft116.starcraft116_core.starcraft116_reaction_policy import (
    build_starcraft116_status_event,
    build_starcraft116_status_event_key,
)
from plugins.StarCraft116.starcraft116_core.starcraft116_state import (
    StarCraft116RuntimeState,
)
from plugins.StarCraft116.starcraft116_core.starcraft116_status import (
    StarCraft116StatusReader,
)
from plugins.StarCraft116.starcraft116_core.starcraft116_ui_callbacks import (
    StarCraft116UiCallbacks,
)
from plugins.StarCraft116.starcraft116_core import starcraft116_status_presenter
from plugins.StarCraft116.starcraft116_core import starcraft116_event_runtime


class StarCraft116:
    #20260702_kpopmodder: Keeps 1.16 BWAPI bot support separate from Remastered/Samase work.
    def __init__(self):
        self.plugin_root = os.path.dirname(__file__)
        self.config_manager = StarCraft116Config(self.plugin_root)
        self.exporter_manager = StarCraft116ExporterManager(self.config_manager)
        self.launch_config_sync = StarCraft116LaunchConfigSync(
            self.config_manager,
            self.exporter_manager,
            self._is_monster_profile,
        )
        self.launcher = StarCraft116Launcher(self.config_manager)
        self.status_reader = StarCraft116StatusReader(self.config_manager)
        self.ui_callbacks = StarCraft116UiCallbacks(self)
        self.event_poller = StarCraft116EventPoller(self)#20260706_kpopmodder
        self._launch_coordinator = None
        self.state = StarCraft116RuntimeState(
            profile=self.config_manager.get_active_profile_name(),
        )
        self._process_manager = StarCraft116ProcessManager()
        self.process_entries = []
        self.last_launch_message = ""
        self.last_setup_message = ""
        self.last_discovery = {}
        self.status_event_callback = None
        self._last_status_event_key = ""
        self.game_event_tailer = StarCraft116GameEventTailer(start_at_end=True)
        self.monster_log_tailer = StarCraft116MonsterLogTailer(start_at_end=True)
        self.bwapi_proxy_event_tailer = StarCraft116GameEventTailer(start_at_end=True)
        self.game_event_thread = None
        self.game_event_stop_event = threading.Event()
        self._game_event_key_times = {}
        self._last_game_event_emit_time = 0.0
        self._shutdown = False
        self.last_screen_observation_payload = None
        self.last_screen_observation_source = None
        self.last_screen_observation_observation = ""
        self.last_screen_observation_time = 0.0

    def create_ui(self):
        profile_choices = self.config_manager.profile_dropdown_choices()
        with gr.Tab("StarCraft 1.16"):
            with gr.Tabs():
                with gr.Tab("Launch"):
                    self.profile_dropdown = gr.Dropdown(
                        label="Profile",
                        choices=profile_choices,
                        value=self.config_manager.get_active_profile_name(),
                        interactive=True,
                    )
                    self.config_status_box = gr.Textbox(
                        label="Config Status",
                        value=self.config_manager.config_message(),
                        lines=3,
                        interactive=False,
                    )
                    self.launch_status_box = gr.Textbox(
                        label="Launch Status",
                        value=self.last_launch_message,
                        lines=5,
                        interactive=False,
                    )
                    self.status_summary_box = gr.Textbox(
                        label="Status Summary",
                        value=self._status_summary_text(),
                        lines=6,
                        interactive=False,
                    )
                    with gr.Row():
                        self.validate_button = gr.Button("Validate Paths")
                        self.launch_button = gr.Button("Launch BWAPI Profile")
                        self.refresh_button = gr.Button("Refresh Status")
                    with gr.Row():
                        self.open_bwapi_ini_button = gr.Button("Open bwapi.ini")
                        self.open_chaoslauncher_folder_button = gr.Button(
                            "Open Chaoslauncher Folder"
                        )
                    with gr.Row():
                        self.open_starcraft_folder_button = gr.Button(
                            "Open StarCraft Folder"
                        )
                        self.clear_tracking_button = gr.Button("Clear Tracking")
                    self.runtime_state_box = gr.Textbox(
                        label="Runtime State",
                        value=self.state.to_json(),
                        lines=10,
                        interactive=False,
                    )
                    self.external_status_box = gr.Textbox(
                        label="External BWAPI Status",
                        value=self._external_status_json(),
                        lines=14,
                        interactive=False,
                    )

                with gr.Tab("Setup"):
                    self.install_dir_box = gr.Textbox(
                        label="Install Folder",
                        value="C:\\StarCraft116",
                        lines=1,
                    )
                    with gr.Row():
                        self.scan_install_button = gr.Button("Scan Folder")
                        self.generate_config_button = gr.Button("Generate Config")
                    self.setup_status_box = gr.Textbox(
                        label="Setup Status",
                        value=self.last_setup_message,
                        lines=8,
                        interactive=False,
                    )
                    self.setup_discovery_box = gr.Textbox(
                        label="Detected Files",
                        value=self._discovery_json(),
                        lines=12,
                        interactive=False,
                    )

            outputs = [
                self.config_status_box,
                self.launch_status_box,
                self.status_summary_box,
                self.runtime_state_box,
                self.external_status_box,
            ]
            self.profile_dropdown.change(
                fn=self.on_profile_change,
                inputs=self.profile_dropdown,
                outputs=outputs,
                queue=False,
            )
            self.validate_button.click(
                fn=self.on_validate_paths,
                inputs=self.profile_dropdown,
                outputs=outputs,
                queue=False,
            )
            self.launch_button.click(
                fn=self.on_launch_click,
                inputs=self.profile_dropdown,
                outputs=outputs,
            )
            self.refresh_button.click(
                fn=self.on_refresh_click,
                inputs=self.profile_dropdown,
                outputs=outputs,
                queue=False,
            )
            self.open_bwapi_ini_button.click(
                fn=self.on_open_bwapi_ini_click,
                inputs=self.profile_dropdown,
                outputs=outputs,
                queue=False,
            )
            self.open_chaoslauncher_folder_button.click(
                fn=self.on_open_chaoslauncher_folder_click,
                inputs=self.profile_dropdown,
                outputs=outputs,
                queue=False,
            )
            self.open_starcraft_folder_button.click(
                fn=self.on_open_starcraft_folder_click,
                inputs=self.profile_dropdown,
                outputs=outputs,
                queue=False,
            )
            self.clear_tracking_button.click(
                fn=self.on_clear_tracking_click,
                inputs=self.profile_dropdown,
                outputs=outputs,
                queue=False,
            )
            self.scan_install_button.click(
                fn=self.on_scan_install_click,
                inputs=self.install_dir_box,
                outputs=[
                    self.setup_status_box,
                    self.setup_discovery_box,
                ],
                queue=False,
            )
            self.generate_config_button.click(
                fn=self.on_generate_config_click,
                inputs=self.install_dir_box,
                outputs=[
                    self.setup_status_box,
                    self.setup_discovery_box,
                    self.config_status_box,
                    self.profile_dropdown,
                ],
                queue=False,
            )

    def start(self, profile_name=None, launch_source="startup"):
        profile_name = (
            profile_name if profile_name is not None else
            self.config_manager.get_active_profile_name()
        )
        source = "startup" if launch_source == "startup" else "manual"

        if not self.config_manager.get_bool("enabled", False):
            message = "start skipped: enabled=false"
            log_print(f"[StarCraft116] [{source}] {message}")
            self.last_launch_message = "[StarCraft116] " + message
            return False

        if launch_source == "startup" and not self.config_manager.get_bool(
            "auto_launch", False
        ):
            message = "start skipped: auto_launch=false"
            log_print(f"[StarCraft116] [{source}] {message}")
            self.last_launch_message = "[StarCraft116] " + message
            return False

        exporter_ok, exporter_message = self._sync_exporter_config(profile_name)
        if not exporter_ok:
            log_print(f"[StarCraft116] [{source}] start skipped: {exporter_message}")
            self.last_launch_message = exporter_message
            self.state.mark_launch_failed(profile_name, exporter_message, [])
            return False

        result = self.launcher.launch(profile_name=profile_name)
        command_displays = [
            self.launcher.build_command_display(command.command)
            for command in result.commands
        ] if result.ok or result.commands else []

        if result.ok:
            self.process_entries = [
                {
                    "label": item.label,
                    "process": item.process,
                    "command": item.command,
                }
                for item in result.processes
            ]
            self.state.mark_launched(
                profile_name,
                result,
                command_displays,
            )
            message = "\n".join(
                message for message in (exporter_message, result.message) if message
            )
            self.last_launch_message = message
            if message:
                log_print(f"[StarCraft116] [{source}] {message}")
            return True

        self.state.mark_launch_failed(profile_name, result.message, command_displays)
        self.last_launch_message = "\n".join(
            message for message in (exporter_message, result.message) if message
        )
        log_print(
            f"[StarCraft116] [{source}] launch skipped: {result.message}"
        )
        return False

    def stop(self):
        if self.config_manager.get_bool("terminate_on_stop", False):
            self._get_process_manager().terminate_all(self.process_entries)
        self.process_entries = []
        self.state.update_from_processes(self.process_entries)
        return True

    def shutdown(self):
        if self._shutdown:
            return

        self._shutdown = True
        try:
            self.stop_game_event_watcher()
            self.stop()
        except Exception as e:
            log_print(f"[StarCraft116] shutdown failed: {e}")

    def set_status_event_callback(self, callback):
        self.status_event_callback = callback
        if callable(callback):
            self.start_game_event_watcher()

    #20260706_kpopmodder: Compatibility shim for extension-level ScreenVision observation events.
    def handle_screen_observation(self, payload=None, **kwargs):
        merged = self._normalize_screen_observation(payload, kwargs)
        self.last_screen_observation_payload = merged
        self.last_screen_observation_source = merged.get("source") or "screen_vision"
        self.last_screen_observation_observation = str(
            merged.get("observation") or ""
        ).strip()
        self.last_screen_observation_time = time.time()
        return {
            "ok": True,
            "action": "screen_observation",
            "payload": merged,
            "source": self.last_screen_observation_source,
        }

    def _normalize_screen_observation(self, payload, extra_kwargs):
        if payload is None and not extra_kwargs:
            return {}

        merged = {}
        if isinstance(payload, dict):
            merged.update(payload)
        elif payload is not None:
            merged["payload"] = payload
        if extra_kwargs:
            merged.update(extra_kwargs)

        nested = merged.get("payload")
        if isinstance(nested, dict):
            merged.update(nested)

        source = str(merged.get("source") or merged.get("observer") or "screen_vision")
        observation = str(merged.get("observation") or "").strip()
        text = str(merged.get("text") or "").strip()
        if not observation and text:
            lines = [line.strip() for line in text.splitlines() if line.strip()]
            if lines:
                observation = lines[0]
        merged["source"] = source.strip()
        merged["observation"] = observation
        merged["text"] = text
        merged["event_type"] = str(merged.get("event_type") or "screen_observation")
        merged["event_name"] = str(merged.get("event_name") or "SCREEN_OBSERVATION")
        return merged

    def start_game_event_watcher(self):
        if not self.config_manager.get_bool("enabled", False):
            log_print("[StarCraft116GameEvents] watcher skipped: enabled=false")
            return False
        if not self.config_manager.get_bool("game_events_enabled", True):
            log_print("[StarCraft116GameEvents] watcher skipped: game_events_enabled=false")
            return False
        if not callable(self.status_event_callback):
            log_print("[StarCraft116GameEvents] watcher skipped: no event callback")
            return False
        if self.game_event_thread is not None and self.game_event_thread.is_alive():
            return True

        path = self.config_manager.resolve_game_events_path()
        self.game_event_tailer.prime_to_end(path)
        monster_log_path = self.config_manager.resolve_monster_log_path()
        if self._is_monster_log_events_active():
            self.monster_log_tailer.prime_to_end(monster_log_path)
        bwapi_proxy_events_path = self.config_manager.resolve_bwapi_proxy_events_path()
        if self._is_bwapi_proxy_events_active():
            self.bwapi_proxy_event_tailer.prime_to_end(bwapi_proxy_events_path)
        self.game_event_stop_event.clear()
        self.game_event_thread = threading.Thread(
            target=self._game_event_watch_loop,
            name="StarCraft116GameEvents",
            daemon=True,
        )
        self.game_event_thread.start()
        log_print(f"[StarCraft116GameEvents] watching: {path}")
        if self._is_monster_log_events_active():
            log_print(f"[StarCraft116MonsterLogEvents] watching: {monster_log_path}")
        if self._is_bwapi_proxy_events_active():
            log_print(
                "[StarCraft116BWAPIProxyEvents] watching: "
                f"{bwapi_proxy_events_path}"
            )
        return True

    def stop_game_event_watcher(self, timeout=0.5):
        self.game_event_stop_event.set()
        thread = self.game_event_thread
        if (
            thread is not None
            and thread.is_alive()
            and threading.current_thread() != thread
        ):
            try:
                thread.join(timeout=timeout)
            except KeyboardInterrupt:
                log_print("[StarCraft116GameEvents] watcher join interrupted")
        return True

    def get_status(self):
        external_status = self._external_status_dict()
        self._refresh_process_state(external_status)
        status = {
            "enabled": self.config_manager.get_bool("enabled", False),
            "auto_launch": self.config_manager.get_bool("auto_launch", False),
            "active_profile": self.config_manager.get_active_profile_name(),
            "config": self.config_manager.config_message(),
            "state": self.state.to_dict(),
            "external": external_status,
            "game_events": self._game_event_status_dict(),
        }
        status["screen_observation"] = {
            "source": self.last_screen_observation_source,
            "observation": self.last_screen_observation_observation,
            "received_at": self.last_screen_observation_time,
        }
        return status

    def on_profile_change(self, profile_name):
        return self._get_ui_callbacks().on_profile_change(profile_name)

    def on_validate_paths(self, profile_name):
        return self._get_ui_callbacks().on_validate_paths(profile_name)

    def on_launch_click(self, profile_name):
        return self._get_ui_callbacks().on_launch_click(profile_name)

    def _sync_exporter_config(self, profile_name):
        coordinator = getattr(self, "_launch_coordinator", None)
        if coordinator is None:
            return self.launch_config_sync.sync(profile_name)
        return coordinator.sync_exporter_config(profile_name)

    def on_refresh_click(self, profile_name):
        return self._get_ui_callbacks().on_refresh_click(profile_name)

    def on_open_bwapi_ini_click(self, profile_name):
        return self._get_ui_callbacks().on_open_management_click(
            profile_name,
            "bwapi_ini",
            "file",
        )

    def on_open_chaoslauncher_folder_click(self, profile_name):
        return self._get_ui_callbacks().on_open_management_click(
            profile_name,
            "chaoslauncher_folder",
            "directory",
        )

    def on_open_starcraft_folder_click(self, profile_name):
        return self._get_ui_callbacks().on_open_management_click(
            profile_name,
            "starcraft_folder",
            "directory",
        )

    def on_clear_tracking_click(self, profile_name):
        return self._get_ui_callbacks().on_clear_tracking_click(profile_name)

    def on_scan_install_click(self, install_dir):
        return self._get_ui_callbacks().on_scan_install_click(install_dir)

    def on_generate_config_click(self, install_dir):
        return self._get_ui_callbacks().on_generate_config_click(install_dir)

    def _get_ui_callbacks(self):
        callbacks = getattr(self, "ui_callbacks", None)
        if callbacks is None:
            callbacks = StarCraft116UiCallbacks(self)
            self.ui_callbacks = callbacks
        return callbacks

    def _get_launch_coordinator(self):
        coordinator = getattr(self, "_launch_coordinator", None)
        if coordinator is None:
            coordinator = StarCraft116LaunchCoordinator(self)
            self._launch_coordinator = coordinator
        return coordinator

    def _get_process_manager(self):
        process_manager = getattr(self, "_process_manager", None)
        if process_manager is None:
            process_manager = StarCraft116ProcessManager()
            self._process_manager = process_manager
        return process_manager

    def _get_event_poller(self):
        poller = getattr(self, "event_poller", None)
        if poller is None:
            poller = StarCraft116EventPoller(self)
            self.event_poller = poller
        return poller

    def _select_profile(self, profile_name, reload_config=False):
        if reload_config:
            self.config_manager.reload()
        profile_name = self.config_manager.set_active_profile(profile_name)
        self.state.profile = profile_name
        return profile_name

    def _refresh_process_state(self, external_status=None):
        self.state.update_from_processes(self.process_entries)
        if external_status:
            self.state.update_from_external_status(external_status)

    def _ui_values(self, emit_status_event=False, event_source="status"):
        external_status = self._external_status_dict()
        self._refresh_process_state(external_status)
        if emit_status_event:
            self._maybe_emit_status_event(external_status, event_source)
        return (
            self.config_manager.config_message(),
            self.last_launch_message,
            self._status_summary_text(external_status),
            self.state.to_json(),
            self._external_status_json(external_status),
        )

    def _discovery_json(self):
        #20260705_kpopmodder: Keep facade method for UI callbacks; presenter owns formatting only.
        return starcraft116_status_presenter.discovery_json(self.last_discovery)

    def _external_status_dict(self):
        try:
            return self.status_reader.snapshot(
                self.config_manager.get_active_profile_name(),
            )
        except Exception as e:
            return {
                "profile": self.config_manager.get_active_profile_name(),
                "error": str(e),
            }

    def _external_status_json(self, external_status=None):
        #20260705_kpopmodder: Preserve JSON output shape while moving display formatting out.
        return starcraft116_status_presenter.external_status_json(
            external_status or self._external_status_dict()
        )

    def _status_summary_text(self, external_status=None):
        #20260705_kpopmodder: UI still calls this method; helper only builds the display text.
        return starcraft116_status_presenter.status_summary_text(
            external_status or self._external_status_dict()
        )

    def _game_event_status_dict(self):
        #20260705_kpopmodder: Keep runtime checks here and pass resolved booleans to pure presenter helper.
        return starcraft116_status_presenter.game_event_status_dict(
            config_manager=self.config_manager,
            game_event_thread=self.game_event_thread,
            last_game_event_emit_time=self._last_game_event_emit_time,
            monster_log_events_active=self._is_monster_log_events_active(),
            bwapi_proxy_events_active=self._is_bwapi_proxy_events_active(),
        )

    def _maybe_emit_status_event(self, external_status, source):
        callback = self.status_event_callback
        if not callable(callback):
            return False
        if not self.config_manager.get_bool("openai_reactions_enabled", True):
            return False

        event = build_starcraft116_status_event(
            external_status,
            source=source,
        )
        if not event:
            return False

        event_key = build_starcraft116_status_event_key(event)
        if event_key == self._last_status_event_key:
            return False

        try:
            callback(event)
        except Exception as e:
            log_print(f"[StarCraft116] status event callback failed: {e}")
            return False
        self._last_status_event_key = event_key
        return True

    def _game_event_watch_loop(self):
        while not self.game_event_stop_event.is_set():
            try:
                self._poll_game_events()
            except Exception as e:
                log_print(f"[StarCraft116GameEvents] poll failed: {e}")
            interval = max(
                0.2,
                self.config_manager.get_float(
                    "game_events_poll_interval_sec",
                    1.0,
                ),
            )
            self.game_event_stop_event.wait(interval)

    def _poll_game_events(self):
        return self._get_event_poller().poll_game_events()

    def _poll_monster_log_events(self):
        return self._get_event_poller().poll_monster_log_events()

    def _poll_bwapi_proxy_events(self):
        return self._get_event_poller().poll_bwapi_proxy_events()

    def _is_monster_log_events_active(self):
        return bool(
            self._is_monster_profile(self.config_manager.get_active_profile_name())
            and self.config_manager.get_bool("monster_log_events_enabled", True)
        )

    def _is_bwapi_proxy_events_active(self):
        return bool(
            self._is_monster_profile(self.config_manager.get_active_profile_name())
            and self.config_manager.get_bool("bwapi_proxy_events_enabled", True)
        )

    @staticmethod
    def _is_monster_profile(profile_name):
        return str(profile_name or "").strip().lower() == "monster"

    @staticmethod
    def _is_noisy_unknown_enemy_destroyed_event(raw_event):
        #20260705_kpopmodder: Keep noisy BWAPI probe destroy events in logs, but do not speak unknown enemy worker-like snapshots.
        return starcraft116_event_runtime.is_noisy_unknown_enemy_destroyed_event(
            raw_event,
        )

    def _maybe_emit_game_event(self, event, use_global_cooldown=True):
        (
            emitted,
            self._game_event_key_times,
            self._last_game_event_emit_time,
        ) = starcraft116_event_runtime.maybe_emit_game_event(
            event=event,
            callback=self.status_event_callback,
            config_manager=self.config_manager,
            game_event_key_times=self._game_event_key_times,
            last_game_event_emit_time=self._last_game_event_emit_time,
            use_global_cooldown=use_global_cooldown,
            log_callback=log_print,
        )
        return emitted

    def _trim_game_event_keys(self, now, cooldown):
        self._game_event_key_times = starcraft116_event_runtime.trim_game_event_keys(
            self._game_event_key_times,
            now,
            cooldown,
        )

    def _open_management_target(self, target_key, target_type):
        paths = self.status_reader.management_paths(
            self.config_manager.get_active_profile_name(),
        )
        target = paths.get(target_key, "")
        labels = {
            "bwapi_ini": "bwapi.ini",
            "chaoslauncher_folder": "Chaoslauncher folder",
            "starcraft_folder": "StarCraft folder",
        }
        label = labels.get(target_key, target_key)
        if not target:
            return f"{label} is not configured."

        if target_type == "file" and not os.path.isfile(target):
            return f"{label} does not exist: {target}"
        if target_type == "directory" and not os.path.isdir(target):
            return f"{label} does not exist: {target}"

        startfile = getattr(os, "startfile", None)
        if startfile is None:
            return f"{label} can only be opened automatically on Windows: {target}"

        try:
            startfile(target)
        except Exception as e:
            return f"Failed to open {label}: {e}"
        return f"Opened {label}: {target}"

    def _write_state_log(self):
        if not self.config_manager.get_bool("write_state_log", True):
            return

        path = self.config_manager.resolve_state_log_path()
        try:
            os.makedirs(os.path.dirname(path), exist_ok=True)
            payload = {
                "time": time.time(),
                "status": self.get_status(),
            }
            with open(path, "a", encoding="utf-8") as file:
                file.write(json.dumps(payload, ensure_ascii=False) + "\n")
        except Exception as e:
            log_print(f"[StarCraft116] state log write failed: {e}")
