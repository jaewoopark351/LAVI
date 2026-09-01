#20260901_kpopmodder: Bind actual submit identity, supervised StoreHome logs, and gameplay without replay.
from __future__ import annotations

import unittest

from minecraft_chatclef.runtime.automatic_deposit.evidence.runtime_observer.status_artifact._test_fixture import (
    status_with_p1_supervised_runtime_artifact,
)
from minecraft_chatclef.runtime.automatic_deposit.evidence.runtime_observer.status_artifact.p1_supervised_runtime_artifact_observer import (
    observe_p1_supervised_runtime_artifact,
)
from minecraft_chatclef.runtime.automatic_deposit.orchestration.live.p1_supervised.execution.p1_supervised_execution_result import (
    seal_p1_supervised_execution_result,
)
from minecraft_chatclef.runtime.automatic_deposit.orchestration.live.p1_supervised._test_fixture import (
    complete_p1_supervised_gameplay_observation,
    complete_p1_supervised_observation,
    ready_p1_supervised_live_fixture,
    supervised_p1_log_delta,
)

from .p1_supervised_verified_evidence_assembly import (
    build_p1_supervised_verified_evidence,
)


class P1SupervisedVerifiedEvidenceAssemblyTests(unittest.TestCase):
    def test_builds_verified_evidence_only_from_complete_correlated_result(self):
        result = _build()

        self.assertTrue(result.ok, result.reason)
        self.assertEqual("VERIFIED_PENDING_RECONCILIATION", result.verdict)
        self.assertFalse(result.reconciled)
        self.assertEqual("lavi-gui-request-17", result.submitted_request_id)
        self.assertEqual("17", result.operation_id)
        self.assertEqual(1, result.submit_call_count)
        self.assertEqual(0, result.automatic_resubmit_count)

    def test_request_id_mismatch_is_inconclusive_and_keeps_submit_count_one(self):
        result = _build(log_request_id="lavi-gui-wrong-request")

        self.assertFalse(result.ok)
        self.assertEqual("INCONCLUSIVE", result.verdict)
        self.assertEqual(
            "P1_STORE_HOME_COMMAND_REQUEST_ID_NOT_EXPECTED",
            result.reason,
        )
        self.assertEqual(1, result.submit_call_count)
        self.assertEqual(0, result.automatic_resubmit_count)

    def test_incomplete_terminal_or_gameplay_is_inconclusive_without_replay(self):
        cases = (
            (
                {"terminal_status": "running"},
                "P1_SUPERVISED_TERMINAL_STATUS_NOT_COMPLETED",
            ),
            (
                {"runtime_reported_completion": False},
                "P1_SUPERVISED_RUNTIME_COMPLETION_NOT_VERIFIED",
            ),
            (
                {"gameplay_observation_complete": False},
                (
                    "P1_SUPERVISED_GAMEPLAY_RECEIPT_CONFLICT:"
                    "gameplay_observation_complete"
                ),
            ),
        )
        for overrides, expected_reason in cases:
            with self.subTest(expected_reason=expected_reason):
                result = _build(observation_overrides=overrides)

                self.assertFalse(result.ok)
                self.assertEqual("INCONCLUSIVE", result.verdict)
                self.assertEqual(expected_reason, result.reason)
                self.assertEqual(1, result.submit_call_count)
                self.assertEqual(0, result.automatic_resubmit_count)

    def test_missing_typed_artifact_fixture_or_gameplay_is_inconclusive(self):
        for missing, expected_reason in (
            ("artifact", "P1_SUPERVISED_RUNTIME_ARTIFACT_NOT_TYPED"),
            ("fixture", "P1_SUPERVISED_LIVE_FIXTURE_NOT_TYPED"),
            ("gameplay", "P1_SUPERVISED_GAMEPLAY_OBSERVATION_NOT_TYPED"),
        ):
            with self.subTest(missing=missing):
                result = _build(missing=missing)

                self.assertFalse(result.ok)
                self.assertEqual("INCONCLUSIVE", result.verdict)
                self.assertEqual(expected_reason, result.reason)

    def test_runtime_session_and_activation_fixture_are_exactly_bound(self):
        cases = (
            (
                {"command_session_id": "different-session"},
                "P1_STORE_HOME_COMMAND_SESSION_ID_NOT_EXPECTED",
            ),
            (
                {"world_key": "different-world"},
                "P1_STORE_HOME_WORLD_KEY_NOT_EXPECTED",
            ),
            (
                {"dimension": "NETHER"},
                "P1_STORE_HOME_DIMENSION_NOT_EXPECTED",
            ),
            (
                {"destination_canonical_key": "wrong|canonical|key"},
                "P1_STORE_HOME_DESTINATION_CANONICAL_KEY_NOT_EXPECTED",
            ),
            (
                {"destination_id": "td_wrong"},
                "P1_STORE_HOME_DESTINATION_ID_NOT_EXPECTED",
            ),
        )
        for log_overrides, expected_reason in cases:
            with self.subTest(expected_reason=expected_reason):
                result = _build(log_overrides=log_overrides)

                self.assertFalse(result.ok)
                self.assertEqual(expected_reason, result.reason)

    def test_gameplay_must_bind_run_operation_artifact_fixture_and_snapshots(self):
        cases = (
            ({"run_id": "different-run"}, "P1_SUPERVISED_GAMEPLAY_RUN_ID_MISMATCH"),
            (
                {"operation_id": "999"},
                "P1_SUPERVISED_GAMEPLAY_OPERATION_ID_MISMATCH",
            ),
            (
                {"artifact_sha256": "f" * 64},
                "P1_SUPERVISED_GAMEPLAY_ARTIFACT_SHA256_MISMATCH",
            ),
            (
                {"fixture_fingerprint": "e" * 64},
                "P1_SUPERVISED_GAMEPLAY_FIXTURE_FINGERPRINT_MISMATCH",
            ),
            (
                {"before_world_snapshot_id": "wrong-world-snapshot"},
                "P1_SUPERVISED_GAMEPLAY_BEFORE_WORLD_SNAPSHOT_MISMATCH",
            ),
            (
                {"before_inventory_snapshot_id": "wrong-inventory-snapshot"},
                "P1_SUPERVISED_GAMEPLAY_BEFORE_INVENTORY_SNAPSHOT_MISMATCH",
            ),
        )
        for gameplay_kwargs, expected_reason in cases:
            with self.subTest(expected_reason=expected_reason):
                result = _build(gameplay_kwargs=gameplay_kwargs)

                self.assertFalse(result.ok)
                self.assertEqual(expected_reason, result.reason)

    def test_gameplay_checkpoint_values_are_not_hardcoded_to_pass(self):
        cases = (
            ("gameplay_observation_complete", False),
            ("gameplay_effect_observed", False),
            ("expected_gameplay_effect_verified", False),
            ("partial_gameplay_effect_observed", True),
            ("unexpected_effect_observed", True),
            ("prohibited_effect_absence_verified", False),
        )
        for key, unsafe_value in cases:
            with self.subTest(key=key):
                result = _build(
                    gameplay_kwargs={"overrides": {key: unsafe_value}}
                )

                self.assertFalse(result.ok)
                self.assertEqual(
                    f"P1_SUPERVISED_GAMEPLAY_CHECKPOINT_INVALID:{key}",
                    result.reason,
                )


