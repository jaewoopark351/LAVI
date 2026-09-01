#20260901_kpopmodder: Verify the sealed production-log delta to P1 evidence adapter end to end.
from __future__ import annotations

import unittest

from ....evidence.latest_log_delta_result import _create_latest_log_delta_result
from .p1_runtime_log_adapter import adapt_p1_store_home_runtime_log


class P1StoreHomeRuntimeLogAdapterTests(unittest.TestCase):
    def test_adapts_realistic_mixed_production_log_to_bound_p1_evidence(self):
        delta = _delta(
            _generic_boundary(9)
            + "\n"
            + _store_home_start(10)
            + "\n"
            + _generic_diagnostic(11)
            + "\n"
            + _store_home_activation(12)
            + "\n"
            + _store_home_terminal(13)
            + "\n"
        )

        result = adapt_p1_store_home_runtime_log(
            delta,
            expected_candidate_position="12, 64, -9",
            expected_run_manifest_id="manifest-p1",
        )

        self.assertTrue(result.ok, result.reason)
        self.assertEqual("17", result.operation_id)
        self.assertTrue(all(result.evidence_mapping().values()))

    def test_propagates_a_malformed_target_as_fail_closed_evidence(self):
        malformed = _store_home_activation(12).replace(
            " diagnosticCaptureStatus=complete",
            "",
        )

        result = adapt_p1_store_home_runtime_log(
            _delta(malformed + "\n"),
            expected_candidate_position="12, 64, -9",
            expected_run_manifest_id="manifest-p1",
        )

        self.assertFalse(result.ok)
        self.assertEqual(
            "P1_STORE_HOME_PRODUCTION_LOG_SCAN_FAILED:"
            "PRODUCTION_LOG_CANDIDATE_MALFORMED:"
            "PRODUCTION_LOG_REQUIRED_FIELD_MISSING",
            result.reason,
        )
        self.assertFalse(any(result.evidence_mapping().values()))

    def test_does_not_accept_another_run_with_internally_consistent_ids(self):
        delta = _delta(
            _store_home_start(10)
            + "\n"
            + _store_home_activation(12)
            + "\n"
            + _store_home_terminal(13)
            + "\n"
        )

        result = adapt_p1_store_home_runtime_log(
            delta,
            expected_candidate_position="12, 64, -9",
            expected_run_manifest_id="different-manifest",
        )

        self.assertFalse(result.ok)
        self.assertEqual("P1_STORE_HOME_RUN_MANIFEST_ID_NOT_EXPECTED", result.reason)

    def test_rejects_bridge_request_identity_as_manual_p1_contamination(self):
        contaminated = _store_home_activation(12).replace(
            "commandRequestId=~EMPTY~",
            "commandRequestId=request-p1",
        )
        delta = _delta(
            _store_home_start(10)
            + "\n"
            + contaminated
            + "\n"
            + _store_home_terminal(13)
            + "\n"
        )

        result = adapt_p1_store_home_runtime_log(
            delta,
            expected_candidate_position="12, 64, -9",
            expected_run_manifest_id="manifest-p1",
        )

        self.assertFalse(result.ok)
        self.assertEqual(
            "P1_STORE_HOME_MANUAL_COMMAND_CONTEXT_CONTAMINATED:"
            "STORE_HOME_CANDIDATE_ACTIVATED:commandRequestId",
            result.reason,
        )
        self.assertEqual("", result.command_request_id)


def _delta(text: str):
    return _create_latest_log_delta_result(
        True,
        "LATEST_LOG_DELTA_READ",
        None,
        100,
        100 + len(text.encode("utf-8")),
        text,
        "utf-8",
        "a" * 64,
    )


def _store_home_start(sequence: int) -> str:
    return _store_home_line(
        sequence,
        "STORE_HOME_OPERATION_STARTED",
        "phase=START",
    )


def _store_home_activation(sequence: int) -> str:
    return _store_home_line(
        sequence,
        "STORE_HOME_CANDIDATE_ACTIVATED",
        "candidateActivated=true activationResult=EXACT_SESSION_INSTALLED "
        "destinationId=trusted-home-1 candidatePosition=12%2C%2064%2C%20-9",
    )


def _store_home_terminal(sequence: int) -> str:
    return _store_home_line(
        sequence,
        "STORE_HOME_OPERATION_TERMINAL_SUMMARY",
        "terminalResult=COMPLETED operationResult=COMPLETED "
        "remainingStackCount=0 storedItems=64 touchedStackCount=2",
    )


def _store_home_line(sequence: int, event: str, extra: str) -> str:
    return (
        "[01:04:05] [Render thread/INFO]: [STDOUT]: ALTO CLEF: "
        "[LAVI ChatClefBoundary] traceId=trace-p1 clientTickId=812 "
        f"eventSequence={sequence} taskInstanceId=task-1 taskRunId=run-1 "
        "parentTaskRunId=unavailable threadName=Render%20thread level=BOUNDARY "
        f"event={event} reason=runtime_test taskClass=lavi.StoreHomeTask "
        "runManifestId=manifest-p1 operationId=17 "
        "commandContextAvailable=false commandRequestId=~EMPTY~ "
        "commandCorrelationId=~EMPTY~ commandSessionId=~EMPTY~ "
        "commandConnectionGeneration=unavailable commandText=~EMPTY~ "
        "commandSource=~EMPTY~ commandContextError=no_active_command "
        f"{extra} diagnosticCaptureStatus=complete"
    )


def _generic_boundary(sequence: int) -> str:
    return (
        "[01:04:04] [Render thread/INFO]: [STDOUT]: ALTO CLEF: "
        "[LAVI ChatClefBoundary] traceId=trace-x clientTickId=811 "
        f"eventSequence={sequence} taskInstanceId=task-x taskRunId=run-x "
        "parentTaskRunId=unavailable threadName=Render%20thread level=BOUNDARY "
        "event=TOOL_SELECTION_DECISION reason=selection "
        "taskClass=lavi.ToolTask selectedSlot=2"
    )


def _generic_diagnostic(sequence: int) -> str:
    return (
        "[01:04:06] [Render thread/INFO]: [STDOUT]: ALTO CLEF: "
        "[LAVI ChatClefDiag] traceId=trace-x clientTickId=813 "
        f"eventSequence={sequence} taskInstanceId=task-x taskRunId=run-x "
        "parentTaskRunId=unavailable threadName=Render%20thread "
        "eventType=TASK phase=START reason=first_tick "
        "taskClass=lavi.UnrelatedTask"
    )


if __name__ == "__main__":
    unittest.main()
