#20260818_kpopmodder: Validate exact command and unique invocation inputs for one batch.
from __future__ import annotations

from collections.abc import Mapping, Sequence

from .batch_environment_snapshot import batch_environment_snapshot


def validate_batch_plan(
    command_environments: object,
) -> tuple[tuple[Mapping[str, object], ...], str]:
    if isinstance(command_environments, (str, bytes)) or not isinstance(
        command_environments,
        Sequence,
    ):
        return (), "batch plan must be a sequence"
    if not command_environments:
        return (), "batch plan must contain at least one command"
    normalized: list[Mapping[str, object]] = []
    invocation_ids: set[str] = set()
    for index, item in enumerate(command_environments):
        if not isinstance(item, Mapping):
            return (), f"batch command {index} must be an object"
        environment = dict(item)
        command = _exact_text(environment.get("command"))
        invocation_id = _exact_text(environment.get("invocation_id"))
        if not command:
            return (), f"batch command {index} is blank"
        if not invocation_id:
            return (), f"batch invocation {index} is blank"
        if invocation_id in invocation_ids:
            return (), f"batch invocation is duplicated: {invocation_id}"
        invocation_ids.add(invocation_id)
        normalized.append(batch_environment_snapshot(environment))
    return tuple(normalized), ""


def _exact_text(value: object) -> str:
    if type(value) is not str or not value or value != value.strip():
        return ""
    return value
