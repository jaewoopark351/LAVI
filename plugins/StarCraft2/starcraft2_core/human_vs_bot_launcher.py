#20260707_kpopmodder: Added placeholder adapter for future human-vs-bot launcher integration.
from __future__ import annotations

import time
from typing import Any, Dict

from .starcraft2_contracts import (
    EngineResultDTO,
    EngineStartCommandDTO,
    EngineStatusDTO,
)
from .starcraft2_engine_interface import StarCraft2EngineInterface
from .starcraft2_state import StarCraft2RuntimeState


class HumanVsBotLauncher(StarCraft2EngineInterface):
    engine_name = "human_vs_bot"
    uses_engine_dto_contract = True

    def __init__(self):
        self.state = StarCraft2RuntimeState(engine=self.engine_name)
        self.config_snapshot: Dict[str, Any] = {}
        self.last_started_at = None

    def start(
        self,
        command: EngineStartCommandDTO,
        event_callback=None,
    ) -> EngineResultDTO:
        command = EngineStartCommandDTO.from_mapping(command)
        self.config_snapshot = command.to_dict()
        self.last_started_at = time.time()
        self.state.engine = self.engine_name
        self.state.running = False
        self.state.last_error = None
        return self._result(True, running=False, status=self.get_status().to_dict())

    def stop(self) -> EngineResultDTO:
        self.state.mark_stopped("placeholder")
        return self._result(
            True,
            running=False,
            status=self.get_status().to_dict(),
            stopped=True,
        )

    def shutdown(self) -> EngineResultDTO:
        return self.stop()

    def get_status(self) -> EngineStatusDTO:
        status = self.state.to_dict()
        status["placeholder"] = True
        status["message"] = (
            "human_vs_bot launcher is reserved for a future ProBots/Human-vs-Bot integration."
        )
        status["config"] = dict(self.config_snapshot)
        status["last_started_at"] = self.last_started_at
        return EngineStatusDTO.from_mapping(status, engine=self.engine_name)

    def is_running(self) -> bool:
        return False
