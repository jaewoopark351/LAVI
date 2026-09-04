#20260905_kpopmodder: Binds one stable callback to one immutable descriptor-derived provider policy.
import secrets

from input_core.input_event.contracts.lavi_input_event import LaviInputEvent
from input_core.input_event.normalization.input_event_text_normalizer import (
    InputEventTextNormalizer,
)
from input_core.input_event.provenance.input_provider_source_resolver import (
    InputProviderSourceResolver,
)


class ProviderBoundInputEventAdapter:
    def __init__(
        self,
        *,
        provider,
        output_callback,
        source_resolver=None,
        event_id_factory=None,
        text_normalizer=None,
    ):
        self._output_callback = output_callback
        self._policy = (source_resolver or InputProviderSourceResolver()).resolve(
            provider
        )
        self._event_id_factory = event_id_factory or self._create_event_id
        self._text_normalizer = text_normalizer or InputEventTextNormalizer()

    @property
    def policy(self):
        return self._policy

    def __call__(self, payload):
        event = LaviInputEvent(
            text=self._text_normalizer.normalize(payload),
            source=self._policy.source,
            event_id=self._event_id_factory(),
            event_kind=self._policy.event_kind,
            final=self._policy.final,
            provider_id=self._policy.provider_id,
            fallback_payload=payload,
        )
        return self._output_callback(event)

    def _create_event_id(self) -> str:
        return secrets.token_hex(16)


__all__ = ["ProviderBoundInputEventAdapter"]
