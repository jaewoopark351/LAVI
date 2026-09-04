#20260904_kpopmodder: Own VTube Studio interrupt subscription and retryable cleanup.
import threading

from core.event_manager import EventType
from core.logger import log_print


class VTubeStudioInterruptSubscription:
    def __init__(self, event_manager, callback):
        self.event_manager = event_manager
        self.callback = callback
        self._lock = threading.RLock()
        self._subscription = None
        self._shutdown_requested = False
        self._unsubscribe_in_progress = False

    @property
    def subscription(self):
        with self._lock:
            return self._subscription

    @property
    def cleanup_pending(self):
        with self._lock:
            return self._subscription is not None or self._unsubscribe_in_progress

    def subscribe(self):
        with self._lock:
            if self._shutdown_requested or self._subscription is not None:
                return False
            self._subscription = self.event_manager.subscribe(
                EventType.INTERRUPT,
                self.callback,
            )
            return True

    def request_shutdown(self):
        with self._lock:
            self._shutdown_requested = True

    def unsubscribe(self):
        self.request_shutdown()
        with self._lock:
            subscription = self._subscription
            if subscription is None:
                return True
            if self._unsubscribe_in_progress:
                return False
            self._unsubscribe_in_progress = True

        try:
            subscription.unsubscribe()
        except Exception as error:
            log_print(
                "[VtubeStudio] shutdown component failed "
                "component=event_subscription "
                f"error_type={type(error).__name__}",
                level="warning",
            )
            return False
        else:
            with self._lock:
                if self._subscription is subscription:
                    self._subscription = None
            return True
        finally:
            with self._lock:
                self._unsubscribe_in_progress = False
