#20260915_kpopmodder: Derive speech only from the terminal owner's correlated immutable list fact.
from plugins.Minecraft.fabric.chatclef.result.instant import InstantCommandPayload
from plugins.Minecraft.fabric.chatclef.result.instant.instant_command_values_validator import InstantCommandValuesValidator
from plugins.Minecraft.fabric.chatclef.transport.fabric_chatclef_active_command import FabricChatClefActiveCommand
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle.descriptor.command_feedback_descriptor import CommandFeedbackDescriptor
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle.terminal.command_terminal_fact import CommandTerminalFact

from .korean_trusted_destination_list_renderer import KoreanTrustedDestinationListRenderer


class TrustedDestinationListSpeechProjector:
    @staticmethod
    def project(fact):
        # The existing result correlator claims this owner only after matching the
        # socket, session, generation, request and command message. Text labels do
        # not replace that fact; no caller receives a SafetyFilter exemption.
        if type(fact) is not CommandTerminalFact:
            return None
        descriptor, owner, payload = fact.descriptor, fact.owner_token, fact.evidence_projection
        if (type(descriptor) is not CommandFeedbackDescriptor
                or type(owner) is not FabricChatClefActiveCommand
                or type(payload) is not InstantCommandPayload
                or type(descriptor.command) is not str or type(owner.command) is not str
                or fact.status != "completed" or fact.verified is not True
                or fact.result_reason != "instant_command_observed"
                or fact.event_id != descriptor.event_id
                or descriptor.command_name != "auto_deposit_trusted_list"
                or descriptor.command.lstrip("@").strip() != "auto_deposit_trusted_list"
                or owner.command.lstrip("@").strip() != payload.command
                or payload.command != "auto_deposit_trusted_list"
                or payload.command_name != descriptor.command_name
                or payload.outcome != "completed" or payload.reason != "LISTED"
                or owner.websocket is None or type(owner.request_id) is not str or not owner.request_id
                or type(owner.session_id) is not str or not owner.session_id
                or type(owner.generation) is not int or owner.generation < 1
                or not InstantCommandValuesValidator.valid(
                    payload.command_name, payload.command, payload.outcome, payload.reason, payload.values)):
            return None
        return KoreanTrustedDestinationListRenderer.render(payload.values, speech=True)
