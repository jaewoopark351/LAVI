#20260907_kpopmodder: Acknowledge one lifecycle response publication exactly once.
from __future__ import annotations

import threading

from .command_feedback_publication_permit import CommandFeedbackPublicationPermit


class CommandFeedbackPublicationAcknowledgement:
    def __init__(
        self,
        *,
        permit: CommandFeedbackPublicationPermit,
        callback,
        coalesced_terminal_selector=None,
        publication_failure_diagnostic_custody=None,
    ) -> None:
        if type(permit) is not CommandFeedbackPublicationPermit:
            raise TypeError("command feedback publication permit must be exact")
        if not callable(callback):
            raise TypeError("command feedback publication callback must be callable")
        if (
            coalesced_terminal_selector is not None
            and not callable(coalesced_terminal_selector)
        ):
            raise TypeError("coalesced terminal selector must be callable")
        self._permit = permit
        self._callback = callback
        self._coalesced_terminal_selector = coalesced_terminal_selector
        #20260908_kpopmodder: Carry opaque STATUS failure custody without interpreting it.
        self._publication_failure_diagnostic_custody = (
            publication_failure_diagnostic_custody
        )
        self._lock = threading.Lock()
        self._spent = False
        self._wait_claimed = False
        self._selection_claimed = False

    @property
    def kind(self) -> str:
        return self._permit.kind

    @property
    def publication_failure_diagnostic_custody(self):
        return self._publication_failure_diagnostic_custody

    def wait_until_ready(self, *, timeout_seconds: float) -> bool:
        with self._lock:
            if self._spent or self._wait_claimed:
                return False
            self._wait_claimed = True
        return self._permit.wait_until_ready(timeout_seconds)

    def acknowledge(self, *, published: bool) -> bool:
        if type(published) is not bool:
            raise TypeError("published must be an exact bool")
        with self._lock:
            if self._spent:
                return False
            self._spent = True
        return self._callback(self._permit, published) is True

    def select_coalesced_terminal(self):
        selector = self._coalesced_terminal_selector
        if selector is None:
            return None
        with self._lock:
            if self._spent or self._selection_claimed:
                return None
            self._selection_claimed = True
            try:
                return selector(self._permit)
            except Exception:
                return None


__all__ = ("CommandFeedbackPublicationAcknowledgement",)
