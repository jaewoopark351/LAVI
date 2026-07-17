#20260717_kpopmodder: Split from legacy multi-class module for AGENTS 29.1 file/type separation.

#20260708_kpopmodder: Added tail-style ProBots log watcher for passive SC2 commentary.
from __future__ import annotations

import os
import threading
import time
from dataclasses import dataclass
from typing import Any, Callable, Dict, Iterable, List, Optional

from core.logger import log_print


LogLineCallback = Callable[[str, str], None]

@dataclass
class WatchedLogState:
    path: str
    offset: int = 0
    exists: bool = False
    last_message: str = ""
    lines_seen: int = 0
