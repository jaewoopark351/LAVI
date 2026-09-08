#20260907_kpopmodder: Lock strict nested GET evidence and exact legacy compatibility.
from __future__ import annotations

import unittest
from copy import deepcopy
from types import SimpleNamespace

from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO
from plugins.Minecraft.common.protocol.command_result_status import CommandResultStatus
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle import (
    CommandTerminalEvidenceEvaluator,
)


class CommandTerminalEvidenceEvaluatorTests(unittest.TestCase):
    def setUp(self) -> None:
        self.evaluator = CommandTerminalEvidenceEvaluator()
        self.context = _context("oak_log", 3)

    def test_accepts_authoritative_generic_get_profile(self):
        self.assertTrue(
            self.evaluator.verified(
                _result(_nested_data("oak_log", 3, before=4, after=7)),
                context=self.context,
            )
        )

    def test_profile_identity_payload_and_exact_integer_fail_closed(self):
        base = _nested_data("oak_log", 3, before=4, after=7)
        cases = []
        for key in ("effect_profile_id", "effect_profile_version", "effect_payload"):
            value = deepcopy(base)
            value.pop(key)
            cases.append(value)
        for key, replacement in (
            ("effect_profile_id", "unknown"),
            ("effect_profile_version", True),
            ("effect_profile_version", 2),
            ("effect_kind", "store_home"),
        ):
            value = deepcopy(base)
            value[key] = replacement
            cases.append(value)
        for key, replacement in (
            ("target_item", "birch_log"),
            ("target_match_ids", ["minecraft:oak_log", "minecraft:oak_log"]),
            ("target_match_ids", ["minecraft:oak_log", "minecraft:birch_log"]),
            ("target_match_ids", ["mod:oak_log"]),
            ("quantity_semantics", "ENSURE_TOTAL"),
            ("requested_delta", True),
            ("requested_delta", 2),
            ("before_target_count", True),
            ("target_count_delta", 2),
            ("effect_observation_status", "unavailable"),
            ("effect_observation_status", "before_unavailable"),
            ("effect_observation_status", "after_unavailable"),
            ("effect_observation_status", "stale_world_binding"),
            ("effect_observation_reason", ""),
        ):
            value = deepcopy(base)
            value["effect_payload"][key] = replacement
            cases.append(value)
        extra = deepcopy(base)
        extra["effect_payload"]["wire_goal_count"] = 3
        cases.append(extra)

        for data in cases:
            with self.subTest(data=data):
                self.assertFalse(
                    self.evaluator.verified(
                        _result(data),
                        context=self.context,
                    )
                )

    def test_cross_family_markers_never_fall_through_to_get(self):
        for mutation in (
            {"operation": "store_home"},
            {"store_home_result": {}},
            {"request_kind": "stop_control_v1"},
            {"operation": "stop_ai"},
        ):
            data = _nested_data("oak_log", 3, before=4, after=7)
            data.update(mutation)
            with self.subTest(mutation=mutation):
                self.assertFalse(
                    self.evaluator.verified(
                        _result(data),
                        context=self.context,
                    )
                )

    def test_target_match_id_bound_agrees_with_java_projection(self):
        payload = _nested_data("log", 1, before=0, after=1)
        payload["effect_payload"]["target_match_ids"] = [
            f"minecraft:generated_{index:04d}" for index in range(2048)
        ]
        context = _context("log", 1)
        context.before_target_count = 0

        self.assertTrue(
            self.evaluator.verified(_result(payload), context=context)
        )
        payload["effect_payload"]["target_match_ids"].append(
            "minecraft:generated_overflow"
        )
        self.assertFalse(
            self.evaluator.verified(_result(payload), context=context)
        )

    def test_nested_and_flat_transition_requires_exact_diamond_agreement(self):
        context = _context("diamond_pickaxe", 1, verb="craft")
        data = _nested_data("diamond_pickaxe", 1, before=0, after=1)
        data.update(
            {
                "target_item": "diamond_pickaxe",
                "requested_count": 1,
                "before_target_count": 0,
                "after_target_count": 1,
                "target_count_delta": 1,
                "effect_observation_status": "authoritative",
                "effect_observation_reason": "acquisition_delta_observed",
            }
        )

        self.assertTrue(self.evaluator.verified(_result(data), context=context))
        contradiction = deepcopy(data)
        contradiction["target_count_delta"] = 2
        self.assertFalse(
            self.evaluator.verified(_result(contradiction), context=context)
        )
        self.assertFalse(
            self.evaluator.verified(_result(data), context=self.context)
        )

    def test_nested_exact_diamond_transition_does_not_infer_request_verb(self):
        data = _nested_data("diamond_pickaxe", 1, before=0, after=1)
        data.update(
            {
                "target_item": "diamond_pickaxe",
                "requested_count": 1,
                "before_target_count": 0,
                "after_target_count": 1,
                "target_count_delta": 1,
                "effect_observation_status": "authoritative",
                "effect_observation_reason": "acquisition_delta_observed",
            }
        )

        for verb in ("craft", "acquire", "mining"):
            with self.subTest(verb=verb):
                context = _context("diamond_pickaxe", 1, verb=verb)
                context.descriptor.command = "@get diamond_pickaxe 1"
                context.descriptor.detail_level = "raw_typed"
                self.assertTrue(
                    self.evaluator.verified(_result(data), context=context)
                )

    def test_descriptor_cautious_rollout_cannot_claim_strong_get_success(self):
        context = _context("oak_log", 2, verb="acquire")
        context.descriptor.rollout_state = "cautious"

        self.assertFalse(
            self.evaluator.verified(
                _result(_nested_data("oak_log", 2, before=4, after=6)),
                context=context,
            )
        )

    def test_legacy_flat_profile_is_restricted_to_exact_diamond_craft(self):
        data = {
            "result_reason": "matching_task_finished",
            "result_fidelity": "callback_plus_matching_user_task_event",
            "effect_kind": "get_acquisition_delta",
            "target_item": "diamond_pickaxe",
            "requested_count": 1,
            "before_target_count": 0,
            "after_target_count": 1,
            "target_count_delta": 1,
            "effect_observation_status": "authoritative",
            "effect_observation_reason": "acquisition_delta_observed",
        }

        self.assertTrue(
            self.evaluator.verified(
                _result(data),
                context=_context("diamond_pickaxe", 1, verb="craft"),
            )
        )
        self.assertFalse(
            self.evaluator.verified(
                _result(data),
                context=_context("diamond_pickaxe", 1, verb="acquire"),
            )
        )
        mismatched_command = _context("diamond_pickaxe", 1, verb="craft")
        mismatched_command.descriptor.command = "get dirt 1"
        self.assertFalse(
            self.evaluator.verified(
                _result(data),
                context=mismatched_command,
            )
        )


