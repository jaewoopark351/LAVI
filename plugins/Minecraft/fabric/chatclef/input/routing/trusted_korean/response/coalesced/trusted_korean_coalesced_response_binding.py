#20260915_kpopmodder: Bind deferred output selection to the one live-authorized publication turn.
from dataclasses import replace
import threading

from input_core.input_event.provenance.trusted_user_ingress.routed_response_deferred_selection import RoutedResponseDeferredSelection
from input_core.input_event.provenance.trusted_user_ingress.routed_response_emission_capability import RoutedResponseEmissionCapability
from plugins.Minecraft.fabric.chatclef.response.command_lifecycle.coalesced.command_lifecycle_coalesced_response_factory import CommandLifecycleCoalescedResponseFactory
from plugins.Minecraft.fabric.chatclef.response.command_lifecycle.publication.command_feedback_ready_decision_acknowledgement import CommandFeedbackReadyDecisionAcknowledgement
from plugins.Minecraft.fabric.chatclef.response.command_lifecycle.selection.command_feedback_initial_response_selector import CommandFeedbackInitialResponseSelector
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle.command_feedback_server_api import CommandFeedbackServerApi
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle.publication.command_feedback_publication_acknowledgement import CommandFeedbackPublicationAcknowledgement
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle.publication.command_feedback_publication_permit import CommandFeedbackPublicationPermit
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle.terminal.command_terminal_fact import CommandTerminalFact


class TrustedKoreanCoalescedResponseBinding:
    def __init__(self, ready, event):
        self._ready = ready
        self._ack = ready._acknowledgement
        self._permit = self._ack._permit
        self._token = self._permit.lifecycle_token
        self._selector = self._ack._coalesced_terminal_selector
        self._factory = ready._coalesced_response_factory
        self._event_id = event.event_id
        self._decision = None
        self._response = None
        self._resolved = False
        self._selection_valid = False
        self._lock = threading.Lock()

    @staticmethod
    def create(decision, event):
        ready = decision.response_publication_acknowledgement
        if (decision.response_kind != "command_start"
                or type(ready) is not CommandFeedbackReadyDecisionAcknowledgement
                or type(ready._initial_response_selector) is not CommandFeedbackInitialResponseSelector
                or type(ready._coalesced_response_factory) is not CommandLifecycleCoalescedResponseFactory
                or type(ready._acknowledgement) is not CommandFeedbackPublicationAcknowledgement):
            return None
        ack = ready._acknowledgement
        selector = ack._coalesced_terminal_selector
        if (type(ack._permit) is not CommandFeedbackPublicationPermit
                or ack._permit.kind != CommandFeedbackPublicationPermit.START
                or type(getattr(selector, "__self__", None)) is not CommandFeedbackServerApi
                or getattr(selector, "__func__", None) is not CommandFeedbackServerApi._select_coalesced_terminal):
            return None
        return TrustedKoreanCoalescedResponseBinding(ready, event)

    def selection(self):
        return RoutedResponseDeferredSelection(
            self, TrustedKoreanCoalescedResponseBinding.commit_selected)

    def bind_decision(self, decision):
        with self._lock:
            if self._decision is not None:
                return False
            self._decision = decision
            self._ready._trusted_response_binding = self
            return True

    def resolve(self, decision):
        with self._lock:
            if decision is not self._decision or self._resolved:
                self._observe(False, "decision_identity_or_replay")
                return None
            self._resolved = True
            if (self._ack._coalesced_terminal_selector is not self._selector
                    or self._ready._coalesced_response_factory is not self._factory):
                self._observe(False, "selection_owner_changed")
                return None
        # Selection is exclusively the existing exact server/permit owner. Never
        # interpret a similarly named caller callback as output authority.
        terminal = CommandFeedbackPublicationAcknowledgement.select_coalesced_terminal(
            self._ack, expected_selector=self._selector)
        if terminal is None:
            with self._lock:
                self._selection_valid = True
            return decision
        if (type(terminal) is not CommandTerminalFact
                or terminal.event_id != self._event_id
                or terminal.descriptor.event_id != self._event_id):
            self._observe(False, "terminal_event_mismatch")
            return None
        try:
            response = CommandLifecycleCoalescedResponseFactory.build(
                self._factory, terminal)
        except Exception:
            self._observe(False, "render_failed")
            return None
        if response.event_id != self._event_id:
            self._observe(False, "rendered_event_mismatch")
            return None
        with self._lock:
            self._response = response
            self._selection_valid = True
        capability = decision.response_emission_capability
        if type(capability) is not RoutedResponseEmissionCapability:
            self._observe(False, "capability_type_mismatch")
            return None
        replacement = RoutedResponseEmissionCapability._replace_from_selection(capability, self)
        self._observe(replacement is not None, "selected" if replacement is not None else "replacement_rejected")
        if replacement is None:
            return None
        return replace(decision, response_text=response.text,
                       response_speech_text=response.speech_text,
                       route_kind=response.route_kind, response_kind=response.response_kind,
                       response_emission_capability=replacement,
                       response_publication_acknowledgement=self._ready,
                       presentation_detail_log=response.presentation_detail_log)

    def commit_selected(self, callback):
        if type(self) is not TrustedKoreanCoalescedResponseBinding:
            return None
        # Lock order: capability -> binding -> acknowledgement -> permit.
        # acknowledge/reset never acquire a capability or binding lock. Holding
        # the permit condition makes cancellation and emission commit ordered.
        with self._lock, self._ack._lock, self._permit._condition:
            if (self._decision is None or not self._selection_valid or self._ack._spent
                    or self._ready._acknowledgement is not self._ack
                    or self._ack._permit is not self._permit
                    or self._ack._coalesced_terminal_selector is not self._selector
                    or self._ready._coalesced_response_factory is not self._factory
                    or self._permit.lifecycle_token is not self._token
                    or self._permit._state != CommandFeedbackPublicationPermit._READY):
                return None
            if self._response is None:
                return callback(self._decision.response_text, "minecraft_chatclef", "command_start", None)
            return callback(self._response.text, "minecraft_chatclef", "command_coalesced", self._response.speech_text)

    def _observe(self, accepted, reason):
        try:
            from core.logger import log_print
            log_print("[Minecraft] event=command_coalesced_output_authority "
                      f"event_id={self._event_id} old_kind=command_start "
                      f"new_kind=command_coalesced accepted={str(accepted).lower()} "
                      f"reason={reason}")
        except Exception:
            pass


__all__ = ("TrustedKoreanCoalescedResponseBinding",)
