# 20260901_kpopmodder: Require an exact P1 manual harness run at collect and consume time.
from __future__ import annotations

import re

from .....scenario.transport_mode import AutomaticDepositTransportMode
from ....latest_log_byte_cursor import (
    AutomaticDepositLatestLogByteCursor,
    automatic_deposit_latest_log_cursor_fingerprint,
)
from ....run_manifest import AutomaticDepositRunManifest
from .p1_harness_run_binding import P1HarnessRunBinding
from .p1_harness_run_fingerprint import p1_harness_run_manifest_fingerprint


_SHA256 = re.compile(r"[0-9a-fA-F]{64}\Z", re.ASCII)


def verify_p1_harness_run_input(run_manifest: object) -> tuple[str, ...]:
    if not isinstance(run_manifest, AutomaticDepositRunManifest):
        return ("P1_RUNTIME_OBSERVATION_RUN_MANIFEST_NOT_TYPED",)
    errors: list[str] = []
    if run_manifest.row_id != "P1":
        errors.append("P1_RUNTIME_OBSERVATION_RUN_ROW_NOT_P1")
    if (
        run_manifest.transport_mode
        is not AutomaticDepositTransportMode.OPERATOR_MANUAL_OBSERVE_ONLY
    ):
        errors.append("P1_RUNTIME_OBSERVATION_RUN_TRANSPORT_NOT_MANUAL")
    if not _bounded_identity(run_manifest.run_id):
        errors.append("P1_RUNTIME_OBSERVATION_HARNESS_RUN_ID_INVALID")
    if not _bounded_identity(run_manifest.operation_id):
        errors.append("P1_RUNTIME_OBSERVATION_HARNESS_OPERATION_ID_INVALID")
    if not isinstance(
        run_manifest.latest_log_cursor,
        AutomaticDepositLatestLogByteCursor,
    ):
        errors.append("P1_RUNTIME_OBSERVATION_RUN_CURSOR_NOT_TYPED")
    if not isinstance(run_manifest.fixture_fingerprint, str) or _SHA256.fullmatch(
        run_manifest.fixture_fingerprint
    ) is None:
        errors.append("P1_RUNTIME_OBSERVATION_FIXTURE_FINGERPRINT_INVALID")
    try:
        p1_harness_run_manifest_fingerprint(run_manifest)
    except (TypeError, ValueError):
        errors.append(
            "P1_RUNTIME_OBSERVATION_HARNESS_RUN_FINGERPRINT_UNAVAILABLE"
        )
    return tuple(errors)


def verify_p1_harness_run_binding(
    binding: object,
    run_manifest: object,
) -> tuple[str, ...]:
    errors = list(verify_p1_harness_run_input(run_manifest))
    if not isinstance(binding, P1HarnessRunBinding):
        return tuple(
            dict.fromkeys(
                ("P1_RUNTIME_OBSERVATION_HARNESS_RUN_BINDING_NOT_TYPED", *errors)
            )
        )
    if not isinstance(run_manifest, AutomaticDepositRunManifest):
        return tuple(dict.fromkeys(errors))
    transport_value = (
        run_manifest.transport_mode.value
        if isinstance(run_manifest.transport_mode, AutomaticDepositTransportMode)
        else ""
    )
    try:
        run_fingerprint = p1_harness_run_manifest_fingerprint(run_manifest)
    except (TypeError, ValueError):
        run_fingerprint = ""
        errors.append(
            "P1_RUNTIME_OBSERVATION_HARNESS_RUN_FINGERPRINT_UNAVAILABLE"
        )

    comparisons = (
        (
            binding.run_id,
            run_manifest.run_id,
            "P1_RUNTIME_OBSERVATION_HARNESS_RUN_ID_MISMATCH",
        ),
        (
            binding.operation_id,
            run_manifest.operation_id,
            "P1_RUNTIME_OBSERVATION_HARNESS_OPERATION_ID_MISMATCH",
        ),
        (
            binding.row_id,
            run_manifest.row_id,
            "P1_RUNTIME_OBSERVATION_HARNESS_ROW_ID_MISMATCH",
        ),
        (
            binding.transport_mode,
            transport_value,
            "P1_RUNTIME_OBSERVATION_HARNESS_TRANSPORT_MISMATCH",
        ),
        (
            binding.fixture_fingerprint,
            run_manifest.fixture_fingerprint,
            "P1_RUNTIME_OBSERVATION_HARNESS_FIXTURE_MISMATCH",
        ),
        (
            binding.run_manifest_fingerprint,
            run_fingerprint,
            "P1_RUNTIME_OBSERVATION_HARNESS_RUN_FINGERPRINT_MISMATCH",
        ),
    )
    for actual, expected, reason in comparisons:
        if actual != expected:
            errors.append(reason)
    if isinstance(
        run_manifest.latest_log_cursor,
        AutomaticDepositLatestLogByteCursor,
    ):
        try:
            cursor_fingerprint = (
                automatic_deposit_latest_log_cursor_fingerprint(
                    run_manifest.latest_log_cursor
                )
            )
        except (AttributeError, OSError, TypeError, ValueError):
            cursor_fingerprint = ""
            errors.append(
                "P1_RUNTIME_OBSERVATION_HARNESS_CURSOR_FINGERPRINT_UNAVAILABLE"
            )
        if (
            cursor_fingerprint
            and binding.latest_log_cursor_fingerprint != cursor_fingerprint
        ):
            errors.append("P1_RUNTIME_OBSERVATION_HARNESS_CURSOR_MISMATCH")
    return tuple(dict.fromkeys(errors))


def _bounded_identity(value: object) -> bool:
    return (
        isinstance(value, str)
        and value == value.strip()
        and 0 < len(value) <= 256
        and not any(
            ord(character) < 0x20 or ord(character) == 0x7F
            for character in value
        )
    )
