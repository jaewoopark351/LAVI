#20260827_kpopmodder: Prevent generic task completion from becoming false STORE_HOME success.
from __future__ import annotations

import unittest

from plugins.Minecraft.fabric.chatclef.response import ChatClefCommandResponseRenderer
from plugins.Minecraft.fabric.chatclef.input.routing.submission.submission_result_normalizer import (
    MinecraftChatClefSubmissionResultNormalizer,
)
from plugins.Minecraft.fabric.chatclef.input.routing.submission.reconciliation.submission_reconciliation_policy import (
    MinecraftChatClefSubmissionReconciliationPolicy,
)


class StoreHomeResponseContractTests(unittest.TestCase):
    def setUp(self):
        self.renderer = ChatClefCommandResponseRenderer()
        self.translation = {
            "command": "store_home",
            "intent": {"intent_type": "store_home", "slots": {}},
        }

    def test_completed_requires_consistent_typed_store_home_success(self):
        generic = self.renderer.render_submitted(
            self.translation,
            _result("completed", None),
        )
        contradictory = self.renderer.render_submitted(
            self.translation,
            _result(
                "completed",
                _payload("COMPLETED", goal_satisfied=False),
            ),
        )
        conflicting_outer_status = self.renderer.render_submitted(
            self.translation,
            _result("failed", _payload("COMPLETED", stored_items=14)),
        )
        verified = self.renderer.render_submitted(
            self.translation,
            _result("completed", _payload("COMPLETED", stored_items=14)),
        )

        self.assertNotIn("집 보관 완료:", generic)
        self.assertIn("실제 저장 결과를 확인하지 못했어요", generic)
        self.assertNotIn("집 보관 완료:", contradictory)
        self.assertNotIn("집 보관 완료:", conflicting_outer_status)
        self.assertIn(
            "실제 저장 결과를 확인하지 못했어요",
            conflicting_outer_status,
        )
        self.assertEqual(
            "[Minecraft] 집 보관 완료: 아이템 14개를 저장했어요.",
            verified,
        )

    def test_partial_results_report_item_and_stack_units_separately(self):
        text = self.renderer.render_submitted(
            self.translation,
            _result(
                "completed",
                _payload(
                    "PARTIAL_TRUSTED_CAPACITY_EXHAUSTED",
                    stored_items=14,
                    remaining_stacks=3,
                ),
            ),
        )

        self.assertIn("아이템 14개", text)
        self.assertIn("stack 3개", text)
        self.assertIn("빈 공간이 없어요", text)
        self.assertNotIn("집 보관 완료:", text)

    def test_reporting_only_zero_remaining_keeps_partial_meaning(self):
        text = self.renderer.render_submitted(
            self.translation,
            _result(
                "completed",
                _payload(
                    "PARTIAL_TRUSTED_DESTINATIONS_UNAVAILABLE",
                    stored_items=14,
                    remaining_stacks=0,
                ),
            ),
        )
        invalid_zero_stored = self.renderer.render_submitted(
            self.translation,
            _result(
                "completed",
                _payload(
                    "PARTIAL_TRUSTED_DESTINATIONS_UNAVAILABLE",
                    stored_items=0,
                    remaining_stacks=2,
                ),
            ),
        )

        self.assertIn("완료를 확인할 수는 없습니다", text)
        self.assertNotIn("stack 0개가 남았습니다", text)
        self.assertNotIn("집 보관 완료:", text)
        self.assertIn("실제 저장 결과를 확인하지 못했어요", invalid_zero_stored)

    def test_each_failure_result_keeps_a_distinct_user_meaning(self):
        expectations = {
            "NO_USABLE_TRUSTED_DESTINATION": "사용할 수 있는 trusted 상자",
            "NO_TRUSTED_CAPACITY": "빈 공간이 없어",
            "CURSOR_NOT_EMPTY": "cursor에 아이템",
            "MANIFEST_STALE": "인벤토리 변경",
            "CONTEXT_CHANGED": "dimension 변경",
            "TRANSFER_UNCONFIRMED": "전송을 확인하지 못해",
            "INTERRUPTED": "다른 작업이 시작되어",
        }

        for store_home_result, expected in expectations.items():
            with self.subTest(store_home_result=store_home_result):
                text = self.renderer.render_submitted(
                    self.translation,
                    _result("completed", _payload(store_home_result)),
                )
                self.assertIn(expected, text)
                self.assertNotIn("집 보관 완료:", text)

    def test_submission_normalization_preserves_typed_terminal_fields(self):
        payload = _payload("COMPLETED", stored_items=33)
        raw = {
            "request_id": "store-home-terminal-1",
            "ok": True,
            "status": {
                "request_id": "store-home-terminal-1",
                "ok": True,
                "status": "completed",
                "error_code": None,
                "message": "completed",
                "data": dict(payload),
            },
            "error": None,
            "message": "completed",
            "details": dict(payload),
        }

        normalized = MinecraftChatClefSubmissionResultNormalizer().normalize(
            raw,
            expected_request_id="store-home-terminal-1",
        )

        self.assertEqual(payload, normalized["details"])
        self.assertEqual(payload, normalized["status"]["data"])
        self.assertIn(
            "아이템 33개를 저장했어요",
            self.renderer.render_submitted(self.translation, normalized),
        )

    def test_typed_cursor_rejection_survives_outer_unknown_status(self):
        payload = _payload("CURSOR_NOT_EMPTY")
        raw = {
            "request_id": "store-home-cursor-1",
            "ok": False,
            "status": {
                "request_id": "store-home-cursor-1",
                "ok": False,
                "status": "unknown",
                "error_code": None,
                "message": "callback completed without a command-owned task",
                "data": dict(payload),
            },
            "error": None,
            "message": "callback completed without a command-owned task",
            "details": dict(payload),
        }

        normalized = MinecraftChatClefSubmissionResultNormalizer().normalize(
            raw,
            expected_request_id="store-home-cursor-1",
        )

        self.assertEqual("unknown", normalized["status"]["status"])
        self.assertEqual(
            "CURSOR_NOT_EMPTY",
            normalized["details"]["store_home_result"],
        )
        self.assertIn(
            "cursor에 아이템",
            self.renderer.render_submitted(self.translation, normalized),
        )
        self.assertTrue(
            MinecraftChatClefSubmissionReconciliationPolicy().is_matching_terminal(
                normalized,
                "store-home-cursor-1",
            )
        )

    def test_malformed_typed_terminal_fails_closed_in_normalization(self):
        payload = _payload("COMPLETED", stored_items=10)
        payload["goal_satisfied"] = False
        raw = {
            "request_id": "store-home-malformed-1",
            "ok": True,
            "status": {
                "request_id": "store-home-malformed-1",
                "ok": True,
                "status": "completed",
                "error_code": None,
                "message": "completed",
                "data": dict(payload),
            },
            "error": None,
            "message": "completed",
            "details": dict(payload),
        }

        normalized = MinecraftChatClefSubmissionResultNormalizer().normalize(
            raw,
            expected_request_id="store-home-malformed-1",
        )

        self.assertEqual("unknown", normalized["status"]["status"])
        self.assertNotIn("store_home_result", normalized["details"])

    def test_completed_typed_terminal_requires_completed_outer_status(self):
        payload = _payload("COMPLETED", stored_items=10)
        raw = {
            "request_id": "store-home-conflicting-status-1",
            "ok": False,
            "status": {
                "request_id": "store-home-conflicting-status-1",
                "ok": False,
                "status": "failed",
                "error_code": None,
                "message": "failed",
                "data": dict(payload),
            },
            "error": None,
            "message": "failed",
            "details": dict(payload),
        }

        normalized = MinecraftChatClefSubmissionResultNormalizer().normalize(
            raw,
            expected_request_id="store-home-conflicting-status-1",
        )

        self.assertEqual("unknown", normalized["status"]["status"])
        self.assertNotIn("store_home_result", normalized["details"])


def _payload(
    store_home_result: str,
    *,
    stored_items: int = 0,
    remaining_stacks: int = 0,
    goal_satisfied: bool | None = None,
) -> dict[str, object]:
    if goal_satisfied is None:
        goal_satisfied = store_home_result == "COMPLETED"
    return {
        "operation": "store_home",
        "store_home_result": store_home_result,
        "stored_items": stored_items,
        "remaining_stacks": remaining_stacks,
        "reason": "test_reason",
        "goal_satisfied": goal_satisfied,
    }


def _result(status: str, payload: dict[str, object] | None) -> dict[str, object]:
    data = dict(payload or {})
    return {
        "ok": status in {"accepted", "running", "completed"},
        "status": {"status": status, "data": dict(data)},
        "details": dict(data),
    }


if __name__ == "__main__":
    unittest.main()
