#20260905_kpopmodder: Assemble STOP result handling outside the legacy demultiplexer facade.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.response.stop import (
    StopControlResponseRenderer,
)

from plugins.Minecraft.fabric.chatclef.transport.control.stop.stop_control_terminal_result_validator import (
    StopControlTerminalResultValidator,
)
from plugins.Minecraft.fabric.chatclef.transport.control.stop.stop_control_transition_logger import StopControlTransitionLogger
from plugins.Minecraft.fabric.chatclef.transport.control.stop.result_handling.stop_control_result_diagnostic_reporter import (
    StopControlResultDiagnosticReporter,
)
from plugins.Minecraft.fabric.chatclef.transport.control.stop.result_handling.stop_control_result_handling_coordinator import (
    StopControlResultHandlingCoordinator,
)
from plugins.Minecraft.fabric.chatclef.transport.control.stop.result_handling.stop_control_result_parser_validator import (
    StopControlResultParserValidator,
)
from plugins.Minecraft.fabric.chatclef.transport.control.stop.result_handling.stop_control_result_route_matcher import StopControlResultRouteMatcher
from plugins.Minecraft.fabric.chatclef.transport.control.stop.result_handling.stop_control_terminal_response_publisher import (
    StopControlTerminalResponsePublisher,
)
from plugins.Minecraft.fabric.chatclef.transport.control.stop.result_handling.stop_control_terminal_state_coordinator import (
    StopControlTerminalStateCoordinator,
)


class StopControlResultHandlingComponentGraph:
    def __init__(
        self,
        *,
        command_lock: object,
        connection_ownership: object,
        tracker_registry: object,
        admission_barrier: object,
        terminal_listener: object,
        diagnostics: object,
        validator: StopControlTerminalResultValidator | None,
        renderer: StopControlResponseRenderer | None,
        transition_logger: StopControlTransitionLogger | None,
    ):
        terminal_validator = validator or StopControlTerminalResultValidator()
        terminal_renderer = renderer or StopControlResponseRenderer()
        transitions = transition_logger or StopControlTransitionLogger(
            diagnostics
        )
        diagnostic_reporter = StopControlResultDiagnosticReporter(
            diagnostics=diagnostics,
            transition_logger=transitions,
        )
        self.coordinator = StopControlResultHandlingCoordinator(
            command_lock=command_lock,
            tracker_registry=tracker_registry,
            route_matcher=StopControlResultRouteMatcher(connection_ownership),
            parser_validator=StopControlResultParserValidator(
                terminal_validator
            ),
            state_coordinator=StopControlTerminalStateCoordinator(
                connection_ownership=connection_ownership,
                tracker_registry=tracker_registry,
                admission_barrier=admission_barrier,
            ),
            diagnostic_reporter=diagnostic_reporter,
            terminal_publisher=StopControlTerminalResponsePublisher(
                terminal_listener=terminal_listener,
                renderer=terminal_renderer,
                diagnostic_reporter=diagnostic_reporter,
            ),
        )


__all__ = ("StopControlResultHandlingComponentGraph",)