def _context(target: str, requested: int, *, verb: str = "acquire"):
    descriptor = SimpleNamespace(
        command_name="get",
        command=f"get {target} {requested}",
        target_item=target,
        requested_count=requested,
        quantity_semantics="acquire_delta",
        acquisition_verb_class=verb,
        detail_level="typed",
        rollout_state="verified",
    )
    return SimpleNamespace(descriptor=descriptor, before_target_count=4 if target == "oak_log" else 0)


def _nested_data(target: str, requested: int, *, before: int, after: int):
    return {
        "result_reason": "matching_task_finished",
        "result_fidelity": "callback_plus_matching_user_task_event",
        "effect_profile_id": "fabric_chatclef_get_acquire_delta",
        "effect_profile_version": 1,
        "effect_kind": "get_acquisition_delta",
        "effect_payload": {
            "target_item": target,
            "target_match_ids": [f"minecraft:{target}"],
            "quantity_semantics": "ACQUIRE_DELTA",
            "requested_delta": requested,
            "before_target_count": before,
            "after_target_count": after,
            "target_count_delta": after - before,
            "effect_observation_status": "authoritative",
            "effect_observation_reason": "acquisition_delta_observed",
        },
    }


def _result(data: dict):
    return CommandResultDTO(
        request_id="request-1",
        ok=True,
        status=CommandResultStatus.COMPLETED,
        data=data,
    )


if __name__ == "__main__":
    unittest.main()
