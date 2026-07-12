import json
import os
import socket
import threading
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlparse

from core.logger import log_print


def find_available_port(host, start_port, max_attempts=100):
    for port in range(int(start_port), int(start_port) + int(max_attempts)):
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as test_socket:
            try:
                test_socket.bind((host, port))
                return port
            except OSError:
                continue
    raise RuntimeError(f"No Chess web port available from {start_port}.")


#20260628_kpopmodder: Local HTTP bridge keeps Chess UI outside Gradio internals.
class ChessWebServer:
    def __init__(self, controller, static_dir, host="127.0.0.1", port=8790):
        self.controller = controller
        self.static_dir = os.path.abspath(static_dir)
        self.host = str(host or "127.0.0.1")
        self.port = int(port or 8790)
        self.httpd = None
        self.thread = None
        self.url = None

    def start(self):
        if self.httpd is not None:
            return self.url

        port = find_available_port(self.host, self.port)
        handler = self._build_handler()
        self.httpd = ThreadingHTTPServer((self.host, port), handler)
        self.thread = threading.Thread(
            target=self.httpd.serve_forever,
            name="ChessWebServer",
            daemon=True,
        )
        self.thread.start()
        self.url = f"http://{self.host}:{port}/"
        return self.url

    def shutdown(self):
        if self.httpd is None:
            return

        try:
            self.httpd.shutdown()
            self.httpd.server_close()
        finally:
            self.httpd = None
            self.url = None

    def _build_handler(self):
        controller = self.controller
        static_dir = self.static_dir

        #20260628_kpopmodder: Handler captures only the controller and static root.
        class ChessRequestHandler(SimpleHTTPRequestHandler):
            def __init__(self, *args, **kwargs):
                super().__init__(*args, directory=static_dir, **kwargs)

            def do_GET(self):
                parsed = urlparse(self.path)
                if parsed.path == "/api/state":
                    self._send_json(controller.state())
                    return
                if parsed.path == "/":
                    self.path = "/index.html"
                return super().do_GET()

            def do_POST(self):
                try:
                    parsed = urlparse(self.path)
                    body = self._read_json()
                    if parsed.path == "/api/new-game":
                        result = controller.new_game()
                    elif parsed.path == "/api/move":
                        result = controller.apply_human_move(
                            body.get("from", ""),
                            body.get("to", ""),
                            body.get("promotion", ""),
                        )
                    elif parsed.path == "/api/ai-move":
                        result = controller.apply_ai_move()
                    elif parsed.path == "/api/start-engine":
                        result = controller.start_engine()
                    elif parsed.path == "/api/stop-engine":
                        result = controller.stop_engine()
                    elif parsed.path == "/api/resign-reset":
                        result = controller.reset_or_resign()
                    else:
                        result = controller.state(ok=False)
                        result["message"] = "Unknown Chess API endpoint."
                except Exception as e:
                    request_path = getattr(self, "path", "")
                    log_print(f"[Chess] API error {request_path}: {e}")
                    try:
                        result = controller.state(ok=False)
                    except Exception:
                        result = {"ok": False}
                    result["message"] = f"Chess API error: {e}"
                self._send_json(result)

            def log_message(self, format, *args):
                return

            def _read_json(self):
                length = int(self.headers.get("Content-Length", "0") or 0)
                if length <= 0:
                    return {}
                raw = self.rfile.read(length)
                if not raw:
                    return {}
                return json.loads(raw.decode("utf-8"))

            def _send_json(self, payload):
                data = json.dumps(payload).encode("utf-8")
                self.send_response(200)
                self.send_header("Content-Type", "application/json")
                self.send_header("Content-Length", str(len(data)))
                self.end_headers()
                self.wfile.write(data)

        return ChessRequestHandler
