# #20260630_kpopmodder: Added optional StarCraft Remastered Samase launcher and coach bridge.
# import json
# import os
# import threading
# import time

# import gradio as gr

# from core.logger import log_print
# from plugins.StarCraftRemastered.core.command import StarCraftCommand
# from plugins.StarCraftRemastered.lav_bridge.starcraft_log_router import (
#     StarCraftLogRouter,
# )
# from plugins.StarCraftRemastered.lav_bridge.bwapi_runtime_bridge import (
#     BWAPIRuntimeBridge,
# )
# from plugins.StarCraftRemastered.providers import (
#     BWAPICompatProvider,
#     SamaseProvider,
#     ScreenInputProvider,
# )
# from plugins.StarCraftRemastered.bwapi_shim_manifest import BWAPIShimManifest
# from plugins.StarCraftRemastered.starcraft_config import StarCraftConfig
# from plugins.StarCraftRemastered.starcraft_launcher import StarCraftLauncher
# from plugins.StarCraftRemastered.starcraft_state import StarCraftRuntimeState


# class StarCraftRemastered:
#     #20260630_kpopmodder: Keeps StarCraft AI mode support opt-in and outside direct game control.
#     def __init__(self):
#         self.plugin_root = os.path.dirname(__file__)
#         self.prompt_path = os.path.join(
#             self.plugin_root,
#             "prompts",
#             "starcraft_coach_prompt.txt",
#         )
#         self.config_manager = StarCraftConfig(self.plugin_root)
#         self.launcher = StarCraftLauncher(self.config_manager)
#         self.bwapi_shim_manifest = BWAPIShimManifest(self.plugin_root)
#         self.log_router = StarCraftLogRouter()
#         self.runtime_bridge = BWAPIRuntimeBridge(
#             snapshot_path=self.config_manager.resolve_bwapi_snapshot_path(),
#             command_queue_path=self.config_manager.resolve_bwapi_command_queue_path(),
#         )
#         self.provider = self._build_provider()
#         self.bwapi = BWAPICompatProvider(self.provider)
#         self.state = StarCraftRuntimeState(
#             profile=str(self.config_manager.get("profile", "bwmetaai")),
#         )
#         self.output_event_listeners = []
#         self.output_lock = threading.RLock()
#         self.samase_state_bridge_stop = threading.Event()
#         self.samase_state_bridge_thread = None
#         self.samase_state_bridge_last_error = ""
#         self.screen_observation_provider = None
#         self.process = None
#         self.last_path_message = ""
#         self.awaiting_coach_response = False
#         self._shutdown = False

#     def create_ui(self):
#         with gr.Tab("StarCraft"):
#             self.config_status_box = gr.Textbox(
#                 label="Config Status",
#                 value=self.config_manager.config_message(),
#                 lines=3,
#                 interactive=False,
#             )
#             self.path_status_box = gr.Textbox(
#                 label="Path / Launch Status",
#                 value=self.last_path_message,
#                 lines=5,
#                 interactive=False,
#             )

#             with gr.Row():
#                 self.validate_button = gr.Button("Validate Paths")
#                 self.launch_button = gr.Button("Launch StarCraft Remastered AI")
#                 self.refresh_button = gr.Button("Refresh Status")

#             with gr.Row():
#                 self.pull_observation_button = gr.Button("Pull ScreenVision")
#                 self.send_coach_button = gr.Button("Send Coach Prompt")

#             self.runtime_state_box = gr.Textbox(
#                 label="Runtime State",
#                 value=self.state.to_json(),
#                 lines=10,
#                 interactive=False,
#             )
#             self.last_observation_box = gr.Textbox(
#                 label="Last Observation",
#                 value=self.state.last_observation,
#                 lines=5,
#                 interactive=False,
#             )
#             self.last_coach_box = gr.Textbox(
#                 label="Last Coach Message",
#                 value=self.state.last_coach_message,
#                 lines=5,
#                 interactive=False,
#             )

#             outputs = [
#                 self.config_status_box,
#                 self.path_status_box,
#                 self.runtime_state_box,
#                 self.last_observation_box,
#                 self.last_coach_box,
#             ]
#             self.validate_button.click(
#                 fn=self.on_validate_paths,
#                 outputs=outputs,
#                 queue=False,
#             )
#             self.launch_button.click(
#                 fn=self.on_launch_click,
#                 outputs=outputs,
#             )
#             self.refresh_button.click(
#                 fn=self.on_refresh_click,
#                 outputs=outputs,
#                 queue=False,
#             )
#             self.pull_observation_button.click(
#                 fn=self.on_pull_observation_click,
#                 outputs=outputs,
#                 queue=False,
#             )
#             self.send_coach_button.click(
#                 fn=self.on_send_coach_click,
#                 outputs=outputs,
#             )

