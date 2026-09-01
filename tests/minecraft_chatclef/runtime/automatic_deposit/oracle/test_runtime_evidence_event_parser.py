#20260831_kpopmodder: Lock the complete correlated runtime event schema.
from __future__ import annotations

import unittest

from .runtime_evidence_event_parser import (
    parse_automatic_deposit_runtime_evidence_event,
)


class AutomaticDepositRuntimeEvidenceEventParserTests(unittest.TestCase):
    def test_complete_schema_parses(self):
        event, reason = parse_automatic_deposit_runtime_evidence_event(_line())

        self.assertEqual("RUNTIME_EVIDENCE_EVENT_PARSED", reason)
        self.assertIsNotNone(event)
        self.assertEqual("transfer_observed", event.evidence_key)

    def test_capture_status_is_required(self):
        event, reason = parse_automatic_deposit_runtime_evidence_event(
            _line().replace(" diagnosticCaptureStatus=complete", "")
        )

        self.assertIsNone(event)
        self.assertEqual("RUNTIME_EVIDENCE_REQUIRED_FIELD_MISSING", reason)

    def test_incomplete_capture_cannot_be_product_evidence(self):
        event, reason = parse_automatic_deposit_runtime_evidence_event(
            _line().replace(
                "diagnosticCaptureStatus=complete",
                "diagnosticCaptureStatus=incomplete",
            )
        )

        self.assertIsNone(event)
        self.assertEqual("DIAGNOSTIC_CAPTURE_NOT_COMPLETE", reason)

    def test_unknown_field_is_rejected(self):
        event, reason = parse_automatic_deposit_runtime_evidence_event(
            _line() + " callerClaimedPass=true"
        )

        self.assertIsNone(event)
        self.assertEqual("RUNTIME_EVIDENCE_UNKNOWN_FIELD", reason)


def _line() -> str:
    return (
        "[LAVI ChatClefBoundary] "
        "schemaVersion=automatic-deposit-evidence%2Fv1 "
        "event=automaticDepositEvidence eventSequence=1 "
        "runId=run-R3 rowId=R3 operationId=operation-R3 "
        f"artifactSha256={'1' * 64} fixtureFingerprint={'d' * 64} "
        "evidenceOwner=RUNTIME_LOG evidenceKey=transfer_observed "
        "evidenceValue=true diagnosticCaptureStatus=complete"
    )


if __name__ == "__main__":
    unittest.main()
