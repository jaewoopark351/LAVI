#20260818_kpopmodder: Own matching Fabric ChatClef command-result acceptance.
from __future__ import annotations

from typing import Any

from .result_handling.delivery import (
    FabricChatClefCommandResultTerminalDelivery,
)
from .result_handling.diagnostics import (
    FabricChatClefCommandResultDiagnostics,
)
from .result_handling.lifecycle import (
    FabricChatClefCommandResultLifecycleProjector,
)
from .result_handling.parsing import FabricChatClefCommandResultParser


class FabricChatClefCommandResultHandler:
    def __init__(
        self,
        *,
        connection_ownership,
        command_lock,
        diagnostics,
        crafting_feedback_result_coordinator=None,
        crafting_feedback_terminal_delivery=None,
    ):
        self._connection_ownership = connection_ownership
        self._command_lock = command_lock
        #20260907_kpopmodder: Keep this facade as result-flow orchestration only.
        self._result_parser = FabricChatClefCommandResultParser()
        self._result_diagnostics = FabricChatClefCommandResultDiagnostics(
            diagnostics
        )
        self._lifecycle_projector = FabricChatClefCommandResultLifecycleProjector(
            crafting_feedback_result_coordinator
        )
        self._terminal_delivery = FabricChatClefCommandResultTerminalDelivery(
            crafting_feedback_terminal_delivery
        )

    def handle(self, websocket: Any, envelope: Any) -> None:
        raw_payload = self._result_parser.extract_payload(envelope)
        try:
            result = self._result_parser.parse_payload(raw_payload)
        except Exception as error:
            self._result_diagnostics.report_malformed(error)
            return

        with self._command_lock:
            #20260907_kpopmodder: Freeze lifecycle facts inside the accepted-result transaction.
            expected_active = self._connection_ownership.active_command_owner
            outcome = self._connection_ownership.accept_result_and_reconcile(
                websocket=websocket,
                envelope=envelope,
                result=result,
                raw_payload=raw_payload,
            )
            terminal_projection = self._lifecycle_projector.capture(
                websocket=websocket,
                envelope=envelope,
                result=result,
                outcome=outcome,
                expected_active=expected_active,
            )

        # Rendering deliberately occurs after releasing command_lock.
        terminal_response = self._lifecycle_projector.render(terminal_projection)
        if not outcome.accepted:
            self._result_diagnostics.report_rejected(
                envelope=envelope,
                result=result,
                outcome=outcome,
            )
            return
        self._result_diagnostics.report_accepted(
            result=result,
            outcome=outcome,
        )
        #20260907_kpopmodder: Deliver outside the lock through the fault-contained publisher.
        self._terminal_delivery.publish(terminal_response)
