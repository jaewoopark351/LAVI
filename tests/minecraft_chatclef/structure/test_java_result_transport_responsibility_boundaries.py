#20260905_kpopmodder: Lock the Java command-result transport responsibility split.
from __future__ import annotations

import unittest
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[3]
JAVA_ROOT = (
    PROJECT_ROOT
    / "plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java"
    / "lavi/minecraft/fabric/chatclef/bridge/transport"
)
RESULT_ROOT = JAVA_ROOT / "result"


class JavaResultTransportResponsibilityBoundaryTests(unittest.TestCase):
    def test_public_sender_is_a_transport_sequence_facade(self):
        source = self._read(JAVA_ROOT / "FabricChatClefResultEnvelopeSender.java")

        for delegate in (
            "admission.evaluate",
            "encoder.encode",
            "transportSubmission.submit",
        ):
            with self.subTest(delegate=delegate):
                self.assertIn(delegate, source)
        for implementation_detail in (
            ".sendText(",
            "json.encode",
            ".whenComplete(",
            "diagnostics.warn",
            "completionSink.accept",
        ):
            with self.subTest(implementation_detail=implementation_detail):
                self.assertNotIn(implementation_detail, source)

    def test_result_effects_live_in_focused_collaborators(self):
        expectations = {
            "FabricChatClefResultSendAdmission.java": (
                "NO_SOCKET",
                "GENERATION_MISMATCH",
            ),
            "FabricChatClefResultEnvelopeEncoder.java": (
                "envelopeBuilder.build",
                "jsonEncoder.encode",
            ),
            "FabricChatClefResultEnvelopeBuilder.java": (
                "envelopeFactory.commandResult",
            ),
            "FabricChatClefResultJsonEncoder.java": (
                "json.encode",
                "diagnostics.encodeFailed",
            ),
            "FabricChatClefResultTransportSubmission.java": (
                "socket.sendText",
                "asyncCompletion.complete",
            ),
            "FabricChatClefResultAsyncCompletion.java": (
                "asyncSendFailed",
                "completionRouter.complete",
            ),
            "FabricChatClefResultSendCompletionRouter.java": (
                "ordinaryCompletionSink.accept",
                "stopCompletion.accept",
            ),
            "FabricChatClefResultSendDiagnostics.java": ("diagnostics.warn",),
        }
        for filename, fragments in expectations.items():
            with self.subTest(filename=filename):
                source = self._read(RESULT_ROOT / filename)
                for fragment in fragments:
                    self.assertIn(fragment, source)

    def test_result_package_has_no_single_file_terminal_subpackage(self):
        self.assertEqual([], [path for path in RESULT_ROOT.iterdir() if path.is_dir()])
        paths = tuple(RESULT_ROOT.glob("*.java"))
        self.assertGreaterEqual(len(paths), 10)
        for path in paths:
            with self.subTest(path=path.relative_to(PROJECT_ROOT)):
                source = self._read(path)
                self.assertTrue(
                    any(
                        line.startswith("//20260905_kpopmodder:")
                        for line in source.splitlines()[:5]
                    )
                )
                self.assertEqual(1, source.count("public final class "))

    @staticmethod
    def _read(path: Path) -> str:
        return path.read_text(encoding="utf-8")


if __name__ == "__main__":
    unittest.main()
