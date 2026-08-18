#20260819_kpopmodder: Own the immutable representative GET acquisition batch plan.
from __future__ import annotations

from .gameplay_test_objective import GET_ACQUISITION_DELTA_OBJECTIVE
from .live_get_batch_step import LiveGetBatchStep


GET_BATCH_PLAN: tuple[LiveGetBatchStep, ...] = (
    LiveGetBatchStep(
        command="조약돌 1개 캐줘",
        expected_item_id="minecraft:cobblestone",
        expected_item_delta=1,
        gameplay_test_objective=GET_ACQUISITION_DELTA_OBJECTIVE,
    ),
    LiveGetBatchStep(
        command="석탄 1개 캐와줘",
        expected_item_id="minecraft:coal",
        expected_item_delta=1,
        gameplay_test_objective=GET_ACQUISITION_DELTA_OBJECTIVE,
    ),
    LiveGetBatchStep(
        command="철 1개 캐줘",
        expected_item_id="minecraft:iron_ingot",
        expected_item_delta=1,
        gameplay_test_objective=GET_ACQUISITION_DELTA_OBJECTIVE,
    ),
    LiveGetBatchStep(
        command="다이아몬드 캐줘",
        expected_item_id="minecraft:diamond",
        expected_item_delta=1,
        gameplay_test_objective=GET_ACQUISITION_DELTA_OBJECTIVE,
    ),
)

COMMANDS: tuple[str, ...] = tuple(step.command for step in GET_BATCH_PLAN)
