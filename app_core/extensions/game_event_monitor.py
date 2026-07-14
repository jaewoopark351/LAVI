#20260715_kpopmodder: Added this module to observe shared GameEventBus delivery without changing game logic.
from __future__ import annotations

from collections import deque
from typing import Any, Callable, Deque, Dict, Optional, Tuple

from core.logger import log_print


class GameEventMonitor:
    #20260715_kpopmodder: Added this class so runtime logs can prove common game events are delivered.
    def __init__(
        self,
        logger: Callable[[str], None] = log_print,
        enabled: bool = True,
        log_every: int = 25,
        recent_limit: int = 20,
    ):
        self.logger = logger
        self.enabled = bool(enabled)
        self.log_every = max(int(log_every or 1), 1)
        self.recent_limit = max(int(recent_limit or 1), 1)
        self.total_events = 0
        self._event_counts: Dict[Tuple[str, str, str], int] = {}
        self._recent_events: Deque[Dict[str, Any]] = deque(maxlen=self.recent_limit)
        self._subscription = None
        self._important_events = {
            "extension_started",
            "game_started",
            "game_ended",
            "game_won",
            "game_lost",
            "proxy_stopped",
        }

    def attach(self, event_bus: Any) -> Any:
        if self._subscription is not None:
            return self._subscription
        subscribe = getattr(event_bus, "subscribe", None)
        if not callable(subscribe):
            return None
        self._subscription = subscribe(self.receive)
        self._log("[GameEventMonitor] subscribed to GameEventBus")
        return self._subscription

    def shutdown(self) -> None:
        self.detach()

    def detach(self) -> None:
        subscription = self._subscription
        self._subscription = None
        unsubscribe = getattr(subscription, "unsubscribe", None)
        if callable(unsubscribe):
            unsubscribe()

    def receive(self, event: Optional[Dict[str, Any]]) -> None:
        if not self.enabled or not isinstance(event, dict):
            return
        event_type = str(event.get("event_type") or event.get("type") or "").strip()
        if not event_type:
            return
        game = str(event.get("game") or event.get("target") or "").strip()
        source = str(event.get("source") or "").strip()
        key = (game, event_type, source)
        count = self._event_counts.get(key, 0) + 1
        self._event_counts[key] = count
        self.total_events += 1
        self._recent_events.append(
            self._snapshot_event(
                event=event,
                event_type=event_type,
                game=game,
                source=source,
                count=count,
            )
        )
        if self._should_log(event_type, count):
            self._log(
                "[GameEventMonitor] received "
                f"game={game or '-'} event={event_type} "
                f"source={source or '-'} count={count} total={self.total_events}"
            )

    def snapshot(self) -> Dict[str, Any]:
        return {
            "enabled": self.enabled,
            "total_events": self.total_events,
            "recent_limit": self.recent_limit,
            "recent_events": list(self._recent_events),
            "event_counts": {
                "|".join(key): value for key, value in self._event_counts.items()
            },
        }

    def _snapshot_event(
        self,
        event: Dict[str, Any],
        event_type: str,
        game: str,
        source: str,
        count: int,
    ) -> Dict[str, Any]:
        return {
            "event_type": event_type,
            "game": game,
            "source": source,
            "time": event.get("time"),
            "count_for_key": count,
            "total": self.total_events,
            "details": self._summarize_details(event.get("details")),
        }

    def _summarize_details(self, details: Any) -> Dict[str, Any]:
        if not isinstance(details, dict):
            return {}
        summary: Dict[str, Any] = {}
        max_items = 12
        for index, (key, value) in enumerate(details.items()):
            if index >= max_items:
                summary["truncated"] = len(details) - max_items
                break
            key_text = str(key)
            if isinstance(value, (str, int, float, bool)) or value is None:
                summary[key_text] = value
            elif isinstance(value, dict):
                summary[key_text] = {"type": "dict", "size": len(value)}
            elif isinstance(value, (list, tuple, set)):
                summary[key_text] = {
                    "type": value.__class__.__name__,
                    "size": len(value),
                }
            else:
                summary[key_text] = {"type": value.__class__.__name__}
        return summary

    def _should_log(self, event_type: str, count: int) -> bool:
        normalized = event_type.strip().lower()
        return (
            count == 1
            or normalized in self._important_events
            or count % self.log_every == 0
        )

    def _log(self, message: str) -> None:
        try:
            self.logger(message)
        except Exception:
            pass
