#20260907_kpopmodder: Freeze one correlated terminal fact before phrase rendering.
from __future__ import annotations

from dataclasses import dataclass, field


@dataclass(frozen=True, slots=True)
class CommandTerminalFact:
    descriptor: object
    status: str
    verified: bool
    dispatch_started: bool
    result_reason: str
    event_id: str
    owner_token: object = field(compare=False, repr=False)
    evidence_projection: object = field(default=None, compare=False, repr=False)
    #20260913_kpopmodder: A validated failure reason is independent of verified arrival.
    failure_projection: object = field(default=None, compare=False, repr=False)
    #20260914_kpopmodder: Keep completed-negative FIND observations outside success evidence.
    query_projection: object = field(default=None, compare=False, repr=False)

    def __post_init__(self) -> None:
        if self.query_projection is not None:
            from plugins.Minecraft.fabric.chatclef.result.find import FindTerminalPayload
            if (type(self.query_projection) is not FindTerminalPayload or self.status != "completed"
                    or getattr(self.descriptor, "command_name", None) != "find"
                    or self.query_projection.find_result != "NOT_OBSERVED_IN_LOADED_SCOPE"
                    or self.verified or self.evidence_projection is not None or self.failure_projection is not None):
                raise ValueError("query projection requires a completed negative FIND terminal")
        if type(self.verified) is not bool or type(self.dispatch_started) is not bool:
            raise TypeError("command terminal proof flags must be exact bools")
        if type(self.status) is not str or not self.status:
            raise ValueError("command terminal status is required")
        if self.failure_projection is not None and (
            self.status != "failed" or self.verified or self.evidence_projection is not None
        ):
            raise ValueError("failure projection requires a non-success failed terminal")


__all__ = ("CommandTerminalFact",)
