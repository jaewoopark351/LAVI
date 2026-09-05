#20260905_kpopmodder: Verify focused activation binding validators and immutable no-profile policy.
from __future__ import annotations

import ast
import inspect
import unittest
from pathlib import Path

from plugins.Minecraft.fabric.chatclef.input.aliases.generic_crafting_defaults.activation import (
    GenericCraftingActivationBindingValidator,
)
from plugins.Minecraft.fabric.chatclef.input.aliases.generic_crafting_defaults.activation.binding_validation import (
    GenericCraftingActivationContextBindingValidator,
    GenericCraftingActivationEventReceiptValidator,
    GenericCraftingActivationProjectionValidator,
    GenericCraftingActivationProofValidator,
    GenericCraftingActivationRegistryRecordValidator,
    GenericCraftingActivationRequestValidator,
)
from plugins.Minecraft.fabric.chatclef.input.aliases.generic_crafting_defaults.activation.binding_validation.generic_crafting_activation_binding_validation_component_graph import (
    GenericCraftingActivationBindingValidationComponentGraph,
)
from plugins.Minecraft.fabric.chatclef.intent.scoped_resolution import NO_PROFILE


PROJECT_ROOT = Path(__file__).resolve().parents[3]
ACTIVATION_ROOT = PROJECT_ROOT / (
    "plugins/Minecraft/fabric/chatclef/input/aliases/"
    "generic_crafting_defaults/activation"
)
NO_PROFILE_PATH = PROJECT_ROOT / (
    "plugins/Minecraft/fabric/chatclef/intent/scoped_resolution/"
    "no_scoped_item_resolution_profile.py"
)


class GenericCraftingActivationBindingStructureTests(unittest.TestCase):
    def test_legacy_binding_validator_is_a_delegate_over_focused_graph(self):
        validator = GenericCraftingActivationBindingValidator()
        graph = validator._component_graph

        self.assertIsInstance(
            graph,
            GenericCraftingActivationBindingValidationComponentGraph,
        )
        expected_types = (
            (graph.proof_validator, GenericCraftingActivationProofValidator),
            (
                graph.event_validator,
                GenericCraftingActivationEventReceiptValidator,
            ),
            (
                graph.context_validator,
                GenericCraftingActivationContextBindingValidator,
            ),
            (
                graph.projection_validator,
                GenericCraftingActivationProjectionValidator,
            ),
            (
                graph.request_validator,
                GenericCraftingActivationRequestValidator,
            ),
            (
                graph.registry_record_validator,
                GenericCraftingActivationRegistryRecordValidator,
            ),
        )
        for instance, expected_type in expected_types:
            with self.subTest(expected_type=expected_type.__name__):
                self.assertIsInstance(instance, expected_type)

        delegates = (
            (
                GenericCraftingActivationBindingValidator.proof_is_valid,
                "self._component_graph.proof_validator.is_valid",
            ),
            (
                GenericCraftingActivationBindingValidator.matches_context,
                "self._component_graph.context_validator.matches",
            ),
            (
                GenericCraftingActivationBindingValidator.request_matches,
                "self._component_graph.request_validator.matches",
            ),
        )
        for method, expected_call in delegates:
            with self.subTest(method=method.__qualname__):
                self.assertIn(expected_call, inspect.getsource(method))

    def test_new_binding_modules_follow_project_structure_contract(self):
        paths = list((ACTIVATION_ROOT / "binding_validation").rglob("*.py"))
        paths.extend(
            (
                ACTIVATION_ROOT
                / "generic_crafting_activation_binding_validator.py",
                NO_PROFILE_PATH,
            )
        )
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

    def test_no_profile_singleton_has_no_mutable_instance_state(self):
        self.assertIsNone(NO_PROFILE.resolve_exact("map"))
        self.assertFalse(hasattr(NO_PROFILE, "__dict__"))
        with self.assertRaises(TypeError):
            NO_PROFILE.policy = "mutable"
        with self.assertRaises(AttributeError):
            object.__setattr__(NO_PROFILE, "policy", "mutable")


if __name__ == "__main__":
    unittest.main()
