#20260901_kpopmodder: Lock guarded RAW @store_home execution and zero-replay supervised P1 behavior.
from __future__ import annotations

import json
import unittest

from minecraft_chatclef.runtime.automatic_deposit.evidence.runtime_observer.status_artifact._test_fixture import (
    status_with_p1_supervised_runtime_artifact,
)
from minecraft_chatclef.runtime.automatic_deposit.evidence.runtime_observer.status_artifact.p1_supervised_runtime_artifact_observer import (
    observe_p1_supervised_runtime_artifact,
)
from minecraft_chatclef.runtime.submission.command_submission_transport import (
    CommandSubmissionTransport,
)
from minecraft_chatclef.runtime.submission.guard_state import (
    OneShotGuardState,
    OneShotGuardStateObservation,
)

from .application import run_p1_supervised_live_application
from ._test_fixture import (
    complete_p1_supervised_gameplay_observation,
    complete_p1_supervised_observation,
    ready_p1_supervised_live_fixture,
    supervised_p1_log_delta,
)
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
_REQUEST_ID = "lavi-gui-request-17"
_REPOSITORY_ROOT = "C:/Vtuber_Souorce_Code/LAVI"


class P1SupervisedApplicationTests(unittest.TestCase):
    def test_non_clear_guard_calls_no_gateway_runner_or_observer(self):
        for state, expected_reason in (
            (
                OneShotGuardState.LIVE_RUN_BLOCK_PRESENT,
                "LIVE_ONE_SHOT_BLOCK_PRESENT",
            ),
            (
                OneShotGuardState.RECONCILIATION_REQUIRED,
                "LIVE_ONE_SHOT_RECONCILIATION_REQUIRED",
            ),
            (
                OneShotGuardState.GUARD_STATE_UNREADABLE,
                "LIVE_ONE_SHOT_GUARD_STATE_UNREADABLE",
            ),
        ):
            with self.subTest(state=state):
                harness = _Harness()

                result = harness.run(guard_state=state)

                self.assertFalse(result.ok)
                self.assertEqual(expected_reason, result.reason)
                self.assertEqual(0, result.submit_call_count)
                self.assertEqual([], harness.calls)

    def test_missing_runtime_artifact_blocks_before_fixture_gateway_and_runner(self):
        harness = _Harness(runtime_artifact_available=False)

        result = harness.run()

        self.assertEqual(
            "P1_SUPERVISED_RUNTIME_ARTIFACT_NOT_VERIFIED",
            result.reason,
        )
        self.assertEqual(["runtime_artifact"], harness.calls)
        self.assertEqual(0, result.submit_call_count)

    def test_nonexclusive_trusted_destination_blocks_submit(self):
        harness = _Harness(trusted_destination_count=3)

        result = harness.run()

        self.assertEqual("TRUSTED_DESTINATION_NOT_EXCLUSIVE", result.reason)
        self.assertEqual(
            ["runtime_artifact", "trusted_destination"],
            harness.calls,
        )
        self.assertEqual(0, result.submit_call_count)

    def test_fixture_without_running_jvm_save_barrier_blocks_submit(self):
        harness = _Harness(fixture_ready=False)

        result = harness.run()

        self.assertEqual("P1_SUPERVISED_FIXTURE_NOT_READY", result.reason)
        self.assertEqual(
            ["runtime_artifact", "trusted_destination"],
            harness.calls,
        )
        self.assertEqual(0, result.submit_call_count)

    def test_missing_external_manifest_or_wrong_approval_transport_blocks_early(self):
        cases = (
            (
                _request(expected_run_manifest_id=""),
                "P1_SUPERVISED_RUN_MANIFEST_ID_MISSING",
            ),
            (
                _request(approved_transport="korean"),
                "P1_SUPERVISED_APPROVAL_TRANSPORT_MISMATCH",
            ),
        )
        for request, expected_reason in cases:
            with self.subTest(expected_reason=expected_reason):
                harness = _Harness(request=request)

                result = harness.run()

                self.assertEqual(expected_reason, result.reason)
                self.assertEqual([], harness.calls)
                self.assertEqual(0, result.submit_call_count)

    def test_invalid_post_submit_dependencies_block_before_gateway_and_runner(self):
        cases = (
            (
                object(),
                "P1_SUPERVISED_POST_SUBMIT_DEPENDENCIES_NOT_TYPED",
            ),
            (
                P1SupervisedPostSubmitDependencies(
                    log_delta_reader=None,  # type: ignore[arg-type]
                    gameplay_observation_reader=lambda _execution, _log: (
                        None,
                        "not used",
                    ),
                    reconciler=lambda _invocation_id, _fingerprint: {"ok": True},
                ),
                "P1_SUPERVISED_POST_SUBMIT_READER_NOT_CALLABLE",
            ),
            (
                P1SupervisedPostSubmitDependencies(
                    log_delta_reader=lambda _execution: (None, "not used"),
                    gameplay_observation_reader=lambda _execution, _log: (
                        None,
                        "not used",
                    ),
                    reconciler=None,  # type: ignore[arg-type]
                ),
                "P1_SUPERVISED_RECONCILER_NOT_CALLABLE",
            ),
            (
                P1SupervisedPostSubmitDependencies(
                    log_delta_reader=lambda _execution: (None, "not used"),
                    gameplay_observation_reader=None,  # type: ignore[arg-type]
                    reconciler=lambda _invocation_id, _fingerprint: {"ok": True},
                ),
                "P1_SUPERVISED_GAMEPLAY_READER_NOT_CALLABLE",
            ),
        )
        for dependencies, expected_reason in cases:
            with self.subTest(expected_reason=expected_reason):
                harness = _Harness()

                result = harness.run(post_submit_dependencies=dependencies)

                self.assertFalse(result.ok)
                self.assertEqual(expected_reason, result.reason)
                self.assertEqual(0, result.submit_call_count)
                self.assertEqual([], harness.calls)

    def test_complete_pre_action_evidence_submits_raw_store_home_exactly_once(self):
        harness = _Harness()

        result = harness.run()

        self.assertTrue(result.ok, result.reason)
        self.assertEqual("PASS", result.verdict)
        self.assertEqual(1, result.submit_call_count)
        self.assertEqual(0, result.automatic_resubmit_count)
        self.assertEqual(_REQUEST_ID, result.submitted_request_id)
        self.assertTrue(result.reconciled)
        self.assertEqual(
            [
                "runtime_artifact",
                "trusted_destination",
                "gateway",
                "runner:raw:@store_home",
                "gateway_submit:@store_home",
                "post_submit_log",
                "post_submit_gameplay",
                "reconcile",
            ],
            harness.calls,
        )

    def test_java_request_id_mismatch_is_inconclusive_and_never_reconciles(self):
        harness = _Harness(log_request_id="lavi-gui-wrong-request")

        result = harness.run()

        self.assertFalse(result.ok)
        self.assertEqual("INCONCLUSIVE", result.verdict)
        self.assertEqual(
            "P1_STORE_HOME_COMMAND_REQUEST_ID_NOT_EXPECTED",
            result.reason,
        )
        self.assertEqual(1, result.submit_call_count)
        self.assertEqual(0, result.automatic_resubmit_count)
        self.assertNotIn("reconcile", harness.calls)

    def test_terminal_or_gameplay_incomplete_never_resubmits_or_reconciles(self):
        for execution_overrides, gameplay_overrides, expected_reason in (
            (
                {"terminal_lifecycle_observed": False},
                None,
                "P1_SUPERVISED_TERMINAL_LIFECYCLE_NOT_OBSERVED",
            ),
            (
                None,
                {"gameplay_observation_complete": False},
                (
                    "P1_SUPERVISED_GAMEPLAY_CHECKPOINT_INVALID:"
                    "gameplay_observation_complete"
                ),
            ),
        ):
            with self.subTest(expected_reason=expected_reason):
                harness = _Harness(
                    observation_overrides=execution_overrides,
                    gameplay_overrides=gameplay_overrides,
                )

                result = harness.run()

                self.assertEqual("INCONCLUSIVE", result.verdict)
                self.assertEqual(expected_reason, result.reason)
                self.assertEqual(1, result.submit_call_count)
                self.assertEqual(0, result.automatic_resubmit_count)
                self.assertNotIn("reconcile", harness.calls)


