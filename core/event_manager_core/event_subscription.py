#20260717_kpopmodder: Isolates event subscription lifecycle from the manager.

from .event_type import EventType


class EventSubscription:
    def __init__(self, manager, event_name: EventType, callback):
        self._manager = manager
        self._event_name = event_name
        self._callback = callback
        self._active = True

    @property
    def active(self):
        return self._active

    def unsubscribe(self):
        if not self._active:
            return False

        removed = self._manager.unsubscribe(
            self._event_name,
            self._callback,
        )
        self._active = False
        return removed
