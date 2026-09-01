#20260901_kpopmodder: Verify command-registry contracts stay split while legacy imports remain compatible.
from __future__ import annotations

import ast
import importlib
import unittest
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[3]
CONTRACT_ROOT = (
    PROJECT_ROOT
    / "plugins"
    / "Minecraft"
    / "fabric"
    / "chatclef"
    / "command_registry"
    / "contracts"
)
EXPECTED_CLASS_BY_FILE = {
    "chatclef_command_readiness_axes.py": "ChatClefCommandReadinessAxes",
    "chatclef_command_spec.py": "ChatClefCommandSpec",
}


class CommandRegistryContractModuleSeparationTests(unittest.TestCase):
    def test_canonical_contract_files_each_define_exactly_one_named_class(self):
        for filename, expected_class in EXPECTED_CLASS_BY_FILE.items():
            with self.subTest(filename=filename):
                path = CONTRACT_ROOT / filename
                self.assertTrue(path.is_file(), f"missing canonical contract: {path}")
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
            "plugins.Minecraft.fabric.chatclef.command_registry.contracts"
        )
        legacy = importlib.import_module(
            "plugins.Minecraft.fabric.chatclef.command_registry."
            "korean_command_registry_model"
        )
        package = importlib.import_module(
            "plugins.Minecraft.fabric.chatclef.command_registry"
        )

        for class_name, filename in (
            ("ChatClefCommandReadinessAxes", "chatclef_command_readiness_axes"),
            ("ChatClefCommandSpec", "chatclef_command_spec"),
        ):
            with self.subTest(class_name=class_name):
                canonical_class = getattr(canonical, class_name)
                self.assertIs(canonical_class, getattr(legacy, class_name))
                self.assertIs(canonical_class, getattr(package, class_name))
                self.assertEqual(
                    "plugins.Minecraft.fabric.chatclef.command_registry.contracts."
                    f"{filename}",
                    canonical_class.__module__,
                )


if __name__ == "__main__":
    unittest.main()
