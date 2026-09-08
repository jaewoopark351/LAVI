#20260907_kpopmodder: Lock the command-result handler responsibility split.
from __future__ import annotations

import inspect
import threading
import unittest
from unittest.mock import Mock

from plugins.Minecraft.fabric.chatclef.transport.command_submission.fabric_chatclef_command_result_handler import (
    FabricChatClefCommandResultHandler,
)
from plugins.Minecraft.fabric.chatclef.transport.command_submission.result_handling.delivery import (
    FabricChatClefCommandResultTerminalDelivery,
)
from plugins.Minecraft.fabric.chatclef.transport.command_submission.result_handling.diagnostics import (
    FabricChatClefCommandResultDiagnostics,
)
from plugins.Minecraft.fabric.chatclef.transport.command_submission.result_handling.lifecycle import (
    FabricChatClefCommandResultLifecycleProjector,
)
from plugins.Minecraft.fabric.chatclef.transport.command_submission.result_handling.parsing import (
    FabricChatClefCommandResultParser,
)


class CommandResultHandlerResponsibilityBoundaryTests(unittest.TestCase):
    def test_handler_installs_responsibility_named_collaborators(self):
        handler = FabricChatClefCommandResultHandler(
            connection_ownership=Mock(),
            command_lock=threading.RLock(),
            diagnostics=Mock(),
            crafting_feedback_result_coordinator=Mock(),
            crafting_feedback_terminal_delivery=Mock(),
        )

        self.assertIsInstance(handler._result_parser, FabricChatClefCommandResultParser)
        self.assertIsInstance(
            handler._result_diagnostics,
            FabricChatClefCommandResultDiagnostics,
        )
        self.assertIsInstance(
            handler._lifecycle_projector,
            FabricChatClefCommandResultLifecycleProjector,
        )
        self.assertIsInstance(
            handler._terminal_delivery,
            FabricChatClefCommandResultTerminalDelivery,
        )

    def test_handler_source_contains_only_result_flow_orchestration(self):
        source = inspect.getsource(FabricChatClefCommandResultHandler)

        self.assertNotIn("json.dumps", source)
        self.assertNotIn("CommandResultDTO", source)
        self.assertNotIn("ignored malformed command result", source)
        self.assertNotIn("ignored command result", source)
        self.assertNotIn("command result ", source)


if __name__ == "__main__":
    unittest.main()
