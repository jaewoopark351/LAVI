#20260815_kpopmodder: Add opt-in live runtime checks for Korean ChatClef commands.
#20260818_kpopmodder: Require fail-closed preflight and structured one-shot observation.
from __future__ import annotations

import unittest

from .preflight.runtime_environment import (
    load_live_runtime_environment,
)
from .submission.gradio_runtime_gateway import (
    LaviGradioRuntimeGateway,
)
from .supervised_live_run import (
    run_supervised_live_command,
)


RUNTIME_ENVIRONMENT = load_live_runtime_environment()
RUNTIME_TESTS_ENABLED = bool(RUNTIME_ENVIRONMENT["live_opt_in"])
MUTATING_RUNTIME_TESTS_ENABLED = bool(RUNTIME_ENVIRONMENT["mutating_opt_in"])


@unittest.skipUnless(
    RUNTIME_TESTS_ENABLED,
    "set LAVI_MINECRAFT_RUNTIME_TESTS=1 to run live LAVI/Minecraft tests",
)
class LaviMinecraftChatClefGradioRuntimeLifecycleTests(unittest.TestCase):
    def setUp(self):
        try:
            gradio_url = str(RUNTIME_ENVIRONMENT.get("gradio_url") or "")
            if not gradio_url:
                raise ValueError("explicit LAVI_GRADIO_URL is required")
            self.gateway = LaviGradioRuntimeGateway(gradio_url)
        except Exception as error:
            message = f"live Gradio gateway unavailable: {type(error).__name__}: {error}"
            if MUTATING_RUNTIME_TESTS_ENABLED:
                self.fail(message)
            self.skipTest(message)

    def test_runtime_status_reports_connected_bridge(self):
        status = self.gateway.read_status()
        bridge = self._bridge(status)
        commands = self._commands(status)

        self.assertTrue(bridge.get("enabled"))
        self.assertTrue(bridge.get("connected"))
        self.assertEqual("connected", bridge.get("lifecycle_state"))
        self.assertIn("active_request_id", commands)
        self.assertIn("active_command", commands)
        self.assertIn("last_result", commands)

    @unittest.skipUnless(
        MUTATING_RUNTIME_TESTS_ENABLED,
        (
            "set LAVI_MINECRAFT_RUNTIME_TESTS=1 and "
            "LAVI_MINECRAFT_RUNTIME_MUTATING=1 to submit a live command"
        ),
    )
    def test_korean_command_reaches_terminal_result_not_only_accepted(self):
        result = run_supervised_live_command(RUNTIME_ENVIRONMENT, self.gateway)
        preflight = result["preflight"]
        observation = result["observation"]
        self.assertEqual("ok", preflight.get("status"), preflight)
        self.assertEqual("accepted", observation.get("submission_outcome"), observation)
        self.assertEqual(1, observation.get("gradio_submit_call_count"), observation)
        self.assertEqual(0, observation.get("automatic_resubmit_count"), observation)
        self.assertEqual(0, observation.get("automatic_rerun_count"), observation)
        self.assertIs(True, observation.get("terminal_lifecycle_observed"), observation)
        self.assertEqual("same_snapshot", observation.get("active_clear_observation"))

    def _bridge(self, status: dict[str, object]) -> dict[str, object]:
        bridge = status.get("details")
        self.assertIsInstance(bridge, dict)
        return dict(bridge)

    def _commands(self, status: dict[str, object]) -> dict[str, object]:
        bridge = self._bridge(status)
        details = bridge.get("details")
        self.assertIsInstance(details, dict)
        commands = details.get("commands")
        self.assertIsInstance(commands, dict)
        return dict(commands)


if __name__ == "__main__":
    unittest.main()
