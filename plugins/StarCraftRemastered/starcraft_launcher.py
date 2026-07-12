# #20260630_kpopmodder: Added safe Samase process launch support for StarCraft Remastered.
# import os
# import subprocess
# from dataclasses import dataclass, field


# @dataclass
# class StarCraftLaunchResult:
#     ok: bool
#     message: str
#     process: object = None
#     command: list = field(default_factory=list)


# class StarCraftLauncher:
#     #20260630_kpopmodder: Launches only user-installed local files; no game assets live in this repo.
#     def __init__(self, config_manager):
#         self.config_manager = config_manager

#     def build_command(self):
#         samase_path = self.config_manager.resolve_path("samase_path")
#         if not samase_path:
#             samase_path = self.config_manager.resolve_path("samase_exe_path")
#         command = [samase_path]
#         command.extend(self._mod_arguments())
#         return command

#     def build_command_display(self, command=None):
#         command = command or self.build_command()
#         return subprocess.list2cmdline([str(part) for part in command])

#     def launch(self):
#         validation = self.config_manager.validate_paths()
#         if not validation.ok:
#             return StarCraftLaunchResult(
#                 ok=False,
#                 message=validation.message(),
#             )

#         if not self.config_manager.get_bool("enabled", False):
#             return StarCraftLaunchResult(
#                 ok=False,
#                 message=(
#                     "StarCraft config enabled=false. Set enabled=true before "
#                     "launching Samase."
#                 ),
#             )

#         command = self.build_command()
#         cwd = self.config_manager.resolve_path("starcraft_x86_dir")
#         if not cwd:
#             cwd = os.path.dirname(command[0])

#         try:
#             process = subprocess.Popen(
#                 command,
#                 cwd=cwd,
#                 shell=False,
#                 env=self.build_environment(),
#             )
#         except FileNotFoundError:
#             return StarCraftLaunchResult(
#                 ok=False,
#                 message=f"Samase executable was not found: {command[0]}",
#                 command=command,
#             )
#         except PermissionError as e:
#             return StarCraftLaunchResult(
#                 ok=False,
#                 message=f"Samase executable could not be started: {e}",
#                 command=command,
#             )
#         except Exception as e:
#             return StarCraftLaunchResult(
#                 ok=False,
#                 message=f"StarCraft Remastered AI launch failed: {e}",
#                 command=command,
#             )

#         return StarCraftLaunchResult(
#             ok=True,
#             message=f"StarCraft Remastered AI launched. pid={process.pid}",
#             process=process,
#             command=command,
#         )

#     def _mod_arguments(self):
#         value = self.config_manager.get("samase_arg", None)
#         if value is None:
#             value = self.config_manager.get("mod_argument", "custom")
#         if isinstance(value, list):
#             return [str(part) for part in value if str(part).strip()]

#         value = str(value or "").strip()
#         if not value:
#             return []
#         return [value]

#     def build_environment(self):
#         env = os.environ.copy()
#         if hasattr(self.config_manager, "resolve_samase_state_path"):
#             env["LAV_SAMASE_STATE_PATH"] = (
#                 self.config_manager.resolve_samase_state_path()
#             )
#         if hasattr(self.config_manager, "resolve_bwapi_snapshot_path"):
#             env["LAV_BWAPI_RM_SNAPSHOT_PATH"] = (
#                 self.config_manager.resolve_bwapi_snapshot_path()
#             )
#         env["LAV_SAMASE_STATE_EVERY_N_FRAMES"] = str(
#             self.config_manager.get_int("samase_state_write_every_n_frames", 8)
#             if hasattr(self.config_manager, "get_int")
#             else 8
#         )
#         env["LAV_SAMASE_HEARTBEAT_INTERVAL_MS"] = str(
#             self.config_manager.get_int(
#                 "samase_initialize_heartbeat_interval_ms",
#                 1000,
#             )
#             if hasattr(self.config_manager, "get_int")
#             else 1000
#         )
#         plugin_path = self.config_manager.resolve_path(
#             "samase_readonly_plugin_dll_path"
#         )
#         if (
#             self.config_manager.get_bool("samase_readonly_plugin_enabled", True)
#             and plugin_path
#             and os.path.isfile(plugin_path)
#         ):
#             existing = str(env.get("SAMASE_MORE_DLLS", "") or "").strip()
#             env["SAMASE_MORE_DLLS"] = (
#                 f"{existing};{plugin_path}" if existing else plugin_path
#             )
#         return env
