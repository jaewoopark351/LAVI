# #20260628_kpopmodder: Added a tiny LAN-only map file server for remote HumanJoiner map sync.
# from __future__ import annotations

# import hashlib
# import os
# import threading
# from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
# from typing import Any, Dict
# from urllib.parse import quote, unquote, urlparse


# class SC2MapFileServer:
#     """Serve one configured SC2 map file to LAN HumanJoiner clients."""

#     def __init__(self, host: str = "0.0.0.0"):
#         self.host = host
#         self.port = 0
#         self._server: ThreadingHTTPServer | None = None
#         self._thread: threading.Thread | None = None
#         self._map_path = ""
#         self._metadata: Dict[str, Any] = {}
#         self.last_error = ""

#     def start(self, map_path: str, port: int) -> Dict[str, Any]:
#         path = os.path.abspath(str(map_path or "").strip().strip('"'))
#         if not path or not os.path.isfile(path):
#             self.stop()
#             self.last_error = f"map_file_not_found: {path}"
#             return {"ok": False, "error": self.last_error, "map_path": path}

#         desired_port = int(port or 0)
#         if desired_port <= 0 or desired_port > 65535:
#             self.stop()
#             self.last_error = f"invalid_map_download_port: {desired_port}"
#             return {"ok": False, "error": self.last_error, "map_path": path}

#         metadata = self._build_metadata(path, desired_port)
#         if (
#             self._server is not None
#             and self.port == desired_port
#             and os.path.normcase(self._map_path) == os.path.normcase(path)
#         ):
#             self._metadata = metadata
#             self.last_error = ""
#             return {"ok": True, "already_running": True, **metadata}

#         self.stop()
#         handler = self._handler_for(path, metadata["map_file_name"])
#         try:
#             server = ThreadingHTTPServer((self.host, desired_port), handler)
#         except OSError as exc:
#             self.last_error = str(exc)
#             return {"ok": False, "error": self.last_error, **metadata}

#         self._server = server
#         self._map_path = path
#         self.port = desired_port
#         self._metadata = metadata
#         self.last_error = ""
#         self._thread = threading.Thread(
#             target=server.serve_forever,
#             name="LAVSC2MapFileServer",
#             daemon=True,
#         )
#         self._thread.start()
#         return {"ok": True, **metadata}

#     def stop(self) -> None:
#         server = self._server
#         self._server = None
#         self._thread = None
#         if server is not None:
#             server.shutdown()
#             server.server_close()
#         self._map_path = ""
#         self._metadata = {}
#         self.port = 0

#     def get_status(self) -> Dict[str, Any]:
#         return {
#             "running": self._server is not None,
#             "host": self.host,
#             "port": self.port,
#             "map_path": self._map_path,
#             "metadata": dict(self._metadata),
#             "last_error": self.last_error,
#         }

#     def _build_metadata(self, path: str, port: int) -> Dict[str, Any]:
#         return {
#             "map_file_name": os.path.basename(path),
#             "map_size": os.path.getsize(path),
#             "map_sha256": self._sha256(path),
#             "map_download_port": port,
#             "map_download_path": f"/map/{quote(os.path.basename(path))}",
#             "map_path": path,
#         }

#     def _handler_for(self, map_path: str, map_file_name: str):
#         expected_path = f"/map/{map_file_name}"

#         class MapRequestHandler(BaseHTTPRequestHandler):
#             def do_GET(self):  # noqa: N802 - stdlib handler API
#                 parsed = urlparse(self.path)
#                 requested = unquote(parsed.path)
#                 if requested not in ("/map", expected_path):
#                     self.send_error(404, "map not found")
#                     return
#                 try:
#                     size = os.path.getsize(map_path)
#                     self.send_response(200)
#                     self.send_header("Content-Type", "application/octet-stream")
#                     self.send_header("Content-Length", str(size))
#                     self.send_header(
#                         "Content-Disposition",
#                         f'attachment; filename="{map_file_name}"',
#                     )
#                     self.end_headers()
#                     with open(map_path, "rb") as handle:
#                         while True:
#                             chunk = handle.read(1024 * 1024)
#                             if not chunk:
#                                 break
#                             self.wfile.write(chunk)
#                 except OSError:
#                     self.send_error(500, "map read failed")

#             def log_message(self, format, *args):  # noqa: A002,N802 - stdlib handler API
#                 return

#         return MapRequestHandler

#     @staticmethod
#     def _sha256(path: str) -> str:
#         digest = hashlib.sha256()
#         with open(path, "rb") as handle:
#             while True:
#                 chunk = handle.read(1024 * 1024)
#                 if not chunk:
#                     break
#                 digest.update(chunk)
#         return digest.hexdigest()
