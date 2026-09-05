#20260905_kpopmodder: Expose only trusted STOP submission and terminal-listener binding.
from __future__ import annotations


class FabricChatClefServerStopApi:
    def __init__(
        self,
        *,
        stop_control_submitter,
        stop_control_claim_registry,
        stop_control_terminal_listener,
    ) -> None:
        self._stop_control_submitter = stop_control_submitter
        self._stop_control_claim_registry = stop_control_claim_registry
        self._stop_control_terminal_listener = stop_control_terminal_listener

    def submit(self, *, event: object, eligibility_proof: object, receipt: object):
        return self._stop_control_submitter.submit(
            event=event,
            eligibility_proof=eligibility_proof,
            receipt=receipt,
        )

    def claim_registry(self):
        return self._stop_control_claim_registry

    def set_terminal_response_callback(self, callback) -> None:
        self._stop_control_terminal_listener.set_callback(callback)
