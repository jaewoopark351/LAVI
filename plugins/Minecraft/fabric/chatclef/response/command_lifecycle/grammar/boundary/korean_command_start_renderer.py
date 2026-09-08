#20260907_kpopmodder: Own deterministic START sentences for lifecycle feedback.
from __future__ import annotations

from ..arguments.command_feedback_descriptor_phrase_semantics import (
    get_verb,
    inventory_default,
    is_typed,
)


class KoreanCommandStartRenderer:
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

    def render(self, descriptor: object, profile: object) -> str:
        family = profile.family
        subject = self._subjects.render(descriptor, profile)
        if family == "item_get":
            action = {"craft": "만들어 줄게", "mining": "캐 올게"}.get(
                get_verb(descriptor),
                "구해 올게",
            )
            return f"{subject} {action}"
        if family == "item_deposit":
            if (
                descriptor.command_name in {"deposit", "deposit_all"}
                and inventory_default(descriptor)
            ):
                return "보관할 수 있는 아이템을 전부 넣을게"
            return f"{subject} 보관할게"
        if family == "item_equip":
            return f"{subject} 장착할게"
        if family == "item_give":
            return f"{subject} 건네줄게"
        if family == "movement_goto":
            destination = self._particle.attach_directional(
                self._locations.render(descriptor)
            )
            return f"{destination} 갈게"
        if family == "movement_follow":
            return f"{subject} 따라갈게"
        if family in {"food_acquisition", "meat_acquisition"}:
            return f"{self._hunger.render(descriptor, family)} 모아 올게"
        if family == "store_home":
            return "아이템을 집에 정리할게"
        return self._command_level(descriptor, profile)

    def _command_level(self, descriptor: object, profile: object) -> str:
        setting = str(getattr(descriptor, "setting_value", "") or "")
        if is_typed(descriptor) and descriptor.command_name == "gamma" and setting:
            return f"밝기를 {self._particle.attach_directional(setting)} 바꿀게"
        if (
            is_typed(descriptor)
            and descriptor.command_name in {"chatclef", "overlay"}
            and setting in {"on", "off"}
        ):
            subject = (
                "ChatClef" if descriptor.command_name == "chatclef" else "오버레이"
            )
            return f"{subject}를 {'켤게' if setting == 'on' else '끌게'}"
        structure = str(getattr(descriptor, "structure_name", "") or "")
        if is_typed(descriptor) and descriptor.command_name == "locate_structure":
            label = {"stronghold": "요새", "desert_temple": "사막 사원"}.get(
                structure,
                "요청한 구조물",
            )
            return f"{self._particle.attach(label, '을', '를')} 찾아볼게"
        values = {
            "attack": "대상을 공격할게",
            "auto_deposit_trust": "자동 보관 대상을 등록할게",
            "auto_deposit_trusted_list": "등록된 자동 보관 위치를 확인할게",
            "auto_deposit_untrust": "자동 보관 대상 등록을 해제할게",
            "자동보관등록": "주변 보관함을 자동 보관 대상으로 등록할게",
            "chatclef": "ChatClef 상태를 바꿀게",
            "gamma": "밝기를 바꿀게",
            "hero": "주변 적을 계속 정리할게",
            "idle": "가만히 기다릴게",
            "locate_structure": "요청한 구조물을 찾아볼게",
            "overlay": "오버레이 상태를 바꿀게",
            "reload_settings": "설정을 다시 불러올게",
            "resetmemory": "ChatClef의 기억을 초기화할게",
            "scan": "요청한 대상을 찾아볼게",
            "gamer": "게임 공략을 시작할게",
            "stop": "멈출게",
        }
        if descriptor.command_name == "auto_deposit_trust" and (
            is_typed(descriptor)
            or getattr(descriptor, "form_kind", "")
            in {"area_16x16", "radius_16x16"}
        ):
            return "주변 16×16 범위에 있는 보관함을 자동 보관 대상으로 등록할게"
        return values.get(
            descriptor.command_name,
            f"{profile.command_label} 작업을 시작할게",
        )


__all__ = ("KoreanCommandStartRenderer",)