#     def shutdown(self):
#         if self._shutdown:
#             return

#         self._shutdown = True
#         self._stop_samase_state_bridge()
#         try:
#             self.provider.stop_all_control()
#             self.provider.disconnect()
#         except Exception as e:
#             log_print(f"[StarCraftRemastered] provider shutdown failed: {e}")
#         #20260630_kpopmodder: Do not terminate the external game process from LAV shutdown.
#         with self.output_lock:
#             self.output_event_listeners.clear()
#         self.screen_observation_provider = None
#         self.process = None

#     def set_screen_observation_provider(self, provider):
#         self.screen_observation_provider = provider

#     def add_output_event_listener(self, function):
#         with self.output_lock:
#             if function in self.output_event_listeners:
#                 return
#             self.output_event_listeners.append(function)

#     def remove_output_event_listener(self, function):
#         with self.output_lock:
#             removed = False
#             while function in self.output_event_listeners:
#                 self.output_event_listeners.remove(function)
#                 removed = True
#             return removed

#     def start(self):
#         if not self.config_manager.get_bool("enabled", False):
#             log_print("[StarCraftRemastered] start skipped: enabled=false")
#             return False
#         try:
#             connected = self.provider.connect()
#             if connected:
#                 self._start_samase_state_bridge()
#             return connected
#         except Exception as e:
#             log_print(f"[StarCraftRemastered] provider start failed: {e}")
#             return False

#     def stop(self):
#         try:
#             self._stop_samase_state_bridge()
#             self.provider.stop_all_control()
#             return self.provider.disconnect()
#         except Exception as e:
#             log_print(f"[StarCraftRemastered] provider stop failed: {e}")
#             return False

#     def get_status(self):
#         try:
#             game_state = self.provider.get_game_state()
#             game_state_payload = game_state.to_dict()
#         except Exception as e:
#             game_state_payload = {"error": str(e)}

#         return {
#             "enabled": self.config_manager.get_bool("enabled", False),
#             "mode": self.config_manager.get("mode", "single_player_only"),
#             "provider": self.config_manager.get("provider", "screen_input"),
#             "bwapi_compat_enabled": self.config_manager.get_bool(
#                 "bwapi_compat_enabled",
#                 True,
#             ),
#             "bwapi_shim_enabled": self.config_manager.get_bool(
#                 "bwapi_shim_enabled",
#                 True,
#             ),
#             "bwapi_shim": self.bwapi_shim_manifest.status(),
#             "bwapi_runtime_bridge": self._runtime_bridge_status(),
#             "saida_compatibility_mode": self.config_manager.get_bool(
#                 "saida_compatibility_mode",
#                 True,
#             ),
#             "allow_battlenet": self.config_manager.get_bool(
#                 "allow_battlenet",
#                 False,
#             ),
#             "allow_multiplayer": self.config_manager.get_bool(
#                 "allow_multiplayer",
#                 False,
#             ),
#             "auto_control": self.config_manager.get_bool("auto_control", False),
#             "game_state": game_state_payload,
#             "recent_logs": self.log_router.get_recent_logs(limit=20),
#         }

#     def get_game_state(self):
#         return self.provider.get_game_state()

#     def send_command(self, command):
#         if not isinstance(command, StarCraftCommand):
#             command = StarCraftCommand.chat_log_only(command)
#         try:
#             return self.provider.send_command(command)
#         except Exception as e:
#             log_print(f"[StarCraftRemastered] send_command failed: {e}")
#             return False

#     def update_screen_observation(self, text):
#         observation = str(text or "").strip()
#         game_state = self._update_provider_screen_observation(observation)
#         self.state.set_observation(observation)
#         self._write_bwapi_snapshot(game_state)
#         self.log_router.log_event("screen observation updated")
#         self._write_state_log()
#         return self.provider.get_game_state()

#     def receive_observation(self, text):
#         observation = str(text or "").strip()
#         if not observation:
#             self.last_path_message = "No ScreenVision observation is available yet."
#             return self._ui_values()

#         game_state = self._update_provider_screen_observation(observation)
#         self.state.set_observation(observation)
#         self._write_bwapi_snapshot(game_state)
#         self.last_path_message = "ScreenVision observation saved for StarCraft coach."
#         self._write_state_log()
#         return self._ui_values()

#     def receive_coach_response(self, text):
#         if not self.awaiting_coach_response:
#             return

