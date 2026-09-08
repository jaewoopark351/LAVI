#20260907_kpopmodder: Isolate generalized feedback delegation from extension behavior.
from __future__ import annotations

from input_core.input_event.contracts.lavi_input_event import LaviInputEvent
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle import (
    CommandFeedbackAdmissionCoordinator,
    CommandFeedbackDescriptorFactory,
)


class MinecraftFabricChatClefCommandFeedbackFacade:
    _RAW_GUI_SOURCE = "lavi_gui"
    _RAW_GUI_PROVIDER_ID = "minecraft_fabric_chatclef_ui"
    _RAW_GUI_EVENT_KIND = "minecraft_raw_gui_submit"

    def __init__(self, adapter, *, descriptor_factory=None) -> None:
        self._adapter = adapter
        self._descriptors = descriptor_factory or CommandFeedbackDescriptorFactory()
        self._admission = CommandFeedbackAdmissionCoordinator(
            live_proof_validator=lambda _proof, _event: False,
            descriptor_factory=self._descriptors,
        )

    def command_name_only_grant(self, command: object, *, input_event: object):
        if (
            type(input_event) is not LaviInputEvent
            or not isinstance(command, str)
            or input_event.text != command
            or input_event.source != self._RAW_GUI_SOURCE
            or input_event.provider_id != self._RAW_GUI_PROVIDER_ID
            or input_event.event_kind != self._RAW_GUI_EVENT_KIND
            or input_event.final is not True
        ):
            return None
        descriptor = self._descriptors.decode_registered_command_name_only(
            command,
            command_source=input_event.source,
            event_id=input_event.event_id,
            provider_id=input_event.provider_id,
            event_kind=input_event.event_kind,
        )
        return self._admission.issue_descriptor(descriptor)

    def reserve(self, grant: object) -> bool:
        callback = getattr(self._adapter, "reserve_command_feedback", None)
        if not callable(callback) and self._legacy_exact_craft(grant):
            callback = getattr(self._adapter, "reserve_crafting_feedback", None)
        return callable(callback) and callback(grant) is True

    def abandon(self, grant: object) -> bool:
        callback = getattr(self._adapter, "abandon_command_feedback", None)
        if not callable(callback) and self._legacy_exact_craft(grant):
            callback = getattr(self._adapter, "abandon_crafting_feedback", None)
        return callable(callback) and callback(grant) is True

    def claim_start(self, grant: object, result: object):
        callback = getattr(self._adapter, "claim_command_feedback_start", None)
        if not callable(callback) and self._legacy_exact_craft(grant):
            callback = getattr(self._adapter, "claim_crafting_feedback_start", None)
        return callback(grant, result) if callable(callback) else False

    def inspect_status(self, query: object):
        callback = getattr(self._adapter, "inspect_command_feedback_status", None)
        if callable(callback):
            return callback(query)
        if (
            getattr(query, "requested_family", "") == "item_get"
            and getattr(query, "target_text", None) == ""
        ):
            legacy = getattr(self._adapter, "inspect_crafting_feedback_status", None)
            if callable(legacy):
                return legacy(None)
        return None

    def set_terminal_response_callback(self, callback) -> None:
        setter = getattr(
            self._adapter,
            "set_command_lifecycle_terminal_response_callback",
            None,
        )
        if not callable(setter):
            setter = getattr(
                self._adapter,
                "set_command_terminal_response_callback",
                None,
            )
        if not callable(setter):
            setter = getattr(
                self._adapter,
                "set_crafting_terminal_response_callback",
                None,
            )
        if callable(setter):
            setter(callback)

    @staticmethod
    def _legacy_exact_craft(grant: object) -> bool:
        descriptor = getattr(grant, "descriptor", None)
        return bool(
            getattr(descriptor, "command", None) == "get diamond_pickaxe 1"
            and getattr(descriptor, "acquisition_verb_class", None) == "craft"
        )


__all__ = ("MinecraftFabricChatClefCommandFeedbackFacade",)
