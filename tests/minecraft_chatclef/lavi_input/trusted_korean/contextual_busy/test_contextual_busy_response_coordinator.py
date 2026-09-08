#20260909_kpopmodder: Verify contextual busy preparation and fail-closed custody.
from __future__ import annotations

import unittest
from dataclasses import replace
from types import SimpleNamespace

from input_core.input_event.contracts import LaviInputEvent
from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_route_decision import (
    MinecraftChatClefInputRouteDecision,
)
from plugins.Minecraft.fabric.chatclef.input.routing.trusted_korean.response.contextual_busy import (
    CONTEXTUAL_BUSY_SUPPRESSED_DECISION,
    ContextualBusyResponseCoordinator,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle import (
    CommandBusyObservedIdentity,
    CommandFeedbackDescriptorFactory,
    CommandFeedbackLifecycleSnapshot,
    CommandFeedbackPublicationAcknowledgement,
    CommandFeedbackPublicationPermit,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle.status.publication import (
    CommandStatusPublicationHandoffFailure,
)


class ContextualBusyResponseCoordinatorTests(unittest.TestCase):
    def test_healthy_response_uses_only_the_active_snapshot_and_preserves_rejection(self):
        acknowledgement, acknowledgements, records = _acknowledgement()
        descriptor = object()
        snapshot = _snapshot(
            acknowledgement=acknowledgement,
            descriptor=descriptor,
        )
        inspections = []
        decision = _busy_decision()
        original_result = decision.result
        original_translation = decision.translation
        coordinator = ContextualBusyResponseCoordinator(
            inspect_busy_status_callback=lambda identity: (
                inspections.append(identity) or snapshot
            ),
            response_renderer=SimpleNamespace(
                render_status=lambda value, query=None: (
                    "다이아 곡괭이 만드는 중이야"
                    if value is snapshot and query is None
                    else self.fail("renderer received mutable or query state")
                )
            ),
            presentation_detail_projector=SimpleNamespace(
                project=lambda value: (
                    (
                        '{"command_name":"get","form_kind":"trusted_translation",'
                        '"target":"diamond_pickaxe"}'
                    )
                    if value is descriptor
                    else self.fail("attempted-command detail was inspected")
                )
            ),
        )

        preparation = _prepare(coordinator, decision)
        prepared = preparation.decision

        self.assertEqual(1, len(inspections))
        self.assertIs(type(inspections[0]), CommandBusyObservedIdentity)
        self.assertEqual("다이아 곡괭이 만드는 중이야", prepared.response_text)
        self.assertEqual("minecraft_command_busy", prepared.reason)
        self.assertIs(original_result, prepared.result)
        self.assertIs(original_translation, prepared.translation)
        self.assertEqual("command_busy_current_work", prepared.route_kind)
        self.assertEqual("command_status", prepared.response_kind)
        self.assertIs(acknowledgement, preparation.local_handoff_token)
        self.assertIs(
            acknowledgement,
            prepared.response_publication_acknowledgement,
        )
        self.assertFalse(prepared.publish_external_response)
        self.assertFalse(prepared.suppress_response)
        self.assertIsNone(prepared.response_emission_capability)
        self.assertNotIn("pumpkin_pie", prepared.response_text)
        self.assertNotIn("pumpkin_pie", prepared.presentation_detail_log)
        self.assertEqual([], acknowledgements)
        self.assertEqual([], records)

    def test_invalid_identity_and_inspection_failure_keep_original_generic_busy(self):
        rows = (
            (
                SimpleNamespace(create=lambda _result: None),
                lambda _identity: self.fail("invalid identity reached inspector"),
                "identity_freeze",
                "none",
            ),
            (
                SimpleNamespace(create=lambda _result: object()),
                lambda _identity: _raise(RuntimeError("private transport detail")),
                "locked_status_inspection",
                "RuntimeError",
            ),
        )
        for identity_factory, inspector, expected_stage, exception_class in rows:
            with self.subTest(stage=expected_stage):
                diagnostics = []
                decision = _busy_decision()
                coordinator = ContextualBusyResponseCoordinator(
                    identity_factory=identity_factory,
                    inspect_busy_status_callback=inspector,
                    failure_logger=SimpleNamespace(
                        record=lambda **values: diagnostics.append(values) or True
                    ),
                )

                preparation = _prepare(coordinator, decision)

                self.assertIs(decision, preparation.decision)
                self.assertIsNone(preparation.local_handoff_token)
                self.assertEqual(1, len(diagnostics))
                self.assertEqual(expected_stage, diagnostics[0]["stage"])
                self.assertEqual(
                    exception_class,
                    diagnostics[0].get("exception_class", "none"),
                )

    def test_raw_handoff_failure_suppresses_without_contextual_duplicate_log(self):
        diagnostics = []
        coordinator = ContextualBusyResponseCoordinator(
            inspect_busy_status_callback=lambda _identity: (
                CommandStatusPublicationHandoffFailure(
                    stage="publication_handoff_snapshot",
                    exception_class="TypeError",
                )
            ),
            failure_logger=SimpleNamespace(
                record=lambda **values: diagnostics.append(values) or True
            ),
        )

        preparation = _prepare(coordinator, _busy_decision())

        self.assertIs(CONTEXTUAL_BUSY_SUPPRESSED_DECISION, preparation.decision)
        self.assertIsNone(preparation.local_handoff_token)
        self.assertEqual([], diagnostics)

    def test_renderer_failure_uses_fixed_cautious_text_under_the_same_ack(self):
        acknowledgement, acknowledgements, records = _acknowledgement()
        coordinator = ContextualBusyResponseCoordinator(
            inspect_busy_status_callback=lambda _identity: _snapshot(
                acknowledgement=acknowledgement,
            ),
            response_renderer=SimpleNamespace(
                render_status=lambda *_args, **_kwargs: _raise(
                    RuntimeError("private renderer detail")
                )
            ),
        )

        preparation = _prepare(coordinator, _busy_decision())

        self.assertEqual(
            "지금 마인크래프트 작업 상태를 확인하지 못했어",
            preparation.decision.response_text,
        )
        self.assertEqual(
            "command_busy_current_work",
            preparation.decision.route_kind,
        )
        self.assertIs(acknowledgement, preparation.local_handoff_token)
        self.assertEqual([("status_rendering", "RuntimeError")], records)
        self.assertEqual([], acknowledgements)

    def test_failed_same_ack_fallback_consumes_ack_once_and_uses_singleton(self):
        acknowledgement, acknowledgements, records = _acknowledgement()
        decorator = SimpleNamespace(
            current_work=lambda *_args, **_kwargs: _raise(RuntimeError("primary")),
            cautious=lambda *_args, **_kwargs: _raise(RuntimeError("fallback")),
        )
        coordinator = ContextualBusyResponseCoordinator(
            inspect_busy_status_callback=lambda _identity: _snapshot(
                acknowledgement=acknowledgement,
            ),
            response_renderer=SimpleNamespace(
                render_status=lambda *_args, **_kwargs: "active work"
            ),
            presentation_detail_projector=SimpleNamespace(
                project=lambda _descriptor: (
                    '{"command_name":"get","form_kind":"trusted_translation",'
                    '"target":"diamond_pickaxe"}'
                )
            ),
            decision_decorator=decorator,
        )

        preparation = _prepare(coordinator, _busy_decision())

        self.assertIs(CONTEXTUAL_BUSY_SUPPRESSED_DECISION, preparation.decision)
        self.assertIsNone(preparation.local_handoff_token)
        self.assertEqual([(False, "status")], acknowledgements)
        self.assertEqual(
            [("primary_decision_assembly", "RuntimeError")],
            records,
        )

    def test_non_busy_decision_is_an_identity_preserving_noop(self):
        decision = MinecraftChatClefInputRouteDecision.not_handled("unrelated")
        coordinator = ContextualBusyResponseCoordinator(
            inspect_busy_status_callback=lambda _identity: self.fail(
                "non-busy decisions must not inspect lifecycle state"
            )
        )

        preparation = _prepare(coordinator, decision)

        self.assertIs(decision, preparation.decision)
        self.assertIsNone(preparation.local_handoff_token)

    def test_pending_and_running_are_independently_rendered_from_frozen_state(self):
        descriptor = CommandFeedbackDescriptorFactory().decode_registered_command_name_only(
            "get diamond_pickaxe 1",
            command_source="lavi_gui",
            event_id="4" * 32,
            provider_id="minecraft_fabric_chatclef_ui",
            event_kind="minecraft_raw_gui_submit",
        )
        descriptor = replace(
            descriptor,
            acquisition_verb_class="craft",
            spoken_target_label="다이아 곡괭이",
        )
        pending_ack, pending_calls, _pending_records = _acknowledgement()
        running_ack, running_calls, _running_records = _acknowledgement()
        snapshots = [
            _snapshot(
                acknowledgement=pending_ack,
                descriptor=descriptor,
                state=CommandFeedbackLifecycleSnapshot.PENDING,
            ),
            _snapshot(
                acknowledgement=running_ack,
                descriptor=descriptor,
                state=CommandFeedbackLifecycleSnapshot.RUNNING,
            ),
        ]
        coordinator = ContextualBusyResponseCoordinator(
            inspect_busy_status_callback=lambda _identity: snapshots.pop(0)
        )

        pending = _prepare(coordinator, _busy_decision()).decision
        running = _prepare(coordinator, _busy_decision()).decision

        self.assertEqual(
            "다이아몬드 곡괭이 만드는 작업이 시작됐는지 확인 중이야",
            pending.response_text,
        )
        self.assertEqual(
            "다이아몬드 곡괭이 만드는 중이야",
            running.response_text,
        )
        self.assertNotEqual(pending.response_text, running.response_text)
        self.assertIs(pending_ack, pending.response_publication_acknowledgement)
        self.assertIs(running_ack, running.response_publication_acknowledgement)
        self.assertIsNot(pending_ack, running_ack)
        self.assertEqual([], pending_calls)
        self.assertEqual([], running_calls)

    def test_duplicate_attempt_fields_are_never_read_for_active_work_rendering(self):
        acknowledgement, acknowledgements, records = _acknowledgement()
        poisoned_attempt = _PoisonedAttemptMapping()
        canonical_result = _busy_decision().result
        decision = MinecraftChatClefInputRouteDecision(
            handled=True,
            reason="minecraft_command_busy",
            response_text="generic busy",
            result=canonical_result,
            translation=poisoned_attempt,
            route_kind="minecraft_command",
        )
        snapshot = _snapshot(acknowledgement=acknowledgement)
        coordinator = ContextualBusyResponseCoordinator(
            inspect_busy_status_callback=lambda _identity: snapshot,
            response_renderer=SimpleNamespace(
                render_status=lambda value, query=None: (
                    "다이아 곡괭이 만드는 중이야"
                    if value is snapshot and query is None
                    else self.fail("unexpected render input")
                )
            ),
            presentation_detail_projector=SimpleNamespace(
                project=lambda _descriptor: ""
            ),
        )

        preparation = _prepare(coordinator, decision)

        self.assertEqual(
            "다이아 곡괭이 만드는 중이야",
            preparation.decision.response_text,
        )
        self.assertIs(poisoned_attempt, preparation.decision.translation)
        self.assertIs(canonical_result, preparation.decision.result)
        self.assertEqual([], acknowledgements)
        self.assertEqual([], records)


def _prepare(coordinator, decision):
    return coordinator.prepare(
        decision=decision,
        event=_event(),
        proof=object(),
        live_proof_validator=lambda _proof, _event: True,
    )


def _busy_decision():
    return MinecraftChatClefInputRouteDecision.handled_result(
        reason="minecraft_command_busy",
        response_text="generic busy",
        result={
            "ok": False,
            "error": "active_command",
            "status": {
                "details": {
                    "commands": {
                        "active_session_id": "session-a",
                        "active_generation": 1,
                        "active_request_id": "request-a",
                        "active_command_message_id": "message-a",
                    }
                }
            },
        },
        translation={"command": "get pumpkin_pie 99"},
        route_kind="minecraft_command",
    )


def _event():
    return LaviInputEvent(
        text="호박 파이 만들어줘",
        source="lavi_chat_ui",
        provider_id="lavi_chat_ui",
        event_kind="chat_submit",
        final=True,
        event_id="3" * 32,
        fallback_payload="호박 파이 만들어줘",
    )


def _snapshot(
    *,
    acknowledgement,
    descriptor=None,
    state=CommandFeedbackLifecycleSnapshot.RUNNING,
):
    return CommandFeedbackLifecycleSnapshot(
        state=state,
        descriptor=descriptor or object(),
        command_name="get",
        requested_family="item_get",
        target_item="diamond_pickaxe",
        requested_count=1,
        result_reason="dispatch_started",
        query_matched=True,
        query_family_matched=True,
        query_target_matched=True,
        owner_present=True,
        availability_reason="",
        publication_acknowledgement=acknowledgement,
        terminal_state="unclaimed",
    )


def _acknowledgement():
    acknowledgements = []
    records = []

    def record_once(stage, exception_class):
        if not records:
            records.append((stage, exception_class))
        return True

    permit = CommandFeedbackPublicationPermit(
        lifecycle_token=object(),
        sequence=1,
        kind=CommandFeedbackPublicationPermit.STATUS,
    )
    acknowledgement = CommandFeedbackPublicationAcknowledgement(
        permit=permit,
        callback=lambda _permit, published: (
            acknowledgements.append((published, _permit.kind)) or True
        ),
        publication_failure_diagnostic_custody=SimpleNamespace(
            record_once=record_once
        ),
    )
    return acknowledgement, acknowledgements, records


def _raise(error):
    raise error


class _PoisonedAttemptMapping(dict):
    def __getitem__(self, _key):
        raise AssertionError("attempted command fields must not be read")

    def get(self, _key, _default=None):
        raise AssertionError("attempted command fields must not be read")

    def __iter__(self):
        raise AssertionError("attempted command fields must not be iterated")


if __name__ == "__main__":
    unittest.main()
