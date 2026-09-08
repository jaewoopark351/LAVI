#20260907_kpopmodder: Contain terminal callback failures without replaying feedback.
from __future__ import annotations


class CraftingFeedbackTerminalDelivery:
    def __init__(
        self,
        *,
        terminal_listener,
        diagnostics,
        terminal_response_factory=None,
    ) -> None:
        self._terminal_listener = terminal_listener
        self._diagnostics = diagnostics
        # Retained only as compatibility metadata for the legacy server API.
        self._terminal_response_factory = terminal_response_factory

    def publish(self, response: object) -> None:
        try:
            self._terminal_listener.publish(response)
        except Exception as error:
            self.report_failure(error)

    @property
    def terminal_response_factory(self):
        return self._terminal_response_factory

    def report_failure(self, error: Exception) -> None:
        try:
            self._diagnostics.warning(
                "crafting terminal response delivery failed "
                f"error={type(error).__name__}"
            )
        except Exception:
            return


__all__ = ("CraftingFeedbackTerminalDelivery",)
