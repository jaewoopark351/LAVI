#20260630_kpopmodder: Covers Ctrl+C-safe ScreenVision auto-watch shutdown cleanup.
import unittest

from plugins.ScreenVision.screen_vision_core.auto_watch_controller import (
    AutoWatchController,
)
from plugins.ScreenVision.screen_vision_core.display_hotkey import (
    ScreenVisionDisplayHotkey,
)
from plugins.ScreenVision.screen_vision_core.latest_display_runtime import (
    ScreenVisionLatestDisplayRuntime,
)


class FakeJoinInterruptedThread:
    def is_alive(self):
        return True

    def join(self, timeout=None):
        raise KeyboardInterrupt()


class FakeLiveTextbox:
    def __init__(self, messages):
        self.messages = messages

    def print(self, message):
        self.messages.append(message)


class AutoWatchControllerShutdownTests(unittest.TestCase):
    def test_shutdown_swallows_keyboard_interrupt_during_join(self):
        status_messages = []
        controller = AutoWatchController(
            capture_callback=lambda: None,
            difference_callback=lambda previous, current: 0.0,
            change_callback=lambda image, difference: None,
            status_callback=status_messages.append,
        )
        controller._thread = FakeJoinInterruptedThread()

        controller.shutdown()

        self.assertEqual(["[ScreenVision] Auto Watch stopped."], status_messages)


class FakeScreenVisionOwner:
    def __init__(self):
        import threading

        self.latest_display_stop_event = threading.Event()
        self.latest_display_thread = None
        self.latest_display_interval_seconds = 0.01
        self.messages = []
        self.live_textbox = FakeLiveTextbox(self.messages)
        self.force_calls = 0

    def force_latest_screen(self, **kwargs):
        self.force_calls += 1
        self.latest_display_stop_event.set()


class ScreenVisionLatestDisplayRuntimeTests(unittest.TestCase):
    def test_loop_uses_owner_force_latest_screen_and_interval_stop_event(self):
        owner = FakeScreenVisionOwner()
        runtime = ScreenVisionLatestDisplayRuntime(owner)

        runtime.loop()

        self.assertEqual(1, owner.force_calls)

    def test_start_and_stop_preserve_status_messages(self):
        owner = FakeScreenVisionOwner()
        runtime = ScreenVisionLatestDisplayRuntime(owner)

        runtime.start()
        runtime.shutdown()
        runtime.stop()

        self.assertIn(
            "[ScreenVision] Latest Display started. interval=0.0s",
            owner.messages,
        )
        self.assertIn("[ScreenVision] Latest Display stopped.", owner.messages)


class FakeKeyboard:
    def __init__(self):
        self.added = []
        self.removed = []

    def add_hotkey(self, hotkey, callback):
        self.added.append((hotkey, callback))
        return "handle"

    def remove_hotkey(self, handle):
        self.removed.append(handle)


class ScreenVisionDisplayHotkeyTests(unittest.TestCase):
    def test_register_and_unregister_delegate_to_keyboard_module(self):
        keyboard = FakeKeyboard()
        messages = []
        callback = lambda: None
        runtime = ScreenVisionDisplayHotkey(
            keyboard_module=keyboard,
            hotkey_provider=lambda: "ctrl+shift+alt+d",
            callback=callback,
            status_callback=messages.append,
        )

        handle = runtime.register()
        cleared = runtime.unregister(handle)

        self.assertEqual("handle", handle)
        self.assertIsNone(cleared)
        self.assertEqual([("ctrl+shift+alt+d", callback)], keyboard.added)
        self.assertEqual(["handle"], keyboard.removed)
        self.assertEqual(
            ["[ScreenVision] Display hotkey registered: ctrl+shift+alt+d"],
            messages,
        )


if __name__ == "__main__":
    unittest.main()
