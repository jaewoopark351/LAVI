# 20260901_kpopmodder: Seal a directly observed manual action without inventing runtime IDs.
from __future__ import annotations

import hashlib
import json
from dataclasses import dataclass, field


_MANUAL_OPERATOR_ACTION_OBSERVATION_SEAL = object()


@dataclass(frozen=True, slots=True)
class ManualOperatorActionObservation:
    schema_version: str
    run_id: str
    row_id: str
    harness_operation_id: str
    action_text: str
    action_fingerprint: str
    observation_source: str
    observer_identity_fingerprint: str
    operator_confirmed: bool
    observation_fingerprint: str
    _seal: object = field(default=None, repr=False, compare=False)
    _integrity: str = field(default="", repr=False, compare=False)

    def __post_init__(self) -> None:
        expected = _fingerprint(
            self.schema_version,
            self.run_id,
            self.row_id,
            self.harness_operation_id,
            self.action_text,
            self.action_fingerprint,
            self.observation_source,
            self.observer_identity_fingerprint,
            self.operator_confirmed,
        )
        if self._seal is not _MANUAL_OPERATOR_ACTION_OBSERVATION_SEAL:
            raise ValueError(
                "manual operator action observation must be created by its collector"
            )
        if self.observation_fingerprint != expected or self._integrity != expected:
            raise ValueError("manual operator action observation integrity mismatch")


def _create_manual_operator_action_observation(
    *,
    run_id: str,
    row_id: str,
    harness_operation_id: str,
    action_text: str,
    action_fingerprint: str,
    observation_source: str,
    observer_identity_fingerprint: str,
    operator_confirmed: bool,
) -> ManualOperatorActionObservation:
    schema_version = "automatic-deposit-manual-operator-action/v1"
    fingerprint = _fingerprint(
        schema_version,
        run_id,
        row_id,
        harness_operation_id,
        action_text,
        action_fingerprint,
        observation_source,
        observer_identity_fingerprint,
        operator_confirmed,
    )
    return ManualOperatorActionObservation(
        schema_version=schema_version,
        run_id=run_id,
        row_id=row_id,
        harness_operation_id=harness_operation_id,
        action_text=action_text,
        action_fingerprint=action_fingerprint,
        observation_source=observation_source,
        observer_identity_fingerprint=observer_identity_fingerprint,
        operator_confirmed=operator_confirmed,
        observation_fingerprint=fingerprint,
        _seal=_MANUAL_OPERATOR_ACTION_OBSERVATION_SEAL,
        _integrity=fingerprint,
    )


def _fingerprint(*values: object) -> str:
    payload = json.dumps(
        values,
        ensure_ascii=False,
        separators=(",", ":"),
    ).encode("utf-8")
    return hashlib.sha256(payload).hexdigest()
