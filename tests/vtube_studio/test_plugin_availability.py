#20260904_kpopmodder: Verify VTube Studio static availability no longer probes its runtime port.
import json
import tempfile
from pathlib import Path
from unittest import mock

from plugin_system.contracts import PluginState
from plugin_system.loader import PluginLoader


PROJECT_ROOT = Path(__file__).resolve().parents[2]


def _load_vtube_handle(spec_finder, tcp_connector):
    temp_path = Path(tempfile.mkdtemp(prefix="vtube-availability-"))
    modules_path = temp_path / "modules.json"
    modules_path.write_text(
        json.dumps({"VtubeStudio": True}),
        encoding="utf-8",
    )
    loader = PluginLoader("plugins")
    loader.plugin_setting_path = str(modules_path)
    loader.availability_probe_service.spec_finder = spec_finder
    loader.availability_probe_service.tcp_connector = tcp_connector
    loader._load_plugins_from_directory(
        str(PROJECT_ROOT / "plugins" / "VtubeStudio")
    )
    return loader.plugins["vtuber"][0]


def test_closed_endpoint_is_not_a_provider_startup_dependency():
    tcp_connector = mock.Mock(side_effect=OSError("port closed"))
    handle = _load_vtube_handle(
        spec_finder=lambda package: object(),
        tcp_connector=tcp_connector,
    )

    assert handle.status == PluginState.READY
    assert handle.descriptor.required_services == ()
    assert handle.descriptor.required_python_packages == ("websocket",)
    assert (
        handle.descriptor.config_schema["service"]["websocket_url"]
        == "ws://localhost:8001"
    )
    tcp_connector.assert_not_called()


def test_missing_websocket_package_remains_unavailable():
    tcp_connector = mock.Mock()
    handle = _load_vtube_handle(
        spec_finder=lambda package: None if package == "websocket" else object(),
        tcp_connector=tcp_connector,
    )

    assert handle.status == PluginState.UNAVAILABLE
    assert handle.diagnostic.reason_code == "missing_static_dependency"
    assert handle.diagnostic.missing_python_packages == ("websocket",)
    assert handle.instance is None
    tcp_connector.assert_not_called()
