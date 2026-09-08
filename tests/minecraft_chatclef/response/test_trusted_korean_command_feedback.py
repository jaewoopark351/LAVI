#20260905_kpopmodder: Verify trusted-only feedback selection and Feature-B isolation.
from __future__ import annotations

import unittest

from plugins.Minecraft.fabric.chatclef.input import (
    MinecraftChatClefInputRouteDecision,
)
from plugins.Minecraft.fabric.chatclef.response.trusted_korean import (
    TrustedKoreanCommandFeedbackFacade,
)


class TrustedKoreanCommandFeedbackTests(unittest.TestCase):
    def test_feature_a_replaces_only_the_scoped_precheck_and_unknown_wording(self):
        facade = TrustedKoreanCommandFeedbackFacade()

        cases = {
            "minecraft_bridge_disconnected": (
                "[Minecraft] 마인크래프트 연결이 끊겨 있어 명령을 보내지 못했어요."
            ),
            "minecraft_command_busy": (
                "[Minecraft] 다른 마인크래프트 명령을 실행 중이라 "
                "지금은 새 명령을 보낼 수 없어요."
            ),
            "minecraft_submission_outcome_unknown": (
                "[Minecraft] 실행 결과를 확인하지 못했어요. "
                "자동으로 다시 보내지 않아요."
            ),
        }
        for reason, expected in cases.items():
            with self.subTest(reason=reason):
                decision = MinecraftChatClefInputRouteDecision.handled_result(
                    reason=reason,
                    response_text="legacy wording",
                )

                self.assertEqual(expected, facade.render(decision))

    def test_item_rejection_wording_is_owned_by_the_trusted_facade(self):
        decision = MinecraftChatClefInputRouteDecision.handled_result(
            reason="minecraft_item_command_translation_rejected",
            response_text="",
            result={"ok": False, "message": ""},
        )

        self.assertEqual(
            "[Minecraft] 어떤 아이템을 준비할지 이해하지 못했어요.",
            TrustedKoreanCommandFeedbackFacade().render(decision),
        )

    def test_contextual_busy_route_preserves_rendered_or_suppressed_text(self):
        facade = TrustedKoreanCommandFeedbackFacade()

        for response_text in ("active task status", ""):
            with self.subTest(response_text=response_text):
                decision = MinecraftChatClefInputRouteDecision.handled_result(
                    reason="minecraft_command_busy",
                    response_text=response_text,
                    result={"ok": False, "error": "active_command"},
                    route_kind="command_busy_current_work",
                    response_kind="command_status",
                    suppress_response=not response_text,
                )

                self.assertEqual(response_text, facade.render(decision))

    def test_serialized_profile_marker_cannot_select_feature_b_wording(self):
        decision = MinecraftChatClefInputRouteDecision.handled_result(
            reason="minecraft_command_routed",
            response_text="[Minecraft] 다락문 1개 수집 명령을 제출했어요.",
            route_kind="minecraft_command",
            translation=_generic_crafting_translation(),
            result=_accepted_result(),
        )

        self.assertEqual(
            "[Minecraft] 다락문 1개 수집 명령을 제출했어요.",
            TrustedKoreanCommandFeedbackFacade().render(decision),
        )

    def test_typed_feature_b_route_selects_dedicated_submission_wording(self):
        decision = MinecraftChatClefInputRouteDecision.handled_result(
            reason="minecraft_command_routed",
            response_text="[Minecraft] 다락문 1개 수집 명령을 제출했어요.",
            route_kind="generic_crafting_defaults",
            translation=_generic_crafting_translation(),
            result=_accepted_result(),
        )

        self.assertEqual(
            "[Minecraft] 다락문 1개를 준비하도록 명령했어요.",
            TrustedKoreanCommandFeedbackFacade().render(decision),
        )

    def test_unhandled_decision_is_never_reworded(self):
        decision = MinecraftChatClefInputRouteDecision.not_handled(
            "minecraft_bridge_disconnected"
        )

        self.assertEqual("", TrustedKoreanCommandFeedbackFacade().render(decision))


def _generic_crafting_translation() -> dict[str, object]:
    return {
        "status": "validated",
        "executable": True,
        "command": "get trapdoor 1",
        "resolved_target": "trapdoor",
        "intent": {
            "intent_type": "get_item",
            "item_phrase": "다락문",
            "quantity": 1,
        },
        "data": {
            "resolution": {
                "data": {"profile_id": "generic_crafting_defaults_v1"}
            }
        },
    }


def _accepted_result() -> dict[str, object]:
    return {
        "ok": True,
        "status": {"status": "accepted", "ok": True, "data": {}},
        "message": "accepted",
        "details": {},
    }


if __name__ == "__main__":
    unittest.main()
