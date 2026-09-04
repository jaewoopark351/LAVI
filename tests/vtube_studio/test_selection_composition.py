#20260904_kpopmodder: Verify the real Vtuber selection path survives an unavailable VTube Studio endpoint.
import json
import sys
import tempfile
import time
from pathlib import Path
from types import ModuleType
from unittest import mock

import plugin_system.selection as selection_module
from app_core.composition_core.core_component_composition_service import (
    CoreComponentCompositionService,
)
from plugin_system.contracts import PluginState
from plugin_system.loader import PluginLoader
from plugins.VtubeStudio.vtube_studio_core.connection import (
    vtube_studio_connection as connection_module,
)
from plugins.VtubeStudio.vtube_studio_core.connection.vtube_studio_connection_state import (
    VTubeStudioConnectionState,
)
from plugins.VtubeStudio.vtube_studio_core.ui.vtube_studio_ui import VTubeStudioUI
from vtuber_core.vtuber_component import Vtuber

from .fakes.refusing_websocket_session import RefusingWebSocketSession
from .fakes.stub_component import StubComponent


PROJECT_ROOT = Path(__file__).resolve().parents[2]

def _isolated_vtube_loader(tcp_connector):
    temp_path = Path(tempfile.mkdtemp(prefix="vtube-selection-"))
    modules_path = temp_path / "modules.json"
    modules_path.write_text(
        json.dumps({"VtubeStudio": True}),
        encoding="utf-8",
    )
    loader = PluginLoader("plugins")
    loader.plugin_setting_path = str(modules_path)
    loader.availability_probe_service.spec_finder = lambda package: object()
    loader.availability_probe_service.tcp_connector = tcp_connector
    loader._load_plugins_from_directory(
        str(PROJECT_ROOT / "plugins" / "VtubeStudio")
    )
    return loader


def test_vtuber_selection_composes_while_endpoint_is_unavailable(monkeypatch):
    RefusingWebSocketSession.attempts = 0
    RefusingWebSocketSession.attempted.clear()
    tcp_connector = mock.Mock(side_effect=AssertionError("port probe is forbidden"))
    loader = _isolated_vtube_loader(tcp_connector)
    monkeypatch.setattr(selection_module, "plugin_loader", loader)
    monkeypatch.setattr(
        connection_module,
        "VTubeStudioWebSocketSession",
        RefusingWebSocketSession,
    )
    monkeypatch.setattr(VTubeStudioUI, "show_authentication_info", lambda self: None)

    selection = None
    plugin = None
    try:
        selection = Vtuber()
        plugin = selection.get_current_plugin()

        assert plugin is not None
        assert selection.current_provider.name == "VtubeStudio"
        assert selection.current_provider.initialized is True
        assert loader.plugins["vtuber"][0].status == PluginState.RUNNING
        assert RefusingWebSocketSession.attempted.wait(1.0)

        deadline = time.monotonic() + 1.0
        while (
            plugin.connection.state
            != VTubeStudioConnectionState.DISCONNECTED_WAIT
            and time.monotonic() < deadline
        ):
            time.sleep(0.001)

        assert plugin.connection.state == VTubeStudioConnectionState.DISCONNECTED_WAIT
        assert plugin.connection.worker_alive is True
        assert RefusingWebSocketSession.attempts == 1
        tcp_connector.assert_not_called()
    finally:
        if selection is not None:
            selection.shutdown()

    assert plugin.connection.worker_alive is False
    assert plugin.connection.state == VTubeStudioConnectionState.STOPPED


def test_core_component_composition_survives_refused_vtube_endpoint(monkeypatch):
    RefusingWebSocketSession.attempts = 0
    RefusingWebSocketSession.attempted.clear()
    tcp_connector = mock.Mock(side_effect=AssertionError("port probe is forbidden"))
    loader = _isolated_vtube_loader(tcp_connector)
    monkeypatch.setattr(selection_module, "plugin_loader", loader)
    monkeypatch.setattr(
        connection_module,
        "VTubeStudioWebSocketSession",
        RefusingWebSocketSession,
    )
    monkeypatch.setattr(VTubeStudioUI, "show_authentication_info", lambda self: None)

    module_contracts = {
        "input_core.input_component": ("Input", StubComponent),
        "llm_core.llm_component": ("LLM", StubComponent),
        "translation_core.translate_component": ("Translate", StubComponent),
        "tts_core.tts_component": ("TTS", StubComponent),
    }
    for module_name, (class_name, component_type) in module_contracts.items():
        module = ModuleType(module_name)
        setattr(module, class_name, component_type)
        monkeypatch.setitem(sys.modules, module_name, module)

    result = None
    plugin = None
    try:
        result = CoreComponentCompositionService().compose()
        plugin = result.vtuber.get_current_plugin()

        assert plugin is not None
        assert RefusingWebSocketSession.attempted.wait(1.0)
        deadline = time.monotonic() + 1.0
        while (
            plugin.connection.state
            != VTubeStudioConnectionState.DISCONNECTED_WAIT
            and time.monotonic() < deadline
        ):
            time.sleep(0.001)
        assert plugin.connection.state == VTubeStudioConnectionState.DISCONNECTED_WAIT
        assert len(result.core_components) == 5
        tcp_connector.assert_not_called()
    finally:
        if result is not None:
            result.vtuber.shutdown()

    assert plugin is not None
    assert plugin.connection.state == VTubeStudioConnectionState.STOPPED
