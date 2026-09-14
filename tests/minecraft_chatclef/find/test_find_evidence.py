#20260914_kpopmodder: Verify strict FIND results, neutral misses, safety and Korean phrase projection.
from copy import deepcopy
from dataclasses import replace
from types import SimpleNamespace
import unittest

from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO
from plugins.Minecraft.fabric.chatclef.result.find import FindTerminalPayload
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle import CommandTerminalEvidenceEvaluator
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle.evidence.command_terminal_evidence_profile_registry import CommandTerminalEvidenceProfileRegistry
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle.evidence.find import FindTerminalEvidenceEvaluator
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle.terminal.command_terminal_fact import CommandTerminalFact
from plugins.Minecraft.fabric.chatclef.response.command_lifecycle import CommandLifecycleResponseRenderer

from .fixtures import context, data, translation


class FindEvidenceTests(unittest.TestCase):
    def test_exploration_reason_matrix_rejects_unknown_wrong_mode_result_kind_and_extra_phase_keys(self):
        for mode in ("report", "approach"):
            for result, reason in (("UNREACHABLE", "exploration_no_progress"),
                    ("TIMEOUT", "discovery_deadline_exhausted"),
                    ("OBSERVATION_BOUNDS_EXHAUSTED", "scan_count_limit_exhausted"),
                    ("OBSERVATION_BOUNDS_EXHAUSTED", "exploration_route_work_limit_exhausted"),
                    ("TIMEOUT", "exploration_step_deadline_exhausted"),
                    ("INTERNAL_ERROR", "owned_cleanup_failed")):
                payload = data(result, mode=mode)
                payload["effect_payload"]["reason"] = reason
                self.assertIsNotNone(FindTerminalPayload.from_data(payload))
                payload["effect_payload"]["reason"] = "unknown_exploration_claim"
                if result != "INTERNAL_ERROR":
                    self.assertIsNone(FindTerminalPayload.from_data(payload))
        for mode, kind, reason in (("report", "entity", "post_discovery_approach_unreachable"),
                ("report", "entity", "approach_deadline_exhausted"),
                ("approach", "block", "post_discovery_approach_unreachable")):
            payload = data("TIMEOUT" if "deadline" in reason else "UNREACHABLE", mode=mode, kind=kind)
            payload["effect_payload"]["reason"] = reason
            self.assertIsNone(FindTerminalPayload.from_data(payload))
        payload = data("UNREACHABLE", mode="approach")
        payload["effect_payload"].update(reason="post_discovery_approach_unreachable", discovery_verified=True)
        self.assertIsNone(FindTerminalPayload.from_data(payload))  # Internal phase proof never adds wire keys.

    def test_exploration_and_post_discovery_failures_have_distinct_cautious_korean_for_both_input_sources(self):
        from plugins.Minecraft.fabric.chatclef.response.command_lifecycle.terminal.find.korean_find_terminal_renderer import KoreanFindTerminalRenderer
        for source in ("lavi_chat_ui", "voice_input_final"):
            for mode, result, reason, phrase, forbidden in (
                    ("report", "UNREACHABLE", "exploration_route_unavailable", "탐험을 계속할 수 없어서", "발견했지만"),
                    ("approach", "UNREACHABLE", "post_discovery_approach_unreachable", "발견했지만", "찾았어"),
                    ("report", "TIMEOUT", "discovery_deadline_exhausted", "탐색을 완료하지는", "접근 시간"),
                    ("approach", "TIMEOUT", "approach_deadline_exhausted", "접근 시간", "찾았어"),
                    ("report", "TIMEOUT", "exploration_step_deadline_exhausted", "탐험 이동의", "발견"),
                    ("approach", "TIMEOUT", "native_approach_step_deadline_exhausted", "접근 이동의", "찾았어"),
                    ("approach", "TIMEOUT", "parent_deadline_exhausted", "요청을 완료하지는", "발견")):
                current = context(source, "마을 주민 찾아서 가까이 가줘" if mode == "approach" else "마을 주민 찾아줘")
                payload = data(result, mode=mode)
                payload["effect_payload"]["reason"] = reason
                decision = self.evaluate(payload, current=current, status="failed")
                self.assertIsNotNone(decision.failure_projection)
                text = KoreanFindTerminalRenderer().render(current.descriptor, status="failed", verified=False,
                    failure=decision.failure_projection)
                self.assertIn(phrase, text)
                self.assertNotIn(forbidden, text)
                self.assertNotIn("X ", text)

    def test_legacy_exception_names_remain_cautious_failures_and_never_new_phase_evidence(self):
        for kind in ("entity", "block", "player", "item"):
            payload = data("INTERNAL_ERROR", kind=kind)
            payload["effect_payload"]["reason"] = "CustomReadFailure"
            self.assertIsNotNone(FindTerminalPayload.from_data(payload))
        payload = data("FOUND_AND_REPORTED")
        payload["effect_payload"]["reason"] = "CustomReadFailure"
        self.assertIsNone(FindTerminalPayload.from_data(payload))

    def evaluate(self, payload=None, *, current=None, status="completed", request="find-request"):
        result = CommandResultDTO(request_id=request, status=status, ok=status == "completed",
            error_code="internal_error" if status == "failed" else None, data=data() if payload is None else payload)
        return CommandTerminalEvidenceEvaluator().evaluate(result, context=current or context())

    def test_chat_and_final_voice_found_matches_one_exact_immutable_binding(self):
        for source in ("lavi_chat_ui", "voice_input_final"):
            decision = self.evaluate(current=context(source))
            self.assertTrue(decision.verified)
            self.assertEqual(-2, decision.projection.fields["x"])
            with self.assertRaises(TypeError):
                decision.projection.fields["x"] = 1

    def test_complete_query_miss_remains_unsatisfied_with_a_separate_neutral_projection(self):
        decision = self.evaluate(data("NOT_OBSERVED_IN_LOADED_SCOPE"))
        self.assertFalse(decision.verified)
        self.assertIsNone(decision.projection)
        self.assertIsNone(decision.failure_projection)
        self.assertFalse(decision.query_projection.find_satisfied)
        self.assertEqual("find_query_miss_verified", decision.decision_reason)

    def test_each_natural_failure_requires_failed_status_and_no_candidate_coordinates(self):
        for reason in ("INVALID_TARGET", "OBSERVATION_BOUNDS_EXHAUSTED", "TARGET_LOST",
                "CANDIDATE_NOT_REVALIDATABLE", "INTERNAL_ERROR", "INTERRUPTED"):
            payload = data(reason)
            decision = self.evaluate(payload, status="failed")
            self.assertFalse(decision.verified)
            self.assertEqual(reason, decision.failure_projection.find_result)
            self.assertIsNone(self.evaluate(payload).failure_projection)
            payload["effect_payload"]["x"] = 1
            self.assertIsNone(self.evaluate(payload, status="failed").failure_projection)

    def test_kind_id_mode_profile_types_unrelated_fields_and_stale_bindings_are_rejected(self):
        for key, value in (("target_kind", "block"), ("canonical_target_id", "minecraft:zombie"),
                ("catalog_digest", "0" * 64), ("resource_generation", True), ("x", True),
                ("find_satisfied", False), ("reason", "bad\nreason"), ("extra", True)):
            payload = data()
            payload["effect_payload"][key] = value
            self.assertFalse(self.evaluate(payload).verified)
        for key, value in (("effect_profile_version", True), ("effect_profile_id", "get"),
                ("result_reason", "command_finish_callback_only"), ("store_home_result", "COMPLETED")):
            payload = data()
            payload[key] = value
            self.assertFalse(self.evaluate(payload).verified)
        self.assertFalse(self.evaluate(request="foreign").verified)
        for key, value in (("generation", 2), ("session_id", "foreign")):
            current = context()
            setattr(current, key, value)
            self.assertFalse(self.evaluate(current=current).verified)

    def test_player_request_digest_preserves_exact_literal_name_and_uuid_candidate_is_separate(self):
        current = context(text="플레이어 Steve 찾아줘")
        payload = data(kind="player", target="Steve")
        self.assertTrue(self.evaluate(payload, current=current).verified)
        self.assertNotEqual(payload["effect_payload"]["player_identity_digest"], payload["effect_payload"]["candidate_identity_digest"])
        changed = data(kind="player", target="steve")
        self.assertFalse(self.evaluate(changed, current=current).verified)
        miss = data("NOT_OBSERVED_IN_LOADED_SCOPE", kind="player", target="Steve")
        self.assertIsNotNone(self.evaluate(miss, current=current).query_projection)

    def test_array_or_object_effect_enums_are_rejected_without_breaking_terminal_delivery(self):
        for key in ("target_kind", "find_result"):
            for malformed in ([], {}):
                payload = data()
                payload["effect_payload"][key] = malformed
                with self.subTest(key=key, malformed=malformed):
                    self.assertFalse(self.evaluate(payload).verified)
        for malformed in ([], {}):
            payload = data()
            payload["effect_profile_id"] = malformed
            self.assertFalse(self.evaluate(payload).verified)

    def test_closed_evaluator_profile_cannot_be_replaced_by_a_foreign_command_or_lifecycle(self):
        result = CommandResultDTO(request_id="find-request", status="completed", ok=True, data=data())
        profile = CommandTerminalEvidenceProfileRegistry().profile("find")
        evaluator = FindTerminalEvidenceEvaluator()
        for changes in ({"command_name": "get"}, {"profile_id": "foreign"},
                {"rollout_state": "cautious"}, {"response_lifecycle_kind": "persistent_task"}):
            decision = evaluator.evaluate(result, data=result.data, context=context(), profile=replace(profile, **changes))
            self.assertFalse(decision.verified)

    def test_compiled_registry_binding_requires_one_actual_session_and_strict_generation(self):
        from plugins.Minecraft.fabric.chatclef.result.find import FindCommandBinding
        for key, malformed in (("find_session_id", ""), ("find_session_id", []),
                ("find_session_id", "session\n"), ("find_connection_generation", True)):
            value = translation()
            value["data"][key] = malformed
            self.assertIsNone(FindCommandBinding.from_translation(value))
        for key in ("target_kind", "mode"):
            for malformed in ([], {}):
                value = translation()
                value["intent"]["slots"][key] = malformed
                self.assertIsNone(FindCommandBinding.from_translation(value))

    def test_approach_success_requires_own_profile_safe_distance_and_same_mode(self):
        current = context(text="마을 주민 찾아서 가까이 가줘")
        payload = data("FOUND_AND_IN_SAFE_RANGE", mode="approach")
        self.assertTrue(self.evaluate(payload, current=current).verified)
        self.assertFalse(self.evaluate(payload).verified)
        payload["effect_payload"]["safe_distance_satisfied"] = False
        self.assertFalse(self.evaluate(payload, current=current).verified)

    def test_ko_response_does_not_promote_miss_bounds_or_unknown_to_found(self):
        renderer = CommandLifecycleResponseRenderer()
        for reason, status, required in (("FOUND_AND_REPORTED", "completed", "X -2, Y 64, Z 5"),
                ("NOT_OBSERVED_IN_LOADED_SCOPE", "completed", "불러온 범위에서는"),
                ("OBSERVATION_BOUNDS_EXHAUSTED", "failed", "탐색 한도")):
            decision = self.evaluate(data(reason), status=status)
            fact = CommandTerminalFact(descriptor=context().descriptor, status=status,
                verified=decision.verified, dispatch_started=True, result_reason="matching_task_finished",
                event_id="a" * 32, owner_token=object(), evidence_projection=decision.projection,
                failure_projection=decision.failure_projection, query_projection=decision.query_projection)
            text = renderer.render_terminal(fact)
            self.assertIn(required, text)
            if not decision.verified:
                self.assertNotIn("찾았어", text)
                self.assertNotIn("X ", text)