class _Harness:
    def __init__(
        self,
        *,
        request: P1SupervisedExecutionRequest | None = None,
        runtime_artifact_available: bool = True,
        trusted_destination_count: int = 1,
        fixture_ready: bool = True,
        log_request_id: str = _REQUEST_ID,
        observation_overrides: dict[str, object] | None = None,
        gameplay_overrides: dict[str, bool] | None = None,
    ) -> None:
        self.request = request or _request()
        self.runtime_artifact_available = runtime_artifact_available
        self.trusted_destination_count = trusted_destination_count
        self.fixture_ready = fixture_ready
        self.log_request_id = log_request_id
        self.observation_overrides = observation_overrides or {}
        self.gameplay_overrides = gameplay_overrides or {}
        self.calls: list[str] = []

    def run(
        self,
        *,
        guard_state: OneShotGuardState = OneShotGuardState.CLEAR,
        post_submit_dependencies: object | None = None,
    ):
        dependencies = post_submit_dependencies
        if dependencies is None:
            dependencies = P1SupervisedPostSubmitDependencies(
                log_delta_reader=self._log_delta_reader,
                gameplay_observation_reader=self._gameplay_observation_reader,
                reconciler=self._reconcile,
            )
        return run_p1_supervised_live_application(
            self.request,
            _runner_environment(self.request),
            guard_state_reader=lambda _root: _guard_observation(guard_state),
            pre_submit_dependencies=P1SupervisedPreSubmitDependencies(
                runtime_artifact_reader=self._runtime_artifact_reader,
                trusted_destination_reader=self._trusted_destination_reader,
            ),
            execution_dependencies=P1SupervisedExecutionDependencies(
                gateway_factory=self._gateway_factory,
                runner=self._runner,
            ),
            post_submit_dependencies=dependencies,
        )

    def _runtime_artifact_reader(self):
        self.calls.append("runtime_artifact")
        if not self.runtime_artifact_available:
            return None, "P1_SUPERVISED_RUNTIME_ARTIFACT_STATUS_MISSING"
        artifact, reason = observe_p1_supervised_runtime_artifact(
            status_with_p1_supervised_runtime_artifact()
        )
        return artifact, reason

    def _trusted_destination_reader(self):
        self.calls.append("trusted_destination")
        if self.trusted_destination_count != 1:
            return None, "TRUSTED_DESTINATION_NOT_EXCLUSIVE"
        return (
            ready_p1_supervised_live_fixture(ready=self.fixture_ready),
            (
                "P1_LIVE_FIXTURE_READY"
                if self.fixture_ready
                else "P1_LIVE_FIXTURE_OBSERVED_INCONCLUSIVE"
            ),
        )

    def _gateway_factory(self, _url: str):
        self.calls.append("gateway")
        return _Gateway(self.calls)

    def _runner(self, environment, gateway, **kwargs):
        transport = kwargs.get("transport")
        self.calls.append(
            f"runner:{transport.value}:{environment.get('command')}"
        )
        self.assert_raw_contract(environment, transport)
        gateway.submit_raw_command(environment["command"])
        observation = complete_p1_supervised_observation()
        observation.update(self.observation_overrides)
        return {"preflight": {"status": "ok"}, "observation": observation}

    def assert_raw_contract(self, environment, transport) -> None:
        if transport is not CommandSubmissionTransport.RAW:
            raise AssertionError("P1 runner transport was not RAW")
        if environment.get("command") != "@store_home":
            raise AssertionError("P1 runner command was not exact @store_home")

    def _log_delta_reader(self, _execution_result):
        self.calls.append("post_submit_log")
        return supervised_p1_log_delta(self.log_request_id), "LATEST_LOG_DELTA_READ"

    def _gameplay_observation_reader(self, _execution_result, _log_delta):
        self.calls.append("post_submit_gameplay")
        return (
            complete_p1_supervised_gameplay_observation(
                overrides=self.gameplay_overrides,
            ),
            "P1_SUPERVISED_GAMEPLAY_OBSERVED",
        )

    def _reconcile(self, _invocation_id: str, _command_fingerprint: str):
        self.calls.append("reconcile")
        return {"ok": True, "reason": "matching one-shot guard reconciled"}


