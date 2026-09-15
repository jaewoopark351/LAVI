#20260915_kpopmodder: Render only validated slot observations without claiming quantity acquisition or server acknowledgement.
from plugins.Minecraft.fabric.chatclef.result.equip import EquipEffectPayload


class KoreanEquipTerminalRenderer:
    def __init__(self, *, particle_renderer):
        self._particles = particle_renderer

    def render(self, payload, descriptor):
        if type(payload) is not EquipEffectPayload:
            return None
        label = getattr(descriptor, "spoken_target_label", "")
        single = len(payload.targets) == 1 and payload.targets[0][1] == 1
        subject = label if single and label else "요청한 장비"
        if payload.outcome == "already_satisfied":
            return f"{self._particles.attach(subject, '은', '는')} 이미 착용하고 있어."
        if payload.outcome == "satisfied":
            return f"{subject} 착용을 확인했어."
        if payload.outcome == "partial":
            return (f"장착 작업은 끝났지만, 요청한 장비 {len(payload.targets)}개 대상 중 "
                    f"{payload.satisfied_count}개 대상만 착용 상태가 확인됐어.")
        if payload.outcome == "not_satisfied":
            return "장착 작업은 끝났지만, 요청한 장비가 장비 슬롯에 착용돼 있지 않아."
        return None
