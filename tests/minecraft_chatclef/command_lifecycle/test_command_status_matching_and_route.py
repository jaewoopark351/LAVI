#20260907_kpopmodder: Lock generalized read-only status specificity and metadata.
from __future__ import annotations

import unittest
from types import SimpleNamespace

from input_core.input_event.contracts import LaviInputEvent
from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_route_decision import (
    MinecraftChatClefInputRouteDecision,
)
from plugins.Minecraft.fabric.chatclef.input.routing.orchestration.minecraft_input_route_sequence import (
    MinecraftInputRouteSequence,
)
from plugins.Minecraft.fabric.chatclef.input.status.command_lifecycle import (
    CommandStatusQuery,
    CommandStatusQueryClassifier,
    CommandStatusRouteOwner,
    CommandStatusTargetResolver,
)
from plugins.Minecraft.fabric.chatclef.response.command_lifecycle import (
    CommandLifecycleResponseRenderer,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle import (
    CommandFeedbackLifecycleSnapshot,
    CommandFeedbackStatusCoordinator,
    CommandTerminalEvidenceProfileRegistry,
    CommandStatusResolution,
    CommandStatusTargetResolver as LifecycleStatusTargetResolver,
)


class CommandStatusMatchingAndRouteTests(unittest.TestCase):
    def setUp(self) -> None:
        self.classifier = CommandStatusQueryClassifier()
        self.craft = _descriptor(
            command_name="get",
            family="item_get",
            target="diamond_pickaxe",
            label="다이아 곡괭이",
        )
        self.goto = _descriptor(
            command_name="goto",
            family="movement_goto",
            coordinates=(1, 64, 3),
        )
        self.resolver = CommandStatusTargetResolver()

    def test_input_resolver_is_the_lifecycle_owned_matcher(self):
        self.assertIs(CommandStatusTargetResolver, LifecycleStatusTargetResolver)

    def test_addressed_generic_any_claims_current_active_command(self):
        query = self.classifier.classify("마크 지금 뭐 하고 있어?")

        self.assertEqual("any", query.requested_family)
        self.assertTrue(query.addressed)
        self.assertTrue(self.resolver.resolve(query, self.goto).claimed)

    def test_prefixless_generic_any_falls_through(self):
        query = self.classifier.classify("뭐 하고 있어?")

        self.assertIsNone(query)

    def test_prefixless_explicit_family_without_target_falls_through(self):
        query = self.classifier.classify("뭐 만드는 중이야?")

        self.assertIsNone(query)

    def test_addressing_does_not_override_family_or_target_mismatch(self):
        family_query = self.classifier.classify("마크 뭐 만드는 중이야?")
        target_query = self.classifier.classify("마크 철 곡괭이 만드는 중이야?")

        family_resolution = self.resolver.resolve(family_query, self.goto)
        target_resolution = self.resolver.resolve(target_query, self.craft)

        self.assertTrue(family_resolution.claimed)
        self.assertFalse(family_resolution.family_matched)
        self.assertTrue(target_resolution.claimed)
        self.assertFalse(target_resolution.target_matched)

    def test_addressed_mismatch_reports_the_actual_active_work(self):
        owner = object()
        coordinator = CommandFeedbackStatusCoordinator(
            evidence_profiles=CommandTerminalEvidenceProfileRegistry(),
        )
        state = SimpleNamespace(
            context=SimpleNamespace(descriptor=self.craft, owner_token=owner),
            terminal_claimed=False,
            latest_result_reason="dispatch_started",
            latest_status="running",
        )
        for text in (
            "마크 어디로 가는 중이야?",
            "마크 철 곡괭이 만드는 중이야?",
        ):
            with self.subTest(text=text):
                query = self.classifier.classify(text)
                snapshot = coordinator.inspect(
                    state=state,
                    active_command=owner,
                    connected=True,
                    quarantine_active=False,
                    query=query,
                )

                self.assertTrue(snapshot.query_matched)
                self.assertEqual(CommandFeedbackLifecycleSnapshot.RUNNING, snapshot.state)
                self.assertEqual(
                    "다이아 곡괭이 만드는 중이야",
                    CommandLifecycleResponseRenderer().render_status(snapshot, query),
                )

    def test_addressed_no_owner_is_distinct_from_unavailable(self):
        query = self.classifier.classify("마크 지금 뭐 하고 있어?")
        snapshot = CommandFeedbackStatusCoordinator(
            evidence_profiles=CommandTerminalEvidenceProfileRegistry(),
        ).inspect(
            state=SimpleNamespace(context=None, terminal_claimed=False),
            active_command=None,
            connected=True,
            quarantine_active=False,
            query=query,
        )

        self.assertEqual(CommandFeedbackLifecycleSnapshot.IDLE, snapshot.state)
        self.assertFalse(snapshot.owner_present)
        self.assertEqual("no_tracked_owner", snapshot.availability_reason)
        self.assertEqual(
            "지금 내가 처리 중인 마인크래프트 명령은 없어",
            CommandLifecycleResponseRenderer().render_status(snapshot, query),
        )

    def test_unaddressed_explicit_target_requires_exact_active_target(self):
        matching = self.classifier.classify("다이아 곡괭이 만드는 중이야?")
        mismatch = self.classifier.classify("철 곡괭이 만드는 중이야?")

        self.assertTrue(self.resolver.resolve(matching, self.craft).claimed)
        self.assertFalse(self.resolver.resolve(mismatch, self.craft).claimed)

    def test_natural_object_particle_keeps_exact_target_identity(self):
        item = self.classifier.classify("다이아 곡괭이를 만드는 중이야?")
        follow = self.classifier.classify("Steve를 따라가는 중이야?")
        follow_descriptor = _descriptor(
            command_name="follow",
            family="movement_follow",
        )
        follow_descriptor.player_name = "Steve"

        self.assertTrue(self.resolver.resolve(item, self.craft).claimed)
        self.assertTrue(
            self.resolver.resolve(follow, follow_descriptor).claimed
        )

    def test_status_coordinator_composes_injected_target_resolver(self):
        resolver = _RejectingResolver()
        coordinator = CommandFeedbackStatusCoordinator(
            evidence_profiles=SimpleNamespace(),
            target_resolver=resolver,
        )
        owner = object()
        state = SimpleNamespace(
            context=SimpleNamespace(descriptor=self.craft, owner_token=owner),
            terminal_claimed=False,
            latest_result_reason="dispatch_started",
            latest_status="running",
        )
        query = CommandStatusQuery(requested_family="item_get", addressed=True)

        snapshot = coordinator.inspect(
            state=state,
            active_command=owner,
            connected=True,
            quarantine_active=False,
            query=query,
        )

        self.assertEqual(1, resolver.calls)
        self.assertFalse(snapshot.query_matched)
        self.assertEqual(CommandFeedbackLifecycleSnapshot.UNAVAILABLE, snapshot.state)

    def test_general_route_and_sequence_preserve_command_status_metadata(self):
        snapshot = CommandFeedbackLifecycleSnapshot(
            state=CommandFeedbackLifecycleSnapshot.RUNNING,
            descriptor=self.craft,
            command_name="get",
            requested_family="item_get",
            target_item="diamond_pickaxe",
            requested_count=1,
            result_reason="dispatch_started",
        )
        owner = CommandStatusRouteOwner(
            extension=SimpleNamespace(
                inspect_command_feedback_status=lambda _query: snapshot
            ),
            live_proof_validator=lambda proof, _event: proof is _PROOF,
            classifier=self.classifier,
            response_renderer=CommandLifecycleResponseRenderer(),
            presentation_detail_projector=SimpleNamespace(
                project=lambda descriptor: (
                    '{"command_name":"get","form_kind":"target_count",'
                    '"requested_count":1,"target":"diamond_pickaxe"}'
                    if descriptor is self.craft
                    else ""
                )
            ),
        )
        event = SimpleNamespace(text="마크 뭐 만드는 중이야?")
        sequence = MinecraftInputRouteSequence(
            input_event_normalizer=SimpleNamespace(normalize=lambda _value: event),
            stop_control_route_owner=None,
            crafting_status_route_owner=owner,
            generic_crafting_defaults_route_owner=None,
            auto_deposit_trust_route_coordinator=None,
            ordinary_command_route_coordinator=None,
        )

        decision = sequence.route(
            event,
            korean_eligibility_proof=_PROOF,
            optional_route_callback=lambda route_owner, route_event, proof, _reason: (
                None
                if route_owner is None
                else route_owner.try_route(route_event, proof)
            ),
            gate_inspection_callback=lambda _text: self.fail(
                "status ownership must not reach command gating"
            ),
        )

        self.assertIs(type(decision), MinecraftChatClefInputRouteDecision)
        self.assertEqual("command_status_query", decision.route_kind)
        self.assertEqual("command_status", decision.response_kind)
        self.assertEqual("다이아 곡괭이 만드는 중이야", decision.response_text)
        self.assertIn("diamond_pickaxe", decision.presentation_detail_log)
        self.assertNotIn("diamond_pickaxe", decision.response_text)
        self.assertFalse(decision.result["command_submitted"])

    def test_addressed_no_owner_is_owned_for_chat_and_voice_but_prefixless_falls_through(self):
        snapshot = CommandFeedbackLifecycleSnapshot(
            state=CommandFeedbackLifecycleSnapshot.IDLE,
            query_matched=True,
            owner_present=False,
            availability_reason="no_tracked_owner",
        )
        owner = CommandStatusRouteOwner(
            extension=SimpleNamespace(
                inspect_command_feedback_status=lambda _query: snapshot
            ),
            live_proof_validator=lambda proof, event: proof == event.event_id,
            classifier=self.classifier,
            response_renderer=CommandLifecycleResponseRenderer(),
        )
        for source, provider_id, event_kind in (
            ("lavi_chat_ui", "lavi_chat_ui", "chat_submit"),
            ("voice_input_final", "VoiceInput", "final_transcript"),
        ):
            with self.subTest(source=source):
                event = LaviInputEvent(
                    text="마크 지금 뭐 하고 있어?",
                    source=source,
                    provider_id=provider_id,
                    event_kind=event_kind,
                    final=True,
                    event_id="a" * 32,
                    fallback_payload="마크 지금 뭐 하고 있어?",
                )
                decision = owner.try_route(event, event.event_id)

                self.assertIsNotNone(decision)
                self.assertEqual("command_status_query", decision.route_kind)
                self.assertEqual(
                    "지금 내가 처리 중인 마인크래프트 명령은 없어",
                    decision.response_text,
                )
                prefixless = LaviInputEvent(
                    text="지금 뭐 하고 있어?",
                    source=source,
                    provider_id=provider_id,
                    event_kind=event_kind,
                    final=True,
                    event_id="b" * 32,
                    fallback_payload="지금 뭐 하고 있어?",
                )
                self.assertIsNone(owner.try_route(prefixless, prefixless.event_id))


class _RejectingResolver:
    def __init__(self) -> None:
        self.calls = 0

    def resolve(self, _query: object, _descriptor: object):
        self.calls += 1
        return CommandStatusResolution(
            claimed=False,
            family_matched=False,
            target_matched=True,
        )


def _descriptor(
    *,
    command_name: str,
    family: str,
    target: str | None = None,
    label: str = "",
    coordinates: tuple[int, int, int] | None = None,
):
    return SimpleNamespace(
        command_name=command_name,
        requested_family=family,
        target_item=target,
        requested_count=1 if target else None,
        spoken_target_label=label,
        acquisition_verb_class="craft",
        player_name="",
        coordinates=coordinates,
        detail_level="typed",
    )


_PROOF = object()


if __name__ == "__main__":
    unittest.main()
