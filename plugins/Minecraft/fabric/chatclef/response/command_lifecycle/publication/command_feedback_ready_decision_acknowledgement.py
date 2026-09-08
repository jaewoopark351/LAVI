#20260907_kpopmodder: Resolve one current-input lifecycle decision only after its turn is ready.
from __future__ import annotations

import threading
from dataclasses import replace


class CommandFeedbackReadyDecisionAcknowledgement:
    def __init__(
        self,
        *,
        acknowledgement,
        initial_response_selector,
        coalesced_response_factory,
    ) -> None:
        self._acknowledgement = acknowledgement
        self._initial_response_selector = initial_response_selector
        self._coalesced_response_factory = coalesced_response_factory
        self._resolution_lock = threading.Lock()
        self._resolution_claimed = False
        self._ready_observed = False

    @property
    def kind(self):
        return getattr(self._acknowledgement, "kind", None)

    def wait_until_ready(self, *, timeout_seconds: float) -> bool:
        ready = self._acknowledgement.wait_until_ready(
            timeout_seconds=timeout_seconds
        ) is True
        if ready:
            with self._resolution_lock:
                self._ready_observed = True
        return ready

    def acknowledge(self, *, published: bool) -> bool:
        return self._acknowledgement.acknowledge(published=published)

    def resolve_ready_decision(self, decision: object):
        with self._resolution_lock:
            if not self._ready_observed or self._resolution_claimed:
                return None
            self._resolution_claimed = True
        terminal = self._initial_response_selector.select(
            self._acknowledgement
        )
        if terminal is None:
            return decision
        response = self._coalesced_response_factory.build(terminal)
        return replace(
            decision,
            response_text=response.text,
            route_kind=response.route_kind,
            response_kind=response.response_kind,
            response_publication_acknowledgement=self,
            presentation_detail_log=response.presentation_detail_log,
        )


__all__ = ("CommandFeedbackReadyDecisionAcknowledgement",)
