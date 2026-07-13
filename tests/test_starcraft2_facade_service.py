#20260713_kpopmodder: Verify StarCraft2 UI callbacks stay behind the facade boundary.
import unittest
from unittest import mock

from plugins.StarCraft2.starcraft2 import StarCraft2
from plugins.StarCraft2.starcraft2_core.starcraft2_dto import (
    StarCraft2LocalMatchCommand,
)
from plugins.StarCraft2.starcraft2_core.starcraft2_event_bus import (
    StarCraft2EventBus,
    _StarCraft2EventBus,
)
from plugins.StarCraft2.starcraft2_core.starcraft2_facade_service import (
    StarCraft2FacadeService,
    _StarCraft2FacadeService,
)


class StarCraft2FacadeServiceTests(unittest.TestCase):
    def test_public_facade_name_keeps_legacy_alias(self):
        self.assertIs(StarCraft2FacadeService, _StarCraft2FacadeService)
        self.assertIs(StarCraft2EventBus, _StarCraft2EventBus)

    def test_local_human_button_delegates_to_facade_service(self):
        plugin = StarCraft2()
        plugin._facade_service.on_local_human_vs_changeling_click = mock.Mock(
            return_value='{"ok": true}'
        )

        result = plugin.on_local_human_vs_changeling_click(
            "C:\\Tools\\LavHumanVsBot.exe",
            "C:\\Tools",
            "--bot changeling --race Protoss",
            "5677,5678",
            ai_race="Zerg",
        )

        self.assertEqual('{"ok": true}', result)
        plugin._facade_service.on_local_human_vs_changeling_click.assert_called_once_with(
            "C:\\Tools\\LavHumanVsBot.exe",
            "C:\\Tools",
            "--bot changeling --race Protoss",
            "5677,5678",
            ai_race="Zerg",
        )

    def test_event_bus_emits_stable_event_contract(self):
        events = []
        bus = StarCraft2EventBus()
        bus.subscribe(events.append)

        delivered = bus.emit({"event_type": "game_started", "details": {"source": "test"}})

        self.assertTrue(delivered)
        self.assertEqual("game_started", events[0]["event_type"])
        self.assertEqual({"source": "test"}, events[0]["details"])
        self.assertEqual("starcraft2", events[0]["source"])

    def test_local_match_command_from_mapping_accepts_stable_ports_key(self):
        command = StarCraft2LocalMatchCommand.from_mapping(
            {
                "executable_path": "C:\\Tools\\LavHumanVsBot.exe",
                "working_directory": "C:\\Tools",
                "args": ["--race", "Protoss"],
                "ports": "5677,5678",
                "bot_name": "changeling",
            }
        )

        self.assertEqual([5677, 5678], command.proxy_ports)
        self.assertEqual("--race Protoss", command.args)
        self.assertEqual("changeling", command.bot_name)


if __name__ == "__main__":
    unittest.main()
