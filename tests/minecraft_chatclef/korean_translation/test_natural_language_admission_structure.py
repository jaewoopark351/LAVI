#20260905_kpopmodder: Verify Korean translation and admission responsibility splits.
from __future__ import annotations

import ast
import inspect
import unittest
from pathlib import Path

from plugins.Minecraft.fabric.chatclef.command_registry.admission import (
    KoreanCommandSubmissionAdmission,
)
from plugins.Minecraft.fabric.chatclef.intent import ChatClefNaturalLanguageService
from plugins.Minecraft.fabric.chatclef.intent.natural_language import (
    ChatClefIntentTranslationPolicy,
    ChatClefItemActionTranslator,
    ChatClefNaturalLanguageComponentGraph,
    ChatClefTranslationInputGuard,
    ChatClefTranslationRejectionFactory,
)


PROJECT_ROOT = Path(__file__).resolve().parents[3]
NATURAL_LANGUAGE_ROOT = (
    PROJECT_ROOT
    / "plugins/Minecraft/fabric/chatclef/intent/natural_language"
)
KOREAN_ADMISSION_ROOT = (
    PROJECT_ROOT
    / "plugins/Minecraft/fabric/chatclef/command_registry/admission/korean_command"
)


class NaturalLanguageAdmissionStructureTests(unittest.TestCase):
    def test_new_modules_have_marker_tuple_exports_and_one_class(self):
        paths = tuple(NATURAL_LANGUAGE_ROOT.rglob("*.py")) + tuple(
            KOREAN_ADMISSION_ROOT.rglob("*.py")
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

    def test_natural_language_facade_only_sequences_collaborators(self):
        source = inspect.getsource(ChatClefNaturalLanguageService)
        self.assertIn("ChatClefNaturalLanguageComponentGraph(", source)
        self.assertIn("self._input_guard.inspect", source)
        self.assertIn("self._intent_policy.translate", source)
        self.assertNotIn("has_dangerous_text", source)
        self.assertNotIn(".resolve(intent.item_phrase)", source)
        self.assertEqual(
            5,
            len(
                {
                    ChatClefNaturalLanguageComponentGraph.__module__,
                    ChatClefTranslationInputGuard.__module__,
                    ChatClefIntentTranslationPolicy.__module__,
                    ChatClefItemActionTranslator.__module__,
                    ChatClefTranslationRejectionFactory.__module__,
                }
            ),
        )

    def test_admission_facade_delegates_each_policy_boundary(self):
        source = inspect.getsource(KoreanCommandSubmissionAdmission)
        self.assertIn("self._stop_policy.inspect", source)
        self.assertIn("self._auto_deposit_trust_policy.inspect", source)
        self.assertIn("self._public_policy.inspect", source)
        self.assertIn("self._auto_deposit_trust_policy.commit", source)
        self.assertNotIn("public_korean_enabled", source)


if __name__ == "__main__":
    unittest.main()
