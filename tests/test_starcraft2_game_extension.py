#20260707_kpopmodder: Added StarCraft2 GameExtension compatibility tests.
import time
import unittest
from unittest import mock

from app_core.extensions.game_extension_context import GameExtensionContext
from app_core.extensions.game_extension_interface import GameExtensionInterface
from app_core.extensions.starcraft2_game_extension import StarCraft2GameExtension
from plugins.StarCraft2.starcraft2_core.starcraft2_observation_tracker import (
    SC2ObservationTracker,
)
from plugins.StarCraft2.starcraft2_core.starcraft2_reaction_policy import (
    StarCraft2ReactionPolicy,
)
from plugins.StarCraft2.starcraft2_core.starcraft2_reaction_runtime import (
    build_starcraft2_reaction_text,
    handle_starcraft2_status_event,
)


class FakeConfigManager:
    def __init__(self, enabled=False, auto_launch=False):
        self.enabled = enabled
        self.auto_launch = auto_launch

    def get_bool(self, key, default=False):
        if key == "enabled":
            return self.enabled
        if key == "auto_launch":
            return self.auto_launch
        return default


class FakeStarCraft2Plugin:
    def __init__(self, enabled=False, auto_launch=False):
        self.config_manager = FakeConfigManager(enabled=enabled, auto_launch=auto_launch)
        self.status_event_callback = None
        self.started = False
        self.stopped = False

    def set_status_event_callback(self, callback):
        self.status_event_callback = callback

    def start(self, overrides=None, launch_source="manual"):
        if not self.config_manager.enabled:
            return {"ok": False, "action": "start", "error": "enabled_false"}
        self.started = True
        return {"ok": True, "action": "start", "running": True}

    def stop(self):
        self.stopped = True
        self.started = False
        return {"ok": True, "action": "stop", "running": False}

    def get_status(self):
        return {
            "enabled": self.config_manager.enabled,
            "state": {"running": self.started},
        }


