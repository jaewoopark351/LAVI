#20260718_kpopmodder: Verify configurable Gradio launch behavior without opening a browser.
import unittest

from app_core.gradio_launch import (
    DEFAULT_GRADIO_HOST,
    DEFAULT_GRADIO_OPEN_BROWSER,
    DEFAULT_GRADIO_PORT_MAX_ATTEMPTS,
    DEFAULT_GRADIO_SHARE,
    DEFAULT_GRADIO_START_PORT,
    load_gradio_launch_options,
)
from app_core.gradio_runtime_launcher import GradioRuntimeLauncher


class GradioLaunchTests(unittest.TestCase):
    def test_load_gradio_launch_options_defaults(self):
        options = load_gradio_launch_options({})

        self.assertEqual(DEFAULT_GRADIO_HOST, options["host"])
        self.assertEqual(47860, DEFAULT_GRADIO_START_PORT)
        self.assertEqual(47860, options["start_port"])
        self.assertGreaterEqual(options["start_port"], 40000)
        self.assertEqual(
            DEFAULT_GRADIO_PORT_MAX_ATTEMPTS,
            options["max_attempts"],
        )
        self.assertEqual(DEFAULT_GRADIO_OPEN_BROWSER, options["open_browser"])
        self.assertEqual(DEFAULT_GRADIO_SHARE, options["share"])

    def test_load_gradio_launch_options_reads_config(self):
        options = load_gradio_launch_options(
            {
                "server_name": "0.0.0.0",
                "server_port": "47870",
                "port_max_attempts": "8",
                "open_browser_on_start": "false",
                "share": "true",
            }
        )

        self.assertEqual("0.0.0.0", options["host"])
        self.assertEqual(47870, options["start_port"])
        self.assertEqual(8, options["max_attempts"])
        self.assertFalse(options["open_browser"])
        self.assertTrue(options["share"])

    def test_load_gradio_launch_options_can_disable_auto_increment(self):
        options = load_gradio_launch_options(
            {
                "server_port": "47870",
                "auto_increment_port": "false",
                "port_max_attempts": "8",
            }
        )

        self.assertEqual(47870, options["start_port"])
        self.assertEqual(1, options["max_attempts"])

    def test_runtime_launcher_passes_resolved_port_and_browser_flag(self):
        calls = []
        interface = _FakeInterface()
        lifecycle = _FakeLifecycle()

        def port_finder(**kwargs):
            calls.append(kwargs)
            return 47871

        launcher = GradioRuntimeLauncher(
            port_finder=port_finder,
            logger=lambda message: None,
        )

        launcher.launch(
            interface,
            runtime_lifecycle=lifecycle,
            host="127.0.0.1",
            start_port=47870,
            max_attempts=3,
            open_browser=True,
            share=False,
        )

        self.assertEqual(
            [
                {
                    "host": "127.0.0.1",
                    "start_port": 47870,
                    "max_attempts": 3,
                }
            ],
            calls,
        )
        self.assertEqual(
            {
                "server_name": "127.0.0.1",
                "server_port": 47871,
                "share": False,
                "inbrowser": True,
            },
            interface.launch_kwargs,
        )
        self.assertTrue(lifecycle.shutdown_called)


class _FakeInterface:
    def __init__(self):
        self.launch_kwargs = None

    def queue(self):
        return self

    def launch(self, **kwargs):
        self.launch_kwargs = kwargs


class _FakeLifecycle:
    def __init__(self):
        self.shutdown_called = False

    def shutdown(self):
        self.shutdown_called = True


if __name__ == "__main__":
    unittest.main()
