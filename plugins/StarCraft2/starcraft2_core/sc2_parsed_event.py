#20260717_kpopmodder: Split from legacy multi-class module for AGENTS 29.1 file/type separation.

#20260708_kpopmodder: Added rules-based SC2 log parser for passive Changeling commentary.
from __future__ import annotations

import re
from collections import deque
from dataclasses import dataclass
from typing import Deque, Optional, Tuple

from .sc2_speech_terms import (
    sc2_strategy_speak_name,
    sc2_unit_speak_name,
    sc2_upgrade_speak_name,
)

@dataclass(frozen=True)
class SC2ParsedEvent:
    message: str
    category: str
    raw_line: str
