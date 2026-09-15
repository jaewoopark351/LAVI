#20260907_kpopmodder: Freeze one correlated terminal fact before phrase rendering.
from __future__ import annotations

from dataclasses import dataclass, field
from plugins.Minecraft.fabric.chatclef.result.instant import InstantCommandPayload
from plugins.Minecraft.fabric.chatclef.result.equip import EquipEffectPayload


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

    def __post_init__(self) -> None:
        if type(self.verified) is not bool or type(self.dispatch_started) is not bool:
            raise TypeError("command terminal proof flags must be exact bools")
        if type(self.status) is not str or not self.status:
            raise ValueError("command terminal status is required")
        if self.failure_projection is not None and (
            (self.status != "failed" and not (
                self.status == "unknown" and type(self.failure_projection) is InstantCommandPayload
                and self.failure_projection.outcome == "unknown") and not (
                    self.status == "completed" and type(self.failure_projection) is EquipEffectPayload
                    and self.failure_projection.observed_mismatch
                    and getattr(self.descriptor, "command_name", None) == "equip"))
            or self.verified or self.evidence_projection is not None
        ):
            raise ValueError("failure projection requires a matching non-success effect or failed terminal")


__all__ = ("CommandTerminalFact",)
