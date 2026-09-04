#20260905_kpopmodder: Verify bounded, event-bound, one-shot H5 claim ownership.
from __future__ import annotations

import json
import pickle
import threading
import unittest

from input_core.input_event.contracts import LaviInputEvent
from plugins.Minecraft.common.dto.command_request_dto import CommandRequestDTO
from plugins.Minecraft.fabric.chatclef.input.auto_deposit_trust.delivery import (
    AutoDepositTrustInputEventClaimRegistry,
)


class AutoDepositTrustInputEventClaimRegistryContractTests(unittest.TestCase):
    def test_duplicate_and_concurrent_claims_have_one_winner(self):
        registry = AutoDepositTrustInputEventClaimRegistry()
        registry._bind_claim_owner(self)
        event = _event("0" * 32)
        outcomes = []
        barrier = threading.Barrier(3)

        def claim_once():
            barrier.wait()
            outcomes.append(registry.claim(event, claim_owner=self))

        threads = [threading.Thread(target=claim_once) for _ in range(2)]
        for thread in threads:
            thread.start()
        barrier.wait()
        for thread in threads:
            thread.join(timeout=1.0)

        self.assertEqual(1, sum(receipt is not None for receipt, _ in outcomes))
        self.assertEqual(1, registry.claimed_count)

    def test_capacity_never_evicts_or_reopens_an_event(self):
        registry = AutoDepositTrustInputEventClaimRegistry(capacity=2)
        registry._bind_claim_owner(self)
        first, _ = registry.claim(_event("0" * 32), claim_owner=self)
        second, _ = registry.claim(_event("1" * 32), claim_owner=self)
        third, reason = registry.claim(_event("2" * 32), claim_owner=self)

        self.assertIsNotNone(first)
        self.assertIsNotNone(second)
        self.assertIsNone(third)
        self.assertEqual(
            "auto_deposit_trust_input_event_capacity_exhausted",
            reason,
        )
        registry.abandon_if_issued(first)
        retried, retry_reason = registry.claim(
            _event("0" * 32),
            claim_owner=self,
        )
        self.assertIsNone(retried)
        self.assertEqual("auto_deposit_trust_duplicate_input_event", retry_reason)

    def test_default_capacity_accepts_4096_and_rejects_the_next_unique_id(self):
        registry = AutoDepositTrustInputEventClaimRegistry()
        registry._bind_claim_owner(self)
        self.assertEqual(4096, registry.CAPACITY)

        for index in range(registry.CAPACITY):
            receipt, reason = registry.claim(
                _event(f"{index:032x}"),
                claim_owner=self,
            )
            self.assertIsNotNone(receipt)
            self.assertEqual("", reason)

        overflow, reason = registry.claim(
            _event("f" * 32),
            claim_owner=self,
        )
        self.assertIsNone(overflow)
        self.assertEqual(
            "auto_deposit_trust_input_event_capacity_exhausted",
            reason,
        )

    def test_inspection_is_non_consuming_and_commit_is_atomic(self):
        registry = AutoDepositTrustInputEventClaimRegistry()
        registry._bind_claim_owner(self)
        event = _event("a" * 32)
        receipt, _ = registry.claim(event, claim_owner=self)
        self.assertIsNotNone(receipt)
        self.assertTrue(
            registry.inspect(
                receipt,
                source=event.source,
                command_name="auto_deposit_trust",
            )
        )
        self.assertEqual("ISSUED", registry.state(receipt))

        request = CommandRequestDTO(
            request_id="h5-claim-1",
            command="auto_deposit_trust area 16x16",
            source=event.source,
            metadata={
                "input_event": {
                    "source": event.source,
                    "provider_id": event.provider_id,
                    "event_kind": event.event_kind,
                    "final": True,
                    "event_id": event.event_id,
                }
            },
        )
        self.assertTrue(
            registry.commit(
                receipt,
                source=event.source,
                command_name="auto_deposit_trust",
                request=request,
            )
        )
        self.assertEqual("SPENT", registry.state(receipt))
        self.assertFalse(
            registry.commit(
                receipt,
                source=event.source,
                command_name="auto_deposit_trust",
                request=request,
            )
        )
        with self.assertRaises(TypeError):
            json.dumps(receipt)
        with self.assertRaises(TypeError):
            pickle.dumps(receipt)

    def test_concurrent_commit_has_one_winner(self):
        registry = AutoDepositTrustInputEventClaimRegistry()
        registry._bind_claim_owner(self)
        event = _event("b" * 32)
        receipt, _ = registry.claim(event, claim_owner=self)
        request = CommandRequestDTO(
            request_id="h5-claim-concurrent",
            command="auto_deposit_trust area 16x16",
            source=event.source,
            metadata={
                "input_event": {
                    "source": event.source,
                    "provider_id": event.provider_id,
                    "event_kind": event.event_kind,
                    "final": True,
                    "event_id": event.event_id,
                }
            },
        )
        outcomes = []
        barrier = threading.Barrier(3)

        def commit_once():
            barrier.wait()
            outcomes.append(
                registry.commit(
                    receipt,
                    source=event.source,
                    command_name="auto_deposit_trust",
                    request=request,
                )
            )

        threads = [threading.Thread(target=commit_once) for _ in range(2)]
        for thread in threads:
            thread.start()
        barrier.wait()
        for thread in threads:
            thread.join(timeout=1.0)

        self.assertEqual([False, True], sorted(outcomes))
        self.assertEqual("SPENT", registry.state(receipt))


def _event(event_id: str) -> LaviInputEvent:
    return LaviInputEvent(
        text="자동보관등록 영역 16x16",
        source="lavi_chat_ui",
        provider_id="lavi_chat_ui",
        event_kind="chat_submit",
        final=True,
        event_id=event_id,
        fallback_payload="자동보관등록 영역 16x16",
    )


if __name__ == "__main__":
    unittest.main()
