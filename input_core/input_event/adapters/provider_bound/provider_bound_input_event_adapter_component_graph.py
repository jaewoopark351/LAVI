#20260905_kpopmodder: Composes provider adaptation and downstream forwarding.
from __future__ import annotations

from input_core.input_event.normalization.input_event_text_normalizer import (
    InputEventTextNormalizer,
)
from input_core.input_event.provenance.input_provider_source_resolver import (
    InputProviderSourceResolver,
)

from .provider_bound_input_event_factory import ProviderBoundInputEventFactory
from .provider_bound_input_event_forwarder import (
    ProviderBoundInputEventForwarder,
)


class ProviderBoundInputEventAdapterComponentGraph:
    def __init__(
        self,
        *,
        provider,
        output_callback,
        source_resolver=None,
        event_id_factory=None,
        text_normalizer=None,
    ) -> None:
        policy = (source_resolver or InputProviderSourceResolver()).resolve(
            provider
        )
        self.event_factory = ProviderBoundInputEventFactory(
            policy=policy,
            event_id_factory=event_id_factory,
            text_normalizer=text_normalizer or InputEventTextNormalizer(),
        )
        self.forwarder = ProviderBoundInputEventForwarder(output_callback)


__all__ = ("ProviderBoundInputEventAdapterComponentGraph",)
