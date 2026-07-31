#20260801_kpopmodder: Verify Phase 1 Fabric ChatClef adapter fails closed.
import importlib
import sys
import threading
import unittest

from plugins.Minecraft.common.dto.command_request_dto import CommandRequestDTO
from plugins.Minecraft.common.protocol.bridge_error_code import BridgeErrorCode
from plugins.Minecraft.common.protocol.bridge_lifecycle_state import (
    BridgeLifecycleState,
)
from plugins.Minecraft.common.protocol.command_result_status import (
    CommandResultStatus,
)


ADAPTER_MODULE = (
    "plugins.Minecraft.fabric.chatclef.adapter.fabric_chatclef_adapter"
)


class MinecraftFabricChatClefAdapterSkeletonTests(unittest.TestCase):
    def test_import_does_not_start_thread_or_socket_module(self):
        sys.modules.pop(ADAPTER_MODULE, None)
        before_threads = threading.active_count()

        module = importlib.import_module(ADAPTER_MODULE)

        self.assertEqual(before_threads, threading.active_count())
        self.assertFalse(hasattr(module, "socket"))
        self.assertFalse(hasattr(module, "websockets"))

    def test_default_status_is_fail_closed_not_fake_connected(self):
        from plugins.Minecraft.fabric.chatclef.adapter.fabric_chatclef_adapter import (
            FabricChatClefAdapter,
        )

        adapter = FabricChatClefAdapter()
        status = adapter.get_status()

        self.assertEqual("fabric_chatclef", adapter.backend_id)
        self.assertFalse(status.enabled)
        self.assertFalse(status.connected)
        self.assertEqual(BridgeLifecycleState.NOT_IMPLEMENTED, status.lifecycle_state)
        self.assertIsNone(status.last_error_code)
        self.assertIsNone(status.last_error_message)

    def test_submit_command_returns_not_implemented_rejection(self):
        from plugins.Minecraft.fabric.chatclef.adapter.fabric_chatclef_adapter import (
            FabricChatClefAdapter,
        )

        adapter = FabricChatClefAdapter()
        result = adapter.submit_command(
            CommandRequestDTO(request_id="cmd-1", command="@get dirt 1")
        )

        self.assertFalse(result.ok)
        self.assertEqual("cmd-1", result.request_id)
        self.assertEqual(CommandResultStatus.REJECTED, result.status)
        self.assertEqual(BridgeErrorCode.NOT_IMPLEMENTED, result.error_code)
        self.assertEqual({}, result.data)


if __name__ == "__main__":
    unittest.main()
