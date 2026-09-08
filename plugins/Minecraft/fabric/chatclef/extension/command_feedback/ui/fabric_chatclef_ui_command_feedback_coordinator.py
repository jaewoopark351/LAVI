#20260907_kpopmodder: Add lifecycle narration around existing direct-GUI submissions only.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.response.command_lifecycle import (
    CommandLifecycleCoalescedResponseFactory,
    CommandLifecycleResponseRenderer,
    CommandLifecycleStartResponseFactory,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle import (
    CommandFeedbackAdmissionCoordinator,
    CommandFeedbackDescriptorFactory,
)

from .fabric_chatclef_ui_feedback_request_binder import (
    FabricChatClefUiFeedbackRequestBinder,
)
from .fabric_chatclef_ui_feedback_start_publisher import (
    FabricChatClefUiFeedbackStartPublisher,
)


class FabricChatClefUiCommandFeedbackCoordinator:
    RAW_SOURCE = "lavi_gui"
    KOREAN_SOURCE = "lavi_gui_korean"

    def __init__(
        self,
        *,
        command_submission,
        natural_language_commands,
        command_feedback_facade,
        start_listener,
        descriptor_factory=None,
        request_binder=None,
        start_publisher=None,
    ) -> None:
        self._command_submission = command_submission
        self._natural_language_commands = natural_language_commands
        self._command_feedback = command_feedback_facade
        self._descriptors = descriptor_factory or CommandFeedbackDescriptorFactory()
        self._admission = CommandFeedbackAdmissionCoordinator(
            live_proof_validator=lambda _proof, _event: False,
            descriptor_factory=self._descriptors,
        )
        self._request_binder = request_binder or FabricChatClefUiFeedbackRequestBinder()
        response_renderer = CommandLifecycleResponseRenderer()
        self._start_publisher = start_publisher or FabricChatClefUiFeedbackStartPublisher(
            response_factory=CommandLifecycleStartResponseFactory(
                response_renderer=response_renderer,
            ),
            coalesced_response_factory=(
                CommandLifecycleCoalescedResponseFactory(
                    response_renderer=response_renderer,
                )
            ),
            listener=start_listener,
        )

    def submit_raw(self, request: object, *, input_event: object):
        bound = self._request_binder.bind(
            request,
            input_event=input_event,
            text_key="command",
            expected_source=self.RAW_SOURCE,
        )
        if bound is None:
            return self._command_submission.submit(request)
        grant = self._command_feedback.command_name_only_grant(
            bound.get("command"),
            input_event=input_event,
        )
        return self._submit(
            grant,
            submit=lambda: self._command_submission.submit(bound),
            fallback_submit=lambda: self._command_submission.submit(request),
        )

    def submit_korean(self, request: object, *, input_event: object):
        bound = self._request_binder.bind(
            request,
            input_event=input_event,
            text_key="text",
            expected_source=self.KOREAN_SOURCE,
        )
        if bound is None:
            return self._natural_language_commands.handle(request)
        translation = self._natural_language_commands.translate(bound)
        descriptor = self._descriptors.from_trusted_translation(
            event=input_event,
            translation=translation,
        )
        grant = self._admission.issue_descriptor(descriptor)
        return self._submit(
            grant,
            submit=lambda: self._natural_language_commands.submit_translated(
                bound,
                translation,
            ),
            fallback_submit=lambda: (
                self._natural_language_commands.submit_translated(
                    request,
                    translation,
                )
            ),
        )

    def _submit(self, grant: object, *, submit, fallback_submit):
        if grant is None or self._command_feedback.reserve(grant) is not True:
            return fallback_submit()
        try:
            result = submit()
            acknowledgement = self._command_feedback.claim_start(grant, result)
            if acknowledgement not in (None, False):
                self._start_publisher.publish(
                    getattr(grant, "descriptor", None),
                    acknowledgement,
                )
            return result
        finally:
            self._command_feedback.abandon(grant)


__all__ = ("FabricChatClefUiCommandFeedbackCoordinator",)
