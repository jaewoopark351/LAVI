#20260819_kpopmodder: Read one explicit loopback endpoint from the live gateway.
from __future__ import annotations

from .loopback_endpoint import inspect_loopback_gradio_url


def inspect_gateway_endpoint(gateway: object) -> dict[str, object]:
    try:
        raw_url = getattr(gateway, "gradio_url")
    except Exception as error:
        return {
            "ok": False,
            "reason": (
                "live gateway endpoint is unavailable: "
                f"{type(error).__name__}: {error}"
            ),
        }
    result = inspect_loopback_gradio_url(raw_url)
    if not result.get("ok"):
        return {
            "ok": False,
            "reason": str(
                result.get("reason") or "live gateway endpoint is invalid"
            ),
        }
    return {"ok": True, "reason": "gateway_endpoint_validated", "url": result["url"]}
