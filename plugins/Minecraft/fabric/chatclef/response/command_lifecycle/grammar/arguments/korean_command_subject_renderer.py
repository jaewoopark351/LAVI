#20260907_kpopmodder: Render bounded item and player subjects for lifecycle phrases.
from __future__ import annotations

from ..korean_particle_renderer import KoreanParticleRenderer
from ..korean_quantity_renderer import KoreanQuantityRenderer
from .command_feedback_descriptor_phrase_semantics import is_typed


class KoreanCommandSubjectRenderer:
    def __init__(self, *, quantity_renderer=None, particle_renderer=None) -> None:
        self._quantity = quantity_renderer or KoreanQuantityRenderer()
        self._particle = particle_renderer or KoreanParticleRenderer()

    def render(self, descriptor: object, profile: object) -> str:
        typed = is_typed(descriptor)
        family = profile.family
        if family.startswith("item_"):
            item = self._item_subject(descriptor, family, typed)
            if family == "item_give":
                player = (
                    str(getattr(descriptor, "player_name", "") or "").strip()
                    if typed
                    else ""
                )
                return f"{player}에게 {item}" if player else item
            return item
        if family == "movement_follow":
            if typed:
                player = str(getattr(descriptor, "player_name", "") or "").strip()
                if player:
                    return self._particle.attach(player, "을", "를")
            return "요청한 플레이어를"
        return profile.command_label

    def _item_subject(self, descriptor: object, family: str, typed: bool) -> str:
        targets = getattr(descriptor, "targets", ()) if typed else ()
        if type(targets) is tuple and targets:
            rendered = tuple(
                self._quantity.render(
                    getattr(target, "spoken_label", ""),
                    getattr(target, "requested_count", None),
                )
                for target in targets
            )
            return self._join_items(rendered)
        if (
            family == "item_equip"
            and getattr(descriptor, "form_kind", "") == "equipment_material_set"
        ):
            material = str(getattr(descriptor, "operation_target", "") or "")
            return {
                "leather": "가죽 방어구 세트",
                "iron": "철 방어구 세트",
                "gold": "금 방어구 세트",
                "diamond": "다이아 방어구 세트",
                "netherite": "네더라이트 방어구 세트",
            }.get(material, "요청한 방어구 세트")
        label = getattr(descriptor, "spoken_target_label", "") if typed else ""
        return self._quantity.render(
            label or "요청한 아이템",
            getattr(descriptor, "requested_count", None) if typed else None,
        )

    def _join_items(self, items: tuple[str, ...]) -> str:
        if len(items) == 1:
            return items[0]
        prefix = ", ".join(items[:-1])
        return f"{self._particle.attach(prefix, '과', '와')} {items[-1]}"


__all__ = ("KoreanCommandSubjectRenderer",)
