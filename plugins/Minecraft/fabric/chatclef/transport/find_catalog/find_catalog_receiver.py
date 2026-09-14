#20260914_kpopmodder: Admit catalog events only at the existing exact active-session transport boundary.
from __future__ import annotations

import threading
import time

from .find_catalog_diagnostics import FindCatalogDiagnostics
from .find_catalog_exchange_state import FindCatalogExchangeState
from .find_catalog_page_validator import FindCatalogPageValidator


class FindCatalogReceiver:
    MAX_RECORDS = FindCatalogPageValidator.MAX_RECORDS
    MAX_PAGES = FindCatalogPageValidator.MAX_PAGES
    MAX_PAGE_RECORDS = FindCatalogPageValidator.MAX_PAGE_RECORDS
    MAX_PAGE_BYTES = FindCatalogPageValidator.MAX_PAGE_BYTES
    MAX_TOTAL_BYTES = FindCatalogPageValidator.MAX_TOTAL_BYTES
    EXCHANGE_SECONDS = FindCatalogExchangeState.EXCHANGE_SECONDS

    def __init__(self, *, connection_ownership, diagnostics, clock=time.monotonic):
        self._ownership = connection_ownership
        self._diagnostics = diagnostics
        self._observer = FindCatalogDiagnostics(diagnostics)
        self._validator = FindCatalogPageValidator()
        self._exchange = FindCatalogExchangeState(clock)
        self._lock = threading.RLock()

    def snapshot(self):
        with self._lock:
            if (not self._ownership.is_connected() or self._exchange.session_identity
                    != (self._ownership.active_session_id, self._ownership.active_generation)):
                self._exchange.clear()
                return None
            return self._exchange.snapshot

    def clear(self):
        with self._lock:
            self._exchange.clear()

    def receive(self, websocket, envelope):
        payload = getattr(envelope, "payload", None)
        if (type(payload) is not dict or type(payload.get("event")) is not str
                or payload["event"] not in {"find_catalog_page", "find_catalog_invalidated"}):
            return False
        with self._lock:
            if (not self._ownership.is_active_websocket(websocket)
                    or getattr(envelope, "session_id", None) != self._ownership.active_session_id):
                self._observe([("rejected", "inactive_session", {})])
                return True
            self._exchange.bind_session(self._ownership.active_session_id, self._ownership.active_generation)
            if payload["event"] == "find_catalog_invalidated":
                reason = self._validator.invalidation_reason(payload, self._ownership.active_generation)
                generation = payload.get("resource_generation") if reason != "invalid_invalidation" else None
                events = self._exchange.invalidate(reason, generation)
            else:
                page, reason = self._validator.decode(payload, self._ownership.active_generation)
                events = self._exchange.invalidate(reason) if page is None else self._exchange.accept(page)
            self._observe(events)
        return True

    def _observe(self, events):
        for event, reason, fields in events:
            self._observer.observe(session=self._ownership.active_session_id,
                generation=self._ownership.active_generation, resource_generation=self._exchange.resource_generation,
                event=event, reason=reason, fields=fields)
