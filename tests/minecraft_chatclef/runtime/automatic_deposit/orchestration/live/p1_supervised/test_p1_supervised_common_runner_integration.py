#20260901_kpopmodder: Prove the common RAW runner binds the accepted request ID through Java-log verification.
from __future__ import annotations

import json
import unittest

from minecraft_chatclef.runtime.automatic_deposit.evidence.runtime_observer.status_artifact._test_fixture import (
    status_with_p1_supervised_runtime_artifact,
)
from minecraft_chatclef.runtime.automatic_deposit.evidence.runtime_observer.status_artifact.p1_supervised_runtime_artifact_observer import (
    observe_p1_supervised_runtime_artifact,
)
from minecraft_chatclef.runtime.observation.terminal_result_observer import (
    observe_terminal_result,
)
from minecraft_chatclef.runtime.preflight.preflight_fixture_factory import (
    process_result_fixture,
    runtime_status_fixture,
)
from minecraft_chatclef.runtime.submission.command_submission_transport import (
    CommandSubmissionTransport,
)
from minecraft_chatclef.runtime.submission.guard_state import (
    OneShotGuardState,
    OneShotGuardStateObservation,
)
from minecraft_chatclef.runtime.supervised_live_run import (
    run_supervised_live_command,
)

from ._test_fixture import (
    complete_p1_supervised_gameplay_observation,
    ready_p1_supervised_live_fixture,
    supervised_p1_log_delta,
)
from .application import run_p1_supervised_live_application
from .execution.p1_supervised_execution_dependencies import (
    P1SupervisedExecutionDependencies,
)
from .post_submit.p1_supervised_post_submit_dependencies import (
    P1SupervisedPostSubmitDependencies,
)
from .pre_submit.p1_supervised_pre_submit_dependencies import (
    P1SupervisedPreSubmitDependencies,
)
from .request.p1_supervised_execution_request import (
    P1SupervisedExecutionRequest,
)


_POSITION = "12, 64, -9"
_MANIFEST_ID = "opaque-p1-run"
_ACCEPTED_REQUEST_ID = "gateway-accepted-request-42"
_REPOSITORY_ROOT = "C:/Vtuber_Souorce_Code/LAVI"


class P1SupervisedCommonRunnerIntegrationTests(unittest.TestCase):
    def test_accepted_raw_gateway_request_id_reaches_seal_and_java_log_match(self):
        harness = _CommonRunnerHarness(java_log_request_id=_ACCEPTED_REQUEST_ID)

        result = harness.run()

        self.assertTrue(result.ok, result.reason)
        self.assertEqual("PASS", result.verdict)
        self.assertEqual(_ACCEPTED_REQUEST_ID, result.submitted_request_id)
        self.assertEqual([_ACCEPTED_REQUEST_ID], harness.post_submit_request_ids)
        self.assertEqual(1, harness.common_runner_call_count)
        self.assertEqual(1, harness.gateway.submit_call_count)
        self.assertEqual(["@store_home"], harness.gateway.raw_commands)
        self.assertEqual([], harness.gateway.korean_commands)
        self.assertEqual(5, harness.gateway.status_call_count)
        self.assertEqual(1, harness.inner_guard.claim_call_count)
        self.assertEqual(1, harness.reconcile_call_count)

    def test_gateway_request_id_mismatching_java_log_is_inconclusive(self):
        harness = _CommonRunnerHarness(
            java_log_request_id="java-log-different-request-43"
        )

        result = harness.run()

        self.assertFalse(result.ok)
        self.assertEqual("INCONCLUSIVE", result.verdict)
        self.assertEqual(
            "P1_STORE_HOME_COMMAND_REQUEST_ID_NOT_EXPECTED",
            result.reason,
        )
        self.assertEqual(_ACCEPTED_REQUEST_ID, result.submitted_request_id)
        self.assertEqual([_ACCEPTED_REQUEST_ID], harness.post_submit_request_ids)
        self.assertEqual(1, harness.common_runner_call_count)
        self.assertEqual(1, harness.gateway.submit_call_count)
        self.assertEqual(0, result.automatic_resubmit_count)
        self.assertEqual(0, harness.reconcile_call_count)


