#20260907_kpopmodder: Report one lifecycle queue item's playback fact without owning queue policy.
from __future__ import annotations


class TtsLifecycleQueueItemPlaybackObserver:
    @staticmethod
    def observe(owner, item, *, played: bool, reason: str):
        if not isinstance(item, dict):
            return None
        event_id = item.get("lifecycle_event_id")
        item_index = item.get("lifecycle_item_index")
        if (
            type(event_id) is not str
            or not event_id
            or type(item_index) is not int
            or item_index < 0
        ):
            return None
        callback = getattr(
            owner,
            "observe_lifecycle_response_playback",
            None,
        )
        if not callable(callback):
            return None
        values = {
            "event_id": event_id,
            "item_index": item_index,
            "played": played,
            "reason": reason,
        }
        route_kind = item.get("lifecycle_route_kind")
        response_kind = item.get("lifecycle_response_kind")
        if all(
            type(value) is str and value
            for value in (route_kind, response_kind)
        ):
            values.update(
                {
                    "route_kind": route_kind,
                    "response_kind": response_kind,
                }
            )
        delivery_token = item.get("lifecycle_delivery_token")
        if type(delivery_token) is int and delivery_token > 0:
            values["delivery_token"] = delivery_token
        try:
            return callback(**values)
        except Exception:
            return None


__all__ = ("TtsLifecycleQueueItemPlaybackObserver",)
