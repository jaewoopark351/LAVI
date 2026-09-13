#20260913_kpopmodder: Verify original-XYZ, live-proof and diagnostic-only GOTO boundaries.
from __future__ import annotations

import copy
from dataclasses import replace
import unittest
from unittest.mock import Mock

from input_core.input_event.contracts.lavi_input_event import LaviInputEvent
from plugins.Minecraft.fabric.chatclef.extension.natural_language.natural_language_command_input import (
    natural_language_text,
)
from plugins.Minecraft.fabric.chatclef.input.routing.goto import (
    GotoInputBinding,
    GotoInputRouteGuard,
    GotoTranslationBindingStage,
)
from plugins.Minecraft.fabric.chatclef.input.routing.goto.goto_input_diagnostics import (
    GotoInputDiagnostics,
)
from plugins.Minecraft.fabric.chatclef.input.routing.goto.goto_translation_binding_validator import (
    GotoTranslationBindingValidator,
)
from plugins.Minecraft.fabric.chatclef.intent.navigation.goto import (
    KoreanGotoCoordinateParser,
)


class GotoInputBoundaryTests(unittest.TestCase):
    def setUp(self):
        self.event = LaviInputEvent(
            text="500 90 -928로 가줘",
            source="lavi_chat_ui",
            provider_id="lavi_chat_ui",
            event_id="a" * 32,
            event_kind="chat_submit",
            final=True,
            fallback_payload=None,
        )
        self.proof = object()
        self.binding = GotoInputBinding(
            True, KoreanGotoCoordinateParser().parse(self.event.text)
        )
        self.translation = {
            "executable": True,
            "command": "goto 500 90 -928",
            "intent": {"intent_type": "goto", "x": 500, "y": 90, "z": -928},
        }

    def test_missing_binding_is_not_confused_with_gui_or_direct_scope(self):
        proof_validator = Mock(return_value=True)
        stage = self._stage(proof_validator)
        rejected = stage.inspect(
            event=self.event, proof=self.proof, binding=None,
            translation=self.translation,
        )
        self.assertEqual("goto_input_binding_missing", rejected.reason)
        for source in ("direct_typed", "lavi_gui_korean"):
            self.assertIsNone(stage.inspect(
                event=replace(self.event, source=source), proof=None,
                binding=None, translation=self.translation,
            ))
        proof_validator.assert_not_called()

    def test_binding_scope_cannot_turn_off_automatic_input_protection(self):
        rejected = self._stage(Mock(return_value=True)).inspect(
            event=self.event, proof=self.proof,
            binding=replace(self.binding, automatic_input=False),
            translation=self.translation,
        )
        self.assertEqual("goto_input_scope_mismatch", rejected.reason)

    def test_proof_closed_during_translation_is_rejected_before_submission(self):
        live = Mock(side_effect=(True, False))
        binding, decision = GotoInputRouteGuard(live_proof_validator=live).inspect(
            self.event, self.proof
        )
        self.assertIsNone(decision)
        rejected = self._stage(live).inspect(
            event=self.event, proof=self.proof, binding=binding,
            translation=self.translation,
        )
        self.assertEqual("goto_live_proof_expired", rejected.reason)
        self.assertEqual(2, live.call_count)

    def test_original_xyz_blocks_both_intent_and_command_change(self):
        validator = GotoTranslationBindingValidator()
        changes = (
            {"intent": {"intent_type": "goto", "x": 501, "y": 90, "z": -928},
             "command": "goto 501 90 -928"},
            {"intent": {"intent_type": "idle"}, "command": "idle"},
            {"intent": {"intent_type": "unknown"}, "executable": False,
             "command": None},
            {"command": "go 500 90 -928"},
            {"command": "goto 500 90 -928 nether"},
        )
        original = copy.deepcopy(self.translation)
        self.assertIsNone(validator.validate(self.binding, self.translation))
        for change in changes:
            with self.subTest(change=change):
                self.assertIsNotNone(validator.validate(
                    self.binding, {**self.translation, **change}
                ))
        self.assertEqual(original, self.translation)

    def test_non_goto_input_cannot_be_promoted_to_goto_by_translation(self):
        binding = GotoInputBinding(
            True, KoreanGotoCoordinateParser().parse("나무 10개 가져와")
        )
        self.assertEqual(
            "goto_original_xyz_required",
            GotoTranslationBindingValidator().validate(binding, self.translation),
        )
        self.assertIsNone(GotoTranslationBindingValidator().validate(
            binding, {"intent": {"intent_type": "get_item"}, "executable": True}
        ))

    def test_diagnostics_disabled_enabled_or_failing_cannot_change_decisions(self):
        records = []
        failing = Mock(side_effect=RuntimeError("diagnostics unavailable"))
        results = []
        for callback in (None, records.append, failing):
            diagnostics = GotoInputDiagnostics(callback)
            live = Mock(return_value=True)
            binding, decision = GotoInputRouteGuard(
                live_proof_validator=live, diagnostics=diagnostics,
            ).inspect(self.event, self.proof)
            matched = self._stage(live, diagnostics).inspect(
                event=self.event, proof=self.proof, binding=binding,
                translation=self.translation,
            )
            results.append((binding, decision, matched))
        self.assertEqual(results[0], results[1])
        self.assertEqual(results[0], results[2])
        self.assertEqual(2, len(records))
        self.assertEqual(2, failing.call_count)
        for record in records:
            self.assertNotIn(self.event.text, record)
            self.assertIn("event_id=" + self.event.event_id, record)
            self.assertLess(len(record), 512)

    def test_question_does_not_acquire_minecraft_ownership_or_require_proof(self):
        live = Mock(side_effect=AssertionError("a question cannot acquire execution"))
        binding, decision = GotoInputRouteGuard(live_proof_validator=live).inspect(
            replace(self.event, text="500 90 -928로 가면 어떻게 돼?"), None
        )
        self.assertFalse(binding.parse_result.executable)
        self.assertFalse(decision.handled)
        live.assert_not_called()

    def test_legacy_text_coercion_preserves_only_valid_goto_raw_text(self):
        raw = "  500, 90, -928 좌표로 가줘  "
        for command in (raw, {"text": raw}, {"command": raw}):
            self.assertEqual(raw, natural_language_text(command))
        for command, expected in (
            ("  나무 10개 가져와  ", "나무 10개 가져와"),
            ({"text": "  나무 10개 가져와  "}, "나무 10개 가져와"),
            (None, ""), (False, ""), ({}, ""), ("  ", ""),
            (" 500 90 -928로 가지 마 ", "500 90 -928로 가지 마"),
        ):
            with self.subTest(command=command):
                self.assertEqual(expected, natural_language_text(command))

    @staticmethod
    def _stage(live, diagnostics=None):
        return GotoTranslationBindingStage(
            live_proof_validator=live,
            diagnostics=diagnostics or GotoInputDiagnostics(),
        )