#         self.awaiting_coach_response = False
#         self.state.set_coach_message(text)
#         self._write_state_log()

#     def on_validate_paths(self):
#         validation = self.config_manager.validate_paths()
#         self.last_path_message = validation.message()
#         self.state.profile = str(self.config_manager.get("profile", "bwmetaai"))
#         return self._ui_values()

#     def on_launch_click(self):
#         self._refresh_process_state()
#         if self.state.running:
#             self.last_path_message = (
#                 f"StarCraft Remastered AI is already running. pid={self.state.pid}"
#             )
#             return self._ui_values()

#         result = self.launcher.launch()
#         self.last_path_message = result.message
#         if result.ok:
#             self.process = result.process
#             self.state.mark_launched(
#                 pid=result.process.pid,
#                 profile=self.config_manager.get("profile", "bwmetaai"),
#                 command=self.launcher.build_command_display(result.command),
#             )
#             log_print(f"[StarCraftRemastered] {result.message}")
#         else:
#             self.state.mark_launch_failed(result.message)
#             if result.command:
#                 self.state.last_launch_command = (
#                     self.launcher.build_command_display(result.command)
#                 )
#             log_print(f"[StarCraftRemastered] launch skipped: {result.message}")

#         self._write_state_log()
#         return self._ui_values()

#     def on_refresh_click(self):
#         self._refresh_process_state()
#         self._sync_samase_state_once()
#         self._write_state_log()
#         return self._ui_values()

#     def on_pull_observation_click(self):
#         observation = self._latest_screen_observation()
#         return self.receive_observation(observation)

#     def on_send_coach_click(self):
#         observation = self.state.last_observation or self._latest_screen_observation()
#         observation = str(observation or "").strip()
#         if not observation:
#             self.last_path_message = "No ScreenVision observation is available yet."
#             return self._ui_values()

#         self.state.set_observation(observation)
#         payload = self._build_coach_payload(observation)
#         self.state.set_coach_request(payload["text"])
#         sent_count = self._send_output(payload)
#         if sent_count:
#             self.awaiting_coach_response = True
#             self.state.set_coach_message("Coach prompt sent to LAV.")
#             self.last_path_message = "Coach prompt sent to LAV."
#         else:
#             self.awaiting_coach_response = False
#             self.state.set_coach_message("No LLM listener is connected.")
#             self.last_path_message = "No LLM listener is connected."

#         self._write_state_log()
#         return self._ui_values()

#     def _build_coach_payload(self, observation):
#         prompt = self._load_coach_prompt()
#         text = (
#             "[StarCraft Remastered ScreenVision Observation]\n"
#             f"{observation}\n\n"
#             "[StarCraft Coach Rules]\n"
#             f"{prompt}\n"
#         )
#         return {
#             "kind": "screen_observation",
#             "source": "StarCraftRemastered",
#             "observation": observation,
#             "text": text,
#             "display_text": f"[StarCraft] {observation}",
#             "remember_history": False,
#         }

#     def _send_output(self, payload):
#         with self.output_lock:
#             listeners = tuple(self.output_event_listeners)

#         sent_count = 0
#         for listener in listeners:
#             try:
#                 listener(payload)
#                 sent_count += 1
#             except Exception as e:
#                 log_print(f"[StarCraftRemastered] output listener error: {e}")
#         return sent_count

#     def _build_provider(self):
#         provider_name = str(
#             self.config_manager.get("provider", "screen_input") or "screen_input"
#         ).strip().lower()
#         if provider_name == "samase":
#             return SamaseProvider(
#                 config=self.config_manager,
#                 launcher=self.launcher,
#                 log_router=self.log_router,
#             )
#         return ScreenInputProvider(
#             config=self.config_manager,
#             log_router=self.log_router,
#         )

#     def _update_provider_screen_observation(self, observation):
#         if hasattr(self.provider, "update_screen_observation"):
#             try:
#                 return self.provider.update_screen_observation(observation)
#             except Exception as e:
#                 log_print(
#                     f"[StarCraftRemastered] provider observation update failed: {e}"
#                 )
#         return None

#     def _write_bwapi_snapshot(self, game_state=None):
#         game_state = game_state or self.provider.get_game_state()
#         try:
#             payload = self.runtime_bridge.write_snapshot(game_state)
#             self.log_router.log_state(game_state)
#             return payload
#         except Exception as e:
#             log_print(f"[StarCraftRemastered] BWAPI snapshot write failed: {e}")
#             return None

