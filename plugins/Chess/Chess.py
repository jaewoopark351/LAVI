import json
import os
from html import escape
from urllib.parse import urlencode

import gradio as gr

from core.logger import log_print
from core.paths import LaviPaths
from plugins.Chess.chess_core.chess_game_controller import ChessGameController
from plugins.Chess.chess_core.lc0_uci_engine import LC0UCIEngine
from plugins.Chess.chess_core.lc0_runtime_downloader import (
    DEFAULT_LC0_DOWNLOAD_FILES,
    DEFAULT_LC0_DOWNLOAD_REPO_ID,
    DEFAULT_LC0_DOWNLOAD_REVISION,
    DEFAULT_LC0_DOWNLOAD_SUBDIR,
    DEFAULT_LC0_REQUIRED_FILES,
    DEFAULT_LC0_RUNTIME_DIR,
    LC0RuntimeDownloader,
)
from plugins.Chess.web.chess_web_server import ChessWebServer


#20260628_kpopmodder: Added this plugin as an optional iframe-based Chess UI.
class Chess:
    def __init__(self):
        self.paths = LaviPaths()
        self.plugin_root = os.path.dirname(__file__)
        self.legacy_config_dir = os.path.join(self.plugin_root, "config")
        self.config_dir = str(self.paths.config_dir)
        self.config_path = str(self.paths.config_path("chess_config.json"))
        self.example_config_path = str(
            self.paths.config_path("chess_config.example.json")
        )
        #20260720_kpopmodder: Prefer root config/ while preserving legacy plugin config fallback.
        self.legacy_config_path = os.path.join(
            self.legacy_config_dir,
            "chess_config.json",
        )
        self.legacy_example_config_path = os.path.join(
            self.legacy_config_dir,
            "chess_config.example.json",
        )
        self.loaded_config_path = self.config_path
        self.static_dir = os.path.join(self.plugin_root, "web", "static")
        self.config = self._load_config()
        self.config_message = self._config_message()
        self.runtime_downloader = LC0RuntimeDownloader()
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
        config_path = self._active_config_path()
        self.loaded_config_path = config_path
        if not os.path.exists(config_path):
            return {}

        try:
            with open(config_path, "r", encoding="utf-8") as file:
                return json.load(file)
        except Exception as e:
            log_print(f"[Chess] config load failed: {e}")
            return {}

    def _config_message(self):
        config_path = self._active_config_path()
        if not os.path.exists(config_path):
            example_path = self._active_example_config_path()
            return (
                "Chess config missing. Copy "
                f"{example_path} to {self.config_path} and set "
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

    def _active_config_path(self):
        for path in (self.config_path, self.legacy_config_path):
            if os.path.exists(path):
                return path
        return self.config_path

    def _active_example_config_path(self):
        for path in (self.example_config_path, self.legacy_example_config_path):
            if os.path.exists(path):
                return path
        return self.example_config_path

    def _build_engine(self):
        if self.config_message:
            return None

        return LC0UCIEngine(
            lc0_path=self._resolve_config_path("lc0_path"),
            weights_path=self._resolve_config_path("weights_path"),
            backend=str(self.config.get("backend", "cuda")),
            cuda_visible_devices=str(
                self.config.get("cuda_visible_devices", "")
            ),
            init_timeout_sec=self._config_float("init_timeout_sec", 15.0),
            move_timeout_sec=self._config_float("move_timeout_sec", 10.0),
            stop_timeout_sec=self._config_float("stop_timeout_sec", 2.0),
            runtime_downloader=self.runtime_downloader,
            runtime_download_config=self._runtime_download_config(),
        )

    def _resolve_config_path(self, key):
        resolved = self.paths.resolve_path(self.config.get(key, ""))
        return str(resolved or "")

    def _runtime_download_config(self):
        return {
            "runtime_dir": self._lc0_runtime_dir(),
            "enabled": self._config_bool("auto_download_lc0", True),
            "repo_id": str(
                self.config.get(
                    "lc0_download_repo_id",
                    DEFAULT_LC0_DOWNLOAD_REPO_ID,
                )
                or DEFAULT_LC0_DOWNLOAD_REPO_ID
            ),
            "revision": str(
                self.config.get(
                    "lc0_download_revision",
                    DEFAULT_LC0_DOWNLOAD_REVISION,
                )
                or DEFAULT_LC0_DOWNLOAD_REVISION
            ),
            "subdir": str(
                self.config.get(
                    "lc0_download_subdir",
                    DEFAULT_LC0_DOWNLOAD_SUBDIR,
                )
                or DEFAULT_LC0_DOWNLOAD_SUBDIR
            ),
            "files": self._config_list(
                "lc0_download_files",
                DEFAULT_LC0_DOWNLOAD_FILES,
            ),
            "required_files": DEFAULT_LC0_REQUIRED_FILES,
            "timeout_sec": self._config_float("lc0_download_timeout_sec", 120.0),
        }

    def _lc0_runtime_dir(self):
        configured = str(self.config.get("lc0_runtime_dir", "") or "").strip()
        if configured:
            resolved = self.paths.resolve_path(configured)
            return str(resolved or "")
        lc0_path = self._resolve_config_path("lc0_path")
        if lc0_path:
            return os.path.dirname(lc0_path)
        return str(self.paths.root_path(*DEFAULT_LC0_RUNTIME_DIR.parts))

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
        server_url = escape(self._iframe_url(), quote=True)
        return (
            '<iframe src="'
            + server_url
            + '" style="width:100%;height:800px;border:0;background:#0d1117;" '
            + 'title="Chess Board"></iframe>'
        )

    #20260718_kpopmodder: Cache-bust iframe URL so Gradio reloads updated Chess static UI.
    def _iframe_url(self):
        version = self._static_version()
        separator = "&" if "?" in self.server_url else "?"
        return self.server_url + separator + urlencode({"v": version})

    def _static_version(self):
        index_path = os.path.join(self.static_dir, "index.html")
        try:
            return str(int(os.path.getmtime(index_path)))
        except Exception:
            return "0"

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

    def _config_list(self, key, default):
        value = self.config.get(key, default)
        if isinstance(value, (list, tuple)):
            return tuple(str(item).strip() for item in value if str(item).strip())
        if isinstance(value, str):
            return tuple(part.strip() for part in value.split(",") if part.strip())
        return tuple(default)
