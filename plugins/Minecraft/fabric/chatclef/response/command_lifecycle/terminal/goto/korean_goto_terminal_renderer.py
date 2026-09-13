#20260913_kpopmodder: Render immutable validated GOTO outcomes into one shared Chat and TTS sentence.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.result.goto.goto_terminal_payload import GotoTerminalPayload

from .korean_goto_failure_reason_catalog import KOREAN_GOTO_FAILURE_REASONS


class KoreanGotoTerminalRenderer:
    def arrival(self, projection: object) -> str | None:
        if (
            type(projection) is not GotoTerminalPayload
            or projection.outcome != "ARRIVED"
            or projection.failure_reason != "NONE"
            or projection.goal_satisfied is not True
            or projection.binding_valid is not True
            or projection.children_quiescent is not True
            or projection.terminal_dimension != projection.binding.world_dimension
        ):
            return None
        return f"{self._destination(projection)} 좌표에 도착했어."

    def failure(self, projection: object) -> str | None:
        if (
            type(projection) is not GotoTerminalPayload
            or projection.outcome != "FAILED"
            or projection.goal_satisfied is not False
        ):
            return None
        reason = KOREAN_GOTO_FAILURE_REASONS.get(projection.failure_reason)
        if reason is None:
            return None
        return f"{self._destination(projection)} 좌표로 이동하지 못했어. {reason}"

    @staticmethod
    def _destination(projection: GotoTerminalPayload) -> str:
        binding = projection.binding
        return f"{binding.target_x}, {binding.target_y}, {binding.target_z}"


__all__ = ("KoreanGotoTerminalRenderer",)
