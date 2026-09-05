#20260905_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations

from .routed_response_request import RoutedResponseRequest


class RoutedResponseRequestValidator:
    def __init__(self, *, history_available: bool) -> None:
        if type(history_available) is not bool:
            raise TypeError("history_available must be an exact bool")
        self._history_available = history_available

    def validate(
        self,
        text,
        *,
        source,
        send_output,
        send_full_output,
        remember_history,
        event_id,
        route_kind,
        response_kind,
    ) -> RoutedResponseRequest:
        if type(source) is not str or not source:
            raise ValueError("source must be a non-empty exact str")
        for name, value in (
            ("send_output", send_output),
            ("send_full_output", send_full_output),
            ("remember_history", remember_history),
        ):
            if type(value) is not bool:
                raise TypeError(f"{name} must be an exact bool")
        if remember_history and not self._history_available:
            raise RuntimeError("routed response history owner is unavailable")
        for name, value in (
            ("route_kind", route_kind),
            ("response_kind", response_kind),
        ):
            if type(value) is not str or not value:
                raise ValueError(f"{name} must be a non-empty exact str")
        return RoutedResponseRequest(
            text=str(text or "").strip(),
            source=source,
            send_output=send_output,
            send_full_output=send_full_output,
            remember_history=remember_history,
            event_id=event_id,
            route_kind=route_kind,
            response_kind=response_kind,
        )


__all__ = ("RoutedResponseRequestValidator",)
