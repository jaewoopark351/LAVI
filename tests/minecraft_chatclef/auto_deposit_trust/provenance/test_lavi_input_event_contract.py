#20260905_kpopmodder: Verifies immutable typed input envelopes and legacy normalization identity.
import re
import unittest
from dataclasses import FrozenInstanceError

from input_core.input_event import LaviInputEvent, LaviInputEventNormalizer


class LaviInputEventContractTests(unittest.TestCase):
    def test_envelope_is_frozen_but_fallback_payload_remains_the_exact_object(self):
        payload = {"text": "hello", "nested": {"kept": True}}
        event = LaviInputEventNormalizer().normalize(payload)

        self.assertIs(payload, event.fallback_payload)
        self.assertEqual("hello", event.text)
        self.assertEqual("untrusted_legacy", event.source)
        self.assertFalse(event.final)
        self.assertRegex(event.event_id, re.compile(r"^[0-9a-f]{32}$"))
        with self.assertRaises(FrozenInstanceError):
            event.source = "voice_input_final"

    def test_typed_event_is_returned_without_reclassification_or_copy(self):
        payload = object()
        event = LaviInputEvent(
            text="hello",
            source="custom",
            event_id="not-normalized-by-this-boundary",
            event_kind="custom",
            final=True,
            provider_id="custom",
            fallback_payload=payload,
        )

        self.assertIs(event, LaviInputEventNormalizer().normalize(event))


__all__ = ["LaviInputEventContractTests"]


if __name__ == "__main__":
    unittest.main()
