#20260717_kpopmodder: Split from legacy multi-class module for AGENTS 29.1 file/type separation.

#202600707_kpopmodder
#20260705_kpopmodder: Added Monster.exe log tailing so standalone BWAPI client logs can drive TTS reactions.
import json
import os
import re
from dataclasses import dataclass, field


@dataclass
class StarCraft116MonsterLogReadResult:
    #20260705_kpopmodder: Mirrors JSONL tailer results while keeping Monster parsing isolated.
    path: str
    events: list = field(default_factory=list)
    errors: list = field(default_factory=list)
    offset: int = 0
