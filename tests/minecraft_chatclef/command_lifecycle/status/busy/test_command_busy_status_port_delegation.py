#20260909_kpopmodder: Pin the new busy STATUS port to direct delegation without legacy fallback.
from __future__ import annotations

import unittest

from plugins.Minecraft.fabric.chatclef.adapter.fabric_chatclef_adapter import (
    FabricChatClefAdapter,
)
from plugins.Minecraft.fabric.chatclef.extension.minecraft_fabric_chatclef_command_feedback_facade import (
    MinecraftFabricChatClefCommandFeedbackFacade,
)
from plugins.Minecraft.fabric.chatclef.transport.server.fabric_chatclef_websocket_server_runtime import (
    FabricChatClefWebSocketServer,
)


class CommandBusyStatusPortDelegationTests(unittest.TestCase):
    def test_facade_passes_the_same_identity_to_only_the_new_port(self):
        identity = object()
        expected = object()
        adapter = _Adapter(expected)
        facade = MinecraftFabricChatClefCommandFeedbackFacade(adapter)

        result = facade.inspect_busy_status(identity)

        self.assertIs(expected, result)
        self.assertEqual([identity], adapter.busy_calls)
        self.assertEqual([], adapter.legacy_calls)

    def test_missing_new_port_does_not_fall_back_to_legacy_status(self):
        adapter = _LegacyOnlyAdapter()
        facade = MinecraftFabricChatClefCommandFeedbackFacade(adapter)

        self.assertIsNone(facade.inspect_busy_status(object()))
        self.assertEqual([], adapter.legacy_calls)

    def test_adapter_and_server_runtime_delegate_the_same_identity(self):
        identity = object()
        expected = object()
        server = _Server(expected)
        adapter = FabricChatClefAdapter.__new__(FabricChatClefAdapter)
        adapter._server = server

        adapter_result = adapter.inspect_command_feedback_busy_status(identity)

        self.assertIs(expected, adapter_result)
        self.assertEqual([identity], server.calls)

        api = _Api(expected)
        runtime = FabricChatClefWebSocketServer.__new__(
            FabricChatClefWebSocketServer
        )
        runtime._command_feedback_api = api

        runtime_result = runtime.inspect_command_feedback_busy_status(identity)

        self.assertIs(expected, runtime_result)
        self.assertEqual([identity], api.calls)


class _Adapter:
    def __init__(self, result) -> None:
        self._result = result
        self.busy_calls = []
        self.legacy_calls = []

    def inspect_command_feedback_busy_status(self, identity):
        self.busy_calls.append(identity)
        return self._result

    def inspect_command_feedback_status(self, query):
        self.legacy_calls.append(query)
        raise AssertionError("legacy STATUS must not be used")


class _LegacyOnlyAdapter:
    def __init__(self) -> None:
        self.legacy_calls = []

    def inspect_command_feedback_status(self, query):
        self.legacy_calls.append(query)
        raise AssertionError("legacy STATUS must not be used")


class _Server:
    def __init__(self, result) -> None:
        self._result = result
        self.calls = []

    def inspect_command_feedback_busy_status(self, identity):
        self.calls.append(identity)
        return self._result


class _Api:
    def __init__(self, result) -> None:
        self._result = result
        self.calls = []

    def inspect_busy_status(self, identity):
        self.calls.append(identity)
        return self._result


if __name__ == "__main__":
    unittest.main()
