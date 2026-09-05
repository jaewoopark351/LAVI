#20260905_kpopmodder: Adapts one local Chat submission into a trusted input event.
import secrets

from input_core.input_event.contracts.lavi_input_event import LaviInputEvent
from input_core.input_event.normalization.input_event_text_normalizer import (
    InputEventTextNormalizer,
)
from input_core.input_event.provenance.input_provider_source_policy import (
    InputProviderSourcePolicy,
)
from input_core.input_event.provenance.lavi_input_source import LAVI_CHAT_UI


class LocalChatInputEventAdapter:
    def __init__(
        self,
        *,
        event_id_factory=None,
        text_normalizer=None,
    ):
        self._event_id_factory = (
            event_id_factory
            if event_id_factory is not None
            else self._create_event_id
        )
        self._text_normalizer = (
            text_normalizer
            if text_normalizer is not None
            else InputEventTextNormalizer()
        )
        self._policy = InputProviderSourcePolicy(
            source=LAVI_CHAT_UI,
            provider_id=LAVI_CHAT_UI,
            event_kind="chat_submit",
            final=True,
        )

    @property
    def policy(self) -> InputProviderSourcePolicy:
        return self._policy

    def adapt(self, message) -> LaviInputEvent:
        return LaviInputEvent(
            text=self._text_normalizer.normalize(message),
            source=self._policy.source,
            event_id=self._event_id_factory(),
            event_kind=self._policy.event_kind,
            final=self._policy.final,
            provider_id=self._policy.provider_id,
            fallback_payload=message,
        )

    def _create_event_id(self) -> str:
        return secrets.token_hex(16)


__all__ = ("LocalChatInputEventAdapter",)
