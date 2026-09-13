#20260913_kpopmodder: Build bound GOTO wire fixtures through the production descriptor boundary.
from dataclasses import asdict
from types import SimpleNamespace

from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO
from plugins.Minecraft.fabric.chatclef.result.goto.goto_command_binding_decoder import GotoCommandBindingDecoder
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle import CommandFeedbackDescriptorFactory


def binding_data(**changes):
    return {
        "request_id": "request-goto", "command_message_id": "message-goto",
        "session_id": "session-goto", "server_connection_generation": 2,
        "java_socket_generation": 3,
        "task_owner": "lavi.minecraft.task.movement.gotopreflight.PreparedGotoTask",
        "task_identity": "64cddc1a", "operation_id": "64cddc1a", "request_shape": "XYZ",
        "target_x": 500, "target_y": 80, "target_z": -950,
        "requested_dimension": None, "world_dimension": "minecraft:overworld",
        **changes,
    }


def descriptor(source="lavi_chat_ui"):
    return CommandFeedbackDescriptorFactory().from_trusted_translation(
        event=SimpleNamespace(
            text="500 80 -950으로 가줘", source=source,
            provider_id="VoiceInput" if source == "voice_input_final" else source,
            event_kind="final_transcript" if source == "voice_input_final" else "chat_submit",
            final=True, event_id="a" * 32,
        ),
        translation={
            "status": "validated", "executable": True, "command": "goto 500 80 -950",
            "resolved_target": None,
            "intent": {"intent_type": "goto", "x": 500, "y": 80, "z": -950,
                       "language": "ko", "original_text": "500 80 -950으로 가줘"},
        },
    )


def context(source="lavi_chat_ui", *, binding=None, **changes):
    bound = binding if binding is not None else GotoCommandBindingDecoder().decode(binding_data())
    return SimpleNamespace(
        **{
            "descriptor": descriptor(source), "request_id": "request-goto",
            "command_message_id": "message-goto", "session_id": "session-goto",
            "generation": 2, "goto_binding": bound, **changes,
        }
    )


def data(*, binding=None, **terminal_changes):
    bound = asdict(binding) if binding is not None else binding_data()
    return {
        "result_reason": "matching_task_finished",
        "result_fidelity": "callback_plus_matching_user_task_event",
        "goto_profile_id": "fabric_chatclef_goto_terminal", "goto_profile_version": 1,
        "goto_binding": dict(bound),
        "goto_terminal": {
            **bound, "outcome": "ARRIVED", "failure_reason": "NONE",
            "goal_satisfied": True, "binding_valid": True, "children_quiescent": True,
            "evidence_kind": "prepared_goto_terminal",
            "terminal_dimension": bound["world_dimension"], **terminal_changes,
        },
    }


def result(payload=None, *, status="completed", request_id="request-goto", error_code=None):
    return CommandResultDTO(
        request_id=request_id, status=status, ok=status == "completed",
        error_code=error_code, data=data() if payload is None else payload,
    )


def failed(reason="HANDOFF_SHORTAGE", **changes):
    return result(
        data(**{"outcome": "FAILED", "goal_satisfied": False,
                "failure_reason": reason, **changes}),
        status="failed", error_code="internal_error",
    )
