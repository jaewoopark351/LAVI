#20260907_kpopmodder: Bound lifecycle playback reservations under the facade lock.
from __future__ import annotations

from .tts_lifecycle_response_playback_state import (
    TtsLifecycleResponsePlaybackState,
)


class TtsLifecycleResponsePlaybackStateRegistry:
    """Caller-synchronized registry; intentionally owns no lock."""

    def __init__(self, *, capacity: int) -> None:
        if type(capacity) is not int or capacity <= 0:
            raise ValueError("capacity must be a positive exact int")
        self._capacity = capacity
        self._states = {}
        self._epoch = 0
        self._next_delivery_token = 1

    def contains(self, identity: tuple[str, str, str]) -> bool:
        return identity in self._states

    def is_full(self) -> bool:
        return len(self._states) >= self._capacity

    def reserve(
        self,
        identity: tuple[str, str, str],
        *,
        response_generation: int | None,
        delivery_mode: str,
        item_count: int,
    ) -> TtsLifecycleResponsePlaybackState:
        event_id, route_kind, response_kind = identity
        state = TtsLifecycleResponsePlaybackState(
            event_id=event_id,
            route_kind=route_kind,
            response_kind=response_kind,
            response_generation=response_generation,
            delivery_mode=delivery_mode,
            item_count=item_count,
            epoch=self._epoch,
            delivery_token=self._allocate_delivery_token(),
        )
        self._states[identity] = state
        return state

    def resolve_identity(
        self,
        event_id: str,
        *,
        route_kind: object,
        response_kind: object,
    ) -> tuple[str, str, str] | None:
        if route_kind is not None or response_kind is not None:
            if any(
                type(value) is not str or not value
                for value in (route_kind, response_kind)
            ):
                return None
            return event_id, route_kind, response_kind
        matches = tuple(
            identity for identity in self._states if identity[0] == event_id
        )
        return matches[0] if len(matches) == 1 else None

    def get(
        self,
        identity: tuple[str, str, str],
    ) -> TtsLifecycleResponsePlaybackState | None:
        return self._states.get(identity)

    def is_current(
        self,
        identity: tuple[str, str, str],
        state: TtsLifecycleResponsePlaybackState,
    ) -> bool:
        return state.epoch == self._epoch and self._states.get(identity) is state

    def remove_if_current(
        self,
        identity: tuple[str, str, str],
        state: TtsLifecycleResponsePlaybackState,
    ) -> bool:
        if self._states.get(identity) is not state:
            return False
        self._states.pop(identity, None)
        return True

    def clear(self) -> None:
        self._states.clear()
        self._epoch += 1

    def _allocate_delivery_token(self) -> int:
        active_tokens = {
            state.delivery_token for state in self._states.values()
        }
        for _ in range(self._capacity + 1):
            token = self._next_delivery_token
            self._next_delivery_token = 1 if token >= 2**63 - 1 else token + 1
            if token not in active_tokens:
                return token
        raise RuntimeError("lifecycle TTS delivery token capacity exhausted")


__all__ = ("TtsLifecycleResponsePlaybackStateRegistry",)
