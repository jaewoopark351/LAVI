#20260707_kpopmodder: Added name-based StarCraft2 engine registry for internal and external bot adapters.
from __future__ import annotations

from typing import Dict, Type

from .ares_sc2_bot_engine import AresSC2BotEngine
from .external_exe_bot_engine import ExternalExeBotEngine
from .external_jar_bot_engine import ExternalJarBotEngine
from .human_vs_bot_launcher import HumanVsBotLauncher
from .internal_lav_bot_engine import InternalLAVBotEngine
from .micromachine_bot_engine import MicroMachineBotEngine
from .starcraft2_engine_interface import (
    InvalidStarCraft2Engine,
    StarCraft2EngineInterface,
)


class StarCraft2EngineRegistry:
    #20260707_kpopmodder: Keep future MicroMachine/Ketroc/ProBots adapters behind logical engine names.
    def __init__(self):
        self._engine_classes: Dict[str, Type[StarCraft2EngineInterface]] = {
            "internal_lav_bot": InternalLAVBotEngine,
            "ares_sc2": AresSC2BotEngine,
            "micromachine": MicroMachineBotEngine,
            "external_exe": ExternalExeBotEngine,
            "external_jar": ExternalJarBotEngine,
            "human_vs_bot": HumanVsBotLauncher,
        }

    def names(self):
        return list(self._engine_classes.keys())

    def get_engine_class(self, name: str):
        return self._engine_classes.get(self._normalize(name))

    def create(self, name: str) -> StarCraft2EngineInterface:
        normalized = self._normalize(name)
        engine_class = self._engine_classes.get(normalized)
        if engine_class is None:
            return InvalidStarCraft2Engine(normalized or name, self.names())
        return engine_class()

    def invalid_engine_result(self, name: str):
        return InvalidStarCraft2Engine(name, self.names()).get_status()

    @staticmethod
    def _normalize(name: str) -> str:
        return str(name or "").strip().lower()
