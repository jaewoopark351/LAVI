#20260905_kpopmodder: Verify canonical bounded STOP transition diagnostics.
import unittest
from types import SimpleNamespace

from plugins.Minecraft.fabric.chatclef.transport.control.stop import (
    StopControlTransitionLogger,
)


class _Diagnostics:
    def __init__(self):
        self.messages = []

    def info(self, message):
        self.messages.append(message)


class StopControlTransitionLoggerTests(unittest.TestCase):
    def test_emits_only_canonical_fields_without_payload_transcript_or_token(self):
        diagnostics = _Diagnostics()
        logger = StopControlTransitionLogger(diagnostics)
        tracker = SimpleNamespace(
            identity=SimpleNamespace(
                request_id="stop-request",
                message_id="stop-message",
                session_id="session-a",
                server_connection_generation=7,
            ),
            target_scope="tracked_command",
            target=SimpleNamespace(
                request_id="ordinary-request",
                command_message_id="ordinary-message",
                session_id="session-a",
                server_connection_generation=7,
                owner_token="SECRET_OWNER_TOKEN",
            ),
        )

        logger.log(
            tracker=tracker,
            wire_data={
                "control_outcome": "unknown",
                "control_reason": "verification_timeout",
                "target_resolution": "exact",
                "raw_transcript": "SECRET_TRANSCRIPT",
                "payload": "SECRET_PAYLOAD",
            },
            control_status="unknown",
            control_result_delivery="received",
            python_stop_barrier_state="closed",
            ordinary_owner_gate_state="closed",
            quarantine_active=True,
            retirement_evidence_kind="unknown_quarantine",
        )

        message = diagnostics.messages[0]
        fields = _transition_fields(message)
        self.assertEqual(
            set(fields),
            set(StopControlTransitionLogger.CANONICAL_FIELDS),
        )
        self.assertEqual(fields["quarantine_active"], "true")
        self.assertEqual(fields["python_stop_barrier_state"], "closed")
        self.assertEqual(fields["retirement_evidence_kind"], "unknown_quarantine")
        self.assertNotIn("SECRET_OWNER_TOKEN", message)
        self.assertNotIn("SECRET_TRANSCRIPT", message)
        self.assertNotIn("SECRET_PAYLOAD", message)


def _transition_fields(message):
    parts = message.split()
    if not parts or parts[0] != "event=stop_control_transition":
        raise AssertionError(f"not a STOP transition log: {message!r}")
    return dict(part.split("=", 1) for part in parts[1:])


if __name__ == "__main__":
    unittest.main()
