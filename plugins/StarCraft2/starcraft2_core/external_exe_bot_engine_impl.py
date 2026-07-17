#20260717_kpopmodder: Split from legacy multi-class module for AGENTS 29.1 file/type separation.

#20260707_kpopmodder: Added safe subprocess adapter for future external Windows SC2 bot executables.
from __future__ import annotations

import os
import subprocess
import threading
import time
from collections import deque
from typing import Any, Dict, Iterable, List, Optional

from core.logger import log_print
from core.process import launch_process

from .starcraft2_contracts import (
    EngineResultDTO,
    EngineStartCommandDTO,
    EngineStatusDTO,
    StarCraft2Event,
)
from .starcraft2_engine_interface import StarCraft2EngineInterface
from .starcraft2_state import StarCraft2RuntimeState

from .external_process_bot_engine import ExternalProcessBotEngine

class ExternalExeBotEngine(ExternalProcessBotEngine):
    engine_name = "external_exe"
    config_section = "external_exe"
