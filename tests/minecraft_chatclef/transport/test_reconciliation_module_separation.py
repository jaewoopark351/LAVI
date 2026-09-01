#20260901_kpopmodder: Verify reconciliation types stay split behind compatible imports.
from __future__ import annotations

import ast
import importlib
import unittest
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[3]
RECONCILIATION_ROOT = (
    PROJECT_ROOT
    / "plugins"
    / "Minecraft"
    / "fabric"
    / "chatclef"
    / "transport"
    / "reconciliation"
)
EXPECTED_CLASS_BY_PATH = {
    "deposit_profile/deposit_reconciliation_profile.py": (
        "DepositReconciliationProfile"
    ),
    "deposit_profile/deposit_reconciliation_profile_resolver.py": (
        "DepositReconciliationProfileResolver"
    ),
    "candidate_tracking/reconciliation_candidate.py": "ReconciliationCandidate",
    "candidate_tracking/candidate_update.py": "CandidateUpdate",
    "candidate_tracking/reconciliation_candidate_tracker.py": (
        "ReconciliationCandidateTracker"
    ),
    "stable_evidence/stable_lifecycle_evidence.py": "StableLifecycleEvidence",
    "stable_evidence/stable_lifecycle_evidence_parse.py": (
        "StableLifecycleEvidenceParse"
    ),
    "stable_evidence/stable_lifecycle_evidence_parser.py": (
        "StableLifecycleEvidenceParser"
    ),
}
LEGACY_MODULE_BY_PACKAGE = {
    "deposit_profile": "deposit_reconciliation_profile",
    "candidate_tracking": "reconciliation_candidate",
    "stable_evidence": "stable_lifecycle_evidence",
}
LEGACY_MODULE_PATHS = tuple(
    f"{module_name}.py" for module_name in LEGACY_MODULE_BY_PACKAGE.values()
)
CLASS_NAMES_BY_PACKAGE = {
    "deposit_profile": (
        "DepositReconciliationProfile",
        "DepositReconciliationProfileResolver",
    ),
    "candidate_tracking": (
        "ReconciliationCandidate",
        "CandidateUpdate",
        "ReconciliationCandidateTracker",
    ),
    "stable_evidence": (
        "StableLifecycleEvidence",
        "StableLifecycleEvidenceParse",
        "StableLifecycleEvidenceParser",
    ),
}
MODULE_PREFIX = (
    "plugins.Minecraft.fabric.chatclef.transport.reconciliation"
)


class ReconciliationModuleSeparationTests(unittest.TestCase):
    def test_canonical_files_each_define_exactly_one_named_class(self):
        for relative_path, expected_class in EXPECTED_CLASS_BY_PATH.items():
            with self.subTest(relative_path=relative_path):
                path = RECONCILIATION_ROOT / relative_path
                self.assertTrue(path.is_file(), f"missing canonical component: {path}")
                source = path.read_text(encoding="utf-8")
                tree = ast.parse(source, filename=str(path))
                class_names = [
                    node.name for node in tree.body if isinstance(node, ast.ClassDef)
                ]

                self.assertEqual([expected_class], class_names)
                self.assertIn(
                    "#20260901_kpopmodder:",
                    "\n".join(source.splitlines()[:4]),
                )

    def test_legacy_modules_are_class_free_compatibility_shims(self):
        for relative_path in LEGACY_MODULE_PATHS:
            with self.subTest(relative_path=relative_path):
                path = RECONCILIATION_ROOT / relative_path
                tree = ast.parse(
                    path.read_text(encoding="utf-8"),
                    filename=str(path),
                )
                class_names = [
                    node.name for node in tree.body if isinstance(node, ast.ClassDef)
                ]

                self.assertEqual([], class_names)

    def test_legacy_and_canonical_imports_are_identical(self):
        for package_name, legacy_module_name in LEGACY_MODULE_BY_PACKAGE.items():
            canonical = importlib.import_module(f"{MODULE_PREFIX}.{package_name}")
            legacy = importlib.import_module(
                f"{MODULE_PREFIX}.{legacy_module_name}"
            )
            for class_name in CLASS_NAMES_BY_PACKAGE[package_name]:
                with self.subTest(package=package_name, class_name=class_name):
                    canonical_class = getattr(canonical, class_name)
                    self.assertIs(canonical_class, getattr(legacy, class_name))
                    self.assertTrue(
                        canonical_class.__module__.startswith(
                            f"{MODULE_PREFIX}.{package_name}."
                        )
                    )

    def test_stable_evidence_constants_are_reexported_unchanged(self):
        canonical = importlib.import_module(f"{MODULE_PREFIX}.stable_evidence")
        legacy = importlib.import_module(
            f"{MODULE_PREFIX}.stable_lifecycle_evidence"
        )

        self.assertEqual(canonical.EXPECTED_VERSION, legacy.EXPECTED_VERSION)
        self.assertEqual(canonical.EXPECTED_STAGE, legacy.EXPECTED_STAGE)


if __name__ == "__main__":
    unittest.main()
