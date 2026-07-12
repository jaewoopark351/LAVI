#20260620_kpopmodder: VTube Studio helper modules are grouped under vtube_studio_core without changing behavior.
import json
import threading
import time

import websocket

from core.logger import log_print


class VTubeStudioConnection:#20260620_kpopmodder
    def __init__(
        self,
        websocket_url,
        on_open,
        on_message,
        on_error,
        on_close,
        reset_authentication_callback
    ):
        self.websocket_url = websocket_url
        self.on_open_callback = on_open
        self.on_message_callback = on_message
        self.on_error_callback = on_error
        self.on_close_callback = on_close
        self.reset_authentication_callback = reset_authentication_callback

        self.ws = None
        self.ws_lock = threading.Lock()
        self.websocket_thread_started = False#20260614_kpopmodder
        self.should_reconnect = True#20260614_kpopmodder
        self.connected = False#20260614_kpopmodder

    def start(self, before_start_callback=None):
        if self.websocket_thread_started:
            log_print("[VtubeStudio] websocket thread already started. skip.")
            return False

        self.websocket_thread_started = True

        if before_start_callback is not None:
            before_start_callback()

        thread = threading.Thread(target=self.websocket_thread, daemon=True)
        thread.start()
        return True

    def websocket_thread(self):#20260614_kpopmodder
        while self.should_reconnect:
            try:
                self.connected = False
                self.reset_authentication_callback()

                self.ws = websocket.WebSocketApp(
                    self.websocket_url,
                    on_open=self.on_open_callback,
                    on_message=self.on_message_callback,
                    on_error=self.on_error_callback,
                    on_close=self.on_close_callback,
                )

                self.ws.run_forever()

            except Exception as e:
                log_print(f"[VtubeStudio] websocket_thread error ignored: {e}")

            self.connected = False
            self.reset_authentication_callback()
            self.ws = None

            log_print("[VtubeStudio] reconnecting in 3 seconds...")
            time.sleep(3)

    def safe_send(self, message):#20260614_kpopmodder
        try:
            if self.ws is None or not self.connected:
                return False

            with self.ws_lock:
                self.ws.send(json.dumps(message))

            return True

        except Exception as e:
            log_print(f"[VtubeStudio] send error ignored: {e}")
            self.connected = False
            self.reset_authentication_callback()
            self.ws = None
            return False

    def mark_open(self):
        self.connected = True

    def mark_closed(self):
        self.connected = False
        self.reset_authentication_callback()
