import json
import os
from html import escape

import gradio as gr

from core.logger import log_print
from plugins.Chess.chess_core.chess_game_controller import ChessGameController
from plugins.Chess.chess_core.lc0_uci_engine import LC0UCIEngine
from plugins.Chess.web.chess_web_server import ChessWebServer


#20260628_kpopmodder: Added this plugin as an optional iframe-based Chess UI.
class Chess:
    def __init__(self):
        self.plugin_root = os.path.dirname(__file__)
        self.config_dir = os.path.join(self.plugin_root, "config")
        self.config_path = os.path.join(self.config_dir, "chess_config.json")
        self.example_config_path = os.path.join(
            self.config_dir,
            "chess_config.example.json",
        )
        self.static_dir = os.path.join(self.plugin_root, "web", "static")
        self.config = self._load_config()
        self.config_message = self._config_message()
        self.engine = self._build_engine()
        self.controller = ChessGameController(
            engine=self.engine,
            movetime_ms=self._config_int("movetime_ms", 1000),
            human_side=str(self.config.get("human_side", "white")),
            ai_side=self.config.get("ai_side"),
        )
        self.web_server = ChessWebServer(
            controller=self.controller,
            static_dir=self.static_dir,
            host=str(self.config.get("web_server_host", "127.0.0.1")),
            port=self._config_int("web_server_port", 8790),
        )
        self.server_url = None
        self.server_message = "Chess web server not started."
        self._shutdown = False

    def create_ui(self):
        self._ensure_server_started()

        with gr.Tab("Chess"):
            gr.Markdown(self._status_markdown())
            if self.server_url:
                gr.HTML(self._iframe_html())
            else:
                gr.Textbox(
                    label="Chess Status",
                    value=self.server_message,
                    interactive=False,
                )

    def shutdown(self):
        if self._shutdown:
            return

        self._shutdown = True
        try:
            self.web_server.shutdown()
        except Exception as e:
            log_print(f"[Chess] web server shutdown failed: {e}")
        try:
            self.controller.shutdown()
        except Exception as e:
            log_print(f"[Chess] controller shutdown failed: {e}")

    def set_ai_move_applied_callback(self, callback):
        self.controller.set_ai_move_applied_callback(callback)

    def set_ai_reaction(self, event_id, reaction):
        return self.controller.set_ai_reaction(event_id, reaction)

    def _ensure_server_started(self):
        if self.server_url:
            return

        try:
            self.server_url = self.web_server.start()
            self.server_message = f"Chess board ready: {self.server_url}"
            log_print(f"[Chess] web server started: {self.server_url}")
        except Exception as e:
            self.server_url = None
            self.server_message = f"Chess web server failed: {e}"
            log_print(f"[Chess] web server failed: {e}")
            return

        if self.config_message:
            log_print(f"[Chess] {self.config_message}")

        if self._config_bool("auto_start_engine", False):
            state = self.controller.start_engine()
            log_print(f"[Chess] auto_start_engine: {state.get('message', '')}")

    def _load_config(self):
        if not os.path.exists(self.config_path):
            return {}

        try:
            with open(self.config_path, "r", encoding="utf-8") as file:
                return json.load(file)
        except Exception as e:
            log_print(f"[Chess] config load failed: {e}")
            return {}

    def _config_message(self):
        if not os.path.exists(self.config_path):
            return (
                "Chess config missing. Copy "
                f"{self.example_config_path} to {self.config_path} and set "
                "lc0_path plus weights_path for BT4-it332."
            )

        missing = []
        for key in ("lc0_path", "weights_path"):
            value = str(self.config.get(key, "")).strip()
            if not value:
                missing.append(key)

        if missing:
            return "Chess config missing required keys: " + ", ".join(missing)

        return ""

    def _build_engine(self):
        if self.config_message:
            return None

        return LC0UCIEngine(
            lc0_path=str(self.config.get("lc0_path", "")),
            weights_path=str(self.config.get("weights_path", "")),
            backend=str(self.config.get("backend", "cuda")),
            cuda_visible_devices=str(
                self.config.get("cuda_visible_devices", "")
            ),
            init_timeout_sec=self._config_float("init_timeout_sec", 15.0),
            move_timeout_sec=self._config_float("move_timeout_sec", 10.0),
            stop_timeout_sec=self._config_float("stop_timeout_sec", 2.0),
        )

    def _status_markdown(self):
        lines = [
            f"Server: {self.server_message}",
        ]
        if self.config_message:
            lines.append(self.config_message)
        return "\n\n".join(lines)

    def _iframe_html(self):
        if not self.server_url:
            return "<div>Chess web server is not ready.</div>"
        server_url = escape(self.server_url, quote=True)
        return (
            '<iframe src="'
            + server_url
            + '" style="width:100%;height:800px;border:0;background:#fff;" '
            + 'title="Chess Board"></iframe>'
        )

    def _config_bool(self, key, default):
        value = self.config.get(key, default)
        return str(value).strip().lower() in {"1", "true", "yes", "on"}

    def _config_int(self, key, default):
        try:
            return int(self.config.get(key, default))
        except Exception:
            return int(default)

    def _config_float(self, key, default):
        try:
            return float(self.config.get(key, default))
        except Exception:
            return float(default)
