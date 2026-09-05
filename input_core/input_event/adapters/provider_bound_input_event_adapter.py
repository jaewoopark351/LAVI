#20260905_kpopmodder: Preserves the provider-bound input adapter API.
from .provider_bound import (
    ProviderBoundInputEventAdapterComponentGraph,
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
        self._components = ProviderBoundInputEventAdapterComponentGraph(
            provider=provider,
            output_callback=output_callback,
            source_resolver=source_resolver,
            event_id_factory=event_id_factory,
            text_normalizer=text_normalizer,
        )
        self._event_factory = self._components.event_factory
        self._forwarder = self._components.forwarder
        self._output_callback = self._forwarder.output_callback
        self._policy = self._event_factory.policy
        self._event_id_factory = self._event_factory.event_id_factory
        self._text_normalizer = self._event_factory.text_normalizer

    @property
    def policy(self):
        return self._policy

    def __call__(self, payload):
        event = self.adapt(payload)
        return self._forwarder.forward(event)

    def adapt(self, payload):
        return self._event_factory.create(payload)

    def _create_event_id(self) -> str:
        return self._event_factory._create_event_id()


__all__ = ("ProviderBoundInputEventAdapter",)
