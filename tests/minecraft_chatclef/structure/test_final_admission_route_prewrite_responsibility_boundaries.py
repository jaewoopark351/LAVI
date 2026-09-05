#20260905_kpopmodder: Lock the final admission, H5 route, and STOP prewrite splits.
from __future__ import annotations

import ast
import inspect
import unittest
from pathlib import Path

from plugins.Minecraft.fabric.chatclef.command_registry.admission.korean_command import (
    KoreanAutoDepositTrustSubmissionPolicy,
)
from plugins.Minecraft.fabric.chatclef.command_registry.admission.korean_command.auto_deposit_trust.korean_auto_deposit_trust_inspector import (
    KoreanAutoDepositTrustInspector,
)
from plugins.Minecraft.fabric.chatclef.command_registry.admission.korean_command.auto_deposit_trust.korean_auto_deposit_trust_claim_lifecycle import (
    KoreanAutoDepositTrustClaimLifecycle,
)
from plugins.Minecraft.fabric.chatclef.input.auto_deposit_trust.routing import (
    AutoDepositTrustRouteCoordinator,
)
from plugins.Minecraft.fabric.chatclef.input.auto_deposit_trust.routing.auto_deposit_trust_route_execution_lifecycle import (
    AutoDepositTrustRouteExecutionLifecycle,
)
from plugins.Minecraft.fabric.chatclef.input.auto_deposit_trust.routing.auto_deposit_trust_route_sequence import (
    AutoDepositTrustRouteSequence,
)
from plugins.Minecraft.fabric.chatclef.transport.control.stop.submission.prewrite.stop_control_prewrite_diagnostic_reporter import (
    StopControlPrewriteDiagnosticReporter,
)
from plugins.Minecraft.fabric.chatclef.transport.control.stop.submission.prewrite.stop_control_prewrite_result_policy import (
    StopControlPrewriteResultPolicy,
)
from plugins.Minecraft.fabric.chatclef.transport.control.stop.submission.stop_control_submission_coordinator import (
    StopControlSubmissionCoordinator,
)


PROJECT_ROOT = Path(__file__).resolve().parents[3]
SPLIT_ROOTS = (
    PROJECT_ROOT
    / "plugins/Minecraft/fabric/chatclef/input/auto_deposit_trust/routing/lifecycle",
    PROJECT_ROOT
    / "plugins/Minecraft/fabric/chatclef/input/auto_deposit_trust/routing/orchestration",
    PROJECT_ROOT
    / "plugins/Minecraft/fabric/chatclef/command_registry/admission/korean_command/auto_deposit_trust",
    PROJECT_ROOT
    / "plugins/Minecraft/fabric/chatclef/transport/control/stop/submission/prewrite/diagnostics",
    PROJECT_ROOT
    / "plugins/Minecraft/fabric/chatclef/transport/control/stop/submission/prewrite/outcome",
)


class FinalAdmissionRoutePrewriteResponsibilityBoundaryTests(unittest.TestCase):
    def test_h5_route_facade_delegates_lifecycle_and_preclaimed_sequence(self):
        route_source = inspect.getsource(AutoDepositTrustRouteCoordinator.route)
        claimed_source = inspect.getsource(
            AutoDepositTrustRouteCoordinator.route_claimed
        )

        self.assertIn("self._execution_lifecycle.execute", route_source)
        self.assertIn("self._route_sequence.route_claimed", claimed_source)
        self.assertEqual(
            2,
            len(
                {
                    AutoDepositTrustRouteExecutionLifecycle.__module__,
                    AutoDepositTrustRouteSequence.__module__,
                }
            ),
        )

    def test_h5_admission_facade_separates_inspection_from_claim_lifecycle(self):
        inspect_source = inspect.getsource(
            KoreanAutoDepositTrustSubmissionPolicy.inspect
        )
        commit_source = inspect.getsource(
            KoreanAutoDepositTrustSubmissionPolicy.commit
        )
        abandon_source = inspect.getsource(
            KoreanAutoDepositTrustSubmissionPolicy.abandon_if_issued
        )

        self.assertIn("self._inspector.inspect", inspect_source)
        self.assertIn("self._claim_lifecycle.commit", commit_source)
        self.assertIn("self._claim_lifecycle.abandon_if_issued", abandon_source)
        self.assertNotEqual(
            KoreanAutoDepositTrustInspector.__module__,
            KoreanAutoDepositTrustClaimLifecycle.__module__,
        )

    def test_stop_submission_sequences_diagnostics_and_result_policy(self):
        source = inspect.getsource(StopControlSubmissionCoordinator.submit)

        self.assertIn("self._prewrite_diagnostic_reporter.report", source)
        self.assertIn("self._prewrite_result_policy.resolve", source)
        self.assertNotEqual(
            StopControlPrewriteDiagnosticReporter.__module__,
            StopControlPrewriteResultPolicy.__module__,
        )

    def test_new_production_modules_have_marker_tuple_exports_and_one_class(self):
        paths = tuple(
            path for root in SPLIT_ROOTS for path in root.rglob("*.py")
        )
        self.assertTrue(paths)
        for path in paths:
            with self.subTest(path=path.relative_to(PROJECT_ROOT)):
                source = path.read_text(encoding="utf-8")
                self.assertIn(
                    "#20260905_kpopmodder",
                    "\n".join(source.splitlines()[:5]),
                )
                tree = ast.parse(source)
                self.assertLessEqual(
                    len(
                        [
                            node
                            for node in tree.body
                            if isinstance(node, ast.ClassDef)
                        ]
                    ),
                    1,
                )
                exports = [
                    node
                    for node in tree.body
                    if isinstance(node, ast.Assign)
                    and any(
                        isinstance(target, ast.Name) and target.id == "__all__"
                        for target in node.targets
                    )
                ]
                self.assertEqual(1, len(exports))
                self.assertIsInstance(exports[0].value, ast.Tuple)


if __name__ == "__main__":
    unittest.main()
