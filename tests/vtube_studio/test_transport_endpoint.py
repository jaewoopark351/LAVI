#20260904_kpopmodder: Verify localhost transport uses VTube Studio's IPv4 loopback listener.
from plugins.VtubeStudio.vtube_studio_core.connection.vtube_studio_transport_endpoint import (
    resolve_vtube_studio_transport_url,
)


def test_localhost_transport_prefers_ipv4_and_preserves_resource():
    assert resolve_vtube_studio_transport_url(
        "ws://localhost:8001/api?source=lavi"
    ) == "ws://127.0.0.1:8001/api?source=lavi"


def test_explicit_or_remote_hosts_are_not_rewritten():
    endpoints = (
        "ws://127.0.0.1:8001",
        "ws://192.0.2.10:8001",
        "wss://example.test/vtube",
    )

    assert [resolve_vtube_studio_transport_url(value) for value in endpoints] == list(
        endpoints
    )
