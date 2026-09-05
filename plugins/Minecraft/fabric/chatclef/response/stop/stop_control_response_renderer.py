#20260905_kpopmodder: Render truthful Korean STOP control feedback from typed evidence.
from __future__ import annotations

from types import MappingProxyType


class StopControlResponseRenderer:
    _LOCAL = MappingProxyType(
        {
            "accepted": "[Minecraft] 마크 AI에게 멈춤을 요청했어요.",
            "stop_control_in_flight": "[Minecraft] 이미 마크 AI 중지 요청을 처리 중이에요.",
            "stop_claim_capacity_exhausted": "[Minecraft] 중지 요청을 안전하게 등록할 여유가 없어 실행하지 않았어요.",
            "bridge_disconnected": "[Minecraft] 마인크래프트 연결이 끊겨 있어 중지 요청을 보내지 못했어요.",
            "stop_control_capability_unavailable": "[Minecraft] 연결된 마인크래프트 모드가 채팅 중지를 지원하지 않아요.",
            "control_send_rejected": "[Minecraft] 중지 요청을 전송하지 못해 실행하지 않았어요.",
            "control_send_unknown": "[Minecraft] 중지 요청이 전달됐는지 확인하지 못했어요. 자동으로 다시 보내지 않아요.",
            "unsafe_stop_phrase": "[Minecraft] 안전하게 확인할 수 없는 정지 표현이라 실행하지 않았어요.",
            "invalid_eligibility_proof": "[Minecraft] 중지 요청의 입력 출처를 확인할 수 없어 실행하지 않았어요.",
        }
    )
    _WIRE = MappingProxyType(
        {
            "invalid_control_profile": "[Minecraft] 중지 요청 형식이 올바르지 않아 실행하지 않았어요.",
            "invalid_target_scope": "[Minecraft] 중지 요청 형식이 올바르지 않아 실행하지 않았어요.",
            "invalid_target_fields": "[Minecraft] 중지 요청 형식이 올바르지 않아 실행하지 않았어요.",
            "stop_control_in_flight": "[Minecraft] 이미 마크 AI 중지 요청을 처리 중이에요.",
            "session_mismatch": "[Minecraft] 중지 요청이 현재 마인크래프트 연결과 일치하지 않아 실행하지 않았어요.",
            "server_generation_mismatch": "[Minecraft] 중지 요청이 현재 마인크래프트 연결과 일치하지 않아 실행하지 않았어요.",
            "deadline_exceeded": "[Minecraft] 중지 요청 시간이 지나 실행하지 않았어요.",
        }
    )

    def render_local(self, reason: str) -> str:
        return self._LOCAL.get(
            str(reason or ""),
            "[Minecraft] 중지 요청을 안전하게 처리하지 못했어요.",
        )

    def render_terminal(self, *, status: str, control_outcome: str, reason: str) -> str:
        if status == "completed" and control_outcome == "stopped":
            return "[Minecraft] 마크 AI를 멈췄어요."
        if control_outcome == "unknown" or status == "unknown":
            return "[Minecraft] 중지 결과를 확인하지 못했어요. 자동으로 다시 보내지 않아요."
        return self._WIRE.get(
            str(reason or ""),
            "[Minecraft] 중지 요청을 실행하지 않았어요.",
        )


__all__ = ("StopControlResponseRenderer",)
