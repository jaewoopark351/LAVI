#20260908_kpopmodder: Verify pure contextual STATUS claim eligibility and exact constraints.
from __future__ import annotations

import unittest
from dataclasses import FrozenInstanceError, replace
from types import SimpleNamespace

from plugins.Minecraft.fabric.chatclef.input.status.command_lifecycle import (
    CommandStatusQuery,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle import (
    CommandFeedbackDescriptorFactory,
    ContextualCommandStatusClaimEvaluation,
    ContextualCommandStatusClaimEvaluator,
    ContextualCommandStatusClaimFailure,
)


class ContextualCommandStatusClaimEvaluatorTests(unittest.TestCase):
    def setUp(self) -> None:
        self.evaluator = ContextualCommandStatusClaimEvaluator()
        self.owner = object()
        self.descriptor = _production_descriptor("get iron_ingot 10")

    def test_prefixless_generic_and_matching_family_claim_exact_owner(self):
        for query in (
            CommandStatusQuery("any", False),
            CommandStatusQuery("item_get", False),
        ):
            with self.subTest(query=query):
                evaluation = self._evaluate(query)
                self.assertTrue(evaluation.claimed)
                self.assertTrue(evaluation.family_matched)
                self.assertTrue(evaluation.target_matched)
                self.assertTrue(evaluation.ordinary_context)
                self.assertTrue(evaluation.owner_matched)

    def test_prefixless_family_target_quantity_and_player_mismatches_fail_closed(self):
        fixtures = (
            (CommandStatusQuery("movement_goto", False), self.descriptor),
            (
                CommandStatusQuery(
                    "item_get",
                    False,
                    f"{self.descriptor.spoken_target_label} 9개",
                ),
                self.descriptor,
            ),
            (
                CommandStatusQuery("movement_follow", False, "Alex"),
                _production_descriptor("follow Steve"),
            ),
        )
        for query, descriptor in fixtures:
            with self.subTest(query=query):
                self.assertFalse(self._evaluate(query, descriptor=descriptor).claimed)

    def test_prefixless_exact_target_quantity_and_player_claim(self):
        self.assertTrue(
            self._evaluate(
                CommandStatusQuery(
                    "item_get",
                    False,
                    f"{self.descriptor.spoken_target_label} 10개",
                )
            ).claimed
        )
        follow = _production_descriptor("follow Steve")
        self.assertTrue(
            self._evaluate(
                CommandStatusQuery("movement_follow", False, "Steve를"),
                descriptor=follow,
            ).claimed
        )

    def test_addressed_mismatch_claims_actual_ordinary_owner_but_keeps_match_facts(self):
        evaluation = self._evaluate(CommandStatusQuery("movement_goto", True))

        self.assertTrue(evaluation.claimed)
        self.assertFalse(evaluation.family_matched)
        self.assertTrue(evaluation.target_matched)

    def test_stop_specialized_stale_and_terminal_contexts_fail_closed(self):
        fixtures = (
            dict(descriptor=_production_descriptor("stop")),
            dict(
                descriptor=replace(
                    self.descriptor,
                    response_lifecycle_kind="specialized_control",
                )
            ),
            dict(active_command=object()),
            dict(terminal_claimed=True),
        )
        for values in fixtures:
            with self.subTest(values=values):
                self.assertFalse(self._evaluate(CommandStatusQuery("any", False), **values).claimed)

    def test_malformed_values_return_rejected_evaluation_without_throwing(self):
        fixtures = (
            dict(query={}),
            dict(query=SimpleNamespace(requested_family="any", addressed=1, target_text="")),
            dict(query=CommandStatusQuery("any", False, "철")),
            dict(query=CommandStatusQuery("item_give", False)),
            dict(query=CommandStatusQuery("movement_goto", False, "집")),
            dict(query=CommandStatusQuery("item_get", False, " 철 ")),
            dict(descriptor={}),
            dict(
                descriptor=SimpleNamespace(
                    command_name="get",
                    requested_family="item_get",
                    target_item="iron_ingot",
                    requested_count=10,
                    spoken_target_label="철",
                    player_name="",
                )
            ),
            dict(descriptor=_descriptor(count=True)),
            dict(terminal_claimed=1),
            dict(owner_token=None, active_command=None),
        )
        for values in fixtures:
            with self.subTest(values=values):
                defaults = dict(
                    query=CommandStatusQuery("any", False),
                    descriptor=self.descriptor,
                    active_command=self.owner,
                    owner_token=self.owner,
                    terminal_claimed=False,
                )
                defaults.update(values)
                evaluation = self.evaluator.evaluate(**defaults)
                self.assertIs(type(evaluation), ContextualCommandStatusClaimEvaluation)
                self.assertFalse(evaluation.claimed)

    def test_unregistered_and_profile_contradictory_descriptors_never_claim(self):
        contradictions = (
            replace(self.descriptor, command_name="unregistered"),
            replace(self.descriptor, lifecycle_kind="command"),
            replace(self.descriptor, phrase_profile_id="wrong_phrase_v1"),
            replace(self.descriptor, evidence_profile_id="wrong_evidence_v1"),
            replace(self.descriptor, rollout_state="cautious"),
            replace(self.descriptor, requested_family="movement_goto"),
            replace(self.descriptor, response_lifecycle_kind="persistent_task"),
        )
        for descriptor in contradictions:
            with self.subTest(descriptor=descriptor):
                evaluation = self._evaluate(
                    CommandStatusQuery("any", False),
                    descriptor=descriptor,
                )
                self.assertFalse(evaluation.claimed)
                self.assertFalse(evaluation.ordinary_context)

    def test_evaluation_is_frozen_and_exact_bool_validated(self):
        evaluation = self._evaluate(CommandStatusQuery("any", False))
        with self.assertRaises(FrozenInstanceError):
            evaluation.claimed = False
        with self.assertRaises(TypeError):
            ContextualCommandStatusClaimEvaluation(True, True, True, True, 1)

    def test_unexpected_resolver_exception_is_bounded(self):
        evaluator = ContextualCommandStatusClaimEvaluator(
            target_resolver=SimpleNamespace(resolve=_raise_runtime_error)
        )
        with self.assertRaises(ContextualCommandStatusClaimFailure) as caught:
            evaluator.evaluate(
                query=CommandStatusQuery("any", False),
                descriptor=self.descriptor,
                active_command=self.owner,
                owner_token=self.owner,
                terminal_claimed=False,
            )
        self.assertEqual("claim_evaluation", caught.exception.stage)
        self.assertEqual("RuntimeError", caught.exception.exception_class)
        self.assertNotIn("secret", str(caught.exception))

    def test_claim_failure_exception_class_uses_exact_ascii_identifier_boundary(self):
        accepted = "E" + ("x" * 95)

        self.assertEqual(
            accepted,
            ContextualCommandStatusClaimFailure(
                exception_class=accepted,
            ).exception_class,
        )
        for rejected in ("E" + ("x" * 96), "잘못된예외", "contains.dot"):
            with self.subTest(rejected=rejected):
                self.assertEqual(
                    "invalid",
                    ContextualCommandStatusClaimFailure(
                        exception_class=rejected,
                    ).exception_class,
                )

    def test_evaluator_preserves_the_first_typed_claim_failure(self):
        first = ContextualCommandStatusClaimFailure(
            exception_class="FirstClaimFailure",
        )

        def raise_first(_query, _descriptor):
            raise first

        evaluator = ContextualCommandStatusClaimEvaluator(
            target_resolver=SimpleNamespace(resolve=raise_first)
        )
        with self.assertRaises(ContextualCommandStatusClaimFailure) as caught:
            evaluator.evaluate(
                query=CommandStatusQuery("any", False),
                descriptor=self.descriptor,
                active_command=self.owner,
                owner_token=self.owner,
                terminal_claimed=False,
            )
        self.assertIs(first, caught.exception)

    def _evaluate(
        self,
        query,
        *,
        descriptor=None,
        active_command=None,
        owner_token=None,
        terminal_claimed=False,
    ):
        return self.evaluator.evaluate(
            query=query,
            descriptor=self.descriptor if descriptor is None else descriptor,
            active_command=self.owner if active_command is None else active_command,
            owner_token=self.owner if owner_token is None else owner_token,
            terminal_claimed=terminal_claimed,
        )


def _descriptor(
    *,
    command_name="get",
    family="item_get",
    target="iron_ingot",
    count=10,
    label="철",
    player="",
    lifecycle_kind="finite_task",
):
    return SimpleNamespace(
        command_name=command_name,
        requested_family=family,
        target_item=target,
        requested_count=count,
        spoken_target_label=label,
        player_name=player,
        response_lifecycle_kind=lifecycle_kind,
    )


def _production_descriptor(command: str):
    descriptor = CommandFeedbackDescriptorFactory().decode_registered_command_name_only(
        command,
        command_source="lavi_gui",
        event_id="0" * 32,
        provider_id="minecraft_fabric_chatclef_ui",
        event_kind="minecraft_raw_gui_submit",
    )
    if descriptor is None:
        raise AssertionError(f"descriptor fixture was not accepted: {command}")
    return descriptor


def _raise_runtime_error(_query, _descriptor):
    raise RuntimeError("secret raw payload")


if __name__ == "__main__":
    unittest.main()
