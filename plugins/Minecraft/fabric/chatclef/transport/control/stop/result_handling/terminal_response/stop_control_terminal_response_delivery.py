#20260905_kpopmodder: Isolate fault-contained STOP terminal response publication.
from __future__ import annotations


class StopControlTerminalResponseDelivery:
    def __init__(self, *, terminal_listener: object, diagnostic_reporter: object):
        self._terminal_listener = terminal_listener
        self._diagnostic_reporter = diagnostic_reporter

    def deliver(self, response: object) -> None:
        try:
            self._terminal_listener.publish(response)
        except Exception as error:
            self._diagnostic_reporter.warn(
                "STOP terminal output listener failed: "
                f"{type(error).__name__}: {error}"
            )


__all__ = ("StopControlTerminalResponseDelivery",)
