#20260715_kpopmodder: Verify the first DTO-native StarCraft2 engine and legacy adapter.
import unittest

from plugins.StarCraft2.starcraft2_core.human_vs_bot_launcher import HumanVsBotLauncher
from plugins.StarCraft2.starcraft2_core.starcraft2_contracts import (
    EngineResultDTO,
    EngineStartCommandDTO,
    EngineStatusDTO,
)
from plugins.StarCraft2.starcraft2_core.starcraft2_engine_interface import (
    LegacyStarCraft2EngineAdapter,
)


#20260715_kpopmodder: Cover DTO-native and compatibility-adapted engine behavior.
class HumanVsBotLauncherContractTests(unittest.TestCase):
    def test_human_vs_bot_uses_typed_engine_contract(self):
        engine = HumanVsBotLauncher()

        result = engine.start(
            EngineStartCommandDTO.from_mapping({"map_name": "PersephoneLE"})
        )
        status = engine.get_status()
        stop_result = engine.stop()

        self.assertIsInstance(result, EngineResultDTO)
        self.assertIsInstance(status, EngineStatusDTO)
        self.assertIsInstance(stop_result, EngineResultDTO)
        self.assertEqual("PersephoneLE", status.to_dict()["config"]["map_name"])
        self.assertTrue(stop_result.stopped)

    def test_legacy_adapter_converts_dict_results_to_dtos(self):
        adapter = LegacyStarCraft2EngineAdapter(_LegacyEngine())

        result = adapter.start(EngineStartCommandDTO.from_mapping({"enabled": True}))
        status = adapter.get_status()
        stop_result = adapter.stop()

        self.assertIsInstance(result, EngineResultDTO)
        self.assertIsInstance(status, EngineStatusDTO)
        self.assertIsInstance(stop_result, EngineResultDTO)
        self.assertTrue(result.running)
        self.assertTrue(stop_result.stopped)


#20260715_kpopmodder: Provide a minimal legacy dict engine for adapter regression coverage.
class _LegacyEngine:
    engine_name = "legacy"

    def __init__(self):
        self.running = False

    def start(self, config, event_callback=None):
        self.running = bool(config.get("enabled"))
        return {"ok": True, "running": self.running, "status": self.get_status()}

    def stop(self):
        self.running = False
        return {"ok": True, "running": False, "stopped": True, "status": self.get_status()}

    def get_status(self):
        return {"running": self.running}

    def is_running(self):
        return self.running


if __name__ == "__main__":
    unittest.main()
