#20260904_kpopmodder: Linearize WebSocket connect and callback admission against sticky cancellation.
import threading


class VTubeStudioWebSocketLaunchCoordinator:
    def __init__(self, cancel_event):
        self.cancel_event = cancel_event
        self._launch_gate = threading.Lock()
        self._callback_gate = threading.RLock()

    def launch(self, connect_callback):
        # Holding the gate across the external connect call gives close() a clear
        # linearization point: either cancellation wins and no call is made, or
        # this launch began first and close waits for its bounded completion.
        with self._launch_gate:
            if self.cancel_event.is_set():
                return False, None
            return True, connect_callback()

    def invoke_if_active(self, callback, *args):
        with self._callback_gate:
            if self.cancel_event.is_set():
                return False
            callback(*args)
            return True

    def cancel_and_wait_for_admitted_work(self):
        self.cancel_event.set()
        with self._launch_gate:
            pass
        with self._callback_gate:
            pass
