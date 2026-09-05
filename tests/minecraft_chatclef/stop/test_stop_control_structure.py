#20260905_kpopmodder: Verify STOP legacy facades and one-responsibility structure.
from __future__ import annotations

import ast
import inspect
import unittest
from pathlib import Path
from types import MappingProxyType

from plugins.Minecraft.fabric.chatclef.input.stop import (
    KoreanStopControlRouteOwner,
    KoreanStopInputClassifier,
    StopControlClaimRegistry,
)
from plugins.Minecraft.fabric.chatclef.response.stop import (
    StopControlResponseRenderer,
)
from plugins.Minecraft.fabric.chatclef.response.trusted_korean.trusted_korean_feedback_route_selector import (
    TrustedKoreanFeedbackRouteSelector,
)
from plugins.Minecraft.fabric.chatclef.transport.control.stop import (
    StopControlResultDemultiplexer,
    StopControlSubmitter,
    StopControlTransitionLogger,
)
from plugins.Minecraft.fabric.chatclef.transport.control.stop.result_handling.stop_control_result_handling_component_graph import (
    StopControlResultHandlingComponentGraph,
)
from plugins.Minecraft.fabric.chatclef.transport.control.stop.submission.stop_control_submission_component_graph import (
    StopControlSubmissionComponentGraph,
)
from plugins.Minecraft.fabric.chatclef.transport.control.stop.submission.send_outcome import (
    StopControlSendDiagnosticStage,
    StopControlSendResultStage,
    StopControlSendStateTransitionStage,
    StopControlTransportDeliveryStage,
)
from plugins.Minecraft.fabric.chatclef.transport.control.stop.submission.send_outcome.stop_control_send_outcome_component_graph import (
    StopControlSendOutcomeComponentGraph,
)
from plugins.Minecraft.fabric.chatclef.transport.control.stop.stop_control_terminal_result_validator import (
    StopControlTerminalResultValidator,
)


PROJECT_ROOT = Path(__file__).resolve().parents[3]
STOP_INPUT_ROOT = (
    PROJECT_ROOT / "plugins/Minecraft/fabric/chatclef/input/stop"
)
STOP_TRANSPORT_ROOT = (
    PROJECT_ROOT / "plugins/Minecraft/fabric/chatclef/transport/control/stop"
)
STOP_RESPONSE_ROOT = (
    PROJECT_ROOT / "plugins/Minecraft/fabric/chatclef/response/stop"
)
TRUSTED_RESPONSE_ROOT = (
    PROJECT_ROOT / "plugins/Minecraft/fabric/chatclef/response/trusted_korean"
)

NEW_STRUCTURE_ROOTS = (
    STOP_INPUT_ROOT / "claims",
    STOP_INPUT_ROOT / "routing",
    STOP_TRANSPORT_ROOT / "diagnostics",
    STOP_TRANSPORT_ROOT / "result_handling",
    STOP_TRANSPORT_ROOT / "submission",
    STOP_TRANSPORT_ROOT / "validation",
    TRUSTED_RESPONSE_ROOT / "selection",
)

LEGACY_FACADE_PATHS = (
    STOP_INPUT_ROOT / "korean_stop_control_route_owner.py",
    STOP_INPUT_ROOT / "stop_control_claim_registry.py",
    STOP_TRANSPORT_ROOT / "stop_control_result_demultiplexer.py",
    STOP_TRANSPORT_ROOT / "stop_control_submitter.py",
    STOP_TRANSPORT_ROOT / "stop_control_terminal_result_validator.py",
    STOP_TRANSPORT_ROOT / "stop_control_transition_logger.py",
    TRUSTED_RESPONSE_ROOT / "trusted_korean_command_feedback_facade.py",
)


