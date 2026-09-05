#20260905_kpopmodder: Enforce focused trusted-input and direct-game responsibility splits.
from __future__ import annotations

import ast
import unittest
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]


class FeatureResponsibilitySplitStructureTests(unittest.TestCase):
    def test_new_production_modules_have_marker_one_class_and_immutable_statics(self):
        roots = (
            PROJECT_ROOT / "input_core/input_event/delivery/trusted_voice",
            PROJECT_ROOT / "llm_core/chat_input/trusted_local",
            PROJECT_ROOT / "llm_core/input_routing/composition",
            PROJECT_ROOT / "llm_core/input_routing/decision",
            PROJECT_ROOT / "llm_core/input_routing/diagnostics",
            PROJECT_ROOT / "llm_core/input_routing/invocation",
            PROJECT_ROOT / "llm_core/input_routing/outcomes",
            PROJECT_ROOT / "llm_core/input_routing/publication",
            PROJECT_ROOT / "llm_core/input_routing/yielding",
            PROJECT_ROOT / "llm_core/trusted_ingress/runtime",
            PROJECT_ROOT
            / "app_core/composition_core/component_wiring/direct_game_input",
        )
        modules = tuple(
            path
            for root in roots
            for path in root.rglob("*.py")
            if path.name != "__init__.py"
        )

        self.assertGreaterEqual(len(modules), 30)
        for path in modules:
            source = path.read_text(encoding="utf-8-sig")
            tree = ast.parse(source, filename=str(path))
            classes = tuple(
                node for node in tree.body if isinstance(node, ast.ClassDef)
            )
            self.assertTrue(
                any(
                    line.startswith("#20260905_kpopmodder")
                    for line in source.splitlines()[:5]
                ),
                path,
            )
            self.assertEqual(1, len(classes), path)
            self.assertFalse(_mutable_static_literals(tree), path)
            self.assertTrue(_uses_tuple_all(tree), path)

    def test_compatibility_facades_do_not_reclaim_extracted_responsibilities(self):
        expectations = (
            (
                "input_core/input_event/delivery/"
                "trusted_voice_input_final_enqueue_coordinator.py",
                (
                    "InputProviderSourcePolicy",
                    "begin_invocation",
                    "offer_registered",
                    ".abandon(",
                    "type(error)",
                ),
            ),
            (
                "llm_core/chat_input/"
                "trusted_local_chat_input_dispatch_coordinator.py",
                (
                    "InputProviderSourcePolicy",
                    "begin_invocation",
                    "create_registered_delivery",
                    "created_event",
                    ".abandon(",
                ),
            ),
            (
                "llm_core/input_routing/routed_input_dispatch_coordinator.py",
                (
                    "route_trusted_user_input",
                    "emit_capability_response",
                    "log_chat_ui_delivery",
                    "RoutedResponseEmission",
                    "_log_callback",
                ),
            ),
            (
                "llm_core/trusted_ingress/llm_trusted_ingress_graph.py",
                (
                    ".claim_graph.validate_consumed_evidence(",
                    ".lease_dispatch_coordinator.accept_registered(",
                    ".lease_dispatch_coordinator.accept_queued(",
                    ".lease_dispatch_coordinator.dispatch_lease(",
                    ".voice_enqueue_factory.create(",
                ),
            ),
            (
                "app_core/composition_core/component_wiring/"
                "direct_game_input_wiring.py",
                (
                    "DirectCallbackInputEventAdapter",
                    "STARCRAFT_REMASTERED",
                    "SCREEN_VISION",
                    "_direct_input_adapters",
                ),
            ),
        )

        for relative_path, forbidden_values in expectations:
            source = (PROJECT_ROOT / relative_path).read_text(
                encoding="utf-8-sig"
            )
            for forbidden in forbidden_values:
                self.assertNotIn(forbidden, source, relative_path)


def _mutable_static_literals(tree: ast.Module):
    scopes = (tree.body,) + tuple(
        node.body for node in tree.body if isinstance(node, ast.ClassDef)
    )
    return tuple(
        node
        for body in scopes
        for node in body
        if isinstance(node, (ast.Assign, ast.AnnAssign))
        and isinstance(getattr(node, "value", None), (ast.List, ast.Dict, ast.Set))
    )


def _uses_tuple_all(tree: ast.Module) -> bool:
    return any(
        isinstance(node, ast.Assign)
        and any(
            isinstance(target, ast.Name) and target.id == "__all__"
            for target in node.targets
        )
        and isinstance(node.value, ast.Tuple)
        for node in tree.body
    )


if __name__ == "__main__":
    unittest.main()