class _Gateway:
    def __init__(self, calls: list[str]) -> None:
        self.calls = calls
        self.submit_call_count = 0

    def submit_raw_command(self, command: str) -> dict[str, object]:
        self.submit_call_count += 1
        self.calls.append(f"gateway_submit:{command}")
        return {"ok": True}


def _request(
    *,
    expected_run_manifest_id: str = _MANIFEST_ID,
    approved_transport: str = "raw",
) -> P1SupervisedExecutionRequest:
    approval = {
        "command": "@store_home",
        "gradio_url": "http://127.0.0.1:47860",
        "backend": "fabric_chatclef",
        "instance": "LAVI_TEST_Fabric01",
        "world": "world-1",
        "invocation_id": "p1-supervised-invocation-1",
        "transport": approved_transport,
        "run_manifest_id": expected_run_manifest_id,
        "approval_source": "operator",
        "one_shot": True,
        "automatic_rerun_disabled": True,
    }
    return P1SupervisedExecutionRequest(
        command="@store_home",
        transport=CommandSubmissionTransport.RAW,
        invocation_id="p1-supervised-invocation-1",
        approval_json=json.dumps(approval, separators=(",", ":")),
        expected_run_manifest_id=expected_run_manifest_id,
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
        "command": request.command,
        "transport": request.transport.value,
        "invocation_id": request.invocation_id,
        "approval_json": request.approval_json,
        "repository_root": request.repository_root,
        "gradio_url": request.gradio_url,
        "expected_backend": request.expected_backend,
        "expected_instance": request.expected_instance,
        "expected_world": request.expected_world,
        "timeout_sec": 30.0,
        "poll_sec": 0.25,
    }


def _guard_observation(state: OneShotGuardState):
    reason = {
        OneShotGuardState.CLEAR: "LIVE_ONE_SHOT_GUARD_CLEAR",
        OneShotGuardState.LIVE_RUN_BLOCK_PRESENT: "LIVE_RUN_BLOCK_PRESENT",
        OneShotGuardState.RECONCILIATION_REQUIRED: "RECONCILIATION_REQUIRED",
        OneShotGuardState.GUARD_STATE_UNREADABLE: "GUARD_STATE_UNREADABLE",
    }[state]
    observation = OneShotGuardStateObservation(
        state=state,
        state_directory="C:/Vtuber_Souorce_Code/LAVI/tests/state",
        invocation_fingerprint=None,
        command_fingerprint=None,
        reason=reason,
    )
    return observation, reason


if __name__ == "__main__":
    unittest.main()
