#20260905_kpopmodder: Verifies only provider.handle.descriptor.id can establish provider provenance.
import unittest
from types import SimpleNamespace

from input_core.input_event.provenance import InputProviderSourceResolver


class InputProviderSourceResolverContractTests(unittest.TestCase):
    def test_voice_input_descriptor_resolves_the_only_microphone_trusted_tuple(self):
        provider = _provider("VoiceInput")

        policy = InputProviderSourceResolver().resolve(provider)

        self.assertEqual(
            ("voice_input_final", "VoiceInput", "final_transcript", True),
            (policy.source, policy.provider_id, policy.event_kind, policy.final),
        )

    def test_name_class_and_payload_like_fields_cannot_replace_missing_descriptor(self):
        provider = SimpleNamespace(
            name="VoiceInput",
            plugin=SimpleNamespace(
                source="voice_input_final",
                provider_id="VoiceInput",
            ),
            handle=SimpleNamespace(descriptor=SimpleNamespace(id=None)),
        )

        policy = InputProviderSourceResolver().resolve(provider)

        self.assertEqual(
            ("untrusted_legacy", "unknown", "legacy", False),
            (policy.source, policy.provider_id, policy.event_kind, policy.final),
        )

    def test_non_voice_descriptor_is_preserved_as_an_unauthorized_source(self):
        policy = InputProviderSourceResolver().resolve(_provider("TwitchChatFetch"))

        self.assertEqual("TwitchChatFetch", policy.source)
        self.assertEqual("TwitchChatFetch", policy.provider_id)
        self.assertEqual("provider_output", policy.event_kind)


def _provider(provider_id):
    return SimpleNamespace(
        handle=SimpleNamespace(descriptor=SimpleNamespace(id=provider_id))
    )


__all__ = ["InputProviderSourceResolverContractTests"]


if __name__ == "__main__":
    unittest.main()