class _CommonRunnerHarness:
    def __init__(self, *, java_log_request_id: str) -> None:
        self.request = _request()
        self.environment = _runner_environment(self.request)
        self.java_log_request_id = java_log_request_id
        self.gateway = _CanonicalGateway(_ACCEPTED_REQUEST_ID)
        self.inner_guard = _RunGuard()
        self.inner_recorder = _ReconciliationRecorder()
        self.common_runner_call_count = 0
        self.gateway_factory_call_count = 0
        self.reconcile_call_count = 0
        self.post_submit_request_ids: list[str] = []

    def run(self):
        return run_p1_supervised_live_application(
            self.request,
            self.environment,
            guard_state_reader=lambda _root: _clear_guard_observation(),
            pre_submit_dependencies=P1SupervisedPreSubmitDependencies(
                runtime_artifact_reader=self._runtime_artifact_reader,
                trusted_destination_reader=self._trusted_destination_reader,
            ),
            execution_dependencies=P1SupervisedExecutionDependencies(
                gateway_factory=self._gateway_factory,
                runner=self._common_runner,
            ),
            post_submit_dependencies=P1SupervisedPostSubmitDependencies(
                log_delta_reader=self._post_submit_log_reader,
                gameplay_observation_reader=self._gameplay_observation_reader,
                reconciler=self._reconcile,
            ),
        )

    def _runtime_artifact_reader(self):
        return observe_p1_supervised_runtime_artifact(
            status_with_p1_supervised_runtime_artifact()
        )

    def _trusted_destination_reader(self):
        return ready_p1_supervised_live_fixture(), "P1_LIVE_FIXTURE_READY"

    def _gateway_factory(self, gradio_url: str):
        self.gateway_factory_call_count += 1
        self.assert_equal(self.request.gradio_url, gradio_url)
        return self.gateway

    def _common_runner(self, environment, gateway, *, transport):
        self.common_runner_call_count += 1
        self.assert_is(CommandSubmissionTransport.RAW, transport)
        return run_supervised_live_command(
            environment,
            gateway,
            process_probe=lambda **_kwargs: process_result_fixture(4100),
            log_identity_inspector=_unexpected_log_identity_inspector,
            terminal_observer=_complete_terminal_observer,
            run_guard=self.inner_guard,
            reconciliation_recorder=self.inner_recorder,
            transport=transport,
        )

    def _post_submit_log_reader(self, execution):
        self.post_submit_request_ids.append(execution.submitted_request_id)
        return (
            supervised_p1_log_delta(self.java_log_request_id),
            "LATEST_LOG_DELTA_READ",
        )

    def _gameplay_observation_reader(self, _execution, _log_delta):
        return (
            complete_p1_supervised_gameplay_observation(),
            "P1_SUPERVISED_GAMEPLAY_OBSERVED",
        )

    def _reconcile(self, _invocation_id: str, _command_fingerprint: str):
        self.reconcile_call_count += 1
        return {"ok": True, "reason": "matching one-shot guard reconciled"}

    @staticmethod
    def assert_equal(expected, actual) -> None:
        if expected != actual:
            raise AssertionError(f"{expected!r} != {actual!r}")

    @staticmethod
    def assert_is(expected, actual) -> None:
        if expected is not actual:
            raise AssertionError(f"{expected!r} is not {actual!r}")


