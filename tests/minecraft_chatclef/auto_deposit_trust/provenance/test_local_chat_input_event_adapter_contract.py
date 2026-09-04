#20260905_kpopmodder: Verifies each local Chat submission receives exact trusted provenance once.
import unittest

from input_core.input_event.adapters import LocalChatInputEventAdapter


class LocalChatInputEventAdapterContractTests(unittest.TestCase):
    def test_local_chat_adapter_stamps_provenance_and_preserves_original_payload(self):
        payload = "@auto_deposit_trust area 16x16"
        adapter = LocalChatInputEventAdapter(
            event_id_factory=lambda: "c" * 32,
        )

        event = adapter.adapt(payload)

        self.assertEqual(
            ("lavi_chat_ui", "lavi_chat_ui", "chat_submit", True),
            (event.source, event.provider_id, event.event_kind, event.final),
        )
        self.assertEqual("c" * 32, event.event_id)
        self.assertIs(payload, event.fallback_payload)

    def test_each_physical_submission_receives_one_fresh_event_id(self):
        event_ids = iter(("a" * 32, "b" * 32))
        adapter = LocalChatInputEventAdapter(
            event_id_factory=lambda: next(event_ids),
        )

        first = adapter.adapt("first")
        second = adapter.adapt("second")

        self.assertEqual(
            ("a" * 32, "b" * 32),
            (first.event_id, second.event_id),
        )


__all__ = ["LocalChatInputEventAdapterContractTests"]


if __name__ == "__main__":
    unittest.main()
