#20260717_kpopmodder: Split from legacy multi-class module for AGENTS 29.1 file/type separation.

from dataclasses import dataclass, field


@dataclass
class StarCraft116LaunchResult:
    ok: bool
    message: str
    processes: list = field(default_factory=list)
    commands: list = field(default_factory=list)
