#20260725_kpopmodder: Added command alias tables so parser classes stay focused on one action family.
from __future__ import annotations

DIMENSION_ALIASES = {
    "overworld": "overworld",
    "world": "overworld",
    "nether": "nether",
    "the_nether": "nether",
    "end": "end",
    "the_end": "end",
    "오버월드": "overworld",
    "네더": "nether",
    "엔드": "end",
    "엔더": "end",
}

SIMPLE_ACTION_ALIASES = {
    "stop": "stop",
    "cancel": "stop",
    "stop action": "stop",
    "cancel action": "stop",
    "멈춰": "stop",
    "중지": "stop",
    "정지": "stop",
    "취소": "stop",
    "status": "status",
    "상태": "status",
    "health": "health",
    "ping": "health",
    "헬스": "health",
    "상태확인": "health",
    "inventory": "inventory",
    "inv": "inventory",
    "인벤토리": "inventory",
    "current action": "current_action",
    "current_action": "current_action",
    "action status": "current_action",
    "현재 액션": "current_action",
    "현재 행동": "current_action",
}

KNOWN_ACTIONS = {
    "health",
    "ping",
    "status",
    "get_status",
    "inventory",
    "get_inventory",
    "current_action",
    "actions_current",
    "get_current_action",
    "get_item",
    "get-item",
    "getitem",
    "goto",
    "go_to",
    "move_to",
    "travel_to",
    "stop",
    "cancel",
    "reload",
}

ITEM_PREFIXES = (
    "get-item",
    "get_item",
    "getitem",
    "get item",
    "get",
    "collect",
    "fetch",
    "bring",
)

GOTO_PREFIXES = (
    "goto",
    "go to",
    "move to",
    "travel to",
)

ITEM_WORDS = (
    "가져",
    "구해",
    "얻어",
    "캐",
    "수집",
)

GOTO_WORDS = (
    "이동",
    "가줘",
    "가자",
    "으로 가",
    "로 가",
    "까지 가",
)
