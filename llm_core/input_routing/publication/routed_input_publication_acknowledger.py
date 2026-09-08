#20260905_kpopmodder: Keep routed-input publication acknowledgement in the established responsibility-split boundary.
#20260907_kpopmodder: Notify optional routed-response publication owners once.
from __future__ import annotations


class RoutedInputPublicationAcknowledger:
    TURN_TIMEOUT_SECONDS = 2.0

    def __init__(self, diagnostics) -> None:
        self._diagnostics = diagnostics

    def wait_for_turn(self, decision: object) -> bool:
        acknowledgement = getattr(
            decision,
            "response_publication_acknowledgement",
            None,
        )
        if acknowledgement is None:
            return True
        waiter = getattr(acknowledgement, "wait_until_ready", None)
        if not callable(waiter):
            self._diagnostics.log_failure("publication_turn")
            self.acknowledge(decision, published=False)
            return False
        try:
            ready = waiter(
                timeout_seconds=self.TURN_TIMEOUT_SECONDS
            ) is True
        except Exception:
            self._diagnostics.log_failure("publication_turn")
            ready = False
        if not ready:
            self.acknowledge(decision, published=False)
        return ready

    def resolve_ready_decision(self, decision: object):
        acknowledgement = getattr(
            decision,
            "response_publication_acknowledgement",
            None,
        )
        resolver = getattr(
            acknowledgement,
            "resolve_ready_decision",
            None,
        )
        if not callable(resolver):
            return decision
        try:
            resolved = resolver(decision)
        except Exception:
            self._diagnostics.log_failure("publication_ready_resolution")
            return None
        if resolved is None:
            self._diagnostics.log_failure("publication_ready_resolution")
            return None
        if (
            getattr(
                resolved,
                "response_publication_acknowledgement",
                None,
            )
            is not acknowledgement
        ):
            self._diagnostics.log_failure("publication_ready_resolution")
            return None
        return resolved

    def acknowledge(self, decision: object, *, published: bool) -> bool:
        acknowledgement = getattr(
            decision,
            "response_publication_acknowledgement",
            None,
        )
        callback = getattr(acknowledgement, "acknowledge", None)
        if not callable(callback):
            return False
        try:
            return callback(published=published) is True
        except Exception:
            self._diagnostics.log_failure("publication_acknowledgement")
            return False


__all__ = ("RoutedInputPublicationAcknowledger",)
