#20260901_kpopmodder: Lock P1 live admission ahead of every observer and mutation boundary.
from __future__ import annotations

import unittest

from minecraft_chatclef.runtime.submission.guard_state import (
    OneShotGuardState,
    OneShotGuardStateObservation,
)

from ...scenario.matrix_catalog import automatic_deposit_matrix_catalog
from ...testing.hermetic_evidence import hermetic_fixture, hermetic_run_manifest
from ..live_matrix_application import (
    run_automatic_deposit_live_matrix_application,
)
from .p1_manual_observer_dependencies import P1ManualObserverDependencies


class P1ManualLiveAdmissionTests(unittest.TestCase):
    def test_each_nonclear_guard_stops_every_live_observer(self):
        expected = (
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
        )
        for state, expected_reason in expected:
            with self.subTest(state=state):
                row, fixture, run = _p1_manifests()
                calls: list[str] = []
                dependencies = _tracking_dependencies(calls)

                result = run_automatic_deposit_live_matrix_application(
                    {"live_opt_in": True, "mutating_opt_in": True},
                    row_id=row.row_id,
                    run_manifest=run,
                    fixture_manifest=fixture,
                    guard_state_reader=lambda _root, value=state: _guard(value),
                    p1_observer_dependencies=dependencies,
                )

                self.assertEqual(expected_reason, result["reason"])
                self.assertEqual(0, result["submit_call_count"])
                self.assertEqual([], calls)

    def test_reconciliation_guard_prevents_every_live_observer_call(self):
        row, fixture, run = _p1_manifests()
        calls: list[str] = []
        dependencies = P1ManualObserverDependencies(
            status_reader=lambda: calls.append("status"),
            runtime_artifact_reader=lambda _cursor: calls.append("runtime_artifact"),
            artifact_recheck_reader=lambda: calls.append("artifact_recheck"),
            carry_on_recheck_reader=lambda: calls.append("carry_on_recheck"),
            trusted_fixture_reader=lambda: calls.append("fixture"),
            operator_action_reader=lambda: calls.append("action"),
            log_delta_reader=lambda: calls.append("log"),
            gameplay_reader=lambda: calls.append("gameplay"),
        )

        result = run_automatic_deposit_live_matrix_application(
            {"live_opt_in": True, "mutating_opt_in": True},
            row_id=row.row_id,
            run_manifest=run,
            fixture_manifest=fixture,
            guard_state_reader=lambda _root: _guard(
                OneShotGuardState.RECONCILIATION_REQUIRED
            ),
            p1_observer_dependencies=dependencies,
        )

        self.assertEqual("LIVE_ONE_SHOT_RECONCILIATION_REQUIRED", result["reason"])
        self.assertEqual("INCONCLUSIVE", result["verdict"])
        self.assertEqual(0, result["submit_call_count"])
        self.assertEqual([], calls)

    def test_malformed_guard_result_fails_before_every_observer(self):
        row, fixture, run = _p1_manifests()
        calls: list[str] = []
        observation, reason = _guard(OneShotGuardState.CLEAR)
        dependencies = P1ManualObserverDependencies(
            status_reader=lambda: calls.append("status"),
            runtime_artifact_reader=lambda _cursor: calls.append("runtime_artifact"),
            artifact_recheck_reader=lambda: calls.append("artifact_recheck"),
            carry_on_recheck_reader=lambda: calls.append("carry_on_recheck"),
            trusted_fixture_reader=lambda: calls.append("fixture"),
            operator_action_reader=lambda: calls.append("action"),
            log_delta_reader=lambda: calls.append("log"),
            gameplay_reader=lambda: calls.append("gameplay"),
        )

        cases = (
            (observation,),
            (observation, reason, "unexpected"),
            (observation, "different reason"),
            {"state": "CLEAR"},
        )
        for malformed in cases:
            with self.subTest(malformed=malformed):
                result = run_automatic_deposit_live_matrix_application(
                    {"live_opt_in": True, "mutating_opt_in": True},
                    row_id=row.row_id,
                    run_manifest=run,
                    fixture_manifest=fixture,
                    guard_state_reader=lambda _root, value=malformed: value,
                    p1_observer_dependencies=dependencies,
                )

                self.assertEqual(
                    "LIVE_ONE_SHOT_GUARD_STATE_UNREADABLE",
                    result["reason"],
                )
                self.assertEqual([], calls)

    def test_clear_guard_reports_exact_missing_read_only_observers(self):
        row, fixture, run = _p1_manifests()

        result = run_automatic_deposit_live_matrix_application(
            {"live_opt_in": True, "mutating_opt_in": True},
            row_id=row.row_id,
            run_manifest=run,
            fixture_manifest=fixture,
            guard_state_reader=lambda _root: _guard(OneShotGuardState.CLEAR),
            p1_observer_dependencies=P1ManualObserverDependencies(),
        )

        self.assertEqual("P1_LIVE_OBSERVER_DEPENDENCIES_MISSING", result["reason"])
        self.assertEqual(
            [
                "artifact_recheck_reader",
                "carry_on_recheck_reader",
                "gameplay_reader",
                "log_delta_reader",
                "operator_action_reader",
                "runtime_artifact_reader",
                "status_reader",
                "trusted_fixture_reader",
            ],
            result["missing_dependencies"],
        )
        self.assertEqual(0, result["submit_call_count"])

    def test_falsey_untyped_dependency_mapping_is_not_replaced_by_defaults(self):
        row, fixture, run = _p1_manifests()

        result = run_automatic_deposit_live_matrix_application(
            {"live_opt_in": True, "mutating_opt_in": True},
            row_id=row.row_id,
            run_manifest=run,
            fixture_manifest=fixture,
            guard_state_reader=lambda _root: _guard(OneShotGuardState.CLEAR),
            p1_observer_dependencies={},
        )

        self.assertEqual(
            "P1_LIVE_OBSERVER_DEPENDENCIES_NOT_TYPED",
            result["reason"],
        )
        self.assertEqual(0, result["submit_call_count"])

    def test_dependency_boundary_cannot_gain_submit_capability(self):
        dependencies = P1ManualObserverDependencies()

        with self.assertRaises((AttributeError, TypeError)):
            dependencies.submit = lambda _command: None


def _p1_manifests():
    row = next(row for row in automatic_deposit_matrix_catalog() if row.row_id == "P1")
    fixture = hermetic_fixture(row)
    return row, fixture, hermetic_run_manifest(row, fixture)


def _guard(state: OneShotGuardState):
    observation = OneShotGuardStateObservation(
        state=state,
        state_directory="C:/repo/tests/tmp_minecraft_chatclef_live_state",
        invocation_fingerprint=("a" * 64 if state is not OneShotGuardState.CLEAR else None),
        command_fingerprint=("b" * 64 if state is not OneShotGuardState.CLEAR else None),
        reason=state.value,
    )
    return observation, observation.reason


def _tracking_dependencies(calls: list[str]) -> P1ManualObserverDependencies:
    return P1ManualObserverDependencies(
        status_reader=lambda: calls.append("status"),
        runtime_artifact_reader=lambda _cursor: calls.append("runtime_artifact"),
        artifact_recheck_reader=lambda: calls.append("artifact_recheck"),
        carry_on_recheck_reader=lambda: calls.append("carry_on_recheck"),
        trusted_fixture_reader=lambda: calls.append("fixture"),
        operator_action_reader=lambda: calls.append("action"),
        log_delta_reader=lambda: calls.append("log"),
        gameplay_reader=lambda: calls.append("gameplay"),
    )


if __name__ == "__main__":
    unittest.main()
