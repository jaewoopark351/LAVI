#20260905_kpopmodder: Lock the Java WebSocket connection responsibility split.
from __future__ import annotations

import unittest
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[3]
CONNECTION_ROOT = (
    PROJECT_ROOT
    / "plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java"
    / "lavi/minecraft/fabric/chatclef/bridge/transport/connection"
)


class JavaConnectionResponsibilityBoundaryTests(unittest.TestCase):
    def test_listener_lifecycle_is_a_callback_delegation_facade(self):
        source = self._read("FabricChatClefWebSocketConnectionLifecycle.java")

        for delegate in (
            "startup.start",
            "shutdown.stop",
            "handshakeCoordinator.onOpen",
            "textReceiver.onText",
            "detachHandler.onClose",
            "detachHandler.onError",
        ):
            with self.subTest(delegate=delegate):
                self.assertIn(delegate, source)
        for implementation_detail in (
            ".newWebSocketBuilder()",
            ".sendText(",
            "frameAssembler.append",
            "enqueueConnectionDetached",
            "resetForShutdown",
        ):
            with self.subTest(implementation_detail=implementation_detail):
                self.assertNotIn(implementation_detail, source)

    def test_each_connection_effect_lives_in_its_focused_owner(self):
        expectations = {
            "FabricChatClefWebSocketConnectionState.java": (
                "lifecycleState.beginRunning",
                "connectFailureLogGate.shouldLog",
            ),
            "FabricChatClefConnectionLifecycleState.java": (
                "AtomicLong connectionGenerations",
                "activeConnectionGeneration",
            ),
            "FabricChatClefConnectFailureLogGate.java": (
                "lastLoggedConnectFailure",
                "shouldLog",
            ),
            "startup/FabricChatClefConnectionStartup.java": (
                "beginRunning()",
                "reconnectCoordinator.runNow",
            ),
            "startup/FabricChatClefConnectionAttempt.java": (
                ".newWebSocketBuilder()",
                "beginConnecting()",
            ),
            "handshake/FabricChatClefHandshakeSender.java": (
                "envelopeFactory.create",
                "encoder.encode",
                "submission.submit",
                "failureHandler.handle",
            ),
            "handshake/FabricChatClefHandshakeEnvelopeFactory.java": (
                "messageFactory.handshake",
            ),
            "handshake/FabricChatClefHandshakeEncoder.java": (
                "json.encode",
            ),
            "handshake/FabricChatClefHandshakeSubmission.java": (
                "sessionGuard.beginHandshake",
                "socket.sendText",
            ),
            "handshake/FabricChatClefHandshakeFailureHandler.java": (
                "bridgeState.markFailed",
                "reconnectAction.run",
            ),
            "FabricChatClefConnectionTextReceiver.java": (
                "frameAssembler.append",
                "inboundMessageHandler.handle",
            ),
            "FabricChatClefConnectionDetachHandler.java": (
                "currentSocketGuard.evaluate",
                "detachCommit.detachCurrent",
                "detachNotification.closed",
                "detachNotification.failed",
            ),
            "FabricChatClefConnectionDetachNotification.java": (
                "commandQueue.enqueueConnectionDetached",
                "sessionGuard.markConnectionDetached",
            ),
            "FabricChatClefConnectionReconnectCoordinator.java": (
                "reconnectScheduler.runLater",
                "config.reconnectDelay()",
            ),
            "FabricChatClefConnectionShutdown.java": (
                "sendClose",
                "resetForShutdown",
            ),
        }
        for relative_path, fragments in expectations.items():
            with self.subTest(relative_path=relative_path):
                source = self._read(relative_path)
                for fragment in fragments:
                    self.assertIn(fragment, source)

    def test_new_connection_owner_files_have_marker_and_one_public_type(self):
        relative_paths = (
            "FabricChatClefWebSocketConnectionState.java",
            "FabricChatClefConnectionLifecycleState.java",
            "FabricChatClefConnectFailureLogGate.java",
            "startup/FabricChatClefConnectionStartup.java",
            "startup/FabricChatClefConnectionAttempt.java",
            "handshake/FabricChatClefConnectionHandshakeCoordinator.java",
            "handshake/FabricChatClefHandshakeSender.java",
            "handshake/FabricChatClefHandshakeEnvelopeFactory.java",
            "handshake/FabricChatClefHandshakeEncoder.java",
            "handshake/FabricChatClefHandshakeSubmission.java",
            "handshake/FabricChatClefHandshakeFailureHandler.java",
            "FabricChatClefConnectionTextReceiver.java",
            "FabricChatClefConnectionDetachHandler.java",
            "FabricChatClefConnectionCurrentSocketGuard.java",
            "FabricChatClefConnectionDetachAdmission.java",
            "FabricChatClefConnectionDetachCommit.java",
            "FabricChatClefConnectionDetachNotification.java",
            "FabricChatClefConnectionDetachDiagnostics.java",
            "FabricChatClefConnectionReconnectCoordinator.java",
            "FabricChatClefConnectionShutdown.java",
        )
        paths = tuple(CONNECTION_ROOT / path for path in relative_paths)
        self.assertEqual(20, len(paths))
        for path in paths:
            with self.subTest(path=path.relative_to(PROJECT_ROOT)):
                source = path.read_text(encoding="utf-8")
                self.assertTrue(
                    any(
                        line.startswith("//20260905_kpopmodder:")
                        for line in source.splitlines()[:5]
                    )
                )
                self.assertEqual(1, source.count("public final class "))

    @staticmethod
    def _read(relative_path: str) -> str:
        return (CONNECTION_ROOT / relative_path).read_text(encoding="utf-8")


if __name__ == "__main__":
    unittest.main()
