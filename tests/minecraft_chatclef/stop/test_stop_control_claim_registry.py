#20260905_kpopmodder: Verify opaque, bounded, one-shot STOP claim ownership.
import copy
import json
import pickle
import unittest

from input_core.input_event.contracts.lavi_input_event import LaviInputEvent
from plugins.Minecraft.fabric.chatclef.input.eligibility import (
    KoreanChatMicrophoneEligibilityProof,
)
from plugins.Minecraft.fabric.chatclef.input.stop import (
    KoreanStopControlRouteOwner,
    StopControlClaimReceipt,
    StopControlClaimRegistry,
)


class _ConsumedEvidence:
    def __init__(self, event, owner):
        self.event = event
        self.owner = owner
        self.open = True

    def is_live_for(self, event, owner):
        return self.open and event is self.event and owner is self.owner

    def close(self):
        self.open = False


def _event(event_id="0" * 32):
    return LaviInputEvent(
        text="멈춰",
        source="lavi_chat_ui",
        event_id=event_id,
        event_kind="chat_submit",
        final=True,
        provider_id="lavi_chat_ui",
        fallback_payload="멈춰",
    )


def _proof(event, owner):
    return KoreanChatMicrophoneEligibilityProof._issue(
        event=event,
        consumed_evidence=_ConsumedEvidence(event, owner),
        owner=owner,
    )


def _registry(owner, *, capacity=StopControlClaimRegistry.DEFAULT_CAPACITY):
    registry = StopControlClaimRegistry(capacity=capacity)
    registry._bind_claim_owner(owner)
    return registry


class StopControlClaimRegistryTests(unittest.TestCase):
    def test_exact_event_and_proof_can_be_spent_once(self):
        owner = object()
        registry = _registry(owner)
        event = _event()
        proof = _proof(event, owner)
        receipt, reason = registry.issue(
            event=event,
            eligibility_proof=proof,
            normalized_phrase="멈춰",
        )
        self.assertEqual(reason, "issued")
        self.assertRaises(TypeError, pickle.dumps, receipt)
        self.assertEqual(
            registry.spend(receipt, event=event, eligibility_proof=proof),
            (True, "spent"),
        )
        self.assertEqual(
            registry.spend(receipt, event=event, eligibility_proof=proof),
            (False, "duplicate_or_spent_stop_claim"),
        )

    def test_copy_foreign_and_capacity_fail_closed(self):
        owner = object()
        registry = _registry(owner, capacity=1)
        event = _event()
        proof = _proof(event, owner)
        receipt, _ = registry.issue(event=event, eligibility_proof=proof, normalized_phrase="멈춰")
        copied = _event()
        self.assertEqual(
            registry.spend(receipt, event=copied, eligibility_proof=proof)[0],
            False,
        )
        event2 = _event("1" * 32)
        receipt2, reason = registry.issue(
            event=event2,
            eligibility_proof=_proof(event2, owner),
            normalized_phrase="중지",
        )
        self.assertIsNone(receipt2)
        self.assertEqual(reason, "stop_claim_capacity_exhausted")

    def test_requires_lowercase_builtin_hex_event_id(self):
        for event_id in ("A" * 32, "0" * 31, "g" * 32):
            with self.subTest(event_id=event_id):
                event = _event(event_id)
                owner = object()
                receipt, _ = _registry(owner).issue(
                    event=event,
                    eligibility_proof=_proof(event, owner),
                    normalized_phrase="멈춰",
                )
                self.assertIsNone(receipt)

    def test_noncanonical_phrase_cannot_issue_stop_authority(self):
        event = _event()
        owner = object()
        receipt, reason = _registry(owner).issue(
            event=event,
            eligibility_proof=_proof(event, owner),
            normalized_phrase="지도 만들어줘",
        )
        self.assertIsNone(receipt)
        self.assertEqual(reason, "invalid_stop_phrase")

    def test_receipt_is_registry_issued_immutable_and_noncopyable(self):
        with self.assertRaises(TypeError):
            StopControlClaimReceipt(
                registry=object(),
                record_key="0" * 32,
                nonce=object(),
            )

        owner = object()
        registry = _registry(owner)
        event = _event()
        proof = _proof(event, owner)
        receipt, reason = registry.issue(
            event=event,
            eligibility_proof=proof,
            normalized_phrase=event.text,
        )
        self.assertEqual(reason, "issued")
        with self.assertRaises(TypeError):
            copy.copy(receipt)
        with self.assertRaises(TypeError):
            copy.deepcopy(receipt)
        with self.assertRaises(TypeError):
            json.dumps(receipt)
        with self.assertRaises(AttributeError):
            receipt._nonce = object()
        self.assertEqual(repr(receipt), "StopControlClaimReceipt(<opaque>)")

        reconstructed = StopControlClaimReceipt._issue(
            registry=receipt._registry,
            record_key=receipt._record_key,
            nonce=receipt._nonce,
        )
        self.assertEqual(
            registry.spend(
                reconstructed,
                event=event,
                eligibility_proof=proof,
            ),
            (False, "invalid_stop_claim_receipt"),
        )

    def test_forged_proof_does_not_activate_stop_specific_route_or_response(self):
        owner = object()
        registry = _registry(owner)
        extension = _RecordingStopExtension()
        decision = KoreanStopControlRouteOwner(
            extension=extension,
            claim_registry=registry,
        ).route(_event(), object())
        self.assertFalse(decision.handled)
        self.assertEqual(extension.calls, [])
        self.assertEqual(registry.record_count, 0)

    def test_closed_proof_rejection_permanently_spends_issued_receipt(self):
        owner = object()
        registry = _registry(owner)
        event = _event()
        proof = _proof(event, owner)
        receipt, _ = registry.issue(
            event=event,
            eligibility_proof=proof,
            normalized_phrase=event.text,
        )
        proof.close()
        self.assertEqual(
            registry.spend(
                receipt,
                event=event,
                eligibility_proof=proof,
            ),
            (False, "closed_eligibility_proof"),
        )
        self.assertEqual(
            registry.spend(
                receipt,
                event=event,
                eligibility_proof=proof,
            ),
            (False, "duplicate_or_spent_stop_claim"),
        )


class _RecordingStopExtension:
    def __init__(self):
        self.calls = []

    def submit_stop_control(self, **kwargs):
        self.calls.append(kwargs)


if __name__ == "__main__":
    unittest.main()
