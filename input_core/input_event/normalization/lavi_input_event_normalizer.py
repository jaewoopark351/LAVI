#20260905_kpopmodder: Normalizes legacy inputs as untrusted while preserving typed events and payload identity.
import secrets

from input_core.input_event.contracts.lavi_input_event import LaviInputEvent
from input_core.input_event.normalization.input_event_text_normalizer import (
    InputEventTextNormalizer,
)
from input_core.input_event.provenance.lavi_input_source import UNTRUSTED_LEGACY


class LaviInputEventNormalizer:
    def __init__(self, event_id_factory=None, text_normalizer=None):
        self._event_id_factory = event_id_factory or self._create_event_id
        self._text_normalizer = text_normalizer or InputEventTextNormalizer()

    def normalize(self, value) -> LaviInputEvent:
        if isinstance(value, LaviInputEvent):
            return value
        return LaviInputEvent(
            text=self._text_normalizer.normalize(value),
            source=UNTRUSTED_LEGACY,
            event_id=self._event_id_factory(),
            event_kind="legacy",
            final=False,
            provider_id="unknown",
            fallback_payload=value,
        )

    def _create_event_id(self) -> str:
        return secrets.token_hex(16)


__all__ = ["LaviInputEventNormalizer"]