class _CanonicalGateway:
    def __init__(self, accepted_request_id: str) -> None:
        self.gradio_url = "http://127.0.0.1:47860"
        self.accepted_request_id = accepted_request_id
        self.submit_call_count = 0
        self.status_call_count = 0
        self.raw_commands: list[str] = []
        self.korean_commands: list[str] = []

    def read_status(self) -> dict[str, object]:
        self.status_call_count += 1
        status = runtime_status_fixture(
            instance="LAVI_TEST_Fabric01",
            world="world-1",
        )
        if self.submit_call_count == 1:
            commands = status["details"]["details"]["commands"]
            commands["last_result"] = {
                "request_id": self.accepted_request_id,
                "ok": True,
                "status": "completed",
                "error_code": None,
                "message": "completed",
                "data": {"result_reason": "completed"},
            }
        return status

    def submit_raw_command(self, command: str) -> dict[str, object]:
        self.submit_call_count += 1
        self.raw_commands.append(command)
        return {
            "ok": True,
            "status": {
                "request_id": self.accepted_request_id,
                "ok": True,
                "status": "accepted",
                "error_code": None,
                "message": "accepted",
                "data": {},
            },
            "error": None,
            "message": "accepted",
            "details": {},
        }

    def submit_korean_command(self, command: str) -> dict[str, object]:
        self.submit_call_count += 1
        self.korean_commands.append(command)
        raise AssertionError("supervised P1 must not use Korean submission")


class _RunGuard:
    def __init__(self) -> None:
        self.claim_call_count = 0

    def claim(self, _invocation_id: str, _command_fingerprint: str):
        self.claim_call_count += 1
        return {"ok": True, "reason": "claimed"}


class _ReconciliationRecorder:
    def __init__(self) -> None:
        self.record_call_count = 0

    def record(
        self,
        _invocation_id: str,
        _command_fingerprint: str,
        _reason: str,
    ):
        self.record_call_count += 1
        return {"ok": True, "reason": "recorded"}


def _complete_terminal_observer(gateway, observation, **kwargs):
    return observe_terminal_result(gateway, observation, **kwargs)


def _unexpected_log_identity_inspector(*_args, **_kwargs):
    raise AssertionError("complete runtime status identity must avoid log fallback")


def _request() -> P1SupervisedExecutionRequest:
    approval = {
        "command": "@store_home",
        "gradio_url": "http://127.0.0.1:47860",
        "backend": "fabric_chatclef",
        "instance": "LAVI_TEST_Fabric01",
        "world": "world-1",
        "invocation_id": "p1-supervised-common-runner-1",
        "transport": "raw",
        "run_manifest_id": _MANIFEST_ID,
        "approval_source": "operator",
        "one_shot": True,
        "automatic_rerun_disabled": True,
    }
    return P1SupervisedExecutionRequest(
        command="@store_home",
        transport=CommandSubmissionTransport.RAW,
        invocation_id="p1-supervised-common-runner-1",
        approval_json=json.dumps(approval, separators=(",", ":")),
        expected_run_manifest_id=_MANIFEST_ID,
        expected_candidate_position=_POSITION,
        gradio_url="http://127.0.0.1:47860",
        expected_backend="fabric_chatclef",
        expected_instance="LAVI_TEST_Fabric01",
        expected_world="world-1",
        repository_root=_REPOSITORY_ROOT,
    )


def _runner_environment(
    request: P1SupervisedExecutionRequest,
) -> dict[str, object]:
    return {
        "live_opt_in": True,
        "mutating_opt_in": True,
        "command": request.command,
        "transport": request.transport.value,
        "invocation_id": request.invocation_id,
        "approval_json": request.approval_json,
        "repository_root": request.repository_root,
        "gradio_url": request.gradio_url,
        "expected_backend": request.expected_backend,
        "expected_instance": request.expected_instance,
        "expected_world": request.expected_world,
        "log_dir": "C:/fixture/Instances/LAVI_TEST_Fabric01/logs",
        "timeout_sec": 1.0,
        "poll_sec": 0.001,
        "fabric_port": 4316,
        "gradio_range_start": 47860,
        "gradio_range_end": 47959,
    }


def _clear_guard_observation():
    reason = "LIVE_ONE_SHOT_GUARD_CLEAR"
    return (
        OneShotGuardStateObservation(
            state=OneShotGuardState.CLEAR,
            state_directory="C:/Vtuber_Souorce_Code/LAVI/tests/state",
            invocation_fingerprint=None,
            command_fingerprint=None,
            reason=reason,
        ),
        reason,
    )


if __name__ == "__main__":
    unittest.main()
