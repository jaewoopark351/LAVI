#20260913_kpopmodder: Reject malformed, cross-dimension, and unrelated request bindings.
from dataclasses import FrozenInstanceError, replace
import unittest

from plugins.Minecraft.fabric.chatclef.result.goto.goto_command_binding_decoder import GotoCommandBindingDecoder
from plugins.Minecraft.fabric.chatclef.result.goto.goto_command_binding_matcher import GotoCommandBindingMatcher
from .goto_binding_fixture import context, payload


class GotoBindingValidationTests(unittest.TestCase):
    def test_valid_binding_is_detached_and_frozen(self):
        wire = payload()
        binding = GotoCommandBindingDecoder().decode(wire)
        wire["target_x"] = 0
        self.assertEqual(500, binding.target_x)
        with self.assertRaises(FrozenInstanceError):
            binding.target_y = 0
        self.assertTrue(GotoCommandBindingMatcher().matches_context(binding, context()))

    def test_wire_rejects_missing_extra_and_bad_exact_types(self):
        valid = payload()
        for name in valid:
            wire = valid.copy()
            del wire[name]
            with self.subTest(missing=name):
                self.assertIsNone(GotoCommandBindingDecoder().decode(wire))
        for changes in (
            {"extra": 1}, {"target_x": True}, {"target_y": 80.0},
            {"target_z": 2**31}, {"server_connection_generation": True},
            {"java_socket_generation": 0}, {"task_owner": []},
            {"task_owner": "UnrelatedTask"}, {"request_id": "x\ny"},
            {"request_shape": "XZ"}, {"requested_dimension": "nether"},
            {"requested_dimension": []}, {"world_dimension": "minecraft:custom"},
        ):
            with self.subTest(changes=changes):
                self.assertIsNone(GotoCommandBindingDecoder().decode(payload(**changes)))

    def test_original_request_identity_and_destination_must_match(self):
        decoder = GotoCommandBindingDecoder()
        matcher = GotoCommandBindingMatcher()
        for changes in (
            {"request_id": "other"}, {"command_message_id": "other"},
            {"session_id": "other"}, {"server_connection_generation": 2},
            {"target_x": 501}, {"target_y": 81}, {"target_z": -951},
            {"requested_dimension": "overworld"},
        ):
            with self.subTest(changes=changes):
                self.assertFalse(matcher.matches_context(decoder.decode(payload(**changes)), context()))

    def test_raw_xyz_supported_and_xz_y_dimension_forms_excluded(self):
        binding = GotoCommandBindingDecoder().decode(payload(requested_dimension="overworld"))
        for form in ("xyz_dimension", "parenthesized_xyz_dimension"):
            self.assertTrue(GotoCommandBindingMatcher().matches_context(binding, context(
                detail_level="raw_typed", form_kind=form,
                coordinate_values=(500, 80, -950), dimension="overworld",
            )))
        for form in ("xz", "y", "dimension", "crosshair_target"):
            self.assertFalse(GotoCommandBindingMatcher().matches_context(binding, context(
                detail_level="raw_typed", form_kind=form,
                coordinate_values=(500, 80, -950), dimension="overworld",
            )))
        self.assertFalse(GotoCommandBindingMatcher().matches_context(
            binding, replace(context(), generation=True),
        ))
