#20260819_kpopmodder: Verify ChatClef replies do not overclaim gameplay effects.
from __future__ import annotations

import unittest

from plugins.Minecraft.fabric.chatclef.response import ChatClefCommandResponseRenderer


class ChatClefCommandResponseRendererTests(unittest.TestCase):
    def test_accepted_get_mentions_submission_only(self):
        response = ChatClefCommandResponseRenderer().render_submitted(
            _translation("iron_chestplate", "철갑바"),
            _result(status="accepted", ok=True),
        )

        self.assertEqual("[Minecraft] 철 흉갑 1개 수집 명령을 제출했어요.", response)
        self.assertNotIn("캤", response)
        self.assertNotIn("얻었", response)

    def test_completed_without_effect_keeps_gameplay_verification_separate(self):
        response = ChatClefCommandResponseRenderer().render_submitted(
            _translation("torch", "횃불"),
            _result(status="completed", ok=True),
        )

        self.assertIn("완료 응답을 받았어요", response)
        self.assertIn("아이템 증가는 별도로 확인해야 해요", response)

    def test_effect_verified_can_report_checked_result(self):
        response = ChatClefCommandResponseRenderer().render_submitted(
            _translation("emerald", "에메랄드"),
            _result(
                status="completed",
                ok=True,
                details={"expected_gameplay_effect_verified": True},
            ),
        )

        self.assertEqual("[Minecraft] 에메랄드 1개 수집 결과를 확인했어요.", response)

    def test_precheck_replies_are_korean_and_state_specific(self):
        renderer = ChatClefCommandResponseRenderer()

        self.assertIn(
            "연결이 끊겨",
            renderer.render_precheck_rejection(
                "minecraft_bridge_disconnected",
                "not connected",
            ),
        )
        self.assertIn(
            "다른 마인크래프트 작업",
            renderer.render_precheck_rejection(
                "minecraft_command_busy",
                "already active",
            ),
        )


def _translation(target: str, item_phrase: str) -> dict[str, object]:
    return {
        "status": "validated",
        "executable": True,
        "command": f"get {target} 1",
        "resolved_target": target,
        "intent": {
            "intent_type": "get_item",
            "item_phrase": item_phrase,
            "quantity": 1,
        },
    }


def _result(
    *,
    status: str,
    ok: bool,
    details: dict[str, object] | None = None,
) -> dict[str, object]:
    return {
        "ok": ok,
        "status": {"status": status, "ok": ok, "data": dict(details or {})},
        "message": status,
        "details": dict(details or {}),
    }


if __name__ == "__main__":
    unittest.main()
