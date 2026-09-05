#20260905_kpopmodder: Isolate STOP transport invocation through the extension seam.
from __future__ import annotations


class StopControlExtensionSubmitter:
    def __init__(self, extension: object):
        self._extension = extension

    def submit(
        self,
        *,
        event: object,
        eligibility_proof: object,
        receipt: object,
    ) -> tuple[object | None, str]:
        submit = getattr(self._extension, "submit_stop_control", None)
        if not callable(submit):
            return None, "bridge_disconnected"
        try:
            return (
                submit(
                    event=event,
                    eligibility_proof=eligibility_proof,
                    receipt=receipt,
                ),
                "",
            )
        except Exception:
            return None, "control_send_unknown"


__all__ = ("StopControlExtensionSubmitter",)
