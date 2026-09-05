#20260905_kpopmodder: Own routed external-response publisher invocation.
from __future__ import annotations


class RoutedInputExternalResponsePublisher:
    def __init__(self, *, response_publisher_callback, diagnostics):
        if not callable(response_publisher_callback):
            raise TypeError("response_publisher_callback must be callable")
        self._response_publisher_callback = response_publisher_callback
        self._diagnostics = diagnostics

    def publish(self, *, message, decision, response_text: str):
        try:
            publisher = self._response_publisher_callback()
            return publisher.emit_capability_response(
                response_text,
                emission_capability=getattr(
                    decision,
                    "response_emission_capability",
                    None,
                ),
                event=message,
                source=str(
                    getattr(
                        decision,
                        "response_source",
                        "minecraft_chatclef",
                    )
                    or "minecraft_chatclef"
                ),
                route_kind=str(
                    getattr(decision, "route_kind", "minecraft_command")
                    or "minecraft_command"
                ),
                response_kind=str(
                    getattr(decision, "response_kind", "immediate")
                    or "immediate"
                ),
            )
        except Exception:
            self._diagnostics.log_failure("publication")
            return None


__all__ = ("RoutedInputExternalResponsePublisher",)