def _build(
    *,
    log_request_id: str = "lavi-gui-request-17",
    observation_overrides: dict[str, object] | None = None,
    missing: str = "",
    log_overrides: dict[str, str] | None = None,
    gameplay_kwargs: dict[str, object] | None = None,
):
    observation = complete_p1_supervised_observation()
    observation.update(observation_overrides or {})
    execution = seal_p1_supervised_execution_result(
        {"preflight": {"status": "ok"}, "observation": observation}
    )
    artifact, artifact_reason = observe_p1_supervised_runtime_artifact(
        status_with_p1_supervised_runtime_artifact()
    )
    if artifact is None:
        raise AssertionError(artifact_reason)
    fixture = ready_p1_supervised_live_fixture()
    gameplay = complete_p1_supervised_gameplay_observation(
        fixture=fixture,
        **(gameplay_kwargs or {}),
    )
    return build_p1_supervised_verified_evidence(
        execution,
        supervised_p1_log_delta(log_request_id, **(log_overrides or {})),
        runtime_artifact=None if missing == "artifact" else artifact,
        live_fixture=None if missing == "fixture" else fixture,
        gameplay_observation=None if missing == "gameplay" else gameplay,
        expected_run_manifest_id="opaque-p1-run",
    )


if __name__ == "__main__":
    unittest.main()
