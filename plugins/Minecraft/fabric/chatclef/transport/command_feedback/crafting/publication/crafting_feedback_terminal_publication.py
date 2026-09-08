#20260907_kpopmodder: Present deferred terminal facts before rendered-only delivery.
from __future__ import annotations


class CraftingFeedbackTerminalPublication:
    def __init__(self, *, terminal_presenter, terminal_delivery) -> None:
        self._terminal_presenter = terminal_presenter
        self._terminal_delivery = terminal_delivery

    def publish(self, terminal_fact: object) -> None:
        try:
            present = getattr(self._terminal_presenter, "present", None)
            response = (
                present(terminal_fact)
                if callable(present)
                else self._terminal_presenter(terminal_fact)
            )
        except Exception as error:
            report_failure = getattr(
                self._terminal_delivery,
                "report_failure",
                None,
            )
            if callable(report_failure):
                report_failure(error)
            return
        self._terminal_delivery.publish(response)


__all__ = ("CraftingFeedbackTerminalPublication",)
