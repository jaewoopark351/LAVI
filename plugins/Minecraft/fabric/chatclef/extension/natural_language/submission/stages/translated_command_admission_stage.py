#20260905_kpopmodder: Preserve translated-command admission APIs as a facade.
from __future__ import annotations

from .admission import TranslatedCommandAdmissionComponentGraph


class TranslatedCommandAdmissionStage:
    def __init__(self, *, admission, registry_provider):
        self._admission = admission
        self._registry_provider = registry_provider
        self._component_graph = TranslatedCommandAdmissionComponentGraph(
            admission=admission,
            registry_provider=registry_provider,
        )
        self._inspector = self._component_graph.inspector
        self._committer = self._component_graph.committer

    def inspect(
        self,
        command_name: str,
        source: str,
        route_claim: object,
    ):
        return self._inspector.inspect(
            command_name,
            source,
            route_claim,
        )

    def commit(self, inspection: object, route_claim: object, request: object):
        return self._committer.commit(inspection, route_claim, request)


__all__ = ("TranslatedCommandAdmissionStage",)
