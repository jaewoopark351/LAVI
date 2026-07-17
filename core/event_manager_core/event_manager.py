#20260717_kpopmodder: Isolates event dispatch state from event DTO and subscription classes.
import threading

from .event_subscription import EventSubscription
from .event_type import EventType


class EventManager:
    def __init__(self):
        # Dictionary to store events and their subscribers
        self._events = {}
        self._lock = threading.RLock()

    def subscribe(self, event_name: EventType, callback):
        """
        Subscribe a callback function to a named event.

        Parameters:
        event_name (str): The name of the event to subscribe to.
        callback (callable): The function to call when the event is triggered.
        """
        if not callable(callback):
            raise TypeError("event callback must be callable")

        with self._lock:
            if event_name not in self._events:
                self._events[event_name] = []

            #20260623_kpopmodder: Event subscriptions are idempotent so UI rebuilds do not duplicate callbacks.
            if callback not in self._events[event_name]:
                self._events[event_name].append(callback)

        return EventSubscription(self, event_name, callback)

    def unsubscribe(self, event_name: EventType, callback):
        """
        Unsubscribe a callback function from a named event.

        Parameters:
        event_name (str): The name of the event to unsubscribe from.
        callback (callable): The function to remove from the subscription list.
        """
        with self._lock:
            callbacks = self._events.get(event_name)
            if not callbacks:
                return False

            removed = False
            while callback in callbacks:
                callbacks.remove(callback)
                removed = True

            # Clean up the event if no more subscribers exist
            if not callbacks:
                del self._events[event_name]

            return removed

    def clear(self, event_name=None):
        with self._lock:
            if event_name is None:
                self._events.clear()
                return

            self._events.pop(event_name, None)

    def subscriber_count(self, event_name=None):
        with self._lock:
            if event_name is None:
                return sum(len(callbacks) for callbacks in self._events.values())

            return len(self._events.get(event_name, []))

    def trigger(self, event_name, *args, **kwargs):
        """
        Trigger a named event, calling all subscribed functions.

        Parameters:
        event_name (str): The name of the event to trigger.
        *args, **kwargs: Arguments to pass to the subscribed callback functions.
        """
        with self._lock:
            callbacks = list(self._events.get(event_name, []))

        for callback in callbacks:
            with self._lock:
                still_subscribed = callback in self._events.get(event_name, [])

            if still_subscribed:
                callback(*args, **kwargs)
