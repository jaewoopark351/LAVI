#20260914_kpopmodder: Build immutable FIND feedback meaning without an acquisition dependency.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.result.find import FindCommandBinding
from plugins.Minecraft.fabric.chatclef.transport.find_catalog.find_catalog_snapshot import safe_text

from ..command_feedback_descriptor import CommandFeedbackDescriptor


class FindFeedbackDescriptorBuilder:
    def build(self, *, event, translation, spec, evidence, lifecycle):
        binding = FindCommandBinding.from_translation(translation)
        if binding is None:
            return None
        label = translation.get("data", {}).get("find_display_label")
        if not safe_text(label, 256) or len(label) > 64:
            label = "요청한 플레이어" if binding.target_kind == "player" else "요청한 대상"
        try:
            return CommandFeedbackDescriptor(
                command_name="find", command=binding.command, command_source=event.source,
                lifecycle_kind=spec.lifecycle_kind, phrase_profile_id="find_phrase_v1",
                evidence_profile_id=evidence.profile_id, rollout_state=evidence.rollout_state,
                event_id=event.event_id, input_source=event.source, provider_id=event.provider_id,
                event_kind=event.event_kind, requested_family="find", intent_kind="find",
                spoken_target_label=label, form_kind="trusted_translation",
                response_lifecycle_kind=lifecycle.response_lifecycle_kind, find_binding=binding,
            )
        except (TypeError, ValueError):
            return None

    def build_raw(self, *, command, source, event_id, provider_id, event_kind, spec, evidence, lifecycle):
        binding = FindCommandBinding.from_command(command)
        if binding is None:
            return None
        try:
            return CommandFeedbackDescriptor(
                command_name="find", command=command, command_source=source, lifecycle_kind=spec.lifecycle_kind,
                phrase_profile_id="find_phrase_v1", evidence_profile_id=evidence.profile_id,
                rollout_state=evidence.rollout_state, event_id=event_id, input_source=source,
                provider_id=provider_id, event_kind=event_kind, requested_family="find", intent_kind="find",
                spoken_target_label="요청한 플레이어" if binding.target_kind == "player" else "요청한 대상",
                detail_level="raw_typed", form_kind="find_report" if binding.mode == "report" else "find_approach",
                response_lifecycle_kind=lifecycle.response_lifecycle_kind, find_binding=binding,
            )
        except (TypeError, ValueError):
            return None
