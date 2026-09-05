#20260905_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations


class StopControlSubmissionAdmissionCoordinator:
    def __init__(
        self,
        *,
        command_lock: object,
        claim_registry: object,
        connection_gate: object,
        target_snapshot_factory: object,
        envelope_factory: object,
        tracker_admission: object,
        result_factory: object,
    ):
        self._command_lock = command_lock
        self._claim_registry = claim_registry
        self._connection_gate = connection_gate
        self._target_snapshot_factory = target_snapshot_factory
        self._envelope_factory = envelope_factory
        self._tracker_admission = tracker_admission
        self._result_factory = result_factory

    def admit(
        self,
        *,
        event: object,
        eligibility_proof: object,
        receipt: object,
    ) -> tuple[object | None, object, object, object]:
        with self._command_lock:
            spent, spend_reason = self._claim_registry.spend(
                receipt,
                event=event,
                eligibility_proof=eligibility_proof,
            )
            if not spent:
                return (
                    self._result_factory.create(False, spend_reason),
                    None,
                    None,
                    None,
                )
            try:
                (
                    admission_reason,
                    loop,
                    session_id,
                    generation,
                    websocket,
                ) = self._connection_gate.inspect()
                if admission_reason:
                    return (
                        self._result_factory.create(False, admission_reason),
                        None,
                        None,
                        None,
                    )
                target = self._target_snapshot_factory.create()
                identity, request, envelope = self._envelope_factory.create(
                    event=event,
                    target=target,
                    session_id=session_id,
                    generation=generation,
                )
            except Exception:
                return (
                    self._result_factory.create(
                        False,
                        "control_send_rejected",
                    ),
                    None,
                    None,
                    None,
                )

            admission_reason, tracker = self._tracker_admission.register(
                identity=identity,
                websocket=websocket,
                request=request,
                event=event,
                target=target,
            )
            if admission_reason != "committed":
                return (
                    self._result_factory.create(False, admission_reason),
                    None,
                    None,
                    None,
                )
            return None, loop, tracker, envelope


__all__ = ("StopControlSubmissionAdmissionCoordinator",)
