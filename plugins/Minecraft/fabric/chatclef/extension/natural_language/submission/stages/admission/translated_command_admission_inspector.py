#20260905_kpopmodder: Isolate translated-command admission inspection.
from __future__ import annotations


class TranslatedCommandAdmissionInspector:
    def __init__(self, *, admission, registry_provider):
        self._admission = admission
        self._registry_provider = registry_provider

    def inspect(
        self,
        command_name: str,
        source: str,
        route_claim: object,
    ):
        return self._admission.inspect(
            command_name,
            source,
            self._registry_provider(),
            route_claim,
        )


__all__ = ("TranslatedCommandAdmissionInspector",)
