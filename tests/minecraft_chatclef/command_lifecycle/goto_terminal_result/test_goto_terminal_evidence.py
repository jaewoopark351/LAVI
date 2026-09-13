#20260913_kpopmodder: Reject crossed identities and contradictory GOTO terminal meanings.
import unittest
from copy import deepcopy
from dataclasses import FrozenInstanceError, replace
from types import SimpleNamespace

from plugins.Minecraft.fabric.chatclef.result.goto.goto_failure_reasons import GOTO_FAILURE_REASONS
from plugins.Minecraft.fabric.chatclef.result.goto.goto_command_binding_decoder import GotoCommandBindingDecoder
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle import (
    CommandTerminalEvidenceEvaluation, CommandTerminalEvidenceEvaluator,
)

from .fixtures import binding_data, context, data, failed, result


class GotoTerminalEvidenceTests(unittest.TestCase):
    def setUp(self):
        self.evaluator = CommandTerminalEvidenceEvaluator()
        self.context = context()

    def evaluate(self, candidate, context_override=None):
        return self.evaluator.evaluate(candidate, context=context_override or self.context)

    def assert_rejected(self, candidate, context_override=None):
        decision = self.evaluate(candidate, context_override)
        self.assertFalse(decision.verified)
        self.assertIsNone(decision.projection)
        self.assertIsNone(decision.failure_projection)

    def test_chat_and_final_microphone_arrival_have_an_immutable_original_target(self):
        for source in ("lavi_chat_ui", "voice_input_final"):
            with self.subTest(source=source):
                candidate = result()
                decision = self.evaluate(candidate, context(source))
                self.assertTrue(decision.verified)
                self.assertEqual("goto_arrival_verified", decision.decision_reason)
                self.assertIsNone(decision.failure_projection)
                self.assertEqual(-950, decision.projection.binding.target_z)
                candidate.data["goto_terminal"]["target_z"] = 1
                self.assertEqual(-950, decision.projection.binding.target_z)
                with self.assertRaises(FrozenInstanceError):
                    decision.projection.binding.target_z = 1

    def test_each_task_failure_has_explicit_failure_projection_and_never_verified_success(self):
        for reason in sorted(GOTO_FAILURE_REASONS):
            with self.subTest(reason=reason):
                decision = self.evaluate(failed(reason))
                self.assertFalse(decision.verified)
                self.assertIsNone(decision.projection)
                self.assertEqual(reason, decision.failure_projection.failure_reason)
                self.assertEqual("goto_failure_verified", decision.decision_reason)

    def test_failed_world_and_cleanup_evidence_does_not_require_arrival_flags(self):
        candidate = failed("WORLD_CHANGED", binding_valid=False, children_quiescent=False,
                           terminal_dimension=None)
        decision = self.evaluate(candidate)
        self.assertEqual("WORLD_CHANGED", decision.failure_projection.failure_reason)
        self.assertFalse(decision.verified)

    def test_every_binding_field_must_equal_previously_bound_running_task(self):
        replacements = {
            "request_id": "other-request", "command_message_id": "other-message",
            "session_id": "other-session", "server_connection_generation": 4,
            "java_socket_generation": 4,
            "task_owner": "lavi.minecraft.task.movement.gotoresult.tracking.ReportedGotoBlockTask",
            "task_identity": "aaaaaaaa", "operation_id": "aaaaaaaa", "request_shape": "XZ",
            "target_x": 501, "target_y": 81, "target_z": -951,
            "requested_dimension": "overworld", "world_dimension": "minecraft:the_nether",
        }
        for key, value in replacements.items():
            with self.subTest(key=key):
                self.assert_rejected(result(data(**{key: value})))

    def test_each_context_header_and_original_destination_must_still_match(self):
        for key, value in {"request_id": "other", "command_message_id": "other",
                           "session_id": "other", "generation": 4}.items():
            with self.subTest(key=key):
                self.assert_rejected(result(), context(**{key: value}))
        for changes in ({"coordinates": (500, 90, -928)}, {"dimension": "nether"},
                        {"command": "goto 500 80 -951"}, {"detail_level": "command_name_only"}):
            with self.subTest(changes=changes):
                changed = context(descriptor=replace(self.context.descriptor, **changes))
                self.assert_rejected(result(), changed)

    def test_no_task_binding_or_mutated_terminal_copy_is_never_sufficient(self):
        for value in (None, {}, SimpleNamespace(**binding_data())):
            with self.subTest(value=value):
                self.assert_rejected(result(), context(goto_binding=value))
        payload = data()
        payload["goto_binding"]["task_identity"] = "bbbbbbbb"
        self.assert_rejected(result(payload))
        self.assert_rejected(result(), context(goto_binding_rejected=True))

    def test_all_terminal_fields_and_profile_markers_are_required(self):
        baseline = data()
        for key in tuple(baseline["goto_terminal"]):
            with self.subTest(key=key):
                payload = deepcopy(baseline)
                del payload["goto_terminal"][key]
                self.assert_rejected(result(payload))
        for key in ("goto_profile_id", "goto_profile_version", "goto_terminal"):
            with self.subTest(key=key):
                payload = deepcopy(baseline)
                del payload[key]
                self.assert_rejected(result(payload))

    def test_malformed_boolean_integer_reason_dimension_and_future_version_fail_closed(self):
        for key, values in {
            "goal_satisfied": (1, "true", None), "binding_valid": (1, "true", None),
            "children_quiescent": (1, "true", None), "target_y": (True, 80.0, "80"),
            "failure_reason": (None, "", "NONE\n", ["NONE"]),
            "terminal_dimension": ([], "overworld", "minecraft:moon"),
            "evidence_kind": (None, [], "new_owner"), "outcome": (None, {}, "COMPLETED"),
            "task_owner": (None, [], {}, "unknown.Owner"),
        }.items():
            for value in values:
                with self.subTest(key=key, value=value):
                    self.assert_rejected(result(data(**{key: value})))
        for version in (None, True, "1", 2):
            with self.subTest(version=version):
                payload = data()
                payload["goto_profile_version"] = version
                self.assert_rejected(result(payload))
        self.assert_rejected(result(data(extra_field=True)))

    def test_status_outcome_failure_and_arrival_predicates_cannot_contradict_each_other(self):
        for status in ("failed", "cancelled", "unknown", "deadline_exceeded", "rejected"):
            with self.subTest(status=status):
                self.assert_rejected(result(status=status))
        for changes in ({"failure_reason": "HANDOFF_SHORTAGE"}, {"goal_satisfied": False},
                        {"binding_valid": False}, {"children_quiescent": False},
                        {"terminal_dimension": "minecraft:the_nether"},
                        {"terminal_dimension": None}):
            with self.subTest(changes=changes):
                self.assert_rejected(result(data(**changes)))
        self.assert_rejected(result(failed().data))
        self.assert_rejected(failed(goal_satisfied=True))

    def test_unknown_failure_reason_and_mismatched_family_never_supply_explanations(self):
        self.assert_rejected(failed("FUTURE_REASON"))
        self.assert_rejected(failed("NONE"))
        for key, value in {"request_kind": "stop_control_v1", "effect_payload": {},
                           "store_home_result": "COMPLETED", "operation": "stop_ai",
                           "goal_satisfied": False}.items():
            with self.subTest(key=key):
                payload = data()
                payload[key] = value
                self.assert_rejected(result(payload))

    def test_legacy_direct_xyz_uses_only_its_own_evidence_owner(self):
        binding = GotoCommandBindingDecoder().decode(binding_data(
            task_owner="lavi.minecraft.task.movement.gotoresult.tracking.ReportedGotoBlockTask"))
        owner_context = context(binding=binding)
        candidate = result(data(binding=binding, evidence_kind="legacy_get_to_block_terminal"))
        self.assertTrue(self.evaluate(candidate, owner_context).verified)
        self.assert_rejected(result(data(binding=binding)), owner_context)
        candidate = result(data(binding=binding, evidence_kind="legacy_get_to_block_terminal",
                                outcome="FAILED", goal_satisfied=False, failure_reason="NO_PROGRESS"),
                           status="failed", error_code="internal_error")
        self.assert_rejected(candidate, owner_context)

    def test_old_completed_and_generic_failed_results_keep_fallback_without_exception(self):
        for status in ("completed", "failed"):
            with self.subTest(status=status):
                self.assert_rejected(result({"result_reason": "matching_task_finished"}, status=status))

    def test_diagnostic_observer_failure_does_not_change_arrival_or_typed_failure(self):
        class BrokenObserver:
            def evidence_decided(self, **_kwargs):
                raise RuntimeError("diagnostics unavailable")
        observer = CommandTerminalEvidenceEvaluator(diagnostic_observer=BrokenObserver())
        for candidate in (result(), failed()):
            with self.subTest(status=candidate.status):
                self.assertEqual(self.evaluate(candidate), observer.evaluate(candidate, context=self.context))

    def test_failure_projection_is_structurally_separate_from_success(self):
        projection = self.evaluate(failed()).failure_projection
        self.assertIsNotNone(projection)
        for arguments in ({"verified": True, "failure_projection": projection},
                          {"verified": False, "projection": projection}):
            with self.assertRaises(ValueError):
                CommandTerminalEvidenceEvaluation(**arguments)


if __name__ == "__main__":
    unittest.main()
