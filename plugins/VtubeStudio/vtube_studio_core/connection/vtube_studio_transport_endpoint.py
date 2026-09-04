#20260904_kpopmodder: Resolve the local VTube Studio transport without changing its configured endpoint.
from urllib.parse import urlsplit, urlunsplit


def resolve_vtube_studio_transport_url(websocket_url):
    """Prefer the IPv4 loopback used by VTube Studio for a localhost endpoint."""
    try:
        endpoint = urlsplit(websocket_url)
        if endpoint.scheme.lower() not in ("ws", "wss"):
            return websocket_url
        if endpoint.hostname is None or endpoint.hostname.casefold() != "localhost":
            return websocket_url
        if endpoint.username is not None or endpoint.password is not None:
            return websocket_url
        port = endpoint.port
    except (AttributeError, TypeError, ValueError):
        return websocket_url

    transport_netloc = "127.0.0.1"
    if port is not None:
        transport_netloc = f"{transport_netloc}:{port}"
    return urlunsplit((
        endpoint.scheme,
        transport_netloc,
        endpoint.path,
        endpoint.query,
        endpoint.fragment,
    ))
