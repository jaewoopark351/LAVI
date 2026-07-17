#20260717_kpopmodder: Split from legacy multi-class module for AGENTS 29.1 file/type separation.

import os
import time

#20260702_kpopmodder: Launches user-installed StarCraft 1.16/BWAPI tooling only.
from core.process import command_line
from .starcraft116_launch_executor import (
    StarCraft116ProcessLauncherRuntime,
    StarCraft116StartedProcess,
)
from .starcraft116_launch_paths import (
    StarCraft116LaunchPlanBuilder,
)


from .starcraft116_launch_result import StarCraft116LaunchResult


class StarCraft116Launcher:
    #20260702_kpopmodder: Keeps BWAPI-era launch order configurable per bot profile.
    def __init__(self, config_manager):
        self.config_manager = config_manager
        self.path_builder = StarCraft116LaunchPlanBuilder(config_manager)
        self.process_runtime = StarCraft116ProcessLauncherRuntime()

    def build_launch_plan(self):
        profile = self.config_manager.get_active_profile()
        return self.path_builder.build_launch_plan(profile)

    def build_command_display(self, command):
        return command_line(command)

    def _coerce_arguments(self, value):
        return self.path_builder._coerce_arguments(value)

    def _launch_elevated(self, launch_command):
        normalized_launch_command = self.process_runtime._normalize_launch_command(
            launch_command,
        )
        return self.process_runtime._launch_elevated(normalized_launch_command)

    def launch(self, profile_name=None):
        validation = self.config_manager.validate_paths(profile_name=profile_name)
        if not validation.ok:
            return StarCraft116LaunchResult(False, validation.message())

        if not self.config_manager.get_bool("enabled", False):
            return StarCraft116LaunchResult(
                False,
                (
                    "StarCraft 1.16 config enabled=false. Set enabled=true "
                    "before launching a BWAPI profile."
                ),
            )

        plan = self.build_launch_plan()
        if not plan:
            return StarCraft116LaunchResult(
                False,
                "No StarCraft 1.16 launch command is configured.",
            )

        started = []
        try:
            env = self.build_environment()
            for launch_command in plan:
                if launch_command.launch_delay_sec > 0:
                    time.sleep(launch_command.launch_delay_sec)
                if launch_command.run_as_admin:
                    process = self._launch_elevated(launch_command)
                else:
                    process = self.process_runtime.launch_command(
                        launch_command,
                        env,
                    )
                started.append(StarCraft116StartedProcess(
                    label=launch_command.label,
                    process=process,
                    command=launch_command.command,
                ))
        except FileNotFoundError as e:
            return StarCraft116LaunchResult(
                False,
                f"StarCraft 1.16 launch file was not found: {e}",
                processes=started,
                commands=plan,
            )
        except PermissionError as e:
            return StarCraft116LaunchResult(
                False,
                f"StarCraft 1.16 launch was blocked by permissions: {e}",
                processes=started,
                commands=plan,
            )
        except Exception as e:
            return StarCraft116LaunchResult(
                False,
                f"StarCraft 1.16 launch failed: {e}",
                processes=started,
                commands=plan,
            )

        pids = ", ".join(
            f"{item.label}={item.process.pid or 'elevated'}" for item in started
        )
        return StarCraft116LaunchResult(
            True,
            f"StarCraft 1.16 profile launched. {pids}",
            processes=started,
            commands=plan,
        )

    def build_environment(self):
        env = os.environ.copy()
        profile_name = self.config_manager.get_active_profile_name()
        profile = self.config_manager.get_active_profile()
        env["LAV_STARCRAFT116_PROFILE"] = profile_name
        profile_environment = profile.get("environment", {})
        if isinstance(profile_environment, dict):
            for key, value in profile_environment.items():
                key = str(key or "").strip()
                if key:
                    env[key] = str(value)
        return env
