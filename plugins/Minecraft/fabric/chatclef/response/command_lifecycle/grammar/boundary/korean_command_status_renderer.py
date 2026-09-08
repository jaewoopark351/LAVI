#20260907_kpopmodder: Own deterministic pending and active STATUS sentences.
from __future__ import annotations

from ..arguments.command_feedback_descriptor_phrase_semantics import (
    get_verb,
    inventory_default,
    is_typed,
)


class KoreanCommandStatusRenderer:
    def __init__(
        self,
        *,
        subject_renderer,
        location_renderer,
        hunger_renderer,
        particle_renderer,
    ) -> None:
        self._subjects = subject_renderer
        self._locations = location_renderer
        self._hunger = hunger_renderer
        self._particle = particle_renderer

    def render(self, descriptor: object, profile: object, state: str) -> str:
        if state == "pending":
            if profile.family == "item_get" and get_verb(descriptor) == "craft":
                subject = self._subjects.render(descriptor, profile)
                return f"{subject} 만드는 작업이 시작됐는지 확인 중이야"
            return f"{profile.command_label} 작업이 시작됐는지 확인 중이야"
        if state != "running":
            return "지금 마인크래프트 작업 상태를 확인하지 못했어"
        family = profile.family
        subject = self._subjects.render(descriptor, profile)
        if family == "item_get":
            action = (
                "만드는 중이야"
                if get_verb(descriptor) == "craft"
                else "구하는 중이야"
            )
            return f"{subject} {action}"
        if family == "item_deposit":
            if (
                descriptor.command_name in {"deposit", "deposit_all"}
                and inventory_default(descriptor)
            ):
                return "아이템을 보관함에 넣는 중이야"
            return f"{subject} 보관하는 중이야"
        if family == "item_equip":
            return f"{subject} 장착하는 중이야"
        if family == "item_give":
            return f"{subject} 건네는 중이야"
        if family == "movement_goto":
            destination = self._particle.attach_directional(
                self._locations.render(descriptor)
            )
            return f"{destination} 가는 중이야"
        if family == "movement_follow":
            return f"{subject} 따라가는 중이야"
        if family in {"food_acquisition", "meat_acquisition"}:
            return f"{self._hunger.render(descriptor, family)} 모으는 중이야"
        if family == "store_home":
            return "아이템을 집에 정리하는 중이야"
        return self._command_level(descriptor, profile)

    def _command_level(self, descriptor: object, profile: object) -> str:
        setting = str(getattr(descriptor, "setting_value", "") or "")
        if is_typed(descriptor) and descriptor.command_name == "gamma" and setting:
            return f"밝기를 {self._particle.attach_directional(setting)} 바꾸는 중이야"
        structure = str(getattr(descriptor, "structure_name", "") or "")
        if is_typed(descriptor) and descriptor.command_name == "locate_structure":
            label = {"stronghold": "요새", "desert_temple": "사막 사원"}.get(
                structure,
                "요청한 구조물",
            )
            return f"{self._particle.attach(label, '을', '를')} 찾는 중이야"
        values = {
            "attack": "대상을 공격하는 중이야",
            "auto_deposit_trust": "자동 보관 대상을 등록하는 중이야",
            "auto_deposit_trusted_list": "자동 보관 위치를 확인하는 중이야",
            "auto_deposit_untrust": "자동 보관 등록을 해제하는 중이야",
            "자동보관등록": "주변 보관함을 등록하는 중이야",
            "chatclef": "ChatClef 상태를 바꾸는 중이야",
            "gamma": "밝기를 바꾸는 중이야",
            "hero": "주변 적을 정리하는 중이야",
            "idle": "가만히 기다리는 중이야",
            "locate_structure": "요청한 구조물을 찾는 중이야",
            "overlay": "오버레이 상태를 바꾸는 중이야",
            "reload_settings": "설정을 다시 불러오는 중이야",
            "resetmemory": "ChatClef의 기억을 초기화하는 중이야",
            "scan": "요청한 대상을 찾는 중이야",
            "gamer": "게임을 공략하는 중이야",
            "stop": "중지 요청을 처리 중이야",
        }
        return values.get(
            descriptor.command_name,
            f"{profile.command_label} 작업을 처리하는 중이야",
        )


__all__ = ("KoreanCommandStatusRenderer",)
