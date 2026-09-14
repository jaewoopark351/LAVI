#20260914_kpopmodder: Own finite catalog diagnostic reservation separately from functional exchange state.
class FindCatalogDiagnostics:
    PROTECTED_EXCHANGES = 4
    RESERVED_SIGNATURES_PER_EXCHANGE = 32

    def __init__(self, diagnostics):
        self._diagnostics = diagnostics
        self._session = None
        self._traces = {}
        self._excluded_reported = False

    def observe(self, *, session, generation, resource_generation, event, reason, fields):
        if self._session != (session, generation):
            self._session = (session, generation)
            self._traces.clear()
            self._excluded_reported = False
        if resource_generation not in self._traces:
            if len(self._traces) >= self.PROTECTED_EXCHANGES:
                if not self._excluded_reported:
                    self._excluded_reported = True
                    self._emit("excluded", "finite_trace_capacity", generation, resource_generation, {})
                return
            self._traces[resource_generation] = set()
        signatures = self._traces[resource_generation]
        signature = (event, reason)
        if signature in signatures or len(signatures) >= self.RESERVED_SIGNATURES_PER_EXCHANGE:
            return
        signatures.add(signature)
        self._emit(event, reason, generation, resource_generation, fields)

    def _emit(self, event, reason, generation, resource_generation, fields):
        try:
            values = " ".join(f"{key}={value}" for key, value in sorted(fields.items()))
            self._diagnostics.info(f"FIND_CATALOG event={event} reason={reason} generation={generation} resource_generation={resource_generation} {values}"[:768])
        except Exception:
            pass
