#20260907_kpopmodder: Own one lifecycle TTS delivery's playback transitions.
from __future__ import annotations

from dataclasses import dataclass, field


@dataclass(slots=True)
class TtsLifecycleResponsePlaybackState:
    event_id: str
    route_kind: str
    response_kind: str
    response_generation: int | None
    delivery_mode: str
    item_count: int
    epoch: int
    delivery_token: int
    enqueue_pending: bool = True
    played: set[int] = field(default_factory=set)
    pending_played: set[int] = field(default_factory=set)
    pending_failure: str | None = None

    def record_observation(
        self,
        *,
        item_index: int,
        played: bool,
        reason: str,
    ) -> tuple[bool, str, int] | None:
        if self.enqueue_pending:
            if played:
                self.pending_played.add(item_index)
            elif self.pending_failure is None:
                self.pending_failure = reason
            return None
        if not played:
            return False, reason, len(self.played)
        self.played.add(item_index)
        if len(self.played) != self.item_count:
            return None
        return True, "played", self.item_count

    def commit_enqueue(self) -> tuple[bool, str, int] | None:
        self.enqueue_pending = False
        self.played.update(self.pending_played)
        pending_failure = self.pending_failure
        self.pending_played.clear()
        self.pending_failure = None
        if pending_failure is not None:
            return False, pending_failure, len(self.played)
        if len(self.played) == self.item_count:
            return True, "played", self.item_count
        return None


__all__ = ("TtsLifecycleResponsePlaybackState",)
