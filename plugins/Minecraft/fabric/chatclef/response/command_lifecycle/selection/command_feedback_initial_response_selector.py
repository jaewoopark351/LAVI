#20260907_kpopmodder: Select an already-staged immediate terminal without rendering it.
from __future__ import annotations


class CommandFeedbackInitialResponseSelector:
    def select(self, publication_acknowledgement: object):
        selector = getattr(
            publication_acknowledgement,
            "select_coalesced_terminal",
            None,
        )
        if not callable(selector):
            return None
        try:
            return selector()
        except Exception:
            return None


__all__ = ("CommandFeedbackInitialResponseSelector",)
