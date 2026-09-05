#20260905_kpopmodder: Preserve the legacy STOP result demultiplexer as a thin facade.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.response.stop import (
    StopControlResponseRenderer,
)

from plugins.Minecraft.fabric.chatclef.transport.control.stop.result_handling.stop_control_result_handling_component_graph import (
    StopControlResultHandlingComponentGraph,
)
from .stop_control_terminal_result_validator import StopControlTerminalResultValidator
from .stop_control_transition_logger import StopControlTransitionLogger


class StopControlResultDemultiplexer:
    def __init__(
        self,
        *,
        command_lock: object,
        connection_ownership: object,
        tracker_registry: object,
        admission_barrier: object,
        terminal_listener: object,
        diagnostics: object,
        validator: StopControlTerminalResultValidator | None = None,
        renderer: StopControlResponseRenderer | None = None,
        transition_logger: StopControlTransitionLogger | None = None,
    ):
        self._component_graph = StopControlResultHandlingComponentGraph(
            command_lock=command_lock,
            connection_ownership=connection_ownership,
            tracker_registry=tracker_registry,
            admission_barrier=admission_barrier,
            terminal_listener=terminal_listener,
            diagnostics=diagnostics,
            validator=validator,
            renderer=renderer,
            transition_logger=transition_logger,
        )
        self._coordinator = self._component_graph.coordinator

    def handle(self, websocket: object, envelope: object) -> bool:
        return self._coordinator.handle(websocket, envelope)


__all__ = ("StopControlResultDemultiplexer",)
