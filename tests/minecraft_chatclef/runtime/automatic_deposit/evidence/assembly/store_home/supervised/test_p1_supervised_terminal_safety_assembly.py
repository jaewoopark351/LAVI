#20260901_kpopmodder: Keep contradictory common-runner terminal facts INCONCLUSIVE without replay.
from __future__ import annotations

import unittest

from minecraft_chatclef.runtime.automatic_deposit.evidence.runtime_observer.status_artifact._test_fixture import (
    status_with_p1_supervised_runtime_artifact,
)
from minecraft_chatclef.runtime.automatic_deposit.evidence.runtime_observer.status_artifact.p1_supervised_runtime_artifact_observer import (
    observe_p1_supervised_runtime_artifact,
)
from minecraft_chatclef.runtime.automatic_deposit.orchestration.live.p1_supervised._test_fixture import (
    complete_p1_supervised_gameplay_observation,
    complete_p1_supervised_observation,
    ready_p1_supervised_live_fixture,
    supervised_p1_log_delta,
)
from minecraft_chatclef.runtime.automatic_deposit.orchestration.live.p1_supervised.execution.p1_supervised_execution_result import (
    seal_p1_supervised_execution_result,
)

from .p1_supervised_verified_evidence_assembly import (
    build_p1_supervised_verified_evidence,
)


class P1SupervisedTerminalSafetyAssemblyTests(unittest.TestCase):
    def test_terminal_safety_contradictions_are_inconclusive_without_replay(self):
        cases = (
            (
                {"connection_state_verified": False},
                "P1_SUPERVISED_RUNTIME_CONNECTION_NOT_VERIFIED",
            ),
            (
                {"observer_timeout": True},
                "P1_SUPERVISED_OBSERVER_TIMEOUT",
            ),
            (
                {"active_request_clear": False},
                "P1_SUPERVISED_ACTIVE_REQUEST_NOT_CLEAR",
            ),
            (
                {"active_clear_observation": "not_observed"},
                "P1_SUPERVISED_ACTIVE_CLEAR_NOT_SAME_SNAPSHOT",
            ),
            (
                {"adapter_command_request_count": 2},
                "P1_SUPERVISED_ADAPTER_REQUEST_COUNT_VIOLATION",
            ),
            (
                {"end_to_end_success": False},
                "P1_SUPERVISED_GAMEPLAY_RECEIPT_CONFLICT:end_to_end_success",
            ),
        )
        for overrides, expected_reason in cases:
            with self.subTest(expected_reason=expected_reason):
                result = _build(overrides)

                self.assertFalse(result.ok)
                self.assertEqual("INCONCLUSIVE", result.verdict)
                self.assertEqual(expected_reason, result.reason)
                self.assertEqual(1, result.submit_call_count)
                self.assertEqual(0, result.automatic_resubmit_count)


def _build(overrides: dict[str, object]):
    observation = complete_p1_supervised_observation()
    observation.update(
        {
            "adapter_command_request_count": "unknown",
            "connection_state_verified": True,
            "connection_state_error": "",
            "observer_timeout": False,
            "active_request_clear": True,
            "active_clear_observation": "same_snapshot",
            "gameplay_effect_observed": True,
            "end_to_end_success": True,
        }
    )
    observation.update(overrides)
    execution = seal_p1_supervised_execution_result(
        {"preflight": {"status": "ok"}, "observation": observation}
    )
    runtime_artifact, artifact_reason = observe_p1_supervised_runtime_artifact(
        status_with_p1_supervised_runtime_artifact()
    )
    if runtime_artifact is None:
        raise AssertionError(artifact_reason)
    live_fixture = ready_p1_supervised_live_fixture()
    return build_p1_supervised_verified_evidence(
        execution,
        supervised_p1_log_delta("lavi-gui-request-17"),
        runtime_artifact=runtime_artifact,
        live_fixture=live_fixture,
        gameplay_observation=complete_p1_supervised_gameplay_observation(
            fixture=live_fixture
        ),
        expected_run_manifest_id="opaque-p1-run",
    )


if __name__ == "__main__":
    unittest.main()
