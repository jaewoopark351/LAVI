#20260914_kpopmodder: Render only verified FIND projections; scope absence is not worldwide absence.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.result.find import FindTerminalPayload


class KoreanFindTerminalRenderer:
    def render(self, projection: object) -> str | None:
        if type(projection) is not FindTerminalPayload:
            return None
        target = projection.label or projection.request.query
        code = projection.code
        if code in FindTerminalPayload.SUCCESS:
            if len(projection.position) != 3:
                return None
            location = ", ".join(str(n) for n in projection.position)
            if code == "FOUND":
                return f"{target} 위치를 확인했어. 좌표 {location}. 이동하지 않았어."
            return f"{target} 근처에 도착했어. 대상 좌표 {location}."
        if code == "NOT_FOUND":
            scope = "주변 각 축" if projection.kind == "block" else "주변 반경"
            return f"현재 로드된 {scope} {projection.radius}블록 범위에서 대상 ‘{target}’을 찾지 못했어."
        if code == "UNKNOWN_TARGET":
            return "대상 이름을 해석하지 못했어. 종류와 정확한 이름 또는 레지스트리 ID를 지정해 줘."
        if code == "AMBIGUOUS_TARGET":
            return "같은 이름의 대상이 여러 종류야. 종류와 ID를 지정해 줘. 후보: " + ", ".join(projection.suggestions)
        reasons = {
            "TARGET_LOST": "찾았던 대상이 사라지거나 탐색 범위를 벗어났어. 도착으로 처리하지 않았어.",
            "NO_APPROACH": "대상은 찾았지만 가까이 설 수 있는 안전한 위치를 찾지 못했어.",
            "APPROACH_TIMEOUT": "대상은 찾았지만 제한 시간 안에 접근하지 못했어.",
            "SEARCH_LIMIT": "탐색 제한에 도달했어. 확인하지 못한 범위까지 없다고 판단하지 않았어.",
            "SCOPE_CHANGED": "월드나 차원이 바뀌어서 찾기를 종료했어.",
            "PLAYER_UNAVAILABLE": "플레이어 상태를 확인할 수 없어서 찾기를 종료했어.",
            "INTERNAL_ERROR": "찾기 처리 중 오류가 생겨서 종료했어. 로그를 확인해 줘.",
        }
        return reasons.get(code)
