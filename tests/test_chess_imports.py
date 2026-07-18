import json
import os
import sys
import unittest
import urllib.error
import urllib.request

#20260628_kpopmodder: Added Chess import and local API error-handling tests.

sys.path.insert(0, os.path.dirname(os.path.dirname(__file__)))

from plugins.Chess.Chess import Chess
from plugins.Chess.chess_core.chess_game_controller import ChessGameController
from plugins.Chess.chess_core.chess_reaction_runtime import (
    handle_chess_ai_move_applied,
    run_chess_ai_reaction,
    speak_chess_ai_reaction,
)
from plugins.Chess.chess_core.lc0_uci_engine import LC0UCIEngine
from plugins.Chess.web.chess_web_server import ChessWebServer


class ChessImportTests(unittest.TestCase):
    def test_chess_modules_import(self):
        self.assertIsNotNone(Chess)
        self.assertIsNotNone(ChessGameController)
        self.assertIsNotNone(LC0UCIEngine)
        self.assertIsNotNone(ChessWebServer)
        self.assertTrue(callable(handle_chess_ai_move_applied))#20260630_kpopmodder
        self.assertTrue(callable(run_chess_ai_reaction))#20260630_kpopmodder
        self.assertTrue(callable(speak_chess_ai_reaction))#20260630_kpopmodder

    def test_iframe_html_uses_server_url(self):
        plugin = Chess()
        plugin.server_url = "http://127.0.0.1:8790/"
        html = plugin._iframe_html()
        self.assertIn("iframe", html)
        self.assertIn(plugin.server_url, html)
        self.assertIn("?v=", html)
        self.assertIn("height:800px", html)
        self.assertIn("border:0", html)
        self.assertIn("background:#0d1117", html)

    def test_chess_web_uses_committed_local_vendor_files(self):
        repo_root = os.path.dirname(os.path.dirname(__file__))
        static_dir = os.path.join(repo_root, "plugins", "Chess", "web", "static")
        index_path = os.path.join(static_dir, "index.html")
        vendor_dir = os.path.join(static_dir, "vendor", "cm-chessboard")

        with open(index_path, "r", encoding="utf-8") as file:
            index_html = file.read()

        self.assertIn("./vendor/cm-chessboard/src/Chessboard.js", index_html)
        self.assertIn("./vendor/cm-chessboard/assets/chessboard.css", index_html)
        for relative_path in (
            "src/Chessboard.js",
            "assets/chessboard.css",
            "assets/pieces/staunty.svg",
            "LICENSE",
            "README.LAV.md",
        ):
            self.assertTrue(
                os.path.exists(os.path.join(vendor_dir, *relative_path.split("/"))),
                relative_path,
            )

    def test_chess_config_paths_resolve_from_project_root(self):
        repo_root = os.path.dirname(os.path.dirname(__file__))
        plugin = Chess()
        plugin.config = {
            "lc0_path": "plugins/Chess/lc0-v0.32.1-windows-gpu-nvidia-cuda12/lc0.exe",
            "weights_path": "plugins/Chess/lc0-v0.32.1-windows-gpu-nvidia-cuda12/BT4-1024x15x32h-swa-6147500-policytune-332.pb.gz",
        }

        self.assertEqual(
            os.path.normcase(
                os.path.join(
                    repo_root,
                    "plugins",
                    "Chess",
                    "lc0-v0.32.1-windows-gpu-nvidia-cuda12",
                    "lc0.exe",
                )
            ),
            os.path.normcase(plugin._resolve_config_path("lc0_path")),
        )


class ChessWebServerTests(unittest.TestCase):
    def test_static_responses_disable_cache(self):
        controller = ChessGameController()
        static_dir = os.path.join(
            os.path.dirname(os.path.dirname(__file__)),
            "plugins",
            "Chess",
            "web",
            "static",
        )
        server = ChessWebServer(
            controller=controller,
            static_dir=static_dir,
            port=18991,
        )
        url = server.start()
        try:
            response = urllib.request.urlopen(url + "?v=test", timeout=3)
            html = response.read().decode("utf-8")
            self.assertIn("<title>LAV Chess</title>", html)
            self.assertIn("no-store", response.headers.get("Cache-Control", ""))
        finally:
            server.shutdown()

    def test_invalid_json_post_returns_json_error(self):
        controller = ChessGameController()
        static_dir = os.path.join(
            os.path.dirname(os.path.dirname(__file__)),
            "plugins",
            "Chess",
            "web",
            "static",
        )
        server = ChessWebServer(
            controller=controller,
            static_dir=static_dir,
            port=18990,
        )
        url = server.start()
        try:
            request = urllib.request.Request(
                url + "api/move",
                data=b"{",
                method="POST",
                headers={"Content-Type": "application/json"},
            )
            try:
                response = urllib.request.urlopen(request, timeout=3)
            except urllib.error.HTTPError as error:
                response = error
            payload = json.loads(response.read().decode("utf-8"))
            self.assertFalse(payload["ok"])
            self.assertIn("Chess API error", payload["message"])
        finally:
            server.shutdown()


if __name__ == "__main__":
    unittest.main()
