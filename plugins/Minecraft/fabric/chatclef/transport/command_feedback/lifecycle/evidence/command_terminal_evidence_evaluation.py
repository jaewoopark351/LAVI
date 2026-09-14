#20260908_kpopmodder: Added this module to freeze one terminal-evidence decision and its optional projection.
from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class CommandTerminalEvidenceEvaluation:
    verified: bool
    projection: object = None
    #20260913_kpopmodder: A verified failure reason is not a successful command.
    failure_projection: object = None
    decision_reason: str = ""
    #20260914_kpopmodder: A verified negative FIND query is not satisfied success.
    query_projection: object = None

    def __post_init__(self) -> None:
        if self.query_projection is not None:
            from plugins.Minecraft.fabric.chatclef.result.find import FindTerminalPayload
            if (type(self.query_projection) is not FindTerminalPayload
                    or self.query_projection.find_result != "NOT_OBSERVED_IN_LOADED_SCOPE"
                    or self.query_projection.find_satisfied or self.verified
                    or self.projection is not None or self.failure_projection is not None):
                raise ValueError("query projection requires a validated negative FIND query")
        if type(self.verified) is not bool:
            raise TypeError("command terminal evidence verified must be an exact bool")
        if not self.verified and self.projection is not None:
            raise ValueError("unverified command evidence cannot carry a projection")
        if self.failure_projection is not None and (
            self.verified or self.projection is not None
        ):
            raise ValueError("failure evidence cannot carry a success projection")
        if type(self.decision_reason) is not str or len(self.decision_reason) > 64:
            raise ValueError("terminal evidence decision reason must be bounded text")


__all__ = ("CommandTerminalEvidenceEvaluation",)
