#20260907_kpopmodder: Correlate one Java result without mutating lifecycle state.
from __future__ import annotations


class CommandResultCorrelator:
    def correlated(
        self,
        *,
        tracker,
        websocket: object,
        envelope: object,
        result: object,
        outcome: object,
        expected_active: object,
    ) -> bool:
        return bool(
            getattr(outcome, "accepted", False) is True
            and getattr(outcome, "reason", "") == "accepted"
            and expected_active is not None
            and tracker.matches_before_snapshot(
                expected_active,
                getattr(outcome, "before_snapshot", None),
            )
            and tracker.matches_result(
                websocket=websocket,
                owner_token=expected_active,
                session_id=getattr(envelope, "session_id", None),
                generation=getattr(expected_active, "generation", None),
                request_id=getattr(result, "request_id", None),
                command_message_id=getattr(envelope, "correlation_id", None),
            )
        )


__all__ = ("CommandResultCorrelator",)
