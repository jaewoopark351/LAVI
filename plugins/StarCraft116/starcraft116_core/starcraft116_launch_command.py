#20260717_kpopmodder: Split from legacy multi-class module for AGENTS 29.1 file/type separation.

#20260706_kpopmodder: Isolates launch-plan construction for StarCraft116 process startup.
import ctypes
import os
import shlex
from dataclasses import dataclass
from ctypes import wintypes


@dataclass
class StarCraft116LaunchCommand:
    label: str
    command: list
    cwd: str = ""
    run_as_admin: bool = False
    launch_delay_sec: float = 0.0
