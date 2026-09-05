#20260905_kpopmodder: Lock focused natural-language and diagnostic projection owners.
from __future__ import annotations

import ast
import inspect
import unittest
from pathlib import Path

from plugins.Minecraft.fabric.chatclef.input.diagnostics.minecraft_korean_feature_admission_projector import (
    MinecraftKoreanFeatureAdmissionProjector,
)
from plugins.Minecraft.fabric.chatclef.input.diagnostics.projection import (
    MinecraftKoreanActivationStatusProjector,
    MinecraftKoreanControlledReasonSanitizer,
    MinecraftKoreanFeatureIdentityClassifier,
    MinecraftKoreanIngressClaimStatusProjector,
    MinecraftKoreanLanguageStatusProjector,
    MinecraftKoreanRouteDecisionProjector,
)
from plugins.Minecraft.fabric.chatclef.intent.natural_language.item import (
    ChatClefItemActionTranslator,
    ChatClefItemCommandCompilationStage,
    ChatClefItemResolutionStage,
    ChatClefItemTargetValidator,
)
from plugins.Minecraft.fabric.chatclef.intent.natural_language.policy import (
    ChatClefIntentAdmissionStage,
    ChatClefIntentTranslationPolicy,
    ChatClefNonItemTranslationStage,
    ChatClefTranslationBranchSelector,
)


PROJECT_ROOT = Path(__file__).resolve().parents[3]
NATURAL_LANGUAGE_ROOT = (
    PROJECT_ROOT / "plugins/Minecraft/fabric/chatclef/intent/natural_language"
)
PROJECTION_ROOT = (
    PROJECT_ROOT / "plugins/Minecraft/fabric/chatclef/input/diagnostics/projection"
)


class FinalTranslationDiagnosticsResponsibilityTests(unittest.TestCase):
    def test_item_translator_only_sequences_three_focused_stages(self):
        source = inspect.getsource(ChatClefItemActionTranslator.translate)

        self.assertIn("self._resolution_stage.resolve", source)
        self.assertIn("self._target_validator.inspect", source)
        self.assertIn("self._compilation_stage.compile", source)
        self.assertEqual(
            3,
            len(
                {
                    ChatClefItemResolutionStage.__module__,
                    ChatClefItemTargetValidator.__module__,
                    ChatClefItemCommandCompilationStage.__module__,
                }
            ),
        )

    def test_intent_policy_only_sequences_admission_branch_and_execution(self):
        source = inspect.getsource(ChatClefIntentTranslationPolicy.translate)

        self.assertIn("self._admission_stage.inspect", source)
        self.assertIn("self._branch_selector.is_item_action", source)
        self.assertIn("self._non_item_translation_stage.translate", source)
        self.assertEqual(
            3,
            len(
                {
                    ChatClefIntentAdmissionStage.__module__,
                    ChatClefTranslationBranchSelector.__module__,
                    ChatClefNonItemTranslationStage.__module__,
                }
            ),
        )

    def test_feature_projector_delegates_identity_status_and_reason_policy(self):
        source = inspect.getsource(MinecraftKoreanFeatureAdmissionProjector.project)

        self.assertIn("self._feature_identity_classifier.classify", source)
        self.assertIn("self._ingress_status_projector.project", source)
        self.assertIn("self._language_status_projector.contains_hangul", source)
        self.assertIn("self._activation_status_projector.project", source)
        self.assertIn("self._route_decision_projector.project", source)
        self.assertIn("self._reason_sanitizer.sanitize", source)
        self.assertEqual(
            6,
            len(
                {
                    MinecraftKoreanFeatureIdentityClassifier.__module__,
                    MinecraftKoreanControlledReasonSanitizer.__module__,
                    MinecraftKoreanIngressClaimStatusProjector.__module__,
                    MinecraftKoreanLanguageStatusProjector.__module__,
                    MinecraftKoreanActivationStatusProjector.__module__,
                    MinecraftKoreanRouteDecisionProjector.__module__,
                }
            ),
        )

    def test_new_modules_have_marker_tuple_exports_and_one_class(self):
        paths = tuple(NATURAL_LANGUAGE_ROOT.rglob("*.py")) + tuple(
            PROJECTION_ROOT.rglob("*.py")
        )
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
