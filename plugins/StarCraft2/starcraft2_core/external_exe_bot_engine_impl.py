#20260717_kpopmodder: Split from legacy multi-class module for AGENTS 29.1 file/type separation.

#20260707_kpopmodder: Added safe subprocess adapter for future external Windows SC2 bot executables.
from __future__ import annotations

from .external_process_bot_engine import ExternalProcessBotEngine


class ExternalExeBotEngine(ExternalProcessBotEngine):
    engine_name = "external_exe"
    config_section = "external_exe"
