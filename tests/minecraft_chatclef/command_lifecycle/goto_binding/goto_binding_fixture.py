#20260913_kpopmodder: Supply one request and independent Java Task-binding fixture.
from dataclasses import replace

from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle.descriptor.command_feedback_descriptor import CommandFeedbackDescriptor
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle.state.command_feedback_context import CommandFeedbackContext


def context(**descriptor_changes):
    descriptor = CommandFeedbackDescriptor(
        command_name="goto", command="goto 500 80 -950", command_source="lavi_chat_ui",
        lifecycle_kind="finite_task", phrase_profile_id="goto_phrase_v1",
        evidence_profile_id="goto_terminal_evidence_v1", rollout_state="verified",
        event_id="a" * 32, input_source="lavi_chat_ui", provider_id="chat",
        event_kind="text", requested_family="movement_goto", intent_kind="goto",
        coordinates=(500, 80, -950), form_kind="trusted_translation",
    )
    return CommandFeedbackContext(
        websocket=object(), owner_token=object(), admission_grant=object(),
        descriptor=replace(descriptor, **descriptor_changes), session_id="session-1",
        generation=1, request_id="request-1", command_message_id="command-1", accepted_at_ms=1,
    )


def payload(**changes):
    return dict(
        request_id="request-1", command_message_id="command-1", session_id="session-1",
        server_connection_generation=1, java_socket_generation=2,
        task_owner="lavi.minecraft.task.movement.gotopreflight.PreparedGotoTask",
        task_identity="64cddc1a", operation_id="operation-1", request_shape="XYZ",
        target_x=500, target_y=80, target_z=-950, requested_dimension=None,
        world_dimension="minecraft:overworld",
    ) | changes


def running_data(**changes):
    return dict(
        goto_profile_id="fabric_chatclef_goto_terminal", goto_profile_version=1,
        goto_binding=payload(),
    ) | changes
