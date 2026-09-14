#20260914_kpopmodder: Own finite catalog exchange state, generation retirement and atomic publication.
from .find_catalog_snapshot import FindCatalogSnapshot, catalog_digest
from .find_catalog_page_validator import FindCatalogPageValidator


class FindCatalogExchangeState:
    EXCHANGE_SECONDS = 10.0

    def __init__(self, clock):
        self._clock = clock
        self.clear()

    def clear(self):
        self.session_identity = None
        self.resource_generation = -1
        self.snapshot = None
        self._pending = None
        self._started_resource_generation = -1

    def bind_session(self, session, generation):
        if self.session_identity != (session, generation):
            self.clear()
            self.session_identity = (session, generation)

    def invalidate(self, reason, generation=None):
        if generation is not None and generation < self.resource_generation:
            return [("rejected", "stale_resource_generation", {})]
        if generation is not None:
            self.resource_generation = generation
            if reason == "CATALOG_INCOMPLETE":
                self._started_resource_generation = max(self._started_resource_generation, generation)
        self.snapshot = None
        self._pending = None
        return [("rejected", reason, {})]

    def accept(self, page):
        if page.resource_generation < self.resource_generation:
            return [("rejected", "stale_resource_generation", {})]
        if (self.snapshot is not None and self.snapshot.resource_generation == page.resource_generation
                and self.snapshot.catalog_digest == page.catalog_digest):
            return [("duplicate", "published_catalog", {})]
        identity = (*self.session_identity, page.resource_generation, page.catalog_digest, page.page_count, page.record_count)
        events = []
        if page.page_index == 0:
            # One finite exchange per resource/session; duplicate page zero never rearms its deadline.
            if page.resource_generation <= self._started_resource_generation:
                return self.invalidate("exchange_already_started")
            self.snapshot = None
            self.resource_generation = page.resource_generation
            self._started_resource_generation = page.resource_generation
            self._pending = [identity, self._clock(), 0, 0, []]
            events.append(("started", "page_zero", {"page_count": page.page_count, "record_count": page.record_count}))
        pending = self._pending
        if (pending is None or identity != pending[0] or page.page_index != pending[2]
                or self._clock() - pending[1] > self.EXCHANGE_SECONDS
                or pending[3] + page.byte_size > FindCatalogPageValidator.MAX_TOTAL_BYTES):
            return events + self.invalidate("sequence_identity_deadline_or_size")
        pending[4].extend(page.records)
        pending[2] += 1
        pending[3] += page.byte_size
        if len(pending[4]) > page.record_count:
            return events + self.invalidate("record_count_exceeded")
        if pending[2] != page.page_count:
            return events
        complete = tuple(pending[4])
        if (len(complete) != page.record_count
                or len({(record.target_kind, record.canonical_target_id) for record in complete}) != len(complete)
                or catalog_digest(complete) != page.catalog_digest):
            return events + self.invalidate("incomplete_duplicate_or_digest")
        self.snapshot = FindCatalogSnapshot(identity[0], identity[1], identity[2], identity[3], complete)
        self._pending = None
        return events + [("published", "complete_validated", {"records": len(complete), "bytes": pending[3]})]
