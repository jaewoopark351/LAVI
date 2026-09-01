#20260818_kpopmodder: Verify supervised composition never bypasses preflight or submits twice.
#20260819_kpopmodder: Use the complete mirrored result fixture required by canonical parsing.
from __future__ import annotations

import json
import unittest

from ..preflight.preflight_fixture_factory import (
    live_environment_fixture,
    log_identity_result_fixture,
    process_result_fixture,
    runtime_status_fixture,
)
from ..supervised_live_run import run_supervised_live_command
from .command_submission_transport import CommandSubmissionTransport


class SupervisedLiveRunTests(unittest.TestCase):
    def test_failed_preflight_submits_zero_commands(self):
        gateway = _SupervisedGateway()
        environment = live_environment_fixture()
        environment["gradio_url"] = ""

        result = run_supervised_live_command(environment, gateway)

        self.assertEqual("fail", result["preflight"]["status"])
        self.assertEqual(0, gateway.submit_call_count)
        self.assertEqual(
            "not_attempted",
            result["observation"]["submission_outcome"],
        )

    def test_opt_out_skips_before_gateway_endpoint_is_required(self):
        environment = live_environment_fixture()
        environment["mutating_opt_in"] = False
        gateway = object()

        result = run_supervised_live_command(environment, gateway)

        self.assertEqual("skip", result["preflight"]["status"])
        self.assertIs(False, result["preflight"]["selected_mutating_run"])
        self.assertEqual(
            "not_attempted",
            result["observation"]["submission_outcome"],
        )

    def test_successful_supervised_flow_submits_once(self):
        gateway = _SupervisedGateway(
            statuses=[runtime_status_fixture() for _index in range(4)]
        )
        run_guard = _RunGuard()
        reconciliation_recorder = _ReconciliationRecorder()

        def terminal_observer(_gateway, observation, **_kwargs):
            observation["terminal_lifecycle_observed"] = True
            observation["terminal_status"] = "completed"
            observation["runtime_reported_completion"] = True
            return observation

        result = run_supervised_live_command(
            live_environment_fixture(),
            gateway,
            process_probe=lambda **_kwargs: process_result_fixture(4100),
            log_identity_inspector=log_identity_result_fixture,
            terminal_observer=terminal_observer,
            run_guard=run_guard,
            reconciliation_recorder=reconciliation_recorder,
        )

        self.assertEqual("ok", result["preflight"]["status"])
        self.assertEqual(1, gateway.submit_call_count)
        self.assertIs(
            True,
            result["observation"]["terminal_lifecycle_observed"],
        )
        self.assertTrue(result["observation"]["reconciliation_required"])
        self.assertEqual(1, run_guard.claim_calls)
        self.assertEqual(1, reconciliation_recorder.record_calls)
        self.assertEqual(
            [live_environment_fixture()["command"]],
            gateway.submitted_commands,
        )

    def test_unapproved_raw_transport_stops_before_guard_and_submit(self):
        environment = live_environment_fixture()
        run_guard = _RunGuard()
        gateway = _SupervisedGateway(
            statuses=[runtime_status_fixture() for _index in range(4)]
        )

        result = run_supervised_live_command(
            environment,
            gateway,
            process_probe=lambda **_kwargs: process_result_fixture(4100),
            log_identity_inspector=log_identity_result_fixture,
            terminal_observer=lambda _gateway, observation, **_kwargs: observation,
            run_guard=run_guard,
            reconciliation_recorder=_ReconciliationRecorder(),
            transport=CommandSubmissionTransport.RAW,
        )

        self.assertEqual("fail", result["preflight"]["status"])
        self.assertEqual("approval", result["preflight"]["stage"])
        self.assertIn("transport", result["preflight"]["reason"])
        self.assertEqual(0, run_guard.claim_calls)
        self.assertEqual(0, gateway.submit_call_count)
        self.assertEqual([], gateway.submitted_commands)
        self.assertEqual([], gateway.raw_submitted_commands)

    def test_approved_raw_store_home_transport_never_calls_korean_submit(self):
        environment = _raw_store_home_environment()
        gateway = _SupervisedGateway(
            statuses=[runtime_status_fixture() for _index in range(4)]
        )

        result = run_supervised_live_command(
            environment,
            gateway,
            process_probe=lambda **_kwargs: process_result_fixture(4100),
            log_identity_inspector=log_identity_result_fixture,
            terminal_observer=lambda _gateway, observation, **_kwargs: observation,
            run_guard=_RunGuard(),
            reconciliation_recorder=_ReconciliationRecorder(),
            transport=CommandSubmissionTransport.RAW,
        )

        self.assertEqual("accepted", result["observation"]["submission_outcome"])
        self.assertEqual(
            CommandSubmissionTransport.RAW.value,
            result["preflight"]["observed"]["approved_transport"],
        )
        self.assertEqual(
            "request-raw-1",
            result["observation"]["submitted_request_id"],
        )
        self.assertEqual(1, gateway.submit_call_count)
        self.assertEqual([], gateway.submitted_commands)
        self.assertEqual(
            ["@store_home"],
            gateway.raw_submitted_commands,
        )

    def test_existing_one_shot_block_prevents_submission(self):
        gateway = _SupervisedGateway(
            statuses=[runtime_status_fixture(), runtime_status_fixture()]
        )
        result = run_supervised_live_command(
            live_environment_fixture(),
            gateway,
            process_probe=lambda **_kwargs: process_result_fixture(4100),
            log_identity_inspector=log_identity_result_fixture,
            run_guard=_RunGuard(claim_ok=False),
        )

        self.assertEqual("fail", result["preflight"]["status"])
        self.assertEqual("pre_submit_recheck", result["preflight"]["stage"])
        self.assertEqual(0, gateway.submit_call_count)

    def test_gateway_endpoint_must_match_the_approved_endpoint(self):
        gateway = _SupervisedGateway(
            statuses=[runtime_status_fixture(), runtime_status_fixture()],
            gradio_url="http://127.0.0.1:47861",
        )

        result = run_supervised_live_command(
            live_environment_fixture(),
            gateway,
            process_probe=lambda **_kwargs: process_result_fixture(4100),
            log_identity_inspector=log_identity_result_fixture,
        )

        self.assertEqual("fail", result["preflight"]["status"])
        self.assertEqual("endpoint", result["preflight"]["stage"])
        self.assertEqual(0, gateway.submit_call_count)

    def test_gateway_endpoint_change_after_guard_prevents_submission(self):
        gateway = _SupervisedGateway(
            statuses=[runtime_status_fixture() for _index in range(4)]
        )

        result = run_supervised_live_command(
            live_environment_fixture(),
            gateway,
            process_probe=lambda **_kwargs: process_result_fixture(4100),
            log_identity_inspector=log_identity_result_fixture,
            run_guard=_MutatingRunGuard(
                lambda: setattr(
                    gateway,
                    "gradio_url",
                    "http://127.0.0.1:47861",
                )
            ),
        )

        self.assertEqual("fail", result["preflight"]["status"])
        self.assertEqual("pre_submit_recheck", result["preflight"]["stage"])
        self.assertEqual(0, gateway.submit_call_count)

    def test_guard_result_ok_must_be_exact_true(self):
        for malformed_ok in ("true", 1, 1.0, None, [], {}):
            with self.subTest(malformed_ok=malformed_ok):
                gateway = _SupervisedGateway(
                    statuses=[runtime_status_fixture(), runtime_status_fixture()]
                )

                result = run_supervised_live_command(
                    live_environment_fixture(),
                    gateway,
                    process_probe=lambda **_kwargs: process_result_fixture(4100),
                    log_identity_inspector=log_identity_result_fixture,
                    run_guard=_RunGuard(claim_ok=malformed_ok),
                )

                self.assertEqual("fail", result["preflight"]["status"])
                self.assertEqual(0, gateway.submit_call_count)

    def test_ticket_recheck_blocks_coherent_environment_changes_after_guard(self):
        cases = {
            "command": _mutate_command,
            "endpoint": _mutate_endpoint,
            "backend": _mutate_backend,
            "instance": _mutate_instance,
            "world": _mutate_world,
            "transport": _mutate_transport,
            "approval": _mutate_approval_source,
        }
        for name, mutate in cases.items():
            with self.subTest(name=name):
                environment = live_environment_fixture()
                evidence = _MutableRuntimeEvidence(environment)
                gateway = _SupervisedGateway(status_factory=evidence.status)

                result = run_supervised_live_command(
                    environment,
                    gateway,
                    process_probe=evidence.process,
                    log_identity_inspector=log_identity_result_fixture,
                    run_guard=_MutatingRunGuard(lambda: mutate(environment)),
                )

                self.assertEqual("fail", result["preflight"]["status"])
                self.assertEqual("pre_submit_recheck", result["preflight"]["stage"])
                self.assertIn("approved live-run ticket changed", result["preflight"]["reason"])
                self.assertEqual(0, gateway.submit_call_count)

    def test_ticket_recheck_blocks_process_identity_change_after_guard(self):
        mutations = {
            "pid": lambda evidence: setattr(evidence, "process_id", 4200),
            "creation_date": lambda evidence: setattr(
                evidence,
                "creation_date",
                "20260818120100.000000+540",
            ),
            "executable": lambda evidence: setattr(
                evidence,
                "executable_path",
                "c:\\python312\\python.exe",
            ),
        }
        for name, mutate in mutations.items():
            with self.subTest(name=name):
                environment = live_environment_fixture()
                evidence = _MutableRuntimeEvidence(environment)
                gateway = _SupervisedGateway(status_factory=evidence.status)

                result = run_supervised_live_command(
                    environment,
                    gateway,
                    process_probe=evidence.process,
                    log_identity_inspector=log_identity_result_fixture,
                    run_guard=_MutatingRunGuard(lambda: mutate(evidence)),
                )

                self.assertEqual("fail", result["preflight"]["status"])
                self.assertEqual(
                    "pre_submit_recheck",
                    result["preflight"]["stage"],
                )
                self.assertIn(
                    "process_identity_fingerprint",
                    result["preflight"]["reason"],
                )
                self.assertEqual(0, gateway.submit_call_count)


