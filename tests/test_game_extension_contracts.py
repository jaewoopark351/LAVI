#20260715_kpopmodder: Verify shared GameExtension DTO/runtime/event contracts.
import unittest

from app_core.extensions import (
    GameCommandDTO,
    GameEventBus,
    GameEventDTO,
    GameResultDTO,
    GameRuntimeContextRegistry,
    GameStatusDTO,
)


class GameExtensionContractsTests(unittest.TestCase):
    def test_command_dto_preserves_legacy_dict_payload(self):
        command = GameCommandDTO.from_mapping(
            {
                "action": "Start",
                "profile_name": "default",
                "metadata": {"source": "test"},
            }
        )

        self.assertEqual("start", command.action)
        self.assertEqual("default", command.to_legacy_dict()["profile_name"])
        self.assertEqual({"source": "test"}, command.metadata)

    def test_result_and_status_dtos_accept_dict_payloads(self):
        result = GameResultDTO.from_mapping(
            {"ok": True, "status": {"running": True}},
            action="start",
        )
        status = GameStatusDTO.from_mapping(
            {
                "extension": {
                    "name": "starcraft2",
                    "initialized": True,
                    "started": True,
                },
                "worker": {"running": True},
            }
        )

        self.assertTrue(result.ok)
        self.assertEqual("start", result.action)
        self.assertEqual("starcraft2", status.name)
        self.assertTrue(status.initialized)
        self.assertTrue(status.worker["running"])

    def test_runtime_context_registry_records_command_result_and_status(self):
        registry = GameRuntimeContextRegistry()
        context = registry.get("StarCraft2")

        context.mark_initialized(True)
        context.mark_started(True)
        context.set_command({"action": "status"})
        context.set_result({"ok": True, "status": {"running": True}}, action="status")
        context.set_status({"running": True})

        snapshot = registry.snapshot()["starcraft2"]
        self.assertTrue(snapshot["initialized"])
        self.assertTrue(snapshot["started"])
        self.assertEqual("status", snapshot["last_command"]["action"])
        self.assertTrue(snapshot["last_result"]["ok"])
        self.assertTrue(snapshot["status"]["running"])

    def test_game_event_bus_delivers_typed_event(self):
        events = []
        bus = GameEventBus()
        bus.subscribe(events.append)

        delivered = bus.emit(
            GameEventDTO(
                event_type="extension_started",
                game="starcraft2",
                details={"source": "test"},
            )
        )

        self.assertTrue(delivered)
        self.assertEqual("extension_started", events[0]["event_type"])
        self.assertEqual("starcraft2", events[0]["game"])


if __name__ == "__main__":
    unittest.main()
