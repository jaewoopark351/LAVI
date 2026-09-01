# 20260901_kpopmodder: Validate and collect only a direct bounded manual-action observation.
from __future__ import annotations

import re

from ....preflight.command_fingerprint import command_fingerprint
from .manual_operator_action_observation import (
    ManualOperatorActionObservation,
    _create_manual_operator_action_observation,
)


_IDENTITY = re.compile(r"[A-Za-z0-9][A-Za-z0-9._:-]{0,127}\Z", re.ASCII)
_SHA256 = re.compile(r"[0-9a-fA-F]{64}\Z", re.ASCII)
_OBSERVATION_SOURCE = "OPERATOR_ACTION_OBSERVER"


def collect_manual_operator_action_observation(
    *,
    run_id: object,
    row_id: object,
    harness_operation_id: object,
    action_text: object,
    observation_source: object,
    observer_identity_fingerprint: object,
    operator_confirmed: object,
) -> tuple[ManualOperatorActionObservation | None, str]:
    if not _valid_identity(run_id):
        return None, "MANUAL_OPERATOR_ACTION_RUN_ID_INVALID"
    if not _valid_identity(row_id):
        return None, "MANUAL_OPERATOR_ACTION_ROW_ID_INVALID"
    if not _valid_identity(harness_operation_id):
        return None, "MANUAL_OPERATOR_ACTION_HARNESS_OPERATION_ID_INVALID"
    if not _valid_action(action_text):
        return None, "MANUAL_OPERATOR_ACTION_TEXT_INVALID"
    if observation_source != _OBSERVATION_SOURCE:
        return None, "MANUAL_OPERATOR_ACTION_SOURCE_INVALID"
    if not isinstance(observer_identity_fingerprint, str) or (
        _SHA256.fullmatch(observer_identity_fingerprint) is None
    ):
        return None, "MANUAL_OPERATOR_ACTION_OBSERVER_IDENTITY_INVALID"
    if operator_confirmed is not True:
        return None, "MANUAL_OPERATOR_ACTION_NOT_CONFIRMED"
    return (
        _create_manual_operator_action_observation(
            run_id=run_id,
            row_id=row_id,
            harness_operation_id=harness_operation_id,
            action_text=action_text,
            action_fingerprint=command_fingerprint(action_text),
            observation_source=observation_source,
            observer_identity_fingerprint=observer_identity_fingerprint.lower(),
            operator_confirmed=True,
        ),
        "MANUAL_OPERATOR_ACTION_OBSERVED",
    )


def _valid_identity(value: object) -> bool:
    return isinstance(value, str) and _IDENTITY.fullmatch(value) is not None


def _valid_action(value: object) -> bool:
    return (
        isinstance(value, str)
        and value == value.strip()
        and 0 < len(value) <= 512
        and not any(
            ord(character) < 0x20 or ord(character) == 0x7F for character in value
        )
    )
