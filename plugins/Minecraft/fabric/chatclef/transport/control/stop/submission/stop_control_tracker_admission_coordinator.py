#20260905_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.transport.control.stop.stop_control_tracker import (
    StopControlTracker,
)


class StopControlTrackerAdmissionCoordinator:
    def __init__(
        self,
        *,
        admission_barrier: object,
        tracker_registry: object,
        transition_reporter: object,
    ):
        self._admission_barrier = admission_barrier
        self._tracker_registry = tracker_registry
        self._transition_reporter = transition_reporter

    def register(
        self,
        *,
        identity: object,
        websocket: object,
        request: object,
        event: object,
        target: object,
    ) -> tuple[str, StopControlTracker | None]:
        barrier_token = self._admission_barrier.close(identity.as_tuple())
        if barrier_token is None:
            return "stop_control_in_flight", None

        tracker = None
        try:
            tracker = StopControlTracker(
                identity=identity,
                barrier_token=barrier_token,
                transport_websocket=websocket,
                request=request,
                event_id=getattr(event, "event_id", ""),
                source=getattr(event, "source", ""),
                target_scope=request.metadata["target_scope"],
                target=target,
            )
            registered = self._tracker_registry.register(tracker)
        except Exception:
            current = self._tracker_registry.current()
            if tracker is not None and current is tracker:
                tracker.enter_quarantine()
                self._transition_reporter.transition(
                    tracker,
                    diagnostic_disposition=(
                        "tracker_registration_outcome_unknown"
                    ),
                    control_result_delivery="not_applicable",
                    python_stop_barrier_state="closed",
                    quarantine_active=True,
                    retirement_evidence_kind="unknown_quarantine",
                )
                return "control_send_unknown", tracker
            self._admission_barrier.release(barrier_token)
            return "control_send_rejected", None
        if not registered:
            self._admission_barrier.release(barrier_token)
            return "stop_control_in_flight", None
        return "committed", tracker


__all__ = ("StopControlTrackerAdmissionCoordinator",)
