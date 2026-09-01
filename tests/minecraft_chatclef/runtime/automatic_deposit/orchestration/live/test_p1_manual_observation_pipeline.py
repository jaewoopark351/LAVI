#20260901_kpopmodder: Lock the P1 read-only observer order and final guard boundary.
from __future__ import annotations

import unittest
from types import SimpleNamespace
from unittest.mock import patch

from minecraft_chatclef.runtime.submission.guard_state import (
    OneShotGuardState,
    OneShotGuardStateObservation,
)

from ...evidence.artifact_identity_collection import (
    _create_artifact_identity_collection,
)
from ..live_matrix_application import (
    run_automatic_deposit_live_matrix_application,
)
from ._p1_live_test_fixture import P1LiveTestContext, p1_live_test_context
from ._p1_public_fixture_test_observer import (
    observe_public_inconclusive_fixture,
)
from .p1_manual_observer_dependencies import P1ManualObserverDependencies


class P1ManualObservationPipelineTests(unittest.TestCase):
    def test_public_fixture_inconclusive_stops_before_post_action_observers(self):
        fixture_observation, observer_reason = observe_public_inconclusive_fixture()
        self.assertEqual("P1_LIVE_FIXTURE_OBSERVED_INCONCLUSIVE", observer_reason)
        self.assertIsNotNone(fixture_observation)
        context = p1_live_test_context(
            fixture_ready=False,
            fixture_observation=fixture_observation,
        )
        calls: list[str] = []
        cursors: list[object] = []
        guards: list[str] = []

        result = self._run(
            context,
            _dependencies(context, calls, cursors),
            _guard_reader(guards, OneShotGuardState.CLEAR),
        )

        self.assertEqual(
            ["status", "artifact_recheck", "carry_on_recheck", "runtime_artifact", "fixture"],
            calls,
        )
        self.assertEqual(["guard:CLEAR"], guards)
        self.assertEqual([context.run.latest_log_cursor], cursors)
        self.assertIs(context.run.latest_log_cursor, cursors[0])
        self.assertEqual("P1_LIVE_FIXTURE_NOT_READY", result["reason"])
        self.assertTrue(result["fixture_identity_bound"])
        self.assertFalse(result["fixture_ready"])
        self.assertEqual(
            ["RUNNING_JVM_SAVE_BARRIER_NOT_AVAILABLE"],
            result["fixture_limitations"],
        )
        self.assertEqual(
            context.fixture_observation.observation_fingerprint,
            result["fixture_observation_fingerprint"],
        )
        self.assertEqual(0, result["submit_call_count"])

    def test_private_ready_fixture_reads_all_evidence_then_evaluates(self):
        context = p1_live_test_context(fixture_ready=True)
        calls: list[str] = []
        cursors: list[object] = []
        guards: list[str] = []

        result = self._run(
            context,
            _dependencies(context, calls, cursors),
            _guard_reader(
                guards,
                OneShotGuardState.CLEAR,
                OneShotGuardState.CLEAR,
            ),
        )

        self.assertEqual(
            [
                "status",
                "artifact_recheck",
                "carry_on_recheck",
                "runtime_artifact",
                "fixture",
                "operator_action",
                "log_delta",
                "gameplay",
            ],
            calls,
        )
        self.assertEqual(["guard:CLEAR", "guard:CLEAR"], guards)
        self.assertEqual("PASS", result["verdict"])
        self.assertEqual("ALL_REQUIRED_EVIDENCE_VERIFIED", result["reason"])
        self.assertEqual(0, result["submit_call_count"])
        self.assertRegex(
            str(result["runtime_observation_bundle_fingerprint"]),
            r"\A[0-9a-f]{64}\Z",
        )

    def test_final_reconciliation_guard_stops_immediately_before_matrix_evaluation(self):
        context = p1_live_test_context(fixture_ready=True)
        calls: list[str] = []
        cursors: list[object] = []
        guards: list[str] = []

        with patch(
            "minecraft_chatclef.runtime.automatic_deposit.orchestration.live."
            "p1_pipeline.p1_matrix_evaluator.run_automatic_deposit_matrix_row"
        ) as evaluator:
            result = self._run(
                context,
                _dependencies(context, calls, cursors),
                _guard_reader(
                    guards,
                    OneShotGuardState.CLEAR,
                    OneShotGuardState.RECONCILIATION_REQUIRED,
                ),
            )

        self.assertEqual("LIVE_ONE_SHOT_RECONCILIATION_REQUIRED", result["reason"])
        self.assertEqual(0, result["submit_call_count"])
        self.assertEqual(
            [
                "status",
                "artifact_recheck",
                "carry_on_recheck",
                "runtime_artifact",
                "fixture",
                "operator_action",
                "log_delta",
                "gameplay",
            ],
            calls,
        )
        self.assertEqual(
            ["guard:CLEAR", "guard:RECONCILIATION_REQUIRED"],
            guards,
        )
        evaluator.assert_not_called()

    def test_malformed_final_guard_stops_before_matrix_evaluation(self):
        context = p1_live_test_context(fixture_ready=True)
        dependencies = _dependencies(context, [], [])
        guard_calls = 0

        def guard_reader(_repository_root: str):
            nonlocal guard_calls
            guard_calls += 1
            if guard_calls == 1:
                return _guard(OneShotGuardState.CLEAR)
            return {"state": "CLEAR"}

        with patch(
            "minecraft_chatclef.runtime.automatic_deposit.orchestration.live."
            "p1_pipeline.p1_matrix_evaluator.run_automatic_deposit_matrix_row"
        ) as evaluator:
            result = self._run(context, dependencies, guard_reader)

        self.assertEqual("LIVE_ONE_SHOT_GUARD_STATE_UNREADABLE", result["reason"])
        self.assertEqual(2, guard_calls)
        self.assertEqual(0, result["submit_call_count"])
        evaluator.assert_not_called()

    def test_matrix_submit_count_is_never_propagated(self):
        context = p1_live_test_context(fixture_ready=True)
        dependencies = _dependencies(context, [], [])
        with patch(
            "minecraft_chatclef.runtime.automatic_deposit.orchestration.live."
            "p1_pipeline.p1_matrix_evaluator.run_automatic_deposit_matrix_row",
            return_value=SimpleNamespace(submit_call_count=1),
        ):
            result = self._run(
                context,
                dependencies,
                _guard_reader(
                    [],
                    OneShotGuardState.CLEAR,
                    OneShotGuardState.CLEAR,
                ),
            )

        self.assertEqual("P1_LIVE_MATRIX_SUBMIT_COUNT_INVALID", result["reason"])
        self.assertEqual(0, result["submit_call_count"])

    def test_each_external_reader_must_return_its_exact_sealed_type(self):
        names = (
            "status_reader",
            "artifact_recheck_reader",
            "carry_on_recheck_reader",
            "runtime_artifact_reader",
            "trusted_fixture_reader",
            "operator_action_reader",
            "log_delta_reader",
            "gameplay_reader",
        )
        for name in names:
            with self.subTest(name=name):
                context = p1_live_test_context(fixture_ready=True)
                calls: list[str] = []
                cursors: list[object] = []
                dependencies = _dependencies(context, calls, cursors)
                object.__setattr__(dependencies, name, lambda *args: ({}, "OBSERVED"))

                result = self._run(
                    context,
                    dependencies,
                    _guard_reader([], OneShotGuardState.CLEAR),
                )

                self.assertEqual(
                    "P1_LIVE_OBSERVER_VALUE_NOT_TYPED",
                    result["reason"],
                )
                self.assertEqual(name, result["observer_name"])
                self.assertEqual(0, result["submit_call_count"])

    def test_typed_failed_artifact_recheck_is_still_fail_closed(self):
        context = p1_live_test_context(fixture_ready=True)
        calls: list[str] = []
        cursors: list[object] = []
        dependencies = _dependencies(context, calls, cursors)
        failed = _create_artifact_identity_collection(False, "ARTIFACT_CHANGED")
        object.__setattr__(
            dependencies,
            "artifact_recheck_reader",
            lambda: (failed, "ARTIFACT_RECHECK_OBSERVED"),
        )

        result = self._run(
            context,
            dependencies,
            _guard_reader([], OneShotGuardState.CLEAR),
        )

        self.assertEqual("P1_ARTIFACT_RECHECK_NOT_VERIFIED", result["reason"])
        self.assertEqual(
            ["ARTIFACT_RECHECK_FAILED:ARTIFACT_CHANGED"],
            result["violated_contracts"],
        )
        self.assertEqual(0, result["submit_call_count"])

    def test_new_runtime_without_run_manifest_stops_before_fixture_observation(self):
        context = p1_live_test_context(fixture_ready=True)
        calls: list[str] = []
        cursors: list[object] = []
        guards: list[str] = []
        dependencies = _dependencies(context, calls, cursors)

        def missing_run_manifest(cursor):
            calls.append("runtime_artifact")
            cursors.append(cursor)
            return (
                None,
                "JVM_LOG_PREFIX_ARTIFACT_NOT_OBSERVED:"
                "JVM_RUN_MANIFEST_RECORD_MISSING",
            )

        object.__setattr__(
            dependencies,
            "runtime_artifact_reader",
            missing_run_manifest,
        )

        result = self._run(
            context,
            dependencies,
            _guard_reader(guards, OneShotGuardState.CLEAR),
        )

        self.assertEqual(
            ["status", "artifact_recheck", "carry_on_recheck", "runtime_artifact"],
            calls,
        )
        self.assertEqual(["guard:CLEAR"], guards)
        self.assertEqual([context.run.latest_log_cursor], cursors)
        self.assertEqual(
            "P1_LIVE_OBSERVER_REPORTED_INCONCLUSIVE",
            result["reason"],
        )
        self.assertEqual("runtime_artifact_reader", result["observer_name"])
        self.assertEqual(
            "JVM_LOG_PREFIX_ARTIFACT_NOT_OBSERVED:"
            "JVM_RUN_MANIFEST_RECORD_MISSING",
            result["observer_source_reason"],
        )
        self.assertEqual(0, result["submit_call_count"])

    def test_observer_exception_does_not_leak_message_or_path(self):
        context = p1_live_test_context(fixture_ready=True)
        dependencies = _dependencies(context, [], [])

        def raise_private_error():
            raise RuntimeError("C:/private/world/player.dat")

        object.__setattr__(dependencies, "status_reader", raise_private_error)
        result = self._run(
            context,
            dependencies,
            _guard_reader([], OneShotGuardState.CLEAR),
        )

        self.assertEqual("P1_LIVE_OBSERVER_CALL_FAILED", result["reason"])
        self.assertEqual("status_reader", result["observer_name"])
        self.assertEqual("RuntimeError", result["observer_error_type"])
        self.assertNotIn("private", repr(result))
        self.assertNotIn("player.dat", repr(result))

    def test_inconclusive_source_reason_does_not_leak_a_path(self):
        context = p1_live_test_context(fixture_ready=True)
        dependencies = _dependencies(context, [], [])
        object.__setattr__(
            dependencies,
            "status_reader",
            lambda: (None, "C:/private/world/player.dat"),
        )

        result = self._run(
            context,
            dependencies,
            _guard_reader([], OneShotGuardState.CLEAR),
        )

        self.assertEqual(
            "P1_LIVE_OBSERVER_REPORTED_INCONCLUSIVE",
            result["reason"],
        )
        self.assertNotIn("observer_source_reason", result)
        self.assertNotIn("private", repr(result))

    @staticmethod
    def _run(
        context: P1LiveTestContext,
        dependencies: P1ManualObserverDependencies,
        guard_reader,
    ) -> dict[str, object]:
        return run_automatic_deposit_live_matrix_application(
            {"live_opt_in": True, "mutating_opt_in": True},
            row_id=context.row.row_id,
            run_manifest=context.run,
            fixture_manifest=context.fixture,
            guard_state_reader=guard_reader,
            p1_observer_dependencies=dependencies,
        )


