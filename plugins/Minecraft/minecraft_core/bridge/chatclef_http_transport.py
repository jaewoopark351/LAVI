#20260725_kpopmodder: Added transport-only HTTP logic for the local ChatClef bridge.
from __future__ import annotations

import json
from typing import Any, Dict
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen


class ChatClefHttpTransport:
    def __init__(
        self,
        base_url: str = "http://127.0.0.1:4316",
        timeout_sec: float = 3.0,
        opener=None,
    ):
        self.base_url = str(base_url or "http://127.0.0.1:4316").rstrip("/")
        self.timeout_sec = float(timeout_sec or 3.0)
        self.opener = opener or urlopen

    def request_json(
        self,
        method: str,
        path: str,
        payload: Dict[str, Any] | None = None,
    ) -> Dict[str, Any]:
        url = self._url(path)
        request = self._build_request(method, url, payload)

        try:
            with self.opener(request, timeout=self.timeout_sec) as response:
                return self._decode_response(response.read(), url=url)
        except HTTPError as error:
            result = self._decode_response(error.read(), url=url)
            result.setdefault("ok", False)
            result.setdefault("error", "http_error")
            result.setdefault("status_code", error.code)
            result.setdefault("message", str(error))
            return result
        except TimeoutError as error:
            return self._connection_error(url, "bridge_timeout", error)
        except URLError as error:
            reason = getattr(error, "reason", error)
            return self._connection_error(url, "bridge_unreachable", reason)
        except OSError as error:
            return self._connection_error(url, "bridge_unreachable", error)

    def _build_request(
        self,
        method: str,
        url: str,
        payload: Dict[str, Any] | None = None,
    ) -> Request:
        data = None
        headers = {"Accept": "application/json"}
        if payload is not None:
            data = json.dumps(payload).encode("utf-8")
            headers["Content-Type"] = "application/json"

        return Request(
            url,
            data=data,
            headers=headers,
            method=str(method or "GET").upper(),
        )

    def _url(self, path: str) -> str:
        return f"{self.base_url}/{str(path or '').lstrip('/')}"

    def _decode_response(self, raw: bytes, *, url: str) -> Dict[str, Any]:
        text = raw.decode("utf-8", errors="replace") if raw else ""
        if not text.strip():
            return {"ok": True, "url": url}
        try:
            decoded = json.loads(text)
        except json.JSONDecodeError:
            return {
                "ok": False,
                "error": "invalid_json",
                "message": "ChatClef bridge returned non-JSON response.",
                "raw": text,
                "url": url,
            }
        if isinstance(decoded, dict):
            decoded.setdefault("url", url)
            return decoded
        return {
            "ok": False,
            "error": "invalid_json",
            "message": "ChatClef bridge JSON response must be an object.",
            "raw": decoded,
            "url": url,
        }

    def _connection_error(self, url: str, code: str, error: Any) -> Dict[str, Any]:
        return {
            "ok": False,
            "error": code,
            "message": str(error),
            "url": url,
        }
