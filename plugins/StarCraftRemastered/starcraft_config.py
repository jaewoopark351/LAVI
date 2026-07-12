# #20260630_kpopmodder: Added config helpers for the optional Samase launch plugin.
# import json
# import os
# from dataclasses import dataclass, field


# DEFAULT_CONFIG = {
#     "enabled": False,
#     "mode": "single_player_only",
#     "launch_method": "samase",
#     "provider": "screen_input",
#     "profile": "bwmetaai",
#     "starcraft_x86_dir": "",
#     "samase_path": "C:\\Program Files (x86)\\StarCraft\\x86\\samase-0.8.31.exe",
#     "samase_arg": "custom",
#     "samase_exe_path": "",
#     "starcraft_exe_path": "",
#     "mod_argument": "custom",
#     "allow_battlenet": False,
#     "allow_multiplayer": False,
#     "auto_control": False,
#     "bwapi_compat_enabled": True,
#     "bwapi_shim_enabled": True,
#     "bwapi_snapshot_path": "logs\\starcraft_bwapi_rm_snapshot.json",
#     "bwapi_command_queue_path": "logs\\starcraft_bwapi_rm_commands.jsonl",
#     "samase_state_path": "logs\\starcraft_samase_readonly_state.json",
#     "samase_state_bridge_enabled": True,
#     "samase_state_poll_interval_sec": 0.25,
#     "samase_state_write_every_n_frames": 8,
#     "samase_initialize_heartbeat_interval_ms": 1000,
#     "samase_readonly_plugin_enabled": True,
#     "samase_readonly_plugin_dll_path": (
#         "plugins\\StarCraftRemastered\\samase_readonly_plugin\\target\\"
#         "i686-pc-windows-msvc\\release\\lav_samase_readonly_plugin.dll"
#     ),
#     "saida_compatibility_mode": True,
#     "custom_scripts_dir": "",
#     "aiscript_bin_path": "",
#     "auto_launch": False,
#     "launch_timeout_sec": 15,
#     "enable_screenvision_coach": True,
#     "coach_language": "ko",
#     "coach_style": "short_tsundere",
#     "observation_interval_sec": 5,
#     "write_state_log": True,
#     "state_log_path": "logs\\starcraft_remastered_state.jsonl",
# }


# @dataclass
# class StarCraftPathCheck:
#     ok: bool
#     messages: list = field(default_factory=list)

#     def message(self):
#         if not self.messages:
#             return "StarCraft paths are ready."
#         return "\n".join(str(message) for message in self.messages)


# class StarCraftConfig:
#     #20260630_kpopmodder: Keep user-specific game paths outside the repo.
#     def __init__(self, plugin_root=None):
#         self.plugin_root = plugin_root or os.path.dirname(__file__)
#         self.project_root = os.path.dirname(os.path.dirname(self.plugin_root))
#         self.config_dir = os.path.join(self.plugin_root, "config")
#         self.config_path = os.path.join(
#             self.config_dir,
#             "starcraft_remastered_config.json",
#         )
#         self.example_config_path = os.path.join(
#             self.config_dir,
#             "starcraft_remastered_config.example.json",
#         )
#         self.config = dict(DEFAULT_CONFIG)
#         self.config_exists = False
#         self.load_error = ""
#         self.load()

#     def load(self):
#         self.config = dict(DEFAULT_CONFIG)
#         self.config_exists = os.path.exists(self.config_path)
#         self.load_error = ""

#         if not self.config_exists:
#             return self.config

#         try:
#             with open(self.config_path, "r", encoding="utf-8") as file:
#                 loaded = json.load(file)
#         except Exception as e:
#             self.load_error = str(e)
#             return self.config

#         if not isinstance(loaded, dict):
#             self.load_error = "config root must be a JSON object"
#             return self.config

#         self.config.update(loaded)
#         self._sync_aliases(loaded)
#         return self.config

