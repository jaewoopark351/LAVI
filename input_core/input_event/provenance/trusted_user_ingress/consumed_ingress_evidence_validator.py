#20260905_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations

from input_core.input_event.contracts.lavi_input_event import LaviInputEvent

from .consumed_ingress_evidence import ConsumedIngressEvidence


class ConsumedIngressEvidenceValidator:
    def __init__(self, *, registry_token: object) -> None:
        if registry_token is None:
            raise ValueError("registry_token is required")
        self._registry_token = registry_token

    def validate(self, event: object, evidence: object) -> bool:
        if (
            type(event) is not LaviInputEvent
            or type(evidence) is not ConsumedIngressEvidence
            or getattr(evidence, "_registry_token", None)
            is not self._registry_token
            or evidence.event is not event
        ):
            return False
        try:
            return evidence.is_live_for(event) is True
        except Exception:
            return False


__all__ = ("ConsumedIngressEvidenceValidator",)