class _SupervisedGateway:
    def __init__(
        self,
        *,
        statuses=None,
        status_factory=None,
        gradio_url="http://127.0.0.1:47860",
    ):
        self._statuses = list(statuses or [])
        self._status_factory = status_factory
        self.gradio_url = gradio_url
        self.submit_call_count = 0
        self.submitted_commands = []
        self.raw_submitted_commands = []

    def submit_korean_command(self, command):
        self.submit_call_count += 1
        self.submitted_commands.append(command)
        return {
            "ok": True,
            "status": {
                "request_id": "request-1",
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

    def submit_raw_command(self, command):
        self.submit_call_count += 1
        self.raw_submitted_commands.append(command)
        return {
            "ok": True,
            "status": {
                "request_id": "request-raw-1",
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

    def read_status(self):
        if self._status_factory is not None:
            return dict(self._status_factory())
        if not self._statuses:
            raise AssertionError("unexpected status read")
        return dict(self._statuses.pop(0))


class _RunGuard:
    def __init__(self, *, claim_ok=True):
        self._claim_ok = claim_ok
        self.claim_calls = 0

    def claim(self, _invocation_id, _command_fingerprint):
        self.claim_calls += 1
        return {
            "ok": self._claim_ok,
            "reason": "claimed" if self._claim_ok else "existing live-run block",
        }


class _MutatingRunGuard:
    def __init__(self, mutate):
        self._mutate = mutate

    def claim(self, _invocation_id, _command_fingerprint):
        self._mutate()
        return {"ok": True, "reason": "claimed"}


class _MutableRuntimeEvidence:
    def __init__(self, environment):
        self.environment = environment
        self.process_id = 4100
        self.creation_date = "20260818120000.000000+540"
        self.executable_path = (
            "c:\\vtuber_souorce_code\\lavi\\venv\\scripts\\python.exe"
        )

    def status(self):
        return runtime_status_fixture(
            backend=str(self.environment["expected_backend"]),
            instance=str(self.environment["expected_instance"]),
            world=str(self.environment["expected_world"]),
        )

    def process(self, **_kwargs):
        return process_result_fixture(
            self.process_id,
            creation_date=self.creation_date,
            executable_path=self.executable_path,
        )


def _mutate_command(environment):
    environment["command"] = "석탄 1개 캐와줘"
    _update_approval(environment, command=environment["command"])


def _mutate_endpoint(environment):
    environment["gradio_url"] = "http://127.0.0.1:47861"
    _update_approval(environment, gradio_url=environment["gradio_url"])


def _mutate_backend(environment):
    environment["expected_backend"] = "fabric_chatclef_changed"
    _update_approval(environment, backend=environment["expected_backend"])


def _mutate_instance(environment):
    environment["expected_instance"] = "LAVI_TEST_Fabric02"
    _update_approval(environment, instance=environment["expected_instance"])


def _mutate_world(environment):
    environment["expected_world"] = "changed-world"
    _update_approval(environment, world=environment["expected_world"])


def _mutate_transport(environment):
    environment["transport"] = CommandSubmissionTransport.RAW.value
    _update_approval(environment, transport=environment["transport"])


def _mutate_approval_source(environment):
    _update_approval(environment, approval_source="changed_approval")


def _update_approval(environment, **changes):
    approval = json.loads(str(environment["approval_json"]))
    approval.update(changes)
    environment["approval_json"] = json.dumps(approval, ensure_ascii=False)


def _raw_store_home_environment():
    environment = live_environment_fixture()
    environment["command"] = "@store_home"
    environment["transport"] = CommandSubmissionTransport.RAW.value
    _update_approval(
        environment,
        command=environment["command"],
        transport=environment["transport"],
    )
    return environment



class _ReconciliationRecorder:
    def __init__(self):
        self.record_calls = 0

    def record(self, _invocation_id, _command_fingerprint, _reason):
        self.record_calls += 1
        return {"ok": True, "reason": "recorded"}


if __name__ == "__main__":
    unittest.main()
