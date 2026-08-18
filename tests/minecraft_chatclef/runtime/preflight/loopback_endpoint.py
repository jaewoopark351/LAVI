#20260818_kpopmodder: Validate the explicit loopback Gradio endpoint for mutating tests.
from __future__ import annotations

from urllib.parse import urlsplit


def inspect_loopback_gradio_url(url: object) -> dict[str, object]:
    text = str(url or "").strip()
    if not text:
        return _failure("explicit LAVI_GRADIO_URL is required")
    try:
        parsed = urlsplit(text)
        port = parsed.port
    except ValueError as error:
        return _failure(f"invalid Gradio URL: {error}")
    if parsed.scheme != "http":
        return _failure("Gradio URL must use http")
    if parsed.username is not None or parsed.password is not None:
        return _failure("Gradio URL must not contain user information")
    if parsed.hostname not in {"127.0.0.1", "::1"}:
        return _failure("Gradio URL must use an explicit loopback address")
    if port is None:
        return _failure("Gradio URL must include an explicit port")
    if parsed.path not in {"", "/"} or parsed.query or parsed.fragment:
        return _failure("Gradio URL must identify only the loopback origin")
    host = f"[{parsed.hostname}]" if parsed.hostname == "::1" else parsed.hostname
    return {
        "ok": True,
        "reason": "validated_loopback_endpoint",
        "url": f"http://{host}:{port}",
        "host": parsed.hostname,
        "port": port,
    }


def _failure(reason: str) -> dict[str, object]:
    return {"ok": False, "reason": reason, "url": "", "host": "", "port": None}
