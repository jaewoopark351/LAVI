#20260707_kpopmodder: Added StarCraft2 engine registry selection tests.
import unittest

from plugins.StarCraft2.starcraft2_core.ares_sc2_bot_engine import AresSC2BotEngine
from plugins.StarCraft2.starcraft2_core.external_exe_bot_engine import (
    ExternalExeBotEngine,
)
from plugins.StarCraft2.starcraft2_core.external_jar_bot_engine import (
    ExternalJarBotEngine,
)
from plugins.StarCraft2.starcraft2_core.human_vs_bot_launcher import (
    HumanVsBotLauncher,
)
from plugins.StarCraft2.starcraft2_core.internal_lav_bot_engine import (
    InternalLAVBotEngine,
)
from plugins.StarCraft2.starcraft2_core.micromachine_bot_engine import (
    MicroMachineBotEngine,
)
from plugins.StarCraft2.starcraft2_core.starcraft2_engine_registry import (
    StarCraft2EngineRegistry,
)
from plugins.StarCraft2.starcraft2_core.starcraft2_contracts import EngineResultDTO


class StarCraft2EngineRegistryTests(unittest.TestCase):
    def test_selects_builtin_engines(self):
        registry = StarCraft2EngineRegistry()

        self.assertIsInstance(registry.create("internal_lav_bot"), InternalLAVBotEngine)
        self.assertIsInstance(registry.create("ares_sc2"), AresSC2BotEngine)
        self.assertIsInstance(registry.create("micromachine"), MicroMachineBotEngine)
        self.assertIsInstance(registry.create("external_exe"), ExternalExeBotEngine)
        self.assertIsInstance(registry.create("external_jar"), ExternalJarBotEngine)
        self.assertIsInstance(registry.create("human_vs_bot"), HumanVsBotLauncher)

    #20260715_kpopmodder: Lock all registered engines to the typed public contract.
    def test_builtin_engines_expose_typed_dto_contract(self):
        registry = StarCraft2EngineRegistry()

        for name in registry.names():
            with self.subTest(engine=name):
                engine = registry.create(name)
                self.assertTrue(engine.uses_engine_dto_contract)

    def test_invalid_engine_returns_safe_error_engine(self):
        engine = StarCraft2EngineRegistry().create("not_a_real_engine")

        result = engine.start({})

        self.assertIsInstance(result, EngineResultDTO)
        self.assertFalse(result.ok)
        self.assertEqual("unknown_engine", result.error)
        self.assertFalse(result.running)
        self.assertIn("internal_lav_bot", result.status.status["valid_engines"])


if __name__ == "__main__":
    unittest.main()
