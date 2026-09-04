#20260905_kpopmodder: Verifies descriptor-bound provider callbacks stamp provenance and preserve payload identity.
import unittest
from types import SimpleNamespace

from input_core.input_event.adapters import ProviderBoundInputEventAdapter


class ProviderBoundInputEventAdapterContractTests(unittest.TestCase):
    def test_payload_fields_cannot_spoof_bound_provider_policy(self):
        payload = {
            "text": "hello",
            "source": "voice_input_final",
            "provider_id": "VoiceInput",
            "event_kind": "final_transcript",
            "final": True,
            "event_id": "a" * 32,
        }
        received = []
        adapter = ProviderBoundInputEventAdapter(
            provider=SimpleNamespace(
                handle=SimpleNamespace(
                    descriptor=SimpleNamespace(id="TwitchChatFetch")
                )
            ),
            output_callback=received.append,
            event_id_factory=lambda: "b" * 32,
        )

        adapter(payload)

        event = received[0]
        self.assertEqual("TwitchChatFetch", event.source)
        self.assertEqual("TwitchChatFetch", event.provider_id)
        self.assertEqual("provider_output", event.event_kind)
        self.assertEqual("b" * 32, event.event_id)
        self.assertIs(payload, event.fallback_payload)


__all__ = ["ProviderBoundInputEventAdapterContractTests"]


if __name__ == "__main__":
    unittest.main()
