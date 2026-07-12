#20260707_kpopmodder: Added placeholder adapter for future human-vs-bot launcher integration.
from __future__ import annotations

import time
from typing import Any, Dict

from .starcraft2_engine_interface import StarCraft2EngineInterface
from .starcraft2_state import StarCraft2RuntimeState


class HumanVsBotLauncher(StarCraft2EngineInterface):
    engine_name = "human_vs_bot"

    def __init__(self):
        self.state = StarCraft2RuntimeState(engine=self.engine_name)
        self.config_snapshot: Dict[str, Any] = {}
        self.last_started_at = None

    def start(self, config: Dict[str, Any], event_callback=None) -> Dict[str, Any]:
        self.config_snapshot = dict(config or {})
        self.last_started_at = time.time()
        self.state.engine = self.engine_name
        self.state.running = False
        self.state.last_error = None
        return self._result(True, running=False, status=self.get_status())

    def stop(self) -> Dict[str, Any]:
        self.state.mark_stopped("placeholder")
        return self._result(True, running=False, status=self.get_status())

    def shutdown(self) -> Dict[str, Any]:
        return self.stop()

    def get_status(self) -> Dict[str, Any]:
        status = self.state.to_dict()
        status["placeholder"] = True
        status["message"] = (
            "human_vs_bot launcher is reserved for a future ProBots/Human-vs-Bot integration."
        )
        status["config"] = dict(self.config_snapshot)
        status["last_started_at"] = self.last_started_at
        return status

    def is_running(self) -> bool:
        return False

