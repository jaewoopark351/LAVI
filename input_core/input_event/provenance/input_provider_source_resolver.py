#20260905_kpopmodder: Resolves provenance only from the loader-owned provider handle descriptor.
from input_core.input_event.provenance.input_provider_source_policy import (
    InputProviderSourcePolicy,
)
from input_core.input_event.provenance.lavi_input_source import (
    UNTRUSTED_LEGACY,
    VOICE_INPUT_FINAL,
)


class InputProviderSourceResolver:
    VOICE_INPUT_PROVIDER_ID = "VoiceInput"

    def resolve(self, provider) -> InputProviderSourcePolicy:
        provider_id = self._descriptor_id(provider)
        if provider_id == self.VOICE_INPUT_PROVIDER_ID:
            return InputProviderSourcePolicy(
                source=VOICE_INPUT_FINAL,
                provider_id=self.VOICE_INPUT_PROVIDER_ID,
                event_kind="final_transcript",
                final=True,
            )
        if provider_id:
            return InputProviderSourcePolicy(
                source=provider_id,
                provider_id=provider_id,
                event_kind="provider_output",
                final=True,
            )
        return InputProviderSourcePolicy(
            source=UNTRUSTED_LEGACY,
            provider_id="unknown",
            event_kind="legacy",
            final=False,
        )

    def _descriptor_id(self, provider) -> str:
        handle = getattr(provider, "handle", None)
        descriptor = getattr(handle, "descriptor", None)
        provider_id = getattr(descriptor, "id", None)
        if type(provider_id) is not str or not provider_id:
            return ""
        return provider_id


__all__ = ["InputProviderSourceResolver"]
