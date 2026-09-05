#20260905_kpopmodder: Project only feature-activation outcome status.
from __future__ import annotations


class MinecraftKoreanActivationStatusProjector:
    def project(
        self,
        *,
        proof_issued: bool,
        feature_scope: str,
        handled: bool,
        route_decision: object,
    ) -> str:
        if not proof_issued:
            return "not_attempted"
        if feature_scope == "none":
            return "not_activated"
        result = getattr(route_decision, "result", None)
        if type(result) is dict and result.get("ok") is True:
            return "activated"
        if handled:
            return "handled_without_activation"
        return "not_activated"


__all__ = ("MinecraftKoreanActivationStatusProjector",)
