#20260905_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations


class StopControlResultHandlingCoordinator:
    def __init__(
        self,
        *,
        command_lock: object,
        tracker_registry: object,
        route_matcher: object,
        parser_validator: object,
        state_coordinator: object,
        diagnostic_reporter: object,
        terminal_publisher: object,
    ):
        self._command_lock = command_lock
        self._tracker_registry = tracker_registry
        self._route_matcher = route_matcher
        self._parser_validator = parser_validator
        self._state_coordinator = state_coordinator
        self._diagnostic_reporter = diagnostic_reporter
        self._terminal_publisher = terminal_publisher

    def handle(self, websocket: object, envelope: object) -> bool:
        payload, data = self._route_matcher.extract(envelope)
        with self._command_lock:
            tracker = self._tracker_registry.current()
            if tracker is None:
                if self._route_matcher.has_stop_marker(data):
                    self._diagnostic_reporter.warn(
                        "dropped stop result without live tracker"
                    )
                    return True
                return False

            matches_routing_identity = (
                self._route_matcher.matches_tracker_routing_identity(
                    payload,
                    envelope,
                    tracker,
                )
            )
            if not self._route_matcher.matches_live_connection(
                websocket,
                tracker,
            ):
                if (
                    matches_routing_identity
                    or self._route_matcher.has_stop_marker(data)
                ):
                    self._diagnostic_reporter.warn(
                        "dropped stop result from stale connection"
                    )
                    return True
                return False
            if not matches_routing_identity:
                if self._route_matcher.has_stop_marker(data):
                    self._diagnostic_reporter.warn(
                        "dropped mismatched stop result while tracker is live"
                    )
                    return True
                return False
            if tracker.quarantined:
                self._diagnostic_reporter.warn(
                    "dropped matching stop result for quarantined tracker"
                )
                return True

            result, decision, error = self._parser_validator.parse_and_inspect(
                payload=payload,
                envelope=envelope,
                tracker=tracker,
            )
            if error is not None:
                (
                    frozen_owner_match,
                    barrier_state,
                    retirement_evidence,
                    diagnostic_disposition,
                ) = self._state_coordinator.quarantine_malformed(tracker)
                self._diagnostic_reporter.warn(
                    "malformed matching stop result: "
                    f"{type(error).__name__}: {error}"
                )
            else:
                (
                    frozen_owner_match,
                    barrier_state,
                    retirement_evidence,
                    diagnostic_disposition,
                ) = self._state_coordinator.apply(
                    result=result,
                    decision=decision,
                    tracker=tracker,
                )

        self._diagnostic_reporter.log_terminal(
            tracker=tracker,
            data=data,
            decision=decision,
            diagnostic_disposition=diagnostic_disposition,
            barrier_state=barrier_state,
            frozen_owner_match=frozen_owner_match,
            retirement_evidence=retirement_evidence,
        )
        self._terminal_publisher.publish(tracker=tracker, decision=decision)
        return True


__all__ = ("StopControlResultHandlingCoordinator",)
