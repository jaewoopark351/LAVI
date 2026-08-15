#20260815_kpopmodder: Add opt-in live runtime checks for Korean ChatClef commands.
from __future__ import annotations

import json
import os
import time
import unittest


RUNTIME_TESTS_ENABLED = os.environ.get("LAVI_MINECRAFT_RUNTIME_TESTS") == "1"
MUTATING_RUNTIME_TESTS_ENABLED = (
    os.environ.get("LAVI_MINECRAFT_RUNTIME_MUTATING") == "1"
)
DEFAULT_GRADIO_URL = "http://127.0.0.1:47860"
DEFAULT_KOREAN_COMMAND = "돌 1개 가져와줘"
TERMINAL_STATUSES = {
    "completed",
    "rejected",
    "failed",
    "cancelled",
    "deadline_exceeded",
    "unknown",
}


@unittest.skipUnless(
    RUNTIME_TESTS_ENABLED,
    "set LAVI_MINECRAFT_RUNTIME_TESTS=1 to run live LAVI/Minecraft tests",
)
class LaviMinecraftChatClefGradioRuntimeLifecycleTests(unittest.TestCase):
    def setUp(self):
        try:
            from gradio_client import Client
        except Exception as error:
            self.skipTest(f"gradio_client unavailable: {type(error).__name__}: {error}")
        self.client = Client(
            os.environ.get("LAVI_GRADIO_URL", DEFAULT_GRADIO_URL),
            verbose=False,
        )

    def test_runtime_status_reports_connected_bridge(self):
        status = self._status_payload()
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
        initial_status = self._status_payload()
        initial_commands = self._commands(initial_status)
        self.assertIsNone(
            initial_commands.get("active_request_id"),
            (
                "live runtime command test requires an idle bridge; "
                "stop the current ChatClef task manually with @stop first"
            ),
        )

        command = os.environ.get(
            "LAVI_MINECRAFT_RUNTIME_KOREAN_COMMAND",
            DEFAULT_KOREAN_COMMAND,
        )
        submitted = self.client.predict(
            command=command,
            api_name="/on_submit_korean_command_click",
        )
        submit_result = json.loads(submitted[0])
        self.assertTrue(submit_result["ok"], submit_result)
        self.assertEqual("accepted", submit_result["status"]["status"])
        submitted_request_id = str(submit_result["status"]["request_id"])
        self.assertTrue(submitted_request_id)

        timeout_sec = float(os.environ.get("LAVI_MINECRAFT_RUNTIME_TIMEOUT_SEC", "60"))
        poll_sec = float(os.environ.get("LAVI_MINECRAFT_RUNTIME_POLL_SEC", "2"))
        deadline = time.monotonic() + timeout_sec
        last_snapshot = {}

        while time.monotonic() < deadline:
            status = self._status_payload()
            commands = self._commands(status)
            last_snapshot = dict(commands)
            last_result = commands.get("last_result") or {}
            if _is_matching_terminal_result(last_result, submitted_request_id):
                self.assertIsNone(commands.get("active_request_id"))
                data = last_result.get("data") or {}
                if isinstance(data, dict) and "result_reason" in data:
                    self.assertTrue(str(data["result_reason"]).strip())
                return
            time.sleep(poll_sec)

        self.fail(
            "live command did not reach a terminal result; "
            f"last command snapshot={json.dumps(last_snapshot, ensure_ascii=False)}"
        )

    def _status_payload(self) -> dict[str, object]:
        result = self.client.predict(api_name="/on_refresh_click_2")
        return json.loads(result[3])

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


class LaviMinecraftChatClefRuntimeResultMatchingTests(unittest.TestCase):
    def test_stale_terminal_result_does_not_match_new_request(self):
        self.assertFalse(
            _is_matching_terminal_result(
                {"request_id": "old-request", "status": "completed"},
                "new-request",
            )
        )

    def test_matching_terminal_result_requires_request_id_and_terminal_status(self):
        self.assertTrue(
            _is_matching_terminal_result(
                {"request_id": "request-1", "status": "completed"},
                "request-1",
            )
        )
        self.assertFalse(
            _is_matching_terminal_result(
                {"request_id": "request-1", "status": "accepted"},
                "request-1",
            )
        )


def _is_matching_terminal_result(
    last_result: object,
    submitted_request_id: str,
) -> bool:
    if not isinstance(last_result, dict):
        return False
    if str(last_result.get("request_id") or "") != submitted_request_id:
        return False
    return str(last_result.get("status") or "") in TERMINAL_STATUSES


if __name__ == "__main__":
    unittest.main()
