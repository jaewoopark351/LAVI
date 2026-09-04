#20260905_kpopmodder: Lock H5 registry readiness independently from its public rollout switch.
from __future__ import annotations

import unittest

from input_core.input_event.contracts import LaviInputEvent
from plugins.Minecraft.fabric.chatclef.command_registry import (
    KoreanChatClefCommandRegistry,
)
from plugins.Minecraft.fabric.chatclef.command_registry.admission import (
    AutoDepositTrustClaimedSubmissionAuthorizer,
    AutoDepositTrustCommandAdmission,
    KoreanCommandSubmissionAdmission,
)
from plugins.Minecraft.fabric.chatclef.input.auto_deposit_trust.delivery import (
    AutoDepositTrustInputEventClaimRegistry,
)


class AutoDepositTrustRegistryAdmissionContractTests(unittest.TestCase):
    def test_public_rollout_and_rollback_do_not_change_parse_or_admission_readiness(self):
        active = KoreanChatClefCommandRegistry()
        disabled_before = _PublicDisabledKoreanRegistry()
        disabled_after = _PublicDisabledKoreanRegistry()

        for registry, public in (
            (disabled_before, False),
            (active, True),
            (disabled_after, False),
        ):
            with self.subTest(public=public, registry_id=id(registry)):
                axes = registry.spec("auto_deposit_trust").readiness_axes
                self.assertTrue(axes.source_registered)
                self.assertTrue(axes.korean_parse_compile_ready)
                self.assertTrue(axes.python_admission_ready)
                self.assertTrue(axes.bridge_lifecycle_ready)
                self.assertFalse(axes.gameplay_effect_verifiable)
                self.assertEqual(public, axes.public_korean_enabled)

    def test_public_false_rejects_a_valid_live_claim_without_submission_authority(self):
        claim_registry = AutoDepositTrustInputEventClaimRegistry()
        claim_owner = object()
        claim_registry._bind_claim_owner(claim_owner)
        event = LaviInputEvent(
            text="자동보관등록 영역 16x16",
            source="lavi_chat_ui",
            provider_id="lavi_chat_ui",
            event_kind="chat_submit",
            final=True,
            event_id="f" * 32,
            fallback_payload="자동보관등록 영역 16x16",
        )
        receipt, reason = claim_registry.claim(event, claim_owner=claim_owner)
        self.assertEqual("", reason)
        self.assertIsNotNone(receipt)

        h5_admission = AutoDepositTrustCommandAdmission(
            authorizer=AutoDepositTrustClaimedSubmissionAuthorizer(
                claim_registry
            )
        )
        admission = KoreanCommandSubmissionAdmission(
            auto_deposit_trust_admission=h5_admission
        )
        decision = admission.inspect(
            "auto_deposit_trust",
            "lavi_chat_ui",
            _PublicDisabledKoreanRegistry(),
            receipt,
        )

        self.assertFalse(decision.allowed)
        self.assertEqual("korean_command_not_public", decision.reason_code)
        self.assertEqual("ISSUED", claim_registry.state(receipt))
        admission.abandon_if_issued(receipt)
        self.assertEqual("SPENT", claim_registry.state(receipt))


class _PublicDisabledKoreanRegistry(KoreanChatClefCommandRegistry):
    _PUBLIC_KOREAN_COMMANDS = (
        KoreanChatClefCommandRegistry._PUBLIC_KOREAN_COMMANDS
        - frozenset({"auto_deposit_trust"})
    )


if __name__ == "__main__":
    unittest.main()