#     def _sync_aliases(self, loaded):
#         if "samase_path" not in loaded and "samase_exe_path" in loaded:
#             self.config["samase_path"] = self.config.get("samase_exe_path", "")
#         if "samase_exe_path" not in loaded and "samase_path" in loaded:
#             self.config["samase_exe_path"] = self.config.get("samase_path", "")
#         if "samase_arg" not in loaded and "mod_argument" in loaded:
#             self.config["samase_arg"] = self.config.get("mod_argument", "custom")
#         if "mod_argument" not in loaded and "samase_arg" in loaded:
#             self.config["mod_argument"] = self.config.get("samase_arg", "custom")

#     def reload(self):
#         return self.load()

#     def get(self, key, default=None):
#         return self.config.get(key, default)

#     def get_bool(self, key, default=False):
#         value = self.config.get(key, default)
#         if isinstance(value, bool):
#             return value
#         return str(value).strip().lower() in {"1", "true", "yes", "on"}

#     def get_int(self, key, default=0):
#         try:
#             return int(self.config.get(key, default))
#         except Exception:
#             return int(default)

#     def get_float(self, key, default=0.0):
#         try:
#             return float(self.config.get(key, default))
#         except Exception:
#             return float(default)

#     def config_message(self):
#         if not self.config_exists:
#             return (
#                 "StarCraft config missing. Copy "
#                 f"{self.example_config_path} to {self.config_path} and set "
#                 "your local StarCraft Remastered, Samase, and AI script paths."
#             )

#         if self.load_error:
#             return f"StarCraft config load failed: {self.load_error}"

#         if not self.get_bool("enabled", False):
#             return "StarCraft config loaded. enabled=false in plugin config."

#         return "StarCraft config loaded."

#     def resolve_path(self, key):
#         value = str(self.config.get(key, "") or "").strip().strip("\"'")
#         if not value:
#             return ""

#         value = os.path.expandvars(os.path.expanduser(value))
#         if os.path.isabs(value):
#             return os.path.normpath(value)
#         return os.path.normpath(os.path.join(self.project_root, value))

#     def resolve_state_log_path(self):
#         path = self.resolve_path("state_log_path")
#         if path:
#             return path
#         return os.path.join(
#             self.project_root,
#             "logs",
#             "starcraft_remastered_state.jsonl",
#         )

#     def resolve_bwapi_snapshot_path(self):
#         path = self.resolve_path("bwapi_snapshot_path")
#         if path:
#             return path
#         return os.path.join(
#             self.project_root,
#             "logs",
#             "starcraft_bwapi_rm_snapshot.json",
#         )

#     def resolve_bwapi_command_queue_path(self):
#         path = self.resolve_path("bwapi_command_queue_path")
#         if path:
#             return path
#         return os.path.join(
#             self.project_root,
#             "logs",
#             "starcraft_bwapi_rm_commands.jsonl",
#         )

#     def resolve_samase_state_path(self):
#         path = self.resolve_path("samase_state_path")
#         if path:
#             return path
#         return os.path.join(
#             self.project_root,
#             "logs",
#             "starcraft_samase_readonly_state.json",
#         )

#     def validate_paths(self):
#         self.reload()
#         messages = []

#         if not self.config_exists:
#             return StarCraftPathCheck(False, [self.config_message()])

#         if self.load_error:
#             return StarCraftPathCheck(False, [self.config_message()])

#         checks = (
#             ("starcraft_x86_dir", "directory", "StarCraft x86 directory"),
#             ("samase_exe_path", "file", "Samase executable"),
#             ("starcraft_exe_path", "file", "StarCraft executable"),
#             ("custom_scripts_dir", "directory", "Samase custom scripts directory"),
#             ("aiscript_bin_path", "file", "AI script aiscript.bin"),
#         )

#         for key, path_type, label in checks:
#             path = self.resolve_path(key)
#             if not path:
#                 messages.append(f"{label} is not configured: {key}")
#                 continue

#             if path_type == "directory" and not os.path.isdir(path):
#                 messages.append(f"{label} does not exist: {path}")
#             elif path_type == "file" and not os.path.isfile(path):
#                 messages.append(f"{label} does not exist: {path}")

#         if messages:
#             return StarCraftPathCheck(False, messages)

#         return StarCraftPathCheck(
#             True,
#             ["StarCraft Remastered, Samase, and AI script paths look ready."],
#         )
