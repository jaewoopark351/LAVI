#20260914_kpopmodder: Render validated FIND observations without free-text TTS or success promotion.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.result.find import FindTerminalPayload
from plugins.Minecraft.fabric.chatclef.transport.find_catalog.find_catalog_snapshot import safe_text
from ...grammar.korean_particle_renderer import KoreanParticleRenderer


class KoreanFindTerminalRenderer:
    @staticmethod
    def label(descriptor):
        value = getattr(descriptor, "spoken_target_label", "")
        return value if safe_text(value, 64) else "요청한 대상"

    def start(self, descriptor):
        label = KoreanParticleRenderer().attach(self.label(descriptor), "을", "를")
        mode = getattr(getattr(descriptor, "find_binding", None), "mode", "report")
        return f"{label} 찾아서 가까이 가볼게" if mode == "approach" else f"{label} 찾아서 위치를 알려줄게"

    def render(self, descriptor, *, status, verified, evidence=None, failure=None, query=None):
        terminal = evidence if verified else (query if status == "completed" else failure)
        subject = KoreanParticleRenderer().attach(self.label(descriptor), "을", "를")
        if type(terminal) is FindTerminalPayload:
            result = terminal.find_result
            if status == "completed" and verified and terminal.find_satisfied:
                if result == "FOUND_AND_REPORTED":
                    fields = terminal.fields
                    return f"{subject} 찾았어. 확인한 위치는 X {fields['x']}, Y {fields['y']}, Z {fields['z']}야"
                if result in {"FOUND_AND_IN_SAFE_RANGE", "ALREADY_IN_SAFE_RANGE"}:
                    return f"{subject} 찾아서 가까이 도착했어"
            if status == "completed" and result == "NOT_OBSERVED_IN_LOADED_SCOPE" and not terminal.find_satisfied:
                return f"불러온 범위에서는 {subject} 확인하지 못했어"
            if status == "failed":
                if result == "UNREACHABLE":
                    if terminal.reason == "post_discovery_approach_unreachable":
                        return "대상을 발견했지만 허용된 이동 방식으로 가까이 갈 수는 없었어"
                    if terminal.reason in {"exploration_route_unavailable", "exploration_no_progress"}:
                        return "탐험을 계속할 수 없어서 대상을 찾지는 못했어"
                    return "허용된 이동 방식으로 가까이 갈 수는 없었어"
                if result == "TIMEOUT":
                    if terminal.reason == "exploration_step_deadline_exhausted":
                        return "탐험 이동의 시간 한도에 도달해서 탐색을 계속하지는 못했어"
                    if terminal.reason == "native_approach_step_deadline_exhausted":
                        return "접근 이동의 시간 한도에 도달해서 가까이 도착하지는 못했어"
                    if terminal.reason == "discovery_deadline_exhausted":
                        return "찾기 시간 한도에 도달해서 탐색을 완료하지는 못했어"
                    if terminal.reason == "approach_deadline_exhausted":
                        return "접근 시간 한도에 도달해서 가까이 도착하지는 못했어"
                    return "찾기 작업의 시간 한도에 도달해서 요청을 완료하지는 못했어"
                return {
                    "OBSERVATION_BOUNDS_EXHAUSTED": "탐색 한도에 도달해서 전부 확인하지는 못했어",
                    "TARGET_LOST": "확인하던 대상을 지금은 확인할 수 없어",
                    "CANDIDATE_NOT_REVALIDATABLE": "확인하던 대상을 지금은 확인할 수 없어",
                    "INVALID_TARGET": "찾을 대상을 확인하지 못했어",
                    "INTERNAL_ERROR": "대상을 찾는 중 오류가 발생했어",
                    "INTERRUPTED": "대상을 찾는 작업이 중단됐어",
                }.get(result, "검색 결과를 확인하지 못했어")
        return {
            "completed": "찾기 작업은 끝났는데, 검색 결과는 확인하지 못했어",
            "failed": "대상을 찾는 작업에 실패했어",
            "rejected": "찾기 명령은 실행하지 못했어",
            "cancelled": "대상을 찾는 작업이 중단됐어",
            "deadline_exceeded": "찾기 작업이 시간 안에 끝나지 않았어",
        }.get(status, "검색 결과를 확인하지 못했어")
