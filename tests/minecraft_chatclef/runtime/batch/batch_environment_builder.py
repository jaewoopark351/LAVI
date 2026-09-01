#20260818_kpopmodder: Expand one validated batch approval into exact one-shot environments.
from __future__ import annotations

import json
from collections.abc import Mapping, Sequence


def build_batch_command_environments(
    base_environment: Mapping[str, object],
    approval: Mapping[str, object],
    approved_steps: Sequence[Mapping[str, str]],
) -> list[dict[str, object]]:
    environments: list[dict[str, object]] = []
    for step in approved_steps:
        command = str(step["command"])
        invocation_id = str(step["invocation_id"])
        environment = dict(base_environment)
        environment.update(
            {
                "command": command,
                "invocation_id": invocation_id,
                "transport": str(approval["transport"]),
                "expected_item_id": str(step.get("expected_item_id") or ""),
                "expected_item_delta": step.get("expected_item_delta"),
                "gameplay_test_objective": step.get("gameplay_test_objective"),
                "approval_json": json.dumps(
                    {
                        "command": command,
                        "gradio_url": approval["gradio_url"],
                        "backend": approval["backend"],
                        "instance": approval["instance"],
                        "world": approval["world"],
                        "invocation_id": invocation_id,
                        "transport": approval["transport"],
                        "approval_source": approval["approval_source"],
                        "one_shot": True,
                        "automatic_rerun_disabled": True,
                    },
                    ensure_ascii=False,
                    sort_keys=True,
                ),
            }
        )
        environments.append(environment)
    return environments
