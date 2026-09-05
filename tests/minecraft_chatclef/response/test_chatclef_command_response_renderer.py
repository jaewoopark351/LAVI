#20260819_kpopmodder: Verify ChatClef replies do not overclaim gameplay effects.
#20260905_kpopmodder: Lock legacy shared wording and prevent scoped-profile leaks.
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

    def test_item_action_labels_match_command_without_claiming_effect(self):
        renderer = ChatClefCommandResponseRenderer()

        cases = [
            (
                _translation(
                    "iron_chestplate",
                    "철 흉갑",
                    intent_type="equip_item",
                    command="equip iron_chestplate",
                ),
                "철 흉갑 장착 명령을 제출했어요",
            ),
            (
                _translation(
                    "diamond",
                    "다이아몬드",
                    intent_type="deposit_item",
                    command="deposit diamond 2",
                    quantity=2,
                ),
                "다이아몬드 2개 보관 명령을 제출했어요",
            ),
            (
                _translation(
                    "torch",
                    "횃불",
                    intent_type="give_item",
                    command="give Steve torch 1",
                    player_name="Steve",
                ),
                "Steve에게 횃불 1개 전달 명령을 제출했어요",
            ),
        ]
        for translation, expected_text in cases:
            with self.subTest(command=translation["command"]):
                response = renderer.render_submitted(
                    translation,
                    _result(status="accepted", ok=True),
                )

                self.assertIn(expected_text, response)
                self.assertNotIn("성공", response)

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

        self.assertEqual(
            "[Minecraft] 마인크래프트 연결이 끊겨 있어요.",
            renderer.render_precheck_rejection(
                "minecraft_bridge_disconnected",
                "not connected",
            ),
        )
        self.assertEqual(
            "[Minecraft] 지금 다른 마인크래프트 작업을 하고 있어요.",
            renderer.render_precheck_rejection(
                "minecraft_command_busy",
                "already active",
            ),
        )

    def test_unknown_result_preserves_legacy_message_and_no_message_wording(self):
        renderer = ChatClefCommandResponseRenderer()
        translation = _translation("diamond", "다이아몬드")

        with_message = renderer.render_submitted(
            translation,
            {
                "ok": False,
                "status": {"status": "unknown", "ok": False, "data": {}},
                "message": "connection closed",
                "details": {},
            },
        )
        without_message = renderer.render_submitted(
            translation,
            {
                "ok": False,
                "status": {"status": "unknown", "ok": False, "data": {}},
                "message": "",
                "details": {},
            },
        )

        self.assertEqual(
            "[Minecraft] 명령 결과를 확정하지 못했어요: connection closed",
            with_message,
        )
        self.assertEqual(
            "[Minecraft] 명령 결과를 확정하지 못했어요. 자동으로 다시 보내지 않아요.",
            without_message,
        )

    def test_serialized_generic_profile_marker_does_not_change_shared_wording(self):
        translation = _translation("trapdoor", "다락문")
        translation["data"] = {
            "resolution": {
                "data": {"profile_id": "generic_crafting_defaults_v1"}
            }
        }

        response = ChatClefCommandResponseRenderer().render_submitted(
            translation,
            _result(status="accepted", ok=True),
        )

        self.assertEqual(
            "[Minecraft] 다락문 1개 수집 명령을 제출했어요.",
            response,
        )
        self.assertNotIn("준비하도록", response)


def _translation(
    target: str,
    item_phrase: str,
    *,
    intent_type: str = "get_item",
    command: str | None = None,
    quantity: int = 1,
    player_name: str = "",
) -> dict[str, object]:
    return {
        "status": "validated",
        "executable": True,
        "command": command or f"get {target} {quantity}",
        "resolved_target": target,
        "intent": {
            "intent_type": intent_type,
            "item_phrase": item_phrase,
            "quantity": quantity,
            "player_name": player_name,
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
