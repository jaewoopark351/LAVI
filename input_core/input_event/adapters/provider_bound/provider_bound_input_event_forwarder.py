#20260905_kpopmodder: Forwards one adapted provider event downstream.
from __future__ import annotations


class ProviderBoundInputEventForwarder:
    def __init__(self, output_callback) -> None:
        self._output_callback = output_callback

    @property
    def output_callback(self):
        return self._output_callback

    def forward(self, event):
        return self._output_callback(event)


__all__ = ("ProviderBoundInputEventForwarder",)
