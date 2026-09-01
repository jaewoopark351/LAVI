#20260901_kpopmodder: Verify cleanup contracts and stages stay split behind compatible imports.
from __future__ import annotations

import ast
import importlib
import unittest
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[3]
COMPONENT_ROOT = (
    PROJECT_ROOT
    / "plugins"
    / "Minecraft"
    / "fabric"
    / "chatclef"
    / "orchestration"
    / "inventory_cleanup"
)
EXPECTED_CLASS_BY_FILE = {
    "inventory_evidence_state.py": "InventoryEvidenceState",
    "inventory_stack.py": "InventoryStack",
    "inventory_snapshot.py": "InventorySnapshot",
    "inventory_cleanup_postcondition.py": "InventoryCleanupPostcondition",
    "cleanup_target_plan.py": "CleanupTargetPlan",
    "cleanup_admission_decision.py": "CleanupAdmissionDecision",
    "inventory_cleanup_preflight_planner.py": "InventoryCleanupPreflightPlanner",
    "post_cleanup_primary_admission.py": "PostCleanupPrimaryAdmission",
    "chatclef_inventory_cleanup_policy.py": "ChatClefInventoryCleanupPolicy",
}


class InventoryCleanupContractModuleSeparationTests(unittest.TestCase):
    def test_canonical_component_files_each_define_exactly_one_named_class(self):
        for filename, expected_class in EXPECTED_CLASS_BY_FILE.items():
            with self.subTest(filename=filename):
                path = COMPONENT_ROOT / filename
                self.assertTrue(path.is_file(), f"missing canonical component: {path}")
                tree = ast.parse(path.read_text(encoding="utf-8"), filename=str(path))
                class_names = [
                    node.name for node in tree.body if isinstance(node, ast.ClassDef)
                ]

                self.assertEqual([expected_class], class_names)
                self.assertIn(
                    "#20260901_kpopmodder:",
                    "\n".join(path.read_text(encoding="utf-8").splitlines()[:4]),
                )

    def test_legacy_and_package_exports_are_the_canonical_class_objects(self):
        canonical = importlib.import_module(
            "plugins.Minecraft.fabric.chatclef.orchestration.inventory_cleanup"
        )
        legacy_policy = importlib.import_module(
            "plugins.Minecraft.fabric.chatclef.orchestration.inventory_cleanup_policy"
        )
        legacy_state = importlib.import_module(
            "plugins.Minecraft.fabric.chatclef.orchestration.inventory_cleanup_state"
        )
        package = importlib.import_module(
            "plugins.Minecraft.fabric.chatclef.orchestration"
        )

        legacy_owner_by_name = {
            "ChatClefInventoryCleanupPolicy": legacy_policy,
            "CleanupAdmissionDecision": legacy_policy,
            "CleanupTargetPlan": legacy_policy,
            "InventoryCleanupPostcondition": legacy_state,
            "InventoryEvidenceState": legacy_state,
            "InventorySnapshot": legacy_state,
            "InventoryStack": legacy_state,
        }
        for class_name, legacy_owner in legacy_owner_by_name.items():
            with self.subTest(class_name=class_name):
                canonical_class = getattr(canonical, class_name)
                self.assertIs(canonical_class, getattr(legacy_owner, class_name))
                self.assertIs(canonical_class, getattr(package, class_name))
                self.assertTrue(
                    canonical_class.__module__.startswith(
                        "plugins.Minecraft.fabric.chatclef.orchestration."
                        "inventory_cleanup."
                    )
                )


if __name__ == "__main__":
    unittest.main()
