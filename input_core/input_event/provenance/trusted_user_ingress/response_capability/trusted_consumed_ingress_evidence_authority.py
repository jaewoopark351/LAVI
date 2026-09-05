#20260905_kpopmodder: Validates consumed evidence against one ingress authority.
from __future__ import annotations

from ..consumed_ingress_evidence_validator import ConsumedIngressEvidenceValidator


class TrustedConsumedIngressEvidenceAuthority:
    def __init__(self, *, registry_token: object) -> None:
        self.validator = ConsumedIngressEvidenceValidator(
            registry_token=registry_token
        )

    def validate(self, event: object, evidence: object) -> bool:
        return self.validator.validate(event, evidence)


__all__ = ("TrustedConsumedIngressEvidenceAuthority",)
