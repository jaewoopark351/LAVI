#20260818_kpopmodder: Require complete positive gameplay evidence before batch continuation.
from __future__ import annotations

from collections.abc import Mapping

from ..observation.gameplay_outcome import apply_gameplay_evidence


CHECKPOINT_FIELDS = (
    "gameplay_observation_complete",
    "gameplay_effect_observed",
    "expected_gameplay_effect_verified",
    "partial_gameplay_effect_observed",
    "unexpected_effect_observed",
    "prohibited_effect_absence_verified",
)

CHECKPOINT_EVIDENCE_FIELDS = (
    "gameplay_oracle_scope",
    "target_count_before",
    "target_count_after",
    "expected_item_delta",
    "observed_item_delta",
)


def apply_batch_gameplay_checkpoint(
    observation: dict[str, object],
    checkpoint: object,
) -> dict[str, object]:
    if not isinstance(checkpoint, Mapping):
        return _result(False, "gameplay checkpoint is missing")
    values: dict[str, bool | None] = {}
    for field in CHECKPOINT_FIELDS:
        value = checkpoint.get(field)
        if value is not None and not isinstance(value, bool):
            return _result(False, f"gameplay checkpoint field is invalid: {field}")
        values[field] = value
    apply_gameplay_evidence(
        observation,
        observation_complete=values["gameplay_observation_complete"],
        effect_observed=values["gameplay_effect_observed"],
        expected_effect_verified=values["expected_gameplay_effect_verified"],
        partial_effect_observed=values["partial_gameplay_effect_observed"],
        unexpected_effect_observed=values["unexpected_effect_observed"],
        prohibited_effect_absence_verified=values[
            "prohibited_effect_absence_verified"
        ],
    )
    for field in CHECKPOINT_EVIDENCE_FIELDS:
        if field in checkpoint:
            observation[field] = checkpoint[field]
    if observation.get("end_to_end_success") is not True:
        reason = str(
            checkpoint.get("reason")
            or "gameplay checkpoint did not prove end-to-end success"
        )
        return _result(False, reason)
    return _result(True, "gameplay checkpoint verified")


def _result(ok: bool, reason: str) -> dict[str, object]:
    return {"ok": ok, "reason": reason}
