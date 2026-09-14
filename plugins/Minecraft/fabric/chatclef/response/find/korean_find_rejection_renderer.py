#20260914_kpopmodder: Render FIND refusals from closed reason codes rather than arbitrary text.
class KoreanFindRejectionRenderer:
    _MESSAGES = {
        "find_catalog_unavailable": "현재 대상 목록을 확인할 수 없어. 연결이 준비된 뒤 다시 요청해 줘.",
        "find_target_unresolved": "대상 이름을 확인하지 못했어. 종류와 정확한 이름을 알려줘.",
        "find_target_ambiguous": "블록, 몹, 떨어진 아이템 중 무엇을 찾을지 알려줘.",
        "item_find_approach_unsupported": "떨어진 아이템은 위치만 알려줄 수 있어. 접근이나 줍기는 지원하지 않아.",
        "unsupported_find_query": "FIND는 주변 몹·블록·떨어진 아이템의 위치를 찾아. 소지품·상자 내용물·획득 방법은 별도 조회야.",
    }

    @classmethod
    def render(cls, reason):
        return cls._MESSAGES.get(reason, "찾을 대상과 요청을 다시 알려줘.")
