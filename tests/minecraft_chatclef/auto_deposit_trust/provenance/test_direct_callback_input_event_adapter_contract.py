#20260905_kpopmodder: Verifies direct ScreenVision and game callbacks retain exact fallback objects.
import unittest

from input_core.input_event.adapters import DirectCallbackInputEventAdapter


class DirectCallbackInputEventAdapterContractTests(unittest.TestCase):
    def test_screen_vision_mapping_is_not_rebuilt_or_stringified(self):
        payload = {
            "kind": "screen_observation",
            "source": "screen_vision_auto_watch",
            "observation": "inventory",
            "text": "describe this",
            "display_text": "screen changed",
            "remember_history": False,
            "metadata": {"nested": {"kept": True}},
            "payload": {"pixels": "opaque"},
        }
        received = []
        adapter = DirectCallbackInputEventAdapter(
            output_callback=received.append,
            source="screen_vision",
            provider_id="ScreenVision",
            event_kind="screen_observation",
            event_id_factory=lambda: "d" * 32,
        )

        adapter(payload)

        event = received[0]
        self.assertEqual("describe this", event.text)
        self.assertEqual("screen_vision", event.source)
        self.assertIs(payload, event.fallback_payload)
        self.assertIs(payload["metadata"], event.fallback_payload["metadata"])


__all__ = ["DirectCallbackInputEventAdapterContractTests"]


if __name__ == "__main__":
    unittest.main()
