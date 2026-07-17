#20260717_kpopmodder: Split from legacy multi-class module for AGENTS 29.1 file/type separation.

import subprocess
import os
import time
from dataclasses import dataclass, field
#20260702_kpopmodder: Launches user-installed StarCraft 1.16/BWAPI tooling only.
from .starcraft116_launch_executor import (
    StarCraft116ProcessLauncherRuntime,
    StarCraft116StartedProcess,
)
from .starcraft116_launch_paths import (
    StarCraft116LaunchPlanBuilder,
)


@dataclass
class StarCraft116LaunchResult:
    ok: bool
    message: str
    processes: list = field(default_factory=list)
    commands: list = field(default_factory=list)
