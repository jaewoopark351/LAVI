import unittest
from queue import Queue

from app_core.runtime_lifecycle import RuntimeLifecycle
from core.global_state import GlobalKeys


class FakeGlobalState:
    def __init__(self):
        self.values = {}

    def set_value(self, key, value):
        self.values[key] = value


class FakeTimer:
    def __init__(self, interval, callback):
        self.interval = interval
        self.callback = callback
        self.daemon = False
        self.started = False
        self.cancel_count = 0

    def start(self):
        self.started = True

    def cancel(self):
        self.cancel_count += 1


class FakeQueueComponent:
    def __init__(self, empty=True):
        self.input_queue = Queue()
        if not empty:
            self.input_queue.put("pending")


class FakeShutdownComponent:
    def __init__(self, should_raise=False, exception=None):
        self.should_raise = should_raise
        self.exception = exception
        self.shutdown_count = 0

    def shutdown(self):
        self.shutdown_count += 1
        if self.exception is not None:
            raise self.exception
        if self.should_raise:
            raise RuntimeError("test shutdown failure")


class FakeSongPlayer:
    def __init__(self, playing):
        self.playing = playing

    def is_playing(self):
        return self.playing


class FakeStartComponent:
    def __init__(self, should_raise=False):
        self.should_raise = should_raise
        self.start_count = 0
        self.shutdown_count = 0

    def start(self):
        self.start_count += 1
        if self.should_raise:
            raise RuntimeError("component start failed")

    def shutdown(self):
        self.shutdown_count += 1


class RuntimeLifecycleTests(unittest.TestCase):
    def make_lifecycle(
        self,
        *,
        llm_empty=True,
        translate_empty=True,
        tts_empty=True,
        song_player=None,
        managed_components=None,
        timers=None,
        global_state=None,
        core_components=None,
        optional_components=None,
        shutdown_register=None,
    ):
        timers = timers if timers is not None else []

        def timer_factory(interval, callback):
            timer = FakeTimer(interval, callback)
            timers.append(timer)
            return timer

        lifecycle_kwargs = {
            "managed_components": managed_components or [],
            "llm": FakeQueueComponent(llm_empty),
            "translate": FakeQueueComponent(translate_empty),
            "tts": FakeQueueComponent(tts_empty),
            "song_player": song_player,
            "global_state_instance": global_state or FakeGlobalState(),
            "core_components": core_components,
            "optional_components": optional_components,
            "timer_factory": timer_factory,
        }
        if shutdown_register is not None:
            lifecycle_kwargs["shutdown_register"] = shutdown_register

        return RuntimeLifecycle(**lifecycle_kwargs)

    def test_update_globals_sets_idle_true_when_queues_empty(self):
        state = FakeGlobalState()
        timers = []
        lifecycle = self.make_lifecycle(global_state=state, timers=timers)

        lifecycle.update_globals_periodic()

        self.assertTrue(state.values[GlobalKeys.IS_IDLE])
        self.assertEqual(1, len(timers))
        self.assertTrue(timers[0].started)
        self.assertTrue(timers[0].daemon)

    def test_update_globals_sets_idle_false_while_song_is_playing(self):
        state = FakeGlobalState()
        lifecycle = self.make_lifecycle(
            song_player=FakeSongPlayer(playing=True),
            global_state=state,
        )

        lifecycle.update_globals_periodic()

        self.assertFalse(state.values[GlobalKeys.IS_IDLE])

    def test_update_globals_does_not_create_timer_after_shutdown(self):
        state = FakeGlobalState()
        timers = []
        lifecycle = self.make_lifecycle(global_state=state, timers=timers)
        lifecycle.app_shutdown_done = True

        lifecycle.update_globals_periodic()

        self.assertEqual({}, state.values)
        self.assertEqual([], timers)

    def test_shutdown_cancels_timer_once_and_continues_after_component_error(self):
        timer = FakeTimer(0.5, lambda: None)
        first = FakeShutdownComponent()
        failing = FakeShutdownComponent(should_raise=True)
        last = FakeShutdownComponent()
        lifecycle = self.make_lifecycle(
            managed_components=[first, failing, last],
        )
        lifecycle.global_update_loop = timer

        lifecycle.shutdown()
        lifecycle.shutdown()

        self.assertEqual(1, timer.cancel_count)
        self.assertEqual(1, first.shutdown_count)
        self.assertEqual(1, failing.shutdown_count)
        self.assertEqual(1, last.shutdown_count)

    def test_shutdown_continues_after_component_keyboard_interrupt(self):
        timer = FakeTimer(0.5, lambda: None)
        first = FakeShutdownComponent()
        interrupting = FakeShutdownComponent(exception=KeyboardInterrupt())
        last = FakeShutdownComponent()
        lifecycle = self.make_lifecycle(
            managed_components=[first, interrupting, last],
        )
        lifecycle.global_update_loop = timer

        lifecycle.shutdown()

        self.assertEqual(1, timer.cancel_count)
        self.assertEqual(1, first.shutdown_count)
        self.assertEqual(1, interrupting.shutdown_count)
        self.assertEqual(1, last.shutdown_count)

    def test_start_registers_shutdown_and_starts_updates(self):
        component = FakeStartComponent()
        shutdown_hooks = []
        state = FakeGlobalState()
        timers = []
        lifecycle = self.make_lifecycle(
            managed_components=[component],
            global_state=state,
            timers=timers,
            shutdown_register=shutdown_hooks.append,
        )

        lifecycle.start()

        self.assertEqual([lifecycle.shutdown], shutdown_hooks)
        self.assertEqual(1, component.start_count)
        self.assertTrue(state.values[GlobalKeys.IS_IDLE])
        self.assertEqual(1, len(timers))
        self.assertTrue(timers[0].started)

    def test_start_is_idempotent(self):
        component = FakeStartComponent()
        shutdown_hooks = []
        timers = []
        lifecycle = self.make_lifecycle(
            managed_components=[component],
            timers=timers,
            shutdown_register=shutdown_hooks.append,
        )

        lifecycle.start()
        lifecycle.start()

        self.assertEqual(1, len(shutdown_hooks))
        self.assertEqual(1, component.start_count)
        self.assertEqual(1, len(timers))

    def test_start_components_rolls_back_required_component_failure(self):
        core_ok = FakeStartComponent()
        optional = FakeStartComponent()
        core_fail = FakeStartComponent(should_raise=True)

        lifecycle = self.make_lifecycle(
            managed_components=[core_ok, optional, core_fail],
            core_components=[core_ok, core_fail],
            optional_components=[optional],
        )

        with self.assertRaises(RuntimeError):
            lifecycle.start_components()

        self.assertEqual(1, core_ok.start_count)
        self.assertEqual(1, core_ok.shutdown_count)
        self.assertEqual(1, optional.start_count)
        self.assertEqual(1, optional.shutdown_count)
        self.assertEqual(1, core_fail.start_count)
        self.assertEqual(0, core_fail.shutdown_count)

    def test_start_components_keeps_optional_failures_nonfatal(self):
        core = FakeStartComponent()
        optional = FakeStartComponent(should_raise=True)

        lifecycle = self.make_lifecycle(
            managed_components=[core, optional],
            core_components=[core],
            optional_components=[optional],
        )

        lifecycle.start_components()

        self.assertEqual(1, core.start_count)
        self.assertEqual(1, optional.start_count)
        self.assertEqual(0, optional.shutdown_count)
