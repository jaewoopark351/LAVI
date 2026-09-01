#20260831_kpopmodder: Expose only builder-sealed evidence to the row verdict.
from __future__ import annotations

import hashlib
import json
from dataclasses import dataclass, field

_VERIFIED_EVIDENCE_SEAL = object()


@dataclass(frozen=True, slots=True)
class AutomaticDepositVerifiedEvidence:
    run_id: str
    row_id: str
    operation_id: str
    artifact_sha256: str
    fixture_fingerprint: str
    values: tuple[tuple[str, object], ...]
    _seal: object = field(repr=False, compare=False)
    _integrity: str = field(repr=False, compare=False)

    def __post_init__(self) -> None:
        if self._seal is not _VERIFIED_EVIDENCE_SEAL:
            raise ValueError("verified evidence must be created by its verifier")
        if self._integrity != _evidence_integrity(
            self.run_id,
            self.row_id,
            self.operation_id,
            self.artifact_sha256,
            self.fixture_fingerprint,
            self.values,
        ):
            raise ValueError("verified evidence integrity mismatch")

    def as_mapping(self) -> dict[str, object]:
        return dict(self.values)


def _create_verified_evidence(
    *,
    run_id: str,
    row_id: str,
    operation_id: str,
    artifact_sha256: str,
    fixture_fingerprint: str,
    values: dict[str, object],
) -> AutomaticDepositVerifiedEvidence:
    normalized_values = tuple(sorted(values.items()))
    return AutomaticDepositVerifiedEvidence(
        run_id=run_id,
        row_id=row_id,
        operation_id=operation_id,
        artifact_sha256=artifact_sha256,
        fixture_fingerprint=fixture_fingerprint,
        values=normalized_values,
        _seal=_VERIFIED_EVIDENCE_SEAL,
        _integrity=_evidence_integrity(
            run_id,
            row_id,
            operation_id,
            artifact_sha256,
            fixture_fingerprint,
            normalized_values,
        ),
    )


def _evidence_integrity(
    run_id: str,
    row_id: str,
    operation_id: str,
    artifact_sha256: str,
    fixture_fingerprint: str,
    values: tuple[tuple[str, object], ...],
) -> str:
    payload = json.dumps(
        {
            "run_id": run_id,
            "row_id": row_id,
            "operation_id": operation_id,
            "artifact_sha256": artifact_sha256,
            "fixture_fingerprint": fixture_fingerprint,
            "values": values,
        },
        ensure_ascii=False,
        separators=(",", ":"),
        sort_keys=True,
    ).encode("utf-8")
    return hashlib.sha256(payload).hexdigest()