class StarCraft2GameExtensionTests(unittest.TestCase):
    def test_screen_commentary_classifies_starcraft_observation(self):
        self.skipTest("ScreenVision commentary was removed; SC2 uses AI telemetry only.")
        commentary = build_starcraft2_screen_commentary(
            "스타크래프트 II 화면에서 넥서스와 파일런 건설이 보입니다."
        )

        self.assertEqual("construction", commentary["category"])
        self.assertIn("건설", commentary["text"])
        self.assertIsNone(build_starcraft2_screen_commentary("유튜브 화면입니다."))
        self.assertEqual(
            "economy",
            build_starcraft2_screen_commentary(
                "스타크래프트 II 게임에서 블루 크리스탈과 자원을 채취하고 있습니다."
            )["category"],
        )
        self.assertIsNone(
            build_starcraft2_screen_commentary(
                "스타크래프트 관련 YouTube 검색 결과와 편집기 파일 목록입니다."
            )
        )

    def test_screen_commentary_is_active_only_and_throttled(self):
        self.skipTest("ScreenVision commentary was removed; SC2 uses AI telemetry only.")
        plugin = FakeStarCraft2Plugin(enabled=True)
        tts = mock.Mock()
        extension = StarCraft2GameExtension(plugin=plugin)
        extension.initialize(GameExtensionContext(llm=mock.Mock(), tts=tts))
        extension.start()

        plugin.status_event_callback({"event_type": "game_started"})
        tts.receive_input.reset_mock()
        observation = {"observation": "스타크래프트 II에서 넥서스 건설 상황이 보입니다."}
        extension._on_screen_observation_event(observation)
        extension._on_screen_observation_event(observation)

        self.assertEqual(1, tts.receive_input.call_count)
        self.assertIn("건설", tts.receive_input.call_args.args[0])
        extension.stop()
        extension._on_screen_observation_event(observation)
        self.assertEqual(1, tts.receive_input.call_count)

    def test_ai_observation_tracker_ignores_baseline_and_detects_transitions(self):
        tracker = SC2ObservationTracker()
        base = {
            "schema": 1,
            "role": "ai",
            "bot": "sharkbot",
            "game_loop": 224,
            "minerals": 50,
            "food_used": 12,
            "food_cap": 15,
            "food_workers": 12,
            "food_army": 0,
            "army_count": 0,
            "self_units": 12,
            "visible_enemy_units": 0,
            "under_construction_units": 0,
        }
        self.assertEqual([], tracker.update(base))
        changed = dict(base)
        changed.update({"game_loop": 448, "under_construction_units": 1})
        self.assertEqual("building_started", tracker.update(changed)[0]["event_type"])
        self.assertEqual([], tracker.update(changed))

    def test_observation_tracker_emits_each_type_and_completed_upgrade(self):
        tracker = SC2ObservationTracker()
        base = {
            "schema": 1,
            "role": "ai",
            "bot": "changeling",
            "game_loop": 224,
            "minerals": 50,
            "vespene": 0,
            "food_used": 10,
            "food_cap": 20,
            "food_workers": 10,
            "food_army": 0,
            "army_count": 0,
            "idle_workers": 0,
            "self_units": 12,
            "visible_enemy_units": 0,
            "under_construction_units": 0,
            "unit_type_counts": {"104": 10, "106": 1},
            "under_construction_type_counts": {},
            "upgrade_ids": [],
        }
        changed = dict(base)
        changed.update({
            "game_loop": 448,
            "self_units": 17,
            "food_workers": 11,
            "under_construction_units": 1,
            "unit_type_counts": {"89": 1, "104": 11, "105": 2, "106": 1},
            "under_construction_type_counts": {"89": 1},
            "upgrade_ids": [66, 75],
        })

        self.assertEqual([], tracker.update(base))
        events = tracker.update(changed)

        self.assertEqual(
            [
                ("building_started", "89"),
                ("unit_produced", "104"),
                ("unit_produced", "105"),
                ("upgrade_completed", "66"),
                ("upgrade_completed", "75"),
            ],
            [
                (
                    event["event_type"],
                    event["details"].get("unit_type_id")
                    or event["details"].get("upgrade_id"),
                )
                for event in events
            ],
        )

    def test_incomplete_egg_is_not_misclassified_as_building(self):
        tracker = SC2ObservationTracker()
        base = {
            "schema": 1,
            "role": "ai",
            "game_loop": 224,
            "self_units": 1,
            "under_construction_units": 0,
            "unit_type_counts": {"151": 1},
            "under_construction_type_counts": {},
        }
        changed = dict(base)
        changed.update({
            "game_loop": 448,
            "self_units": 2,
            "under_construction_units": 1,
            "unit_type_counts": {"103": 1, "151": 1},
            "under_construction_type_counts": {"103": 1},
        })

        self.assertEqual([], tracker.update(base))
        events = tracker.update(changed)

        self.assertEqual(["unit_produced"], [event["event_type"] for event in events])
        self.assertEqual("103", events[0]["details"]["unit_type_id"])

    def test_reaction_text_uses_shared_speech_name_for_extractor(self):
        event = {
            "event_type": "building_started",
            "details": {"unit_changes": {"88": 1}},
        }

        self.assertEqual(
            "내가 추출장 건설을 시작했어요.",
            build_starcraft2_reaction_text(event),
        )

    def test_reaction_text_uses_shared_speech_name_for_spawning_pool(self):
        event = {
            "event_type": "building_started",
            "details": {"unit_changes": {"89": 1}},
        }

        self.assertEqual(
            "내가 산란못 건설을 시작했어요.",
            build_starcraft2_reaction_text(event),
        )

    def test_plural_building_reaction_uses_generic_counter(self):
        event = {
            "event_type": "building_started",
            "details": {"unit_changes": {"61": 2}},
        }

        self.assertEqual(
            "내가 융화소 2개 건설을 시작했어요.",
            build_starcraft2_reaction_text(event),
        )

    def test_plural_unit_reaction_uses_generic_counter_and_particle(self):
        event = {
            "event_type": "unit_produced",
            "details": {"unit_changes": {"84": 2}},
        }

        self.assertEqual(
            "내가 탐사정 2개를 생산했어요.",
            build_starcraft2_reaction_text(event),
        )

    def test_building_cooldown_is_scoped_by_unit_type(self):
        policy = StarCraft2ReactionPolicy(min_interval_sec=8)
        hatchery = {
            "event_type": "building_started",
            "details": {"unit_changes": {"86": 1}},
        }
        spawning_pool = {
            "event_type": "building_started",
            "details": {"unit_changes": {"89": 1}},
        }

        self.assertEqual("building_started:86", policy.event_key(hatchery))
        self.assertEqual("building_started:89", policy.event_key(spawning_pool))
        self.assertTrue(policy.should_emit(hatchery))
        self.assertTrue(policy.should_emit(spawning_pool))
        self.assertFalse(policy.should_emit(spawning_pool))

    def test_situation_text_uses_shared_speech_name_for_extractor(self):
        event = {
            "event_type": "situation_update",
            "details": {
                "snapshot": {
                    "unit_type_counts": {"88": 1},
                    "minerals": 50,
                    "vespene": 0,
                    "food_used": 12,
                    "food_cap": 14,
                }
            },
        }

        text = build_starcraft2_reaction_text(event)

        self.assertIn("추출장 1기", text)
        self.assertNotIn("extractor", text)

    def test_transient_zerg_events_and_army_milestone_are_log_only(self):
        events = (
            {
                "event_type": "unit_produced",
                "details": {"unit_changes": {"103": 2, "151": -1}},
            },
            {
                "event_type": "unit_produced",
                "details": {"unit_changes": {"151": 1}},
            },
            {
                "event_type": "unit_lost",
                "details": {"unit_type_id": "103", "unit_changes": {"103": 1}},
            },
            {
                "event_type": "unit_lost",
                "details": {"unit_type_id": "151", "unit_changes": {"151": 1}},
            },
            {
                "event_type": "army_milestone",
                "details": {"snapshot": {"army_count": 15}},
            },
        )

        for event in events:
            with self.subTest(event_type=event["event_type"]):
                tts = mock.Mock()
                memory_store = mock.Mock()
                policy = StarCraft2ReactionPolicy(min_interval_sec=0)
                with mock.patch(
                    "plugins.StarCraft2.starcraft2_core.starcraft2_reaction_runtime.log_print"
                ) as log_print:
                    emitted = handle_starcraft2_status_event(
                        mock.Mock(), tts, memory_store, policy, event
                    )

                self.assertFalse(emitted)
                tts.receive_input.assert_not_called()
                log_print.assert_called_once_with(
                    f"[StarCraft2Reaction] event={event['event_type']}"
                )
                memory_store.add_raw_event.assert_called_once()

    def test_non_egg_unit_production_still_uses_tts(self):
        event = {
            "event_type": "unit_produced",
            "details": {"unit_changes": {"106": 1}},
        }
        tts = mock.Mock()

        emitted = handle_starcraft2_status_event(
            mock.Mock(),
            tts,
            None,
            StarCraft2ReactionPolicy(min_interval_sec=0),
            event,
        )

        self.assertTrue(emitted)
        tts.receive_input.assert_called_once_with("내가 대군주를 생산했어요.")

    def test_single_low_signal_production_is_log_only_but_batch_still_speaks(self):
        policy = StarCraft2ReactionPolicy(min_interval_sec=0)
        tts = mock.Mock()
        memory_store = mock.Mock()

        with mock.patch(
            "plugins.StarCraft2.starcraft2_core.starcraft2_reaction_runtime.log_print"
        ):
            emitted = handle_starcraft2_status_event(
                mock.Mock(),
                tts,
                memory_store,
                policy,
                {
                    "event_type": "unit_produced",
                    "details": {"unit_type_id": "104", "unit_changes": {"104": 1}},
                },
            )

        self.assertFalse(emitted)
        tts.receive_input.assert_not_called()
        memory_store.add_raw_event.assert_called_once()

        emitted = handle_starcraft2_status_event(
            mock.Mock(),
            tts,
            memory_store,
            policy,
            {
                "event_type": "unit_produced",
                "details": {"unit_type_id": "104", "unit_changes": {"104": 2}},
            },
        )

        self.assertTrue(emitted)
        tts.receive_input.assert_called_once()

    def test_non_transient_unit_loss_still_uses_tts(self):
        event = {
            "event_type": "unit_lost",
            "details": {"unit_type_id": "106", "unit_changes": {"106": 1}},
        }
        tts = mock.Mock()

        emitted = handle_starcraft2_status_event(
            mock.Mock(),
            tts,
            None,
            StarCraft2ReactionPolicy(min_interval_sec=0),
            event,
        )

        self.assertTrue(emitted)
        tts.receive_input.assert_called_once_with("내 대군주 1기를 잃었어요.")

    def test_game_end_cancels_stale_tts_and_next_game_resumes_speech(self):
        plugin = FakeStarCraft2Plugin(enabled=True)
        tts = mock.Mock()
        extension = StarCraft2GameExtension(plugin=plugin)
        extension.initialize(GameExtensionContext(llm=mock.Mock(), tts=tts))

        plugin.status_event_callback({"event_type": "game_started"})
        tts.reset_mock()

        plugin.status_event_callback({"event_type": "game_ended"})
        plugin.status_event_callback({"event_type": "game_ended"})
        plugin.status_event_callback({
            "event_type": "building_started",
            "details": {"unit_changes": {"88": 1}},
        })
        plugin.status_event_callback({"event_type": "game_won"})

        tts.cancel_pending.assert_called_once_with(reason="starcraft2_game_ended")
        tts.receive_input.assert_called_once_with("내가 이번 경기를 이겼어요.")

        tts.receive_input.reset_mock()
        plugin.status_event_callback({"event_type": "game_started"})
        plugin.status_event_callback({
            "event_type": "building_started",
            "details": {"unit_changes": {"88": 1}},
        })

        tts.receive_input.assert_called_once_with("내가 추출장 건설을 시작했어요.")

    def test_implements_game_extension_interface(self):
        extension = StarCraft2GameExtension(plugin=FakeStarCraft2Plugin())

        self.assertIsInstance(extension, GameExtensionInterface)
        self.assertEqual("starcraft2", extension.name)

    def test_start_stop_status_commands(self):
        plugin = FakeStarCraft2Plugin(enabled=True)
        extension = StarCraft2GameExtension(plugin=plugin)
        extension.initialize(GameExtensionContext(llm=mock.Mock(), tts=mock.Mock()))
        extension.start()

        queued = extension.handle_command({"action": "start"})
        time.sleep(0.15)
        status = extension.handle_command("status")
        extension.stop()

        self.assertTrue(queued["ok"])
        self.assertTrue(queued["queued"])
        self.assertTrue(plugin.started or plugin.stopped)
        self.assertTrue(status["ok"])
        self.assertIn("status", status)
        self.assertTrue(plugin.stopped)

    def test_disabled_state_does_not_auto_launch(self):
        plugin = FakeStarCraft2Plugin(enabled=False, auto_launch=False)
        extension = StarCraft2GameExtension(plugin=plugin)
        extension.initialize(GameExtensionContext(llm=mock.Mock(), tts=mock.Mock()))

        extension.start()
        status = extension.get_status()
        extension.shutdown()

        self.assertFalse(plugin.started)
        self.assertFalse(status["enabled"])
        self.assertTrue(status["worker"]["running"])


if __name__ == "__main__":
    unittest.main()
