#20260905_kpopmodder: Delegate trusted-ingress evidence validation to claim ownership.
from __future__ import annotations


class LlmTrustedIngressEvidenceValidator:
    def __init__(self, claim_graph):
        self._claim_graph = claim_graph

    def validate(self, event, evidence):
        return self._claim_graph.validate_consumed_evidence(event, evidence)


__all__ = ("LlmTrustedIngressEvidenceValidator",)
