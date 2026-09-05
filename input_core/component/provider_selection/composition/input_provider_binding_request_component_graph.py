#20260905_kpopmodder: Composes provider binding-request storage and resolution.
from __future__ import annotations

from ..binding_requests import (
    InputProviderBindingRequestResolver,
    InputProviderBindingRequestStore,
)


class InputProviderBindingRequestComponentGraph:
    def __init__(self, *, bind_callback) -> None:
        self.store = InputProviderBindingRequestStore()
        self.resolver = InputProviderBindingRequestResolver(
            bind_callback=bind_callback,
            store=self.store,
        )


__all__ = ("InputProviderBindingRequestComponentGraph",)
