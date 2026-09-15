#20260915_kpopmodder: Present typed observed values without selecting command success.
from plugins.Minecraft.fabric.chatclef.result.instant import InstantCommandPayload
from .list_output.korean_trusted_destination_list_renderer import KoreanTrustedDestinationListRenderer


class KoreanInstantCommandTerminalRenderer:
    def render(self, projection):
        if type(projection) is not InstantCommandPayload:
            return None
        name, reason, values = projection.command_name, projection.reason, projection.values
        if projection.outcome == "unknown":
            if reason == "RELOAD_RETURNED":
                return "설정 다시 읽기 요청은 처리됐어. 각 설정 파일이 모두 적용됐는지는 확인하지 못했어."
            return "명령 처리는 끝났지만 실제 결과를 확인하지 못했어."
        if projection.outcome == "failed":
            return self._failure(reason)
        if name in {"chatclef", "overlay"}:
            label = "챗클레프" if name == "chatclef" else "오버레이"
            return f"{label}를 {'켰어' if values['enabled'] else '껐어'}."
        if name == "gamma":
            return f"밝기가 {values['value']:g}로 설정됐어."
        if name == "resetmemory":
            return "챗클레프 대화 기억을 초기화했어."
        if name == "scan":
            return f"요청한 블록을 {values['x']}, {values['y']}, {values['z']} 좌표에서 찾았어."
        if name == "auto_deposit_trusted_list":
            return self._destinations(values)
        if "registered" in values:
            suffix = "" if values["coverage_complete"] else " 일부 영역은 확인하지 못했어."
            return (f"자동 보관 장소 {values['registered']}곳을 등록하고, {values['reenabled']}곳을 다시 활성화했어. "
                    f"이미 등록된 곳은 {values['already_registered']}곳이야." + suffix)
        if reason == "REMOVED":
            return "자동 보관 장소 등록을 해제했어."
        if reason == "ALREADY_REGISTERED":
            return "이미 등록된 자동 보관 장소야."
        return "자동 보관 장소를 등록했어." if reason == "REGISTERED" else "자동 보관 장소 등록 정보를 갱신했어."

    @staticmethod
    def _failure(reason):
        return {
            "VALUE_NOT_APPLIED": "요청한 밝기가 실제 설정값에 적용되지 않았어.",
            "SETTING_APPLIED": "요청한 켜기·끄기 상태가 적용되지 않았어.",
            "MEMORY_CLEARED": "대화 기억이 모두 초기화되지는 않았어.",
            "BUTLER_USER_UNAVAILABLE": "기본 상대가 지정되지 않았어. 플레이어 이름을 함께 말해 줘.",
            "PLAYER_NOT_LOADED": "상대 플레이어가 현재 불러온 범위에 없어.",
            "ITEM_UNAVAILABLE": "기존 전달 명령이 요청한 아이템을 사용할 수 없다고 응답했어.",
            "NOT_FOUND": "기존 명령의 검색 범위에서 요청한 대상을 찾지 못했어.",
            "INVALID_BLOCK": "기존 블록 검색 명령이 지원하는 블록이 아니야.",
            "TARGET_UNAVAILABLE": "등록하거나 해제할 보관함을 확인하지 못했어.",
            "AMBIGUOUS_ID": "보관 장소 ID가 여러 곳과 일치해. 정확한 ID를 말해 줘.",
            "PERSISTENCE_FAILED": "자동 보관 장소 정보를 저장하지 못했어.",
            "REGISTRY_READ_FAILED": "자동 보관 장소 등록 정보를 읽지 못했어.",
            "EXTERNAL_MODIFICATION_CONFLICT": "등록 정보가 다른 곳에서 바뀌어 적용하지 못했어.",
            "PREEXISTING_DOUBLE_CHEST_DUPLICATE": "기존 이중 상자 등록이 겹쳐 적용하지 못했어.",
            "AMBIGUOUS_DOUBLE_CHEST": "이중 상자의 연결 상태를 확정할 수 없어 등록하지 않았어.",
            "INVALID_ANCHOR": "자동 보관 등록 범위의 기준 위치를 확인하지 못했어.",
            "SCAN_COVERAGE_INCOMPLETE": "등록 범위를 모두 확인하지 못해서 일괄 등록하지 않았어.",
        }.get(reason, "기존 명령이 요청을 처리하지 못했다고 응답했어.")

    @staticmethod
    def _destinations(values):
        return KoreanTrustedDestinationListRenderer.render(values)
