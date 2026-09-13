#20260913_kpopmodder: Translate only existing typed GOTO failure codes, never remote text.
from types import MappingProxyType


KOREAN_GOTO_FAILURE_REASONS = MappingProxyType({
    "WORLD_CHANGED": "월드나 플레이어 상태가 바뀌었어.",
    "ROOT_REPLACED": "다른 작업으로 교체됐어.",
    "NOT_SURVIVAL": "현재 플레이어 상태에서는 이 이동을 진행할 수 없어.",
    "SETTINGS_CHANGED": "이동에 필요한 설정이 바뀌었어.",
    "PLACING_DISABLED": "블록 설치가 허용되어 있지 않아.",
    "BREAKING_DISABLED": "블록 채굴이 허용되어 있지 않아.",
    "INTERACTION_PAUSED": "블록 상호작용이 일시 중지되어 있어.",
    "INVENTORY_UNAVAILABLE": "가지고 있는 이동 재료를 확인하지 못했어.",
    "UNSAFE_ACCEPTED_STACK": "이동 재료를 안전하게 사용할 수 있는지 확인하지 못했어.",
    "INVENTORY_FULL": "인벤토리에 이동 재료를 넣을 공간이 없어.",
    "NO_SAFE_SOURCE": "주변에서 안전하게 채굴할 재료를 찾지 못했어.",
    "CANDIDATE_LIMIT": "정해진 탐색 범위에서 필요한 재료를 확보하지 못했어.",
    "ACQUISITION_TIMEOUT": "제한 시간 안에 이동 재료를 확보하지 못했어.",
    "NO_PROGRESS": "재료 확보가 계속 진행되지 않았어.",
    "SOURCE_TIMEOUT": "제한 시간 안에 재료 채굴과 수집을 마치지 못했어.",
    "SOURCE_INVALIDATED": "채굴하려던 블록의 상태가 바뀌었어.",
    "DROP_NOT_OBSERVED": "채굴한 재료가 떨어졌는지 확인하지 못했어.",
    "DROP_LOST": "떨어진 재료를 더 이상 확인할 수 없어.",
    "PICKUP_UNCONFIRMED": "재료가 인벤토리에 들어왔는지 확인하지 못했어.",
    "TOOL_NOT_READY": "재료를 채굴할 도구가 준비되지 않았어.",
    "OUT_OF_BOUNDS": "재료 확보 작업이 허용된 범위를 벗어났어.",
    "CLEANUP_TIMEOUT": "이동 작업 정리가 제한 시간 안에 끝나지 않았어.",
    "HANDOFF_SHORTAGE": "이동을 다시 시작할 때 필요한 블록이 부족했어.",
    "ARRIVAL_LOST": "마지막 확인에서 목적지에 안정적으로 서 있는지 확인하지 못했어.",
    "AIR_COLUMN_CHANGED": "이동을 준비하던 공간의 상태가 바뀌었어.",
    "FALLBACK_UNAVAILABLE": "추가 이동 준비를 진행할 조건이 충족되지 않았어.",
    "INTERNAL_ERROR": "이동 처리 중 내부 오류가 발생했어.",
})

__all__ = ("KOREAN_GOTO_FAILURE_REASONS",)