def _dependencies(
    context: P1LiveTestContext,
    calls: list[str],
    cursors: list[object],
) -> P1ManualObserverDependencies:
    def observe(name: str, value: object, reason: str):
        def reader():
            calls.append(name)
            return value, reason

        return reader

    def runtime_artifact_reader(cursor):
        calls.append("runtime_artifact")
        cursors.append(cursor)
        return context.log_prefix, "JVM_RUNTIME_ARTIFACT_LOG_PREFIX_OBSERVED"

    return P1ManualObserverDependencies(
        status_reader=observe(
            "status",
            context.status,
            "PRODUCTION_FABRIC_STATUS_OBSERVED",
        ),
        artifact_recheck_reader=observe(
            "artifact_recheck",
            context.artifact_collection,
            "ARTIFACT_RECHECK_OBSERVED",
        ),
        carry_on_recheck_reader=observe(
            "carry_on_recheck",
            context.carry_on_collection,
            "CARRY_ON_RECHECK_OBSERVED",
        ),
        runtime_artifact_reader=runtime_artifact_reader,
        trusted_fixture_reader=observe(
            "fixture",
            context.fixture_observation,
            "P1_LIVE_FIXTURE_OBSERVED_INCONCLUSIVE"
            if context.fixture_observation.verdict == "INCONCLUSIVE"
            else "P1_LIVE_FIXTURE_OBSERVED_READY",
        ),
        operator_action_reader=observe(
            "operator_action",
            context.operator_action,
            "MANUAL_OPERATOR_ACTION_OBSERVED",
        ),
        log_delta_reader=observe(
            "log_delta",
            context.log_delta,
            "LATEST_LOG_DELTA_READ",
        ),
        gameplay_reader=observe(
            "gameplay",
            context.gameplay_observation,
            "GAMEPLAY_OBSERVATION_COLLECTED",
        ),
    )


def _guard_reader(calls: list[str], *states: OneShotGuardState):
    remaining = list(states)

    def reader(_repository_root: str):
        state = remaining.pop(0)
        calls.append(f"guard:{state.value}")
        return _guard(state)

    return reader


def _guard(state: OneShotGuardState):
    observation = OneShotGuardStateObservation(
        state=state,
        state_directory="C:/repo/tests/tmp_minecraft_chatclef_live_state",
        invocation_fingerprint=(
            None if state is OneShotGuardState.CLEAR else "a" * 64
        ),
        command_fingerprint=(
            None if state is OneShotGuardState.CLEAR else "b" * 64
        ),
        reason=state.value,
    )
    return observation, observation.reason


if __name__ == "__main__":
    unittest.main()
