#20260909_kpopmodder: Verify contextual busy parity across the bounded target domain.
from __future__ import annotations

import json
import unittest
from pathlib import Path

from input_core.input_event.contracts import LaviInputEvent
from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_route_decision import (
    MinecraftChatClefInputRouteDecision,
)
from plugins.Minecraft.fabric.chatclef.input.routing.trusted_korean.response.contextual_busy import (
    ContextualBusyResponseCoordinator,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_equipment_target_composer import (
    ChatClefEquipmentTargetComposer,
)
from plugins.Minecraft.fabric.chatclef.response.command_lifecycle import (
    CommandLifecycleResponseRenderer,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle import (
    CommandFeedbackDescriptor,
    CommandFeedbackDescriptorFactory,
    CommandFeedbackLifecycleSnapshot,
    CommandFeedbackPublicationAcknowledgement,
    CommandFeedbackPublicationPermit,
)


class AllTargetContextualBusyParityMatrixTests(unittest.TestCase):
    def test_catalog_actions_and_equipment_match_independent_status_snapshots(self):
        targets = json.loads(_target_policy_path().read_text(encoding="utf-8"))[
            "targets"
        ]
        self.assertEqual(591, len(targets))
        equipment_targets = ChatClefEquipmentTargetComposer().all_targets()
        self.assertTrue(equipment_targets)
        action_domains = {
            "get": tuple(sorted(targets)),
            "deposit": tuple(sorted(targets)),
            "deposit_all": tuple(sorted(targets)),
            "give": tuple(sorted(targets)),
            "equip": tuple(sorted(equipment_targets)),
        }
        descriptors = CommandFeedbackDescriptorFactory()
        renderer = CommandLifecycleResponseRenderer()
        busy_snapshot = [None]
        coordinator = ContextualBusyResponseCoordinator(
            inspect_busy_status_callback=lambda _identity: busy_snapshot[0],
            response_renderer=renderer,
        )
        event = _busy_event()
        routed = 0

        for command_name, command_targets in action_domains.items():
            for target in command_targets:
                with self.subTest(command_name=command_name, target=target):
                    command = (
                        f"equip {target}"
                        if command_name == "equip"
                        else f"{command_name} {target} 2"
                    )
                    descriptor = descriptors.decode_registered_command_name_only(
                        command,
                        command_source="lavi_gui",
                        event_id="d" * 32,
                        provider_id="minecraft_fabric_chatclef_ui",
                        event_kind="minecraft_raw_gui_submit",
                    )
                    self.assertIs(type(descriptor), CommandFeedbackDescriptor)
                    status_ack, status_calls = _acknowledgement(sequence=1)
                    busy_ack, busy_calls = _acknowledgement(sequence=2)
                    status_snapshot = _snapshot(descriptor, status_ack)
                    busy_snapshot[0] = _snapshot(descriptor, busy_ack)
                    rejected = _busy_decision()
                    original_result = rejected.result
                    original_translation = rejected.translation

                    preparation = coordinator.prepare(
                        decision=rejected,
                        event=event,
                        proof=object(),
                        live_proof_validator=lambda _proof, value: value is event,
                    )
                    contextual_busy = preparation.decision

                    self.assertIsNot(status_snapshot, busy_snapshot[0])
                    self.assertIsNot(status_ack, busy_ack)
                    self.assertEqual(
                        renderer.render_status(status_snapshot, query=None),
                        contextual_busy.response_text,
                    )
                    self.assertIs(original_result, contextual_busy.result)
                    self.assertIs(original_translation, contextual_busy.translation)
                    self.assertIs(
                        busy_ack,
                        contextual_busy.response_publication_acknowledgement,
                    )
                    self.assertIs(busy_ack, preparation.local_handoff_token)
                    self.assertEqual(
                        "command_busy_current_work",
                        contextual_busy.route_kind,
                    )
                    self.assertEqual("command_status", contextual_busy.response_kind)
                    self.assertNotIn("attempted_only_target", contextual_busy.response_text)
                    self.assertNotIn(
                        "attempted_only_target",
                        contextual_busy.presentation_detail_log,
                    )
                    self.assertEqual([], status_calls)
                    self.assertEqual([], busy_calls)
                    routed += 1

        self.assertEqual((4 * 591) + len(equipment_targets), routed)

    def test_representative_authorized_target_outside_catalog_keeps_fallback(self):
        descriptor = CommandFeedbackDescriptorFactory().decode_registered_command_name_only(
            "give future_mod_item 2",
            command_source="lavi_gui",
            event_id="e" * 32,
            provider_id="minecraft_fabric_chatclef_ui",
            event_kind="minecraft_raw_gui_submit",
        )
        self.assertIs(type(descriptor), CommandFeedbackDescriptor)
        status_ack, _status_calls = _acknowledgement(sequence=1)
        busy_ack, _busy_calls = _acknowledgement(sequence=2)
        status_snapshot = _snapshot(descriptor, status_ack)
        busy_snapshot = _snapshot(descriptor, busy_ack)
        renderer = CommandLifecycleResponseRenderer()
        coordinator = ContextualBusyResponseCoordinator(
            inspect_busy_status_callback=lambda _identity: busy_snapshot,
            response_renderer=renderer,
        )

        preparation = coordinator.prepare(
            decision=_busy_decision(),
            event=_busy_event(),
            proof=object(),
            live_proof_validator=lambda _proof, _event: True,
        )

        self.assertEqual(
            renderer.render_status(status_snapshot, query=None),
            preparation.decision.response_text,
        )
        self.assertEqual(
            "요청한 아이템 2개 건네는 중이야",
            preparation.decision.response_text,
        )


def _target_policy_path() -> Path:
    return (
        Path(__file__).resolve().parents[5]
        / "plugins"
        / "Minecraft"
        / "fabric"
        / "chatclef"
        / "intent"
        / "resources"
        / "chatclef_item_command_target_policy.json"
    )


def _snapshot(descriptor, acknowledgement):
    return CommandFeedbackLifecycleSnapshot(
        state=CommandFeedbackLifecycleSnapshot.RUNNING,
        descriptor=descriptor,
        command_name=descriptor.command_name,
        requested_family=descriptor.requested_family,
        target_item=descriptor.target_item,
        requested_count=descriptor.requested_count,
        result_reason="dispatch_started",
        query_matched=True,
        query_family_matched=True,
        query_target_matched=True,
        owner_present=True,
        availability_reason="",
        publication_acknowledgement=acknowledgement,
        terminal_state="unclaimed",
    )


def _acknowledgement(*, sequence):
    calls = []
    acknowledgement = CommandFeedbackPublicationAcknowledgement(
        permit=CommandFeedbackPublicationPermit(
            lifecycle_token=object(),
            sequence=sequence,
            kind=CommandFeedbackPublicationPermit.STATUS,
        ),
        callback=lambda _permit, published: calls.append(published) or True,
    )
    return acknowledgement, calls


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
                        "active_session_id": "active-session",
                        "active_generation": 1,
                        "active_request_id": "active-request",
                        "active_command_message_id": "active-message",
                    }
                }
            },
        },
        translation={
            "command": "get attempted_only_target 999",
            "target": "attempted_only_target",
            "requested_count": 999,
        },
        route_kind="minecraft_command",
    )


def _busy_event():
    return LaviInputEvent(
        text="다른 작업 해줘",
        source="lavi_chat_ui",
        provider_id="lavi_chat_ui",
        event_kind="chat_submit",
        final=True,
        event_id="f" * 32,
        fallback_payload="다른 작업 해줘",
    )


if __name__ == "__main__":
    unittest.main()
