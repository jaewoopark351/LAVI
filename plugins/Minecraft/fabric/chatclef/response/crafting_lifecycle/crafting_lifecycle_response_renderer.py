#20260907_kpopmodder: Render exact natural Korean crafting lifecycle responses.
from __future__ import annotations


class CraftingLifecycleResponseRenderer:
    def render_start(self) -> str:
        return "다이아 곡괭이 만들어 줄게"

    def render_status(self, state: str) -> str:
        if state == "running":
            return "다이아 곡괭이 만드는 중이야"
        if state == "pending":
            return "다이아 곡괭이 만드는 작업이 시작됐는지 확인 중이야"
        if state == "idle":
            return "지금 만드는 중인 아이템은 없어"
        return "지금 제작 상태를 확인하지 못했어"

    def render_terminal(
        self,
        *,
        status: str,
        verified: bool,
        dispatch_started: bool = False,
    ) -> str:
        if status == "completed":
            if verified:
                return "다이아 곡괭이 다 만들었어"
            return (
                "다이아 곡괭이 만드는 작업은 끝났는데,\n"
                "다 만들어졌는지는 확인하지 못했어"
            )
        if status == "failed":
            if dispatch_started:
                return "다이아 곡괭이 만들다가 실패했어"
            return "다이아 곡괭이 만들지 못했어"
        if status == "rejected":
            return "다이아 곡괭이 만들지 못했어"
        if status == "cancelled":
            return "다이아 곡괭이 만들기가 중단됐어"
        if status == "deadline_exceeded":
            return "다이아 곡괭이 만들기가 시간 안에 끝나지 않았어"
        return "다이아 곡괭이 만들기 결과는 확인하지 못했어"


__all__ = ("CraftingLifecycleResponseRenderer",)
