#20260904_kpopmodder: Keep bounded connection-state log formatting separate from transport control.
from core.logger import log_print


def log_connection_event(event, state, level="info", **fields):
    parts = [
        "[VtubeStudio]",
        f"event={_field(event)}",
        f"state={_field(getattr(state, 'value', state))}",
    ]
    for key, value in fields.items():
        if value is not None and value != "":
            parts.append(f"{key}={_field(value)}")
    log_print(" ".join(parts), level=level)


def bounded_reason(error):
    text = str(error or "unknown").replace("\r", " ").replace("\n", " ").strip()
    return (text or "unknown")[:200].replace(" ", "_")


def connection_error_reason(error):
    error_name = type(error).__name__
    if isinstance(error, ConnectionRefusedError):
        return "connection_refused"
    if isinstance(error, TimeoutError) or "Timeout" in error_name:
        return "connection_timeout"
    if error_name in {"WebSocketBadStatusException", "WebSocketProtocolException"}:
        return "websocket_handshake_failed"
    if isinstance(error, OSError):
        return "network_error"
    return "transport_error"


def safe_error_type(error):
    name = type(error).__name__
    return "".join(character for character in name if character.isalnum() or character == "_")[:80]


def _field(value):
    return str(value).replace("\r", " ").replace("\n", " ").replace(" ", "_")
