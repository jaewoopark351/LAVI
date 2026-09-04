#20260905_kpopmodder: Stamps one direct producer identity while retaining the exact emitted object for fallback.
import secrets

from input_core.input_event.contracts.lavi_input_event import LaviInputEvent
from input_core.input_event.normalization.input_event_text_normalizer import (
    InputEventTextNormalizer,
)


class DirectCallbackInputEventAdapter:
    def __init__(
        self,
        *,
        output_callback,
        source,
        provider_id,
        event_kind,
        final=True,
        event_id_factory=None,
        text_normalizer=None,
    ):
        self._output_callback = output_callback
        self._source = source
        self._provider_id = provider_id
        self._event_kind = event_kind
        self._final = final
        self._event_id_factory = event_id_factory or self._create_event_id
        self._text_normalizer = text_normalizer or InputEventTextNormalizer()

    def __call__(self, payload):
        event = LaviInputEvent(
            text=self._text_normalizer.normalize(payload),
            source=self._source,
            event_id=self._event_id_factory(),
            event_kind=self._event_kind,
            final=self._final,
            provider_id=self._provider_id,
            fallback_payload=payload,
        )
        return self._output_callback(event)

    def _create_event_id(self) -> str:
        return secrets.token_hex(16)


__all__ = ["DirectCallbackInputEventAdapter"]
