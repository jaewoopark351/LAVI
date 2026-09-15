#20260915_kpopmodder: Render only closed name-resolution reasons and bounded registry-ID candidates.
from collections.abc import Mapping
import re


class KoreanNameResolutionMessage:
    @staticmethod
    def failure(reason):
        if reason in {"unknown_item_phrase", "empty_item_phrase", "unknown_registered_item", "unknown_registered_target"}:
            return "마인크래프트 대상 이름을 확인하지 못했어. 정확한 이름으로 다시 요청해 줘."
        if reason == "runtime_catalogue_required":
            return "연결된 마인크래프트 대상 정보를 확인하지 못했어. 게임 연결을 확인하고 다시 요청해 줘."
        if reason == "ambiguous_item_phrase":
            return "마인크래프트 대상을 하나로 정하지 못했어. 재질과 정확한 이름을 함께 알려줘."
        if reason in {"native_command_target_unsupported", "target_not_equippable", "target_not_in_chatclef_catalog", "unsupported_material", "unsupported_equipment_material"}:
            return "요청한 대상은 이 마인크래프트 명령이 지원하지 않아. 다른 대상이나 명령을 선택해 줘."
        return None

    @staticmethod
    def ambiguous(resolution):
        if not isinstance(resolution, Mapping) or resolution.get("status") != "ambiguous":
            return None
        reason = resolution.get("reason_code")
        if reason not in {"ambiguous_registered_name", "ambiguous_native_command_token"}:
            return None
        data = resolution.get("data")
        values = data.get("suggestions") if isinstance(data, Mapping) else None
        if (type(values) not in (list, tuple) or not 1 <= len(values) <= 5
                or any(type(v) is not str or len(v) > 256
                       or re.fullmatch(r"[a-z0-9_.-]+:[a-z0-9_./-]+", v) is None for v in values)
                or len(set(values)) != len(values)):
            return None
        if reason == "ambiguous_native_command_token":
            return ("기존 명령이 서로 다른 대상을 같은 이름으로 처리해서 구분할 수 없어. "
                    "이 명령에는 다른 대상을 선택해 줘. 관련 대상(최대 5개): " + ", ".join(values))
        return ("같은 한국어 이름의 대상이 여러 개야. 원하는 대상의 ID를 이름 대신 넣어 다시 말해 줘. "
                "후보(최대 5개): " + ", ".join(values))
