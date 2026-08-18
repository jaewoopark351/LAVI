#20260818_kpopmodder: Lock fail-closed live runtime admission with offline fixtures.
from __future__ import annotations

import json
import unittest

from .preflight_fixture_factory import (
    live_environment_fixture,
    log_identity_result_fixture,
    process_result_fixture,
    runtime_status_fixture,
)
from .preflight_runner import run_live_runtime_preflight


class LiveRuntimePreflightTests(unittest.TestCase):
    def test_missing_opt_in_skips_without_reading_runtime(self):
        environment = live_environment_fixture()
        environment["mutating_opt_in"] = False

        decision = run_live_runtime_preflight(
            environment,
            status_reader=lambda: self.fail("status must not be read"),
        )

        self.assertEqual("skip", decision["status"])
        self.assertFalse(decision["selected_mutating_run"])

    def test_selected_run_without_explicit_url_fails_before_status(self):
        environment = live_environment_fixture()
        environment["gradio_url"] = ""

        decision = run_live_runtime_preflight(
            environment,
            status_reader=lambda: self.fail("status must not be read"),
        )

        self.assertEqual("fail", decision["status"])
        self.assertEqual("approval", decision["stage"])

    def test_approval_mismatch_fails_before_process_probe(self):
        environment = live_environment_fixture()
        approval = json.loads(str(environment["approval_json"]))
        approval["world"] = "wrong-world"
        environment["approval_json"] = json.dumps(approval, ensure_ascii=False)

        decision = run_live_runtime_preflight(
            environment,
            status_reader=lambda: self.fail("status must not be read"),
            process_probe=lambda **_kwargs: self.fail("process must not be probed"),
        )

        self.assertEqual("fail", decision["status"])
        self.assertEqual("approval", decision["stage"])
        self.assertIn("world", decision["reason"])

    def test_valid_evidence_passes_initial_and_pre_submit_recheck(self):
        process_calls: list[int] = []
        status_calls: list[int] = []

        def process_probe(**_kwargs):
            process_calls.append(1)
            return process_result_fixture(4100)

        def status_reader():
            status_calls.append(1)
            return runtime_status_fixture()

        decision = run_live_runtime_preflight(
            live_environment_fixture(),
            status_reader=status_reader,
            process_probe=process_probe,
            log_identity_inspector=log_identity_result_fixture,
        )

        self.assertEqual("ok", decision["status"])
        self.assertEqual("pre_submit_recheck", decision["stage"])
        self.assertEqual(2, len(process_calls))
        self.assertEqual(2, len(status_calls))
        self.assertTrue(decision["observed"]["pre_submit_recheck_passed"])

    def test_busy_status_fails_with_no_pre_submit_recheck(self):
        decision = run_live_runtime_preflight(
            live_environment_fixture(),
            status_reader=lambda: runtime_status_fixture(
                active_request_id="active-1"
            ),
            process_probe=lambda **_kwargs: process_result_fixture(4100),
            log_identity_inspector=log_identity_result_fixture,
        )

        self.assertEqual("fail", decision["status"])
        self.assertEqual("idle", decision["stage"])
        self.assertIn("already active", decision["reason"])

    def test_wrong_backend_fails_closed(self):
        decision = run_live_runtime_preflight(
            live_environment_fixture(),
            status_reader=lambda: runtime_status_fixture(backend="forge_minemind"),
            process_probe=lambda **_kwargs: process_result_fixture(4100),
            log_identity_inspector=log_identity_result_fixture,
        )

        self.assertEqual("fail", decision["status"])
        self.assertIn("backend", decision["reason"])

    def test_disconnected_runtime_fails_closed(self):
        decision = run_live_runtime_preflight(
            live_environment_fixture(),
            status_reader=lambda: runtime_status_fixture(
                connected=False,
                lifecycle_state="disconnected",
            ),
            process_probe=lambda **_kwargs: process_result_fixture(4100),
            log_identity_inspector=log_identity_result_fixture,
        )

        self.assertEqual("fail", decision["status"])
        self.assertIn("connected", decision["reason"])

    def test_missing_active_request_field_fails_closed(self):
        status = runtime_status_fixture()
        del status["details"]["details"]["commands"]["active_request_id"]
        decision = run_live_runtime_preflight(
            live_environment_fixture(),
            status_reader=lambda: status,
            process_probe=lambda **_kwargs: process_result_fixture(4100),
            log_identity_inspector=log_identity_result_fixture,
        )

        self.assertEqual("fail", decision["status"])
        self.assertIn("incomplete", decision["reason"])

    def test_runtime_world_mismatch_fails_without_log_fallback(self):
        decision = run_live_runtime_preflight(
            live_environment_fixture(),
            status_reader=lambda: runtime_status_fixture(
                instance="LAVI_TEST_Fabric01",
                world="wrong-world",
            ),
            process_probe=lambda **_kwargs: process_result_fixture(4100),
            log_identity_inspector=lambda *_args, **_kwargs: self.fail(
                "populated runtime identity must not fall back to logs"
            ),
        )

        self.assertEqual("fail", decision["status"])
        self.assertEqual("world", decision["stage"])
        self.assertIn("world mismatch", decision["reason"])

    def test_missing_log_directory_fails_only_when_status_identity_is_absent(self):
        environment = live_environment_fixture()
        environment["log_dir"] = ""
        decision = run_live_runtime_preflight(
            environment,
            status_reader=runtime_status_fixture,
            process_probe=lambda **_kwargs: process_result_fixture(4100),
        )

        self.assertEqual("fail", decision["status"])
        self.assertEqual("world", decision["stage"])
        self.assertIn("LOG_DIR", decision["reason"])

    def test_listener_owner_change_fails_pre_submit_recheck(self):
        results = iter(
            (process_result_fixture(4100), process_result_fixture(4200))
        )
        decision = run_live_runtime_preflight(
            live_environment_fixture(),
            status_reader=runtime_status_fixture,
            process_probe=lambda **_kwargs: next(results),
            log_identity_inspector=log_identity_result_fixture,
        )

        self.assertEqual("fail", decision["status"])
        self.assertEqual("pre_submit_recheck", decision["stage"])
        self.assertIn("owner identity changed", decision["reason"])

    def test_listener_pid_reuse_fails_pre_submit_recheck(self):
        results = iter(
            (
                process_result_fixture(
                    4100,
                    creation_date="20260818120000.000000+540",
                ),
                process_result_fixture(
                    4100,
                    creation_date="20260818120100.000000+540",
                ),
            )
        )
        decision = run_live_runtime_preflight(
            live_environment_fixture(),
            status_reader=runtime_status_fixture,
            process_probe=lambda **_kwargs: next(results),
            log_identity_inspector=log_identity_result_fixture,
        )

        self.assertEqual("fail", decision["status"])
        self.assertEqual("pre_submit_recheck", decision["stage"])
        self.assertIn("owner identity changed", decision["reason"])

    def test_approval_change_fails_pre_submit_recheck(self):
        environment = live_environment_fixture()
        changed = json.loads(str(environment["approval_json"]))
        changed["invocation_id"] = "changed-invocation"
        approvals = iter(
            (
                environment["approval_json"],
                json.dumps(changed, ensure_ascii=False),
            )
        )
        decision = run_live_runtime_preflight(
            environment,
            status_reader=runtime_status_fixture,
            process_probe=lambda **_kwargs: process_result_fixture(4100),
            log_identity_inspector=log_identity_result_fixture,
            approval_reader=lambda: next(approvals),
        )

        self.assertEqual("fail", decision["status"])
        self.assertEqual("pre_submit_recheck", decision["stage"])
        self.assertIn("invocation_id", decision["reason"])

    def test_world_change_fails_pre_submit_recheck(self):
        statuses = iter(
            (
                runtime_status_fixture(),
                runtime_status_fixture(
                    instance="LAVI_TEST_Fabric01",
                    world="wrong-world",
                ),
            )
        )
        decision = run_live_runtime_preflight(
            live_environment_fixture(),
            status_reader=lambda: next(statuses),
            process_probe=lambda **_kwargs: process_result_fixture(4100),
            log_identity_inspector=log_identity_result_fixture,
        )

        self.assertEqual("fail", decision["status"])
        self.assertEqual("pre_submit_recheck", decision["stage"])
        self.assertIn("world mismatch", decision["reason"])


if __name__ == "__main__":
    unittest.main()