#     def _runtime_bridge_status(self):
#         return {
#             "snapshot_path": self.runtime_bridge.snapshot_path,
#             "command_queue_path": self.runtime_bridge.command_queue_path,
#             "samase_state_path": self.config_manager.resolve_samase_state_path(),
#             "samase_state_exists": os.path.isfile(
#                 self.config_manager.resolve_samase_state_path()
#             ),
#             "samase_state_bridge_enabled": self.config_manager.get_bool(
#                 "samase_state_bridge_enabled",
#                 True,
#             ),
#             "samase_state_bridge_running": (
#                 self.samase_state_bridge_thread is not None
#                 and self.samase_state_bridge_thread.is_alive()
#             ),
#             "samase_state_bridge_last_error": self.samase_state_bridge_last_error,
#             "snapshot_exists": os.path.isfile(self.runtime_bridge.snapshot_path),
#             "command_queue_exists": os.path.isfile(
#                 self.runtime_bridge.command_queue_path
#             ),
#         }

#     def _start_samase_state_bridge(self):
#         if not self._should_sync_samase_state():
#             return False
#         if (
#             self.samase_state_bridge_thread is not None
#             and self.samase_state_bridge_thread.is_alive()
#         ):
#             return True

#         self.samase_state_bridge_stop.clear()
#         self.samase_state_bridge_thread = threading.Thread(
#             target=self._samase_state_bridge_loop,
#             name="StarCraftSamaseStateBridge",
#             daemon=True,
#         )
#         self.samase_state_bridge_thread.start()
#         return True

#     def _stop_samase_state_bridge(self):
#         self.samase_state_bridge_stop.set()
#         thread = self.samase_state_bridge_thread
#         if thread is not None and thread.is_alive():
#             thread.join(timeout=2.0)
#         self.samase_state_bridge_thread = None

#     def _samase_state_bridge_loop(self):
#         interval = max(
#             0.05,
#             self.config_manager.get_float("samase_state_poll_interval_sec", 0.25),
#         )
#         while not self.samase_state_bridge_stop.is_set():
#             self._sync_samase_state_once()
#             self.samase_state_bridge_stop.wait(interval)

#     def _sync_samase_state_once(self):
#         if not self._should_sync_samase_state():
#             return None

#         state_path = self.config_manager.resolve_samase_state_path()
#         if not os.path.isfile(state_path):
#             return None

#         try:
#             game_state = self.provider.get_game_state()
#             payload = self._write_bwapi_snapshot(game_state)
#             self.samase_state_bridge_last_error = ""
#             return payload
#         except Exception as e:
#             self.samase_state_bridge_last_error = str(e)
#             log_print(f"[StarCraftRemastered] Samase state bridge failed: {e}")
#             return None

#     def _should_sync_samase_state(self):
#         provider_name = str(
#             self.config_manager.get("provider", "screen_input") or ""
#         ).strip().lower()
#         return (
#             provider_name == "samase"
#             and self.config_manager.get_bool("samase_state_bridge_enabled", True)
#         )

#     def _latest_screen_observation(self):
#         if self.screen_observation_provider is None:
#             return ""

#         try:
#             return str(self.screen_observation_provider() or "").strip()
#         except Exception as e:
#             log_print(f"[StarCraftRemastered] ScreenVision lookup failed: {e}")
#             return ""

#     def _load_coach_prompt(self):
#         try:
#             with open(self.prompt_path, "r", encoding="utf-8") as file:
#                 return file.read().strip()
#         except Exception as e:
#             log_print(f"[StarCraftRemastered] coach prompt load failed: {e}")
#             return (
#                 "Speak briefly in Korean. Use only visible StarCraft screen "
#                 "evidence and avoid guessing hidden state."
#             )

#     def _refresh_process_state(self):
#         self.state.update_from_process(self.process)
#         self.state.profile = str(self.config_manager.get("profile", "bwmetaai"))

#     def _ui_values(self):
#         self._refresh_process_state()
#         return (
#             self.config_manager.config_message(),
#             self.last_path_message,
#             self.state.to_json(),
#             self.state.last_observation,
#             self.state.last_coach_message,
#         )

#     def _write_state_log(self):
#         if not self.config_manager.get_bool("write_state_log", True):
#             return

#         path = self.config_manager.resolve_state_log_path()
#         try:
#             os.makedirs(os.path.dirname(path), exist_ok=True)
#             payload = {
#                 "time": time.time(),
#                 "state": self.state.to_dict(),
#             }
#             with open(path, "a", encoding="utf-8") as file:
#                 file.write(json.dumps(payload, ensure_ascii=False) + "\n")
#         except Exception as e:
#             log_print(f"[StarCraftRemastered] state log write failed: {e}")
