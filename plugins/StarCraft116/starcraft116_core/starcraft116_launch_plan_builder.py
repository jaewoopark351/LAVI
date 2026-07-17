#20260717_kpopmodder: Split from legacy multi-class module for AGENTS 29.1 file/type separation.

#20260706_kpopmodder: Isolates launch-plan construction for StarCraft116 process startup.
import ctypes
import os
import shlex
from dataclasses import dataclass
from ctypes import wintypes


from .starcraft116_launch_command import StarCraft116LaunchCommand

class StarCraft116LaunchPlanBuilder:
    def __init__(self, config_manager):
        self.config_manager = config_manager

    def build_launch_plan(self, profile):
        plan = []
        if self.config_manager.get_profile_bool(profile, "start_chaoslauncher", False):
            plan.append(
                self._build_command(
                    label="chaoslauncher",
                    profile=profile,
                    path_key="chaoslauncher_path",
                    arguments_key="chaoslauncher_arguments",
                    cwd_key="chaoslauncher_working_dir",
                    run_as_admin_key="chaoslauncher_run_as_admin",
                )
            )
        if self.config_manager.get_profile_bool(profile, "start_starcraft", False):
            plan.append(
                self._build_command(
                    label="starcraft",
                    profile=profile,
                    path_key="starcraft_exe_path",
                    arguments_key="starcraft_arguments",
                    cwd_key="starcraft_working_dir",
                    run_as_admin_key="starcraft_run_as_admin",
                )
            )
        if self.config_manager.get_profile_bool(profile, "start_bot_process", False):
            plan.append(
                self._build_command(
                    label="bot",
                    profile=profile,
                    path_key="bot_process_path",
                    arguments_key="bot_process_arguments",
                    cwd_key="bot_process_working_dir",
                    run_as_admin_key="bot_process_run_as_admin",
                    delay_key="bot_process_launch_delay_sec",
                )
            )
        if self.config_manager.get_profile_bool(profile, "start_observer_process", False):
            plan.append(
                self._build_command(
                    label="observer",
                    profile=profile,
                    path_key="observer_process_path",
                    arguments_key="observer_process_arguments",
                    cwd_key="observer_process_working_dir",
                    run_as_admin_key="observer_process_run_as_admin",
                    delay_key="observer_process_launch_delay_sec",
                )
            )
        return [command for command in plan if command is not None]

    def _build_command(
        self,
        label,
        profile,
        path_key,
        arguments_key,
        cwd_key,
        run_as_admin_key,
        delay_key=None,
    ):
        executable = self.config_manager.resolve_profile_path(profile, path_key)
        if not executable:
            return None

        arguments = profile.get(arguments_key, [])
        working_dir = self.config_manager.resolve_profile_path(profile, cwd_key)
        if not working_dir:
            working_dir = os.path.dirname(executable)

        return StarCraft116LaunchCommand(
            label=label,
            command=self._build_command_args(executable, arguments),
            cwd=working_dir,
            run_as_admin=self.config_manager.get_profile_bool(
                profile,
                run_as_admin_key,
                False,
            ),
            launch_delay_sec=(
                self._coerce_delay(profile.get(delay_key, 0.0))
                if delay_key
                else 0.0
            ),
        )

    @staticmethod
    def _coerce_delay(value):
        #20260705_kpopmodder: Keep launch delay coercion stable while moving parser details out.
        try:
            return max(0.0, float(value))
        except (TypeError, ValueError):
            return 0.0

    def _build_command_args(self, executable, arguments):
        command = [executable]
        command.extend(self._coerce_arguments(arguments))
        return command

    def _coerce_arguments(self, value):
        if isinstance(value, list):
            return [str(part) for part in value if str(part).strip()]

        value = str(value or "").strip()
        if not value:
            return []
        return self._split_argument_string(value)

    @staticmethod
    def _split_argument_string(value):
        #20260703_kpopmodder: Keep Windows quoted paths intact while allowing "-a -b" strings.
        if os.name == "nt":
            argc = ctypes.c_int()
            shell32 = ctypes.windll.shell32
            shell32.CommandLineToArgvW.argtypes = [
                wintypes.LPCWSTR,
                ctypes.POINTER(ctypes.c_int),
            ]
            shell32.CommandLineToArgvW.restype = ctypes.POINTER(wintypes.LPWSTR)
            argv = shell32.CommandLineToArgvW(value, ctypes.byref(argc))
            if not argv:
                raise ctypes.WinError(ctypes.get_last_error())
            try:
                return [
                    argv[index]
                    for index in range(argc.value)
                    if str(argv[index]).strip()
                ]
            finally:
                ctypes.windll.kernel32.LocalFree(argv)

        return [
            str(part)
            for part in shlex.split(value)
            if str(part).strip()
        ]
