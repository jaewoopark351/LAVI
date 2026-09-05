#20260905_kpopmodder: Enforce the trusted Korean route responsibility split.
from __future__ import annotations

import ast
import unittest
from pathlib import Path


class TrustedKoreanRouteStructureTests(unittest.TestCase):
    def test_each_production_module_has_marker_one_class_and_immutable_exports(self):
        root = (
            Path(__file__).resolve().parents[4]
            / "plugins"
            / "Minecraft"
            / "fabric"
            / "chatclef"
            / "input"
            / "routing"
            / "trusted_korean"
        )
        modules = tuple(
            path for path in root.rglob("*.py") if path.name != "__init__.py"
        )

        self.assertGreaterEqual(len(modules), 9)
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

    def test_coordinator_contains_sequence_only_and_delegates_all_owners(self):
        path = (
            Path(__file__).resolve().parents[4]
            / "plugins"
            / "Minecraft"
            / "fabric"
            / "chatclef"
            / "input"
            / "routing"
            / "trusted_korean"
            / "trusted_korean_input_route_coordinator.py"
        )
        source = path.read_text(encoding="utf-8-sig")

        for forbidden in (
            "dataclasses",
            "KoreanChatMicrophoneEligibilityProof",
            "issue_response_emission_capability",
            "_feature_admission_logger",
            "_feature_admission_projector",
            "_close_feature_dispatch_callback",
        ):
            self.assertNotIn(forbidden, source)
        for collaborator in (
            "admission.admit",
            "route_invoker.route_fallthrough",
            "route_invoker.route_trusted",
            "feedback_renderer.render",
            "response_authorizer.authorize",
            "proof_lifecycle_closer.close",
            "proof_validator.is_live",
            "diagnostics_observer.observe",
        ):
            self.assertIn(collaborator, source)


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
