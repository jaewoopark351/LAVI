# 20260901_kpopmodder: Seal the harness run selected for one runtime observation bundle.
from __future__ import annotations

import hashlib
import json
from dataclasses import dataclass, field

from ....latest_log_byte_cursor import (
    automatic_deposit_latest_log_cursor_fingerprint,
)
from ....run_manifest import AutomaticDepositRunManifest
from .p1_harness_run_fingerprint import p1_harness_run_manifest_fingerprint


_P1_HARNESS_RUN_BINDING_SEAL = object()


@dataclass(frozen=True, slots=True)
class P1HarnessRunBinding:
    run_id: str
    operation_id: str
    row_id: str
    transport_mode: str
    fixture_fingerprint: str
    latest_log_cursor_fingerprint: str
    run_manifest_fingerprint: str
    binding_fingerprint: str
    _seal: object = field(default=None, repr=False, compare=False)
    _integrity: str = field(default="", repr=False, compare=False)

    def __post_init__(self) -> None:
        expected = _binding_fingerprint(self)
        if self._seal is not _P1_HARNESS_RUN_BINDING_SEAL:
            raise ValueError("P1 harness run binding must be collector-created")
        if self.binding_fingerprint != expected or self._integrity != expected:
            raise ValueError("P1 harness run binding integrity mismatch")


def _create_p1_harness_run_binding(
    run_manifest: AutomaticDepositRunManifest,
) -> P1HarnessRunBinding:
    values = {
        "run_id": run_manifest.run_id,
        "operation_id": run_manifest.operation_id,
        "row_id": run_manifest.row_id,
        "transport_mode": run_manifest.transport_mode.value,
        "fixture_fingerprint": run_manifest.fixture_fingerprint,
        "latest_log_cursor_fingerprint": (
            automatic_deposit_latest_log_cursor_fingerprint(
                run_manifest.latest_log_cursor
            )
        ),
        "run_manifest_fingerprint": p1_harness_run_manifest_fingerprint(
            run_manifest
        ),
    }
    provisional = P1HarnessRunBinding.__new__(P1HarnessRunBinding)
    for name, value in values.items():
        object.__setattr__(provisional, name, value)
    fingerprint = _binding_fingerprint(provisional)
    return P1HarnessRunBinding(
        **values,
        binding_fingerprint=fingerprint,
        _seal=_P1_HARNESS_RUN_BINDING_SEAL,
        _integrity=fingerprint,
    )


def _binding_fingerprint(binding: P1HarnessRunBinding) -> str:
    payload = json.dumps(
        {
            "fixture_fingerprint": binding.fixture_fingerprint,
            "latest_log_cursor_fingerprint": (
                binding.latest_log_cursor_fingerprint
            ),
            "operation_id": binding.operation_id,
            "row_id": binding.row_id,
            "run_id": binding.run_id,
            "run_manifest_fingerprint": binding.run_manifest_fingerprint,
            "transport_mode": binding.transport_mode,
        },
        ensure_ascii=False,
        separators=(",", ":"),
        sort_keys=True,
    ).encode("utf-8")
    return hashlib.sha256(payload).hexdigest()
