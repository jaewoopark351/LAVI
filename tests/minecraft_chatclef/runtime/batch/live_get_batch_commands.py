#20260818_kpopmodder: Keep the approved representative GET batch in one allowlist.
from __future__ import annotations


GET_BATCH_PLAN: tuple[dict[str, object], ...] = (
    {
        "command": "조약돌 1개 캐줘",
        "expected_item_id": "minecraft:cobblestone",
        "expected_item_delta": 1,
    },
    {
        "command": "석탄 1개 캐와줘",
        "expected_item_id": "minecraft:coal",
        "expected_item_delta": 1,
    },
    {
        "command": "철 1개 캐줘",
        "expected_item_id": "minecraft:iron_ingot",
        "expected_item_delta": 1,
    },
    {
        "command": "다이아몬드 캐줘",
        "expected_item_id": "minecraft:diamond",
        "expected_item_delta": 1,
    },
)

COMMANDS: tuple[str, ...] = tuple(
    str(step["command"]) for step in GET_BATCH_PLAN
)


def attach_get_item_expectations(
    approved_steps: list[dict[str, str]],
) -> tuple[list[dict[str, object]], str]:
    if len(approved_steps) != len(GET_BATCH_PLAN):
        return [], "approved step count does not match the GET batch plan"
    enriched: list[dict[str, object]] = []
    for index, (approved, expected) in enumerate(
        zip(approved_steps, GET_BATCH_PLAN, strict=True)
    ):
        if approved.get("command") != expected["command"]:
            return [], f"approved GET command mismatch at index {index}"
        enriched.append({**approved, **expected})
    return enriched, ""
