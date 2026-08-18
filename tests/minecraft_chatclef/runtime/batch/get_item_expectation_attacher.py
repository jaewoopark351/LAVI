#20260819_kpopmodder: Attach immutable GET expectations to validated approvals.
from __future__ import annotations

from .live_get_batch_plan import GET_BATCH_PLAN


def attach_get_item_expectations(
    approved_steps: list[dict[str, str]],
) -> tuple[list[dict[str, object]], str]:
    if len(approved_steps) != len(GET_BATCH_PLAN):
        return [], "approved step count does not match the GET batch plan"
    enriched: list[dict[str, object]] = []
    for index, (approved, expected) in enumerate(
        zip(approved_steps, GET_BATCH_PLAN, strict=True)
    ):
        if approved.get("command") != expected.command:
            return [], f"approved GET command mismatch at index {index}"
        enriched.append(
            {
                **approved,
                "expected_item_id": expected.expected_item_id,
                "expected_item_delta": expected.expected_item_delta,
                "gameplay_test_objective": expected.gameplay_test_objective,
            }
        )
    return enriched, ""
