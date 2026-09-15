#20260915_kpopmodder: Freeze validated slot evidence without owning command lifecycle or rendering.
from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class EquipEffectPayload:
    command: str
    request_id: str
    session_id: str
    server_connection_generation: int
    java_socket_generation: int
    task_identity: str
    outcome: str
    reason: str
    before_observed_at_ms: int
    after_observed_at_ms: int
    targets: tuple
    before_slots: tuple
    after_slots: tuple
    before_satisfied: tuple[bool, ...]
    after_satisfied: tuple[bool, ...]

    @property
    def satisfied(self) -> bool:
        return self.outcome in {"satisfied", "already_satisfied"}

    @property
    def observed_mismatch(self) -> bool:
        return self.outcome in {"partial", "not_satisfied"}

    @property
    def satisfied_count(self) -> int:
        return sum(self.after_satisfied)
