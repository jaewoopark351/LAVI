#20260915_kpopmodder: Own one immutable catalogue under the existing session-registry lock.
from __future__ import annotations

from types import MappingProxyType
from .catalogue_decoder import FabricCommandCatalogueDecoder


class FabricCommandCatalogueStore:
    def __init__(self, decoder=None):
        self._decoder = decoder or FabricCommandCatalogueDecoder()
        self._session_id = None
        self._snapshot = None

    def replace(self, session_id: str, wrapper: object):
        self._session_id = session_id
        self._snapshot = None
        try:
            decoded = self._decoder.decode(wrapper)
        except ValueError as error:
            return {"available": False, "reason": str(error)}
        self._snapshot = MappingProxyType({**decoded, "session_id": session_id})
        return {"available": True, "sha256": decoded["catalogue_sha256"],
                "entry_count": len(decoded["entries"])}

    def get(self, session_id: str):
        return self._snapshot if session_id == self._session_id else None

    def clear(self, session_id=None):
        if session_id is None or session_id == self._session_id:
            self._snapshot = None
            self._session_id = None
