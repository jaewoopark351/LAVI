# 20260901_kpopmodder: Bind a sealed manual action to the exact P1 row and harness run.
from __future__ import annotations

from ....preflight.command_fingerprint import command_fingerprint
from ...scenario.matrix_row import AutomaticDepositMatrixRow
from ..run_manifest import AutomaticDepositRunManifest
from .manual_operator_action_observation import ManualOperatorActionObservation


def verify_p1_manual_operator_action(
    observation: object,
    row: AutomaticDepositMatrixRow,
    run_manifest: AutomaticDepositRunManifest,
) -> tuple[str, ...]:
    if not isinstance(observation, ManualOperatorActionObservation):
        return ("P1_MANUAL_OPERATOR_ACTION_NOT_TYPED",)
    errors: list[str] = []
    if observation.schema_version != "automatic-deposit-manual-operator-action/v1":
        errors.append("P1_MANUAL_OPERATOR_ACTION_SCHEMA_INVALID")
    for actual, expected, reason in (
        (
            observation.run_id,
            run_manifest.run_id,
            "P1_MANUAL_OPERATOR_ACTION_RUN_ID_MISMATCH",
        ),
        (observation.row_id, row.row_id, "P1_MANUAL_OPERATOR_ACTION_ROW_ID_MISMATCH"),
        (
            observation.harness_operation_id,
            run_manifest.operation_id,
            "P1_MANUAL_OPERATOR_ACTION_HARNESS_OPERATION_ID_MISMATCH",
        ),
        (
            observation.action_text,
            row.expected_operator_action,
            "P1_MANUAL_OPERATOR_ACTION_TEXT_MISMATCH",
        ),
        (
            observation.action_fingerprint,
            run_manifest.operator_action_fingerprint,
            "P1_MANUAL_OPERATOR_ACTION_FINGERPRINT_MISMATCH",
        ),
    ):
        if actual != expected:
            errors.append(reason)
    if observation.action_fingerprint != command_fingerprint(observation.action_text):
        errors.append("P1_MANUAL_OPERATOR_ACTION_CONTENT_FINGERPRINT_MISMATCH")
    if observation.observation_source != "OPERATOR_ACTION_OBSERVER":
        errors.append("P1_MANUAL_OPERATOR_ACTION_SOURCE_INVALID")
    if observation.operator_confirmed is not True:
        errors.append("P1_MANUAL_OPERATOR_ACTION_NOT_CONFIRMED")
    return tuple(errors)
