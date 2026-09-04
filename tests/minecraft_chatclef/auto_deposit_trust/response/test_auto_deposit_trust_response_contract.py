#20260905_kpopmodder: Keep H5 user wording at transport submission evidence only.
from __future__ import annotations

import unittest

from plugins.Minecraft.fabric.chatclef.response import (
    ChatClefCommandResponseRenderer,
)


class AutoDepositTrustResponseContractTests(unittest.TestCase):
    def test_even_generic_completed_evidence_renders_submission_only(self):
        response = ChatClefCommandResponseRenderer().render_submitted(
            {
                "command": "auto_deposit_trust area 16x16",
                "intent": {"intent_type": "auto_deposit_trust_area"},
            },
            {
                "ok": True,
                "status": {"status": "completed"},
                "details": {"registered_count": 9},
            },
        )

        self.assertEqual(
            "[Minecraft] 주변 16×16 자동 보관 대상 등록 명령을 제출했어요.",
            response,
        )
        self.assertNotIn("완료", response)
        self.assertNotIn("9", response)


if __name__ == "__main__":
    unittest.main()