class StopControlStructureTests(unittest.TestCase):
    def test_legacy_public_classes_remain_at_exact_import_paths(self):
        expected_modules = {
            KoreanStopControlRouteOwner: (
                "plugins.Minecraft.fabric.chatclef.input.stop."
                "korean_stop_control_route_owner"
            ),
            StopControlClaimRegistry: (
                "plugins.Minecraft.fabric.chatclef.input.stop."
                "stop_control_claim_registry"
            ),
            StopControlResultDemultiplexer: (
                "plugins.Minecraft.fabric.chatclef.transport.control.stop."
                "stop_control_result_demultiplexer"
            ),
            StopControlSubmitter: (
                "plugins.Minecraft.fabric.chatclef.transport.control.stop."
                "stop_control_submitter"
            ),
            StopControlTerminalResultValidator: (
                "plugins.Minecraft.fabric.chatclef.transport.control.stop."
                "stop_control_terminal_result_validator"
            ),
            StopControlTransitionLogger: (
                "plugins.Minecraft.fabric.chatclef.transport.control.stop."
                "stop_control_transition_logger"
            ),
        }
        for public_type, module_name in expected_modules.items():
            with self.subTest(public_type=public_type.__name__):
                self.assertEqual(module_name, public_type.__module__)

    def test_legacy_behavior_methods_are_thin_delegates(self):
        delegates = (
            (StopControlSubmitter.submit, "self._coordinator.submit"),
            (StopControlResultDemultiplexer.handle, "self._coordinator.handle"),
            (
                StopControlTerminalResultValidator.inspect,
                "self._coordinator.inspect",
            ),
            (KoreanStopControlRouteOwner.route, "self._coordinator.route"),
            (KoreanStopControlRouteOwner.try_route, "self._coordinator.try_route"),
            (StopControlClaimRegistry.issue, "self._state_lifecycle.issue"),
            (StopControlClaimRegistry.spend, "self._state_lifecycle.spend"),
        )
        for method, delegate_call in delegates:
            with self.subTest(method=method.__qualname__):
                self.assertIn(delegate_call, inspect.getsource(method))

    def test_new_production_modules_have_marker_tuple_exports_and_one_class(self):
        paths = list(LEGACY_FACADE_PATHS)
        paths.extend(
            path
            for root in NEW_STRUCTURE_ROOTS
            for path in root.rglob("*.py")
        )
        paths.extend((STOP_INPUT_ROOT / "__init__.py", STOP_TRANSPORT_ROOT / "__init__.py"))
        for path in paths:
            with self.subTest(path=path.relative_to(PROJECT_ROOT)):
                source = path.read_text(encoding="utf-8")
                self.assertIn(
                    "#20260905_kpopmodder",
                    "\n".join(source.splitlines()[:5]),
                )
                tree = ast.parse(source)
                classes = [
                    node for node in tree.body if isinstance(node, ast.ClassDef)
                ]
                self.assertLessEqual(len(classes), 1)
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

    def test_stop_policy_tables_have_no_mutable_class_literal(self):
        for root in (STOP_INPUT_ROOT, STOP_TRANSPORT_ROOT, STOP_RESPONSE_ROOT):
            for path in root.rglob("*.py"):
                tree = ast.parse(path.read_text(encoding="utf-8"))
                for class_node in (
                    node for node in tree.body if isinstance(node, ast.ClassDef)
                ):
                    for statement in class_node.body:
                        value = None
                        if isinstance(statement, ast.Assign):
                            value = statement.value
                        elif isinstance(statement, ast.AnnAssign):
                            value = statement.value
                        with self.subTest(
                            path=path.relative_to(PROJECT_ROOT),
                            class_name=class_node.name,
                        ):
                            self.assertNotIsInstance(
                                value,
                                (ast.Dict, ast.List, ast.Set),
                            )

        self.assertIsInstance(
            StopControlTerminalResultValidator._SUCCESS_PROFILES,
            MappingProxyType,
        )
        self.assertIsInstance(KoreanStopInputClassifier._PHRASES, MappingProxyType)
        self.assertIsInstance(StopControlResponseRenderer._LOCAL, MappingProxyType)
        self.assertIsInstance(StopControlResponseRenderer._WIRE, MappingProxyType)
        with self.assertRaises(TypeError):
            StopControlTerminalResultValidator._SUCCESS_PROFILES[
                "tracked_active_stopped"
            ] = ()

    def test_composition_and_send_outcome_types_have_exact_responsibilities(self):
        self.assertEqual(
            "StopControlSubmissionComponentGraph",
            StopControlSubmissionComponentGraph.__name__,
        )
        self.assertEqual(
            "StopControlResultHandlingComponentGraph",
            StopControlResultHandlingComponentGraph.__name__,
        )
        self.assertEqual(
            "StopControlSendOutcomeComponentGraph",
            StopControlSendOutcomeComponentGraph.__name__,
        )
        stage_types = (
            StopControlTransportDeliveryStage,
            StopControlSendStateTransitionStage,
            StopControlSendDiagnosticStage,
            StopControlSendResultStage,
        )
        self.assertEqual(4, len({stage.__module__ for stage in stage_types}))
        self.assertEqual(
            "TrustedKoreanFeedbackRouteSelector",
            TrustedKoreanFeedbackRouteSelector.__name__,
        )

    def test_legacy_constructors_only_delegate_component_graph_assembly(self):
        submitter_source = inspect.getsource(StopControlSubmitter.__init__)
        demultiplexer_source = inspect.getsource(
            StopControlResultDemultiplexer.__init__
        )

        self.assertIn("StopControlSubmissionComponentGraph(", submitter_source)
        self.assertNotIn("StopControlConnectionAdmissionGate(", submitter_source)
        self.assertIn(
            "StopControlResultHandlingComponentGraph(",
            demultiplexer_source,
        )
        self.assertNotIn(
            "StopControlTerminalStateCoordinator(",
            demultiplexer_source,
        )


if __name__ == "__main__":
    unittest.main()
