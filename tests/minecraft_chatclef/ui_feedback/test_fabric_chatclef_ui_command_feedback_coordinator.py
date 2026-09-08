#20260907_kpopmodder: Verify direct GUI feedback wraps, but never replaces, accepted submission.
from __future__ import annotations

import json
import types
import unittest

from input_core.input_event.contracts.lavi_input_event import LaviInputEvent
from plugins.Minecraft.fabric.chatclef.extension.command_feedback import (
    FabricChatClefUiCommandFeedbackCoordinator,
    FabricChatClefUiFeedbackStartListener,
)
from plugins.Minecraft.fabric.chatclef.response.command_lifecycle import (
    CommandLifecycleCoalescedResponse,
    CommandLifecycleResponseRenderer,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle import (
    CommandFeedbackAdmissionGrant,
    CommandFeedbackDescriptor,
)


class FabricChatClefUiCommandFeedbackCoordinatorTests(unittest.TestCase):
    def setUp(self):
        self.command_submission = _CommandSubmission()
        self.natural_language = _NaturalLanguageCommands()
        self.feedback = _CommandFeedbackFacade()
        self.listener = FabricChatClefUiFeedbackStartListener()
        self.published = []
        self.listener.set_callback(self._publish)
        self.coordinator = FabricChatClefUiCommandFeedbackCoordinator(
            command_submission=self.command_submission,
            natural_language_commands=self.natural_language,
            command_feedback_facade=self.feedback,
            start_listener=self.listener,
            descriptor_factory=_DescriptorFactory(),
        )

    def test_raw_registered_command_submits_once_and_publishes_after_claim(self):
        request = {
            "request_id": "raw-1",
            "command": "@get stone 2",
            "source": "lavi_gui",
            "metadata": {"ui": "fabric_chatclef"},
        }
        event = _event(
            text=request["command"],
            source="lavi_gui",
            event_id="1" * 32,
            event_kind="minecraft_raw_gui_submit",
        )

        result = self.coordinator.submit_raw(request, input_event=event)

        self.assertTrue(result["ok"])
        self.assertEqual(1, len(self.command_submission.calls))
        submitted = self.command_submission.calls[0]
        self.assertIsNot(request, submitted)
        self.assertNotIn("input_event", request["metadata"])
        self.assertEqual("1" * 32, submitted["metadata"]["input_event"]["event_id"])
        self.assertEqual([True], self.feedback.acknowledgement.published)
        self.assertEqual("요청한 아이템 구해 올게", self.published[0].text)

    def test_korean_translation_runs_once_and_preserves_typed_descriptor(self):
        request = {
            "request_id": "ko-1",
            "text": "다이아 곡괭이 만들어줘",
            "source": "lavi_gui_korean",
            "metadata": {"language": "ko"},
        }
        event = _event(
            text=request["text"],
            source="lavi_gui_korean",
            event_id="2" * 32,
            event_kind="minecraft_korean_gui_submit",
        )

        result = self.coordinator.submit_korean(request, input_event=event)

        self.assertTrue(result["ok"])
        self.assertEqual(1, self.natural_language.translate_calls)
        self.assertEqual(1, len(self.natural_language.submission_calls))
        submitted, translation = self.natural_language.submission_calls[0]
        self.assertEqual("2" * 32, submitted["metadata"]["input_event"]["event_id"])
        self.assertEqual("get diamond_pickaxe 1", translation["command"])
        self.assertEqual("다이아 곡괭이 만들어 줄게", self.published[0].text)
        self.assertEqual(
            "diamond_pickaxe",
            json.loads(self.published[0].presentation_detail_log)["target"],
        )
        self.assertNotIn("diamond_pickaxe", self.published[0].text)

    def test_mismatched_event_uses_exact_legacy_path_without_feedback(self):
        request = {
            "request_id": "raw-2",
            "command": "@scan dirt",
            "source": "lavi_gui",
            "metadata": {},
        }
        event = _event(
            text="@scan stone",
            source="lavi_gui",
            event_id="3" * 32,
            event_kind="minecraft_raw_gui_submit",
        )

        self.coordinator.submit_raw(request, input_event=event)

        self.assertEqual([request], self.command_submission.calls)
        self.assertEqual(0, self.feedback.reserve_calls)
        self.assertEqual([], self.published)

    def test_unregistered_raw_command_keeps_original_request_metadata(self):
        request = {
            "request_id": "raw-unknown",
            "command": "not_registered value",
            "source": "lavi_gui",
            "metadata": {"ui": "fabric_chatclef"},
        }
        event = _event(
            text=request["command"],
            source="lavi_gui",
            event_id="5" * 32,
            event_kind="minecraft_raw_gui_submit",
        )
        self.feedback.return_no_grant = True

        self.coordinator.submit_raw(request, input_event=event)

        self.assertIs(request, self.command_submission.calls[0])
        self.assertEqual({"ui": "fabric_chatclef"}, request["metadata"])
        self.assertEqual([], self.published)

    def test_failed_start_delivery_acknowledges_false_and_retires_grant(self):
        self.listener.set_callback(lambda _response: None)
        request = {
            "request_id": "raw-3",
            "command": "get dirt 1",
            "source": "lavi_gui",
            "metadata": {},
        }
        event = _event(
            text=request["command"],
            source="lavi_gui",
            event_id="4" * 32,
            event_kind="minecraft_raw_gui_submit",
        )

        self.coordinator.submit_raw(request, input_event=event)

        self.assertEqual([False], self.feedback.acknowledgement.published)
        self.assertEqual(1, self.feedback.abandon_calls)

    def test_already_staged_success_is_one_terminal_centered_response(self):
        descriptor = _descriptor(
            event_id="6" * 32,
            command="get diamond_pickaxe 1",
            command_source="lavi_gui",
        )
        terminal = _terminal_fact(
            descriptor,
            status="completed",
            verified=True,
            dispatch_started=True,
        )
        self.feedback.acknowledgement.coalesced_terminal = terminal
        request = {
            "request_id": "raw-coalesced-success",
            "command": descriptor.command,
            "source": "lavi_gui",
            "metadata": {},
        }

        self.coordinator.submit_raw(
            request,
            input_event=_event(
                text=request["command"],
                source="lavi_gui",
                event_id=descriptor.event_id,
                event_kind="minecraft_raw_gui_submit",
            ),
        )

        self.assertEqual(1, len(self.published))
        response = self.published[0]
        self.assertIs(type(response), CommandLifecycleCoalescedResponse)
        self.assertEqual("command_coalesced", response.response_kind)
        self.assertEqual(
            CommandLifecycleResponseRenderer().render_terminal(terminal),
            response.text,
        )
        self.assertEqual(
            "diamond_pickaxe",
            json.loads(response.presentation_detail_log)["target"],
        )
        self.assertNotIn("diamond_pickaxe", response.text)
        self.assertEqual(1, self.feedback.acknowledgement.selector_calls)
        self.assertEqual([True], self.feedback.acknowledgement.published)

    def test_already_staged_failure_is_one_terminal_centered_response(self):
        descriptor = _descriptor(
            event_id="7" * 32,
            command="get diamond_pickaxe 1",
            command_source="lavi_gui",
        )
        terminal = _terminal_fact(
            descriptor,
            status="failed",
            verified=False,
            dispatch_started=False,
            result_reason="dispatch_failed",
        )
        self.feedback.acknowledgement.coalesced_terminal = terminal
        request = {
            "request_id": "raw-coalesced-failure",
            "command": descriptor.command,
            "source": "lavi_gui",
            "metadata": {},
        }

        self.coordinator.submit_raw(
            request,
            input_event=_event(
                text=request["command"],
                source="lavi_gui",
                event_id=descriptor.event_id,
                event_kind="minecraft_raw_gui_submit",
            ),
        )

        self.assertEqual(1, len(self.published))
        response = self.published[0]
        self.assertIs(type(response), CommandLifecycleCoalescedResponse)
        self.assertEqual(
            CommandLifecycleResponseRenderer().render_terminal(terminal),
            response.text,
        )
        self.assertEqual(1, self.feedback.acknowledgement.selector_calls)
        self.assertEqual([True], self.feedback.acknowledgement.published)

    def _publish(self, response):
        self.published.append(response)
        return types.SimpleNamespace(output_delivered=True)


class _CommandSubmission:
    def __init__(self):
        self.calls = []

    def submit(self, request):
        self.calls.append(request)
        return _accepted_result()


class _NaturalLanguageCommands:
    def __init__(self):
        self.translate_calls = 0
        self.submission_calls = []

    def translate(self, _request):
        self.translate_calls += 1
        return _translation()

    def handle(self, request):
        self.translate_calls += 1
        return {"legacy": request}

    def submit_translated(self, request, translation):
        self.submission_calls.append((request, translation))
        return _accepted_result()


class _CommandFeedbackFacade:
    def __init__(self):
        self.reserve_calls = 0
        self.abandon_calls = 0
        self.return_no_grant = False
        self.acknowledgement = _Acknowledgement()

    def command_name_only_grant(self, command, *, input_event):
        if self.return_no_grant:
            return None
        return CommandFeedbackAdmissionGrant._issue(
            descriptor=_descriptor(
                event_id=input_event.event_id,
                command=command,
                command_source=input_event.source,
                detail_level=CommandFeedbackDescriptor.COMMAND_NAME_ONLY,
                target_item=None,
                spoken_target_label="",
                requested_count=None,
                acquisition_verb_class="",
            )
        )

    def reserve(self, grant):
        self.reserve_calls += 1
        return grant.reserve()

    def claim_start(self, _grant, _result):
        return self.acknowledgement

    def abandon(self, grant):
        self.abandon_calls += 1
        return grant.abandon_if_reserved()


class _DescriptorFactory:
    def accepts_descriptor(self, descriptor):
        return type(descriptor) is CommandFeedbackDescriptor

    def from_trusted_translation(self, *, event, translation):
        return _descriptor(
            event_id=event.event_id,
            command=translation["command"],
            command_source=event.source,
        )


class _Acknowledgement:
    def __init__(self):
        self.published = []
        self.coalesced_terminal = None
        self.selector_calls = 0

    def wait_until_ready(self, *, timeout_seconds):
        return timeout_seconds == 2.0

    def acknowledge(self, *, published):
        self.published.append(published)
        return True

    def select_coalesced_terminal(self):
        self.selector_calls += 1
        return self.coalesced_terminal


def _descriptor(
    *,
    event_id,
    command,
    command_source,
    detail_level=CommandFeedbackDescriptor.TYPED,
    target_item="diamond_pickaxe",
    spoken_target_label="다이아 곡괭이",
    requested_count=1,
    acquisition_verb_class="craft",
):
    return CommandFeedbackDescriptor(
        command_name="get",
        command=command,
        command_source=command_source,
        lifecycle_kind="task",
        phrase_profile_id="get_phrase_v1",
        evidence_profile_id="get_effect_v1",
        rollout_state="verified",
        event_id=event_id,
        input_source=command_source,
        provider_id="minecraft_fabric_chatclef_ui",
        event_kind=(
            "minecraft_korean_gui_submit"
            if command_source == "lavi_gui_korean"
            else "minecraft_raw_gui_submit"
        ),
        requested_family="item_get",
        intent_kind="get_item",
        target_item=target_item,
        requested_count=requested_count,
        quantity_semantics=CommandFeedbackDescriptor.ACQUIRE_DELTA,
        acquisition_verb_class=acquisition_verb_class,
        spoken_target_label=spoken_target_label,
        detail_level=detail_level,
    )


def _event(*, text, source, event_id, event_kind):
    return LaviInputEvent(
        text=text,
        source=source,
        event_id=event_id,
        event_kind=event_kind,
        final=True,
        provider_id="minecraft_fabric_chatclef_ui",
        fallback_payload=None,
    )


def _translation():
    return {
        "status": "validated",
        "executable": True,
        "command": "get diamond_pickaxe 1",
        "intent": {
            "language": "ko",
            "intent_type": "get_item",
            "quantity": 1,
            "item_phrase": "다이아 곡괭이",
            "original_text": "다이아 곡괭이 만들어줘",
        },
        "resolved_target": "diamond_pickaxe",
    }


def _accepted_result():
    return {
        "ok": True,
        "status": {
            "ok": True,
            "status": "accepted",
            "request_id": "request",
            "data": {},
        },
    }


def _terminal_fact(
    descriptor,
    *,
    status,
    verified,
    dispatch_started,
    result_reason="",
):
    return types.SimpleNamespace(
        descriptor=descriptor,
        status=status,
        verified=verified,
        dispatch_started=dispatch_started,
        result_reason=result_reason,
        event_id=descriptor.event_id,
    )


if __name__ == "__main__":
    unittest.main()
