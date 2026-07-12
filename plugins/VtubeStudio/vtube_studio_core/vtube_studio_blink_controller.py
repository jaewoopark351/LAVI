#20260628_kpopmodder: Auto-blink controller is separate from mouth sync so TTS interrupt behavior stays stable.
import random
import threading

from core.logger import log_print


class VTubeStudioBlinkController:#20260628_kpopmodder
    def __init__(
        self,
        send_callback,
        connected_callback,
        authenticated_callback,
        min_interval_sec=2.8,
        max_interval_sec=6.0,
        close_sec=0.08,
        close_transition_sec=0.12,
        open_transition_sec=0.16,
        transition_steps=4,
        open_value=0.52,#20260628_kpopmodder
        close_value=0.0,
        override_callback=None,
        join_timeout=0.3,
    ):
        self.send_callback = send_callback
        self.connected_callback = connected_callback
        self.authenticated_callback = authenticated_callback
        self.min_interval_sec = float(min_interval_sec)
        self.max_interval_sec = float(max_interval_sec)
        self.close_sec = float(close_sec)
        self.close_transition_sec = float(close_transition_sec)
        self.open_transition_sec = float(open_transition_sec)
        self.transition_steps = max(1, int(transition_steps))
        self.open_value = open_value
        self.close_value = close_value
        self.override_callback = override_callback or (lambda: False)
        self.join_timeout = join_timeout

        self.thread = None
        self.stop_event = threading.Event()
        self.state_lock = threading.RLock()
        self.blinking = False

    def start_if_needed(self):
        if self.thread and self.thread.is_alive():
            return

        self.stop_event.clear()
        self.thread = threading.Thread(target=self.blink_loop, daemon=True)
        self.thread.start()

    def stop(self):
        self.stop_event.set()

        try:
            if (
                self.thread
                and self.thread.is_alive()
                and threading.current_thread() != self.thread
            ):
                self.thread.join(timeout=self.join_timeout)
        except Exception as e:
            log_print(f"[VtubeStudio] blink stop error ignored: {e}")

        try:
            self.set_eye_open(self.open_value)
        except Exception as e:
            log_print(f"[VtubeStudio] blink reset error ignored: {e}")

    def blink_loop(self):
        while not self.stop_event.is_set():
            try:
                wait_sec = random.uniform(
                    self.min_interval_sec,
                    self.max_interval_sec,
                )

                if self.stop_event.wait(wait_sec):
                    break

                if (
                    not self.connected_callback()
                    or not self.authenticated_callback()
                    or self.is_overridden()
                ):
                    continue

                self.blink_once()

            except Exception as e:
                log_print(f"[VtubeStudio] blink loop error ignored: {e}")

    def blink_once(self):
        if (
            not self.connected_callback()
            or not self.authenticated_callback()
            or self.is_overridden()
        ):
            return False

        self._set_blinking(True)
        try:
            if not self.send_eye_transition(
                self.open_value,
                self.close_value,
                self.close_transition_sec,
            ):
                return False

            if self.stop_event.wait(self.close_sec):
                return False

            if not self.send_eye_transition(
                self.close_value,
                self.open_value,
                self.open_transition_sec,
            ):
                return False

            return True
        finally:
            self._set_blinking(False)

    def send_eye_transition(self, start_value, end_value, duration_sec):
        step_count = self.transition_steps

        if step_count == 1:
            if self.is_overridden():
                return False
            self.set_eye_open(end_value)
            return not self.stop_event.is_set() and not self.is_overridden()

        delay_sec = max(0.0, float(duration_sec)) / float(step_count - 1)

        for step_index in range(1, step_count + 1):
            if self.stop_event.is_set() or self.is_overridden():
                return False

            progress = step_index / float(step_count)
            value = start_value + ((end_value - start_value) * progress)
            self.set_eye_open(value)

            if step_index < step_count and self.stop_event.wait(delay_sec):
                return False

        return True

    def is_overridden(self):
        try:
            return bool(self.override_callback())
        except Exception as e:
            log_print(f"[VtubeStudio] blink override check error ignored: {e}")
            return False

    def is_blinking(self):
        with self.state_lock:
            return self.blinking

    def _set_blinking(self, value):
        with self.state_lock:
            self.blinking = bool(value)

    def set_eye_open(self, value):
        value = max(0.0, min(1.0, float(value)))

        self.send_callback({
            "apiName": "VTubeStudioPublicAPI",
            "apiVersion": "1.0",
            "requestID": "blink_set",
            "messageType": "InjectParameterDataRequest",
            "data": {
                "mode": "set",
                "parameterValues": [
                    {
                        "id": "EyeOpenLeft",
                        "value": value
                    },
                    {
                        "id": "EyeOpenRight",
                        "value": value
                    }
                ]
            }
        })
