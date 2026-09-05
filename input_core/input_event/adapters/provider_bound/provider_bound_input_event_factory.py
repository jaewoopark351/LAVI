#20260905_kpopmodder: Adapts provider payloads into immutable provenance events.
from __future__ import annotations

import secrets

from input_core.input_event.contracts.lavi_input_event import LaviInputEvent


class ProviderBoundInputEventFactory:
    def __init__(
        self,
        *,
        policy,
        event_id_factory=None,
        text_normalizer,
    ) -> None:
        self._policy = policy
        self._event_id_factory = event_id_factory or self._create_event_id
        self._text_normalizer = text_normalizer

    @property
    def policy(self):
        return self._policy

    @property
    def event_id_factory(self):
        return self._event_id_factory

    @property
    def text_normalizer(self):
        return self._text_normalizer

    def create(self, payload) -> LaviInputEvent:
        return LaviInputEvent(
            text=self._text_normalizer.normalize(payload),
            source=self._policy.source,
            event_id=self._event_id_factory(),
            event_kind=self._policy.event_kind,
            final=self._policy.final,
            provider_id=self._policy.provider_id,
            fallback_payload=payload,
        )

    @staticmethod
    def _create_event_id() -> str:
        return secrets.token_hex(16)


__all__ = ("ProviderBoundInputEventFactory",)
