#20260717_kpopmodder: Split from legacy multi-class module for AGENTS 29.1 file/type separation.

#20260703_kpopmodder: Consumes exported StarCraft 1.16 game events from JSONL without touching BWAPI memory.
import json
import os
from dataclasses import dataclass, field


@dataclass
class StarCraft116GameEventReadResult:
    #20260703_kpopmodder: Keeps JSONL read results explicit for tests and runtime logging.
    path: str
    events: list = field(default_factory=list)
    errors: list = field(default_factory=list)
    offset: int = 0
