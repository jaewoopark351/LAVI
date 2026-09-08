#20260907_kpopmodder: Capture lifecycle facts and render them after lock release.
from __future__ import annotations

from typing import Any


class FabricChatClefCommandResultLifecycleProjector:
    def __init__(self, result_coordinator: Any) -> None:
        self._result_coordinator = result_coordinator

    def capture(
        self,
        *,
        websocket: Any,
        envelope: Any,
        result: Any,
        outcome: Any,
        expected_active: Any,
    ) -> tuple[Any, Any]:
        if self._result_coordinator is None:
            return None, None
        accept_fact = getattr(self._result_coordinator, "accept_fact", None)
        if callable(accept_fact):
            return (
                accept_fact(
                    websocket=websocket,
                    envelope=envelope,
                    result=result,
                    outcome=outcome,
                    expected_active=expected_active,
                ),
                None,
            )
        return (
            None,
            self._result_coordinator.accept(
                websocket=websocket,
                envelope=envelope,
                result=result,
                outcome=outcome,
                expected_active=expected_active,
            ),
        )

    def render(self, projection: tuple[Any, Any]) -> Any:
        terminal_fact, terminal_response = projection
        if terminal_fact is None:
            return terminal_response
        render = getattr(self._result_coordinator, "render_terminal_fact", None)
        if callable(render):
            return render(terminal_fact)
        return terminal_response
