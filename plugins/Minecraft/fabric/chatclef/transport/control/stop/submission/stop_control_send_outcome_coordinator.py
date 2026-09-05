#20260905_kpopmodder: Sequence focused STOP send-outcome stages without owning their policies.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.transport.control.stop.submission.send_outcome.stop_control_send_outcome_component_graph import (
    StopControlSendOutcomeComponentGraph,
)


class StopControlSendOutcomeCoordinator:
    def __init__(
        self,
        *,
        command_lock: object,
        tracker_registry: object,
        admission_barrier: object,
        envelope_transport: object,
        future_scheduler: object,
        send_timeout_sec: float,
        transition_reporter: object,
        result_factory: object,
    ):
        self._component_graph = StopControlSendOutcomeComponentGraph(
            command_lock=command_lock,
            tracker_registry=tracker_registry,
            admission_barrier=admission_barrier,
            envelope_transport=envelope_transport,
            future_scheduler=future_scheduler,
            send_timeout_sec=send_timeout_sec,
            transition_reporter=transition_reporter,
            result_factory=result_factory,
        )
        self._transport_delivery = self._component_graph.transport_delivery
        self._state_transitions = self._component_graph.state_transitions
        self._diagnostics = self._component_graph.diagnostics
        self._results = self._component_graph.results

    def deliver(
        self,
        *,
        tracker: object,
        envelope: object,
        loop: object,
    ):
        try:
            delivery_status = self._transport_delivery.deliver(
                tracker=tracker,
                envelope=envelope,
                loop=loop,
            )
        except Exception:
            terminal_released, quarantined = (
                self._state_transitions.quarantine_if_unreleased(tracker)
            )
            if terminal_released:
                return self._accepted_after_terminal(tracker)
            self._diagnostics.report_unknown(
                tracker,
                disposition="control_send_outcome_unknown",
                quarantined=quarantined,
            )
            return self._results.unknown()

        terminal_released = self._state_transitions.terminal_was_released(
            tracker
        )
        if terminal_released:
            return self._accepted_after_terminal(tracker)
        if delivery_status == "not_scheduled":
            return self._handle_not_scheduled(tracker)
        if delivery_status == "outcome_unknown":
            return self._handle_unknown_status(
                tracker,
                "control_send_outcome_unknown",
            )
        if delivery_status != "sent":
            return self._handle_unknown_status(
                tracker,
                "invalid_control_send_outcome",
            )

        terminal_released, quarantined = self._state_transitions.snapshot(
            tracker
        )
        self._diagnostics.report_accepted(
            tracker,
            terminal_released=terminal_released,
            quarantined=quarantined,
        )
        return self._results.accepted(tracker)

    def _handle_not_scheduled(self, tracker: object):
        terminal_released, released = (
            self._state_transitions.resolve_not_scheduled(tracker)
        )
        if terminal_released:
            return self._accepted_after_terminal(tracker)
        self._diagnostics.report_not_scheduled(
            tracker,
            released=released,
        )
        return self._results.rejected()

    def _handle_unknown_status(self, tracker: object, disposition: str):
        terminal_released, quarantined = (
            self._state_transitions.quarantine_if_unreleased(tracker)
        )
        if terminal_released:
            return self._accepted_after_terminal(tracker)
        self._diagnostics.report_unknown(
            tracker,
            disposition=disposition,
            quarantined=quarantined,
        )
        return self._results.unknown()

    def _accepted_after_terminal(self, tracker: object):
        self._diagnostics.report_terminal_release(tracker)
        return self._results.accepted(tracker)


__all__ = ("StopControlSendOutcomeCoordinator",)
