#20260905_kpopmodder: Prove that only one live receipt from the owning registry authorizes H5 submission.
from __future__ import annotations

import unittest

from input_core.input_event.contracts import LaviInputEvent
from plugins.Minecraft.fabric.chatclef.command_registry.admission import (
    AutoDepositTrustClaimedSubmissionAuthorizer,
)
from plugins.Minecraft.fabric.chatclef.input.auto_deposit_trust.delivery import (
    AutoDepositTrustInputEventClaimRegistry,
)
from plugins.Minecraft.fabric.chatclef.input.auto_deposit_trust.delivery.contracts import (
    AutoDepositTrustInputClaimReceipt,
)


class AutoDepositTrustClaimedSubmissionAuthorizerContractTests(unittest.TestCase):
    def setUp(self):
        self.owner = object()
        self.registry = AutoDepositTrustInputEventClaimRegistry()
        self.registry._bind_claim_owner(self.owner)
        self.authorizer = AutoDepositTrustClaimedSubmissionAuthorizer(
            self.registry
        )

    def test_missing_foreign_forged_and_spent_receipts_are_rejected(self):
        live, reason = self.registry.claim(
            _event("1" * 32),
            claim_owner=self.owner,
        )
        self.assertEqual("", reason)

        foreign_registry = AutoDepositTrustInputEventClaimRegistry()
        foreign_owner = object()
        foreign_registry._bind_claim_owner(foreign_owner)
        foreign, _ = foreign_registry.claim(
            _event("2" * 32),
            claim_owner=foreign_owner,
        )
        forged = AutoDepositTrustInputClaimReceipt(
            event_id="1" * 32,
            source="lavi_chat_ui",
            provider_id="lavi_chat_ui",
            event_kind="chat_submit",
            registry_token=object(),
            nonce=object(),
        )

        for label, receipt, expected_reason in (
            (
                "missing",
                None,
                "auto_deposit_trust_input_claim_required",
            ),
            (
                "foreign",
                foreign,
                "auto_deposit_trust_input_claim_invalid",
            ),
            (
                "forged",
                forged,
                "auto_deposit_trust_input_claim_invalid",
            ),
        ):
            with self.subTest(label=label):
                decision = self.authorizer.inspect(
                    command_name="auto_deposit_trust",
                    source="lavi_chat_ui",
                    route_claim=receipt,
                )
                self.assertFalse(decision.allowed)
                self.assertEqual(expected_reason, decision.reason_code)

        self.registry.abandon_if_issued(live)
        spent = self.authorizer.inspect(
            command_name="auto_deposit_trust",
            source="lavi_chat_ui",
            route_claim=live,
        )
        self.assertFalse(spent.allowed)
        self.assertEqual(
            "auto_deposit_trust_input_claim_invalid",
            spent.reason_code,
        )

    def test_one_live_owning_receipt_is_admitted_without_consumption(self):
        receipt, _ = self.registry.claim(
            _event("3" * 32),
            claim_owner=self.owner,
        )

        decision = self.authorizer.inspect(
            command_name="auto_deposit_trust",
            source="lavi_chat_ui",
            route_claim=receipt,
        )

        self.assertTrue(decision.allowed)
        self.assertEqual("ISSUED", self.registry.state(receipt))


def _event(event_id: str) -> LaviInputEvent:
    text = "자동보관등록 영역 16x16"
    return LaviInputEvent(
        text=text,
        source="lavi_chat_ui",
        provider_id="lavi_chat_ui",
        event_kind="chat_submit",
        final=True,
        event_id=event_id,
        fallback_payload=text,
    )


if __name__ == "__main__":
    unittest.main()
