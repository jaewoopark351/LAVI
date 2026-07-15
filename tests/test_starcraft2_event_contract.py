#20260715_kpopmodder: Verify the typed SC2 stdout-to-EventBus boundary and legacy callbacks.
import unittest
from unittest import mock

from plugins.StarCraft2.starcraft2_core.starcraft2_contracts import StarCraft2Event
from plugins.StarCraft2.starcraft2_core.starcraft2_event_bus import StarCraft2EventBus
from plugins.StarCraft2.starcraft2_core.starcraft2_event_service import (
    StarCraft2EngineEventService,
    StarCraft2LadderProxyEventService,
)
from plugins.StarCraft2.starcraft2_core.starcraft2_reaction_runtime import (
    StarCraft2ReactionRuntime,
)
from plugins.StarCraft2.starcraft2_core.starcraft2_reaction_io import (
    StarCraft2ReactionMemoryRecorder,
)
from plugins.StarCraft2.starcraft2_core.starcraft2_reaction_policy import (
    StarCraft2ReactionPolicy,
)


class StarCraft2EventContractTests(unittest.TestCase):
    def make_service(self, tracker=None):
        state = mock.Mock()
        bus = StarCraft2EventBus()
        engine_service = StarCraft2EngineEventService(state, event_bus=bus)
        if tracker is None:
            tracker = mock.Mock()
            tracker.update.return_value = []
        service = StarCraft2LadderProxyEventService(
            engine_service,
            tracker,
            event_bus=bus,
        )
        return service, state, bus, tracker

    def test_stdout_line_returns_typed_event(self):
        service, _, _, _ = self.make_service()

        events = service.parse_line("stdout", "Starting the match.")

        self.assertEqual(1, len(events))
        self.assertIsInstance(events[0], StarCraft2Event)
        self.assertEqual("game_started", events[0].event_type)
        self.assertEqual("stdout", events[0].details["source"])

    def test_observation_json_returns_typed_tracker_events(self):
        tracker = mock.Mock()
        tracker.update.return_value = [
            StarCraft2Event("unit_produced", {"unit_type_id": "104"})
        ]
        service, _, _, _ = self.make_service(tracker)

        events = service.parse_line(
            "stdout",
            'prefix LAV_OBSERVATION {"minerals": 50}',
        )

        tracker.update.assert_called_once_with({"minerals": 50})
        self.assertIsInstance(events[0], StarCraft2Event)
        self.assertEqual("unit_produced", events[0].event_type)

    def test_invalid_observation_json_is_ignored_without_tracker_call(self):
        service, _, _, tracker = self.make_service()

        events = service.parse_line("stdout", "LAV_OBSERVATION {invalid-json")

        self.assertEqual([], events)
        tracker.update.assert_not_called()

    def test_game_ended_is_emitted_once_for_both_clients(self):
        service, _, _, _ = self.make_service()
        service.parse_line("stdout", "Starting the match.")

        first = service.parse_line(
            "stdout", "changeling : Client changed status from in_game to ended"
        )
        second = service.parse_line(
            "stdout", "LAVHuman : Client changed status from in_game to ended"
        )

        self.assertEqual(["game_ended"], [event.event_type for event in first])
        self.assertEqual([], second)

    def test_player_results_are_mapped_from_ai_perspective(self):
        service, _, _, _ = self.make_service()

        won = service.parse_line(
            "stdout", "[LavHumanVsBot] Finished with result: Player2Win"
        )
        lost = service.parse_line(
            "stdout", "[LavHumanVsBot] Finished with result: Player1Win"
        )

        self.assertEqual("game_won", won[0].event_type)
        self.assertEqual("game_lost", lost[0].event_type)

    def test_success_diagnostic_and_end_tail_stay_log_only(self):
        service, _, _, _ = self.make_service()

        success = service.parse_line(
            "stdout", "[BotLaunchDiagnostics] failed=false pid=1234"
        )
        tail = service.parse_line(
            "stdout", "expected Action but got RESPONSE_NOT_SET"
        )

        self.assertEqual([], success)
        self.assertEqual([], tail)

    def test_engine_service_updates_state_and_bus_with_typed_event(self):
        service, state, bus, _ = self.make_service()
        bus.emit = mock.Mock(return_value=True)
        event = StarCraft2Event("game_started")

        service.engine_event_service.update_state(event)

        state.update_event.assert_called_once()
        self.assertIs(event, state.update_event.call_args.args[0])
        emitted = bus.emit.call_args.args[0]
        self.assertIsInstance(emitted, StarCraft2Event)
        self.assertIs(event, emitted)

    def test_event_bus_converts_to_dict_only_for_legacy_subscribers(self):
        received = []
        bus = StarCraft2EventBus()
        bus._common_event_bridge.emit = mock.Mock(return_value=True)
        bus.subscribe(received.append)

        event = StarCraft2Event("game_started", {"source": "typed"})
        bus.emit(event)

        self.assertIs(event, bus._common_event_bridge.emit.call_args.args[0])
        self.assertIsInstance(received[0], dict)
        self.assertEqual("game_started", received[0]["event_type"])

    #20260715_kpopmodder: Internal subscribers can stay typed while legacy callbacks get dicts.
    def test_event_bus_typed_subscriber_receives_dto_without_dict_conversion(self):
        received = []
        legacy = []
        bus = StarCraft2EventBus()
        bus.subscribe_typed(received.append)
        bus.subscribe(legacy.append)

        event = StarCraft2Event("proxy_stopped", {"returncode": 0})
        delivered = bus.emit(event)

        self.assertTrue(delivered)
        self.assertIs(event, received[0])
        self.assertIsInstance(legacy[0], dict)
        self.assertEqual("proxy_stopped", legacy[0]["event_type"])

    def test_subscriber_failure_does_not_block_later_subscriber(self):
        received = []
        bus = StarCraft2EventBus()
        bus.subscribe(mock.Mock(side_effect=RuntimeError("subscriber failed")))
        bus.subscribe(received.append)

        delivered = bus.emit(StarCraft2Event("game_started"))

        self.assertTrue(delivered)
        self.assertEqual("game_started", received[0]["event_type"])

    def test_reaction_runtime_passes_typed_event_to_policy_and_memory(self):
        memory_recorder = mock.Mock()
        policy = mock.Mock()
        policy.should_emit.return_value = True
        tts_adapter = mock.Mock()
        tts_adapter.speak.return_value = True
        runtime = StarCraft2ReactionRuntime(
            llm=None,
            tts=None,
            policy=policy,
            memory_recorder=memory_recorder,
            tts_adapter=tts_adapter,
        )
        bus = StarCraft2EventBus()
        bus.subscribe(runtime.handle_status_event)

        bus.emit(StarCraft2Event("game_won", {"result": "Player2Win"}))

        stored = memory_recorder.store_event.call_args.args[0]
        filtered = policy.should_emit.call_args.args[0]
        self.assertIsInstance(stored, StarCraft2Event)
        self.assertIsInstance(filtered, StarCraft2Event)
        self.assertEqual("game_won", stored.event_type)
        tts_adapter.speak.assert_called_once_with("내가 이번 경기를 이겼어요.")

    def test_legacy_status_callback_delegates_to_typed_core(self):
        runtime = StarCraft2ReactionRuntime(llm=None, tts=None)
        runtime.handle_event = mock.Mock(return_value=True)

        result = runtime.handle_status_event(
            {"event_type": "game_won", "details": {"result": "Player2Win"}}
        )

        self.assertTrue(result)
        event = runtime.handle_event.call_args.args[0]
        self.assertIsInstance(event, StarCraft2Event)
        self.assertEqual("game_won", event.event_type)

    def test_policy_accepts_typed_event_contract(self):
        policy = StarCraft2ReactionPolicy(min_interval_sec=0)

        emitted = policy.should_emit(
            StarCraft2Event(
                "unit_produced",
                {"unit_type_id": "106", "unit_changes": {"106": 1}},
            )
        )

        self.assertTrue(emitted)

    def test_memory_recorder_serializes_typed_event_at_storage_edge(self):
        memory_store = mock.Mock()
        recorder = StarCraft2ReactionMemoryRecorder(memory_store)
        event = StarCraft2Event("game_won", {"result": "Player2Win"})

        recorder.store_event(event)

        args = memory_store.add_raw_event.call_args.args
        kwargs = memory_store.add_raw_event.call_args.kwargs
        self.assertEqual("starcraft2_game_event", args[0])
        self.assertIn('"event_type": "game_won"', args[1])
        self.assertEqual({"event_type": "game_won"}, kwargs["metadata"])

    def test_typed_game_end_cancels_once_and_result_still_speaks(self):
        tts_adapter = mock.Mock()
        tts_adapter.speak.return_value = True
        runtime = StarCraft2ReactionRuntime(
            llm=None,
            tts=None,
            policy=StarCraft2ReactionPolicy(min_interval_sec=0),
            tts_adapter=tts_adapter,
            post_game_suppression=True,
        )

        runtime.handle_event(StarCraft2Event("game_ended"))
        runtime.handle_event(StarCraft2Event("game_ended"))
        runtime.handle_event(StarCraft2Event("game_won"))

        tts_adapter.cancel_pending.assert_called_once_with("game_ended", {})
        tts_adapter.speak.assert_called_once_with("내가 이번 경기를 이겼어요.")

    def test_typed_proxy_stopped_is_suppressed_after_game_end(self):
        policy = StarCraft2ReactionPolicy(min_interval_sec=0)
        tts_adapter = mock.Mock()
        runtime = StarCraft2ReactionRuntime(
            llm=None,
            tts=None,
            policy=policy,
            tts_adapter=tts_adapter,
            post_game_suppression=True,
        )

        runtime.handle_event(StarCraft2Event("game_ended"))
        emitted = runtime.handle_event(StarCraft2Event("proxy_stopped"))

        self.assertFalse(emitted)
        tts_adapter.speak.assert_not_called()


if __name__ == "__main__":
    unittest.main()
