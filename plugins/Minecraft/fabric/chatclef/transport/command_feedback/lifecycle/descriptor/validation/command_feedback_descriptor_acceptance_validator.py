#20260907_kpopmodder: Validate an immutable descriptor without rebuilding presentation text.
from __future__ import annotations

from ..command_feedback_descriptor import CommandFeedbackDescriptor
from ..command_feedback_family_mapping import COMMAND_FEEDBACK_FAMILIES


class CommandFeedbackDescriptorAcceptanceValidator:
    def __init__(
        self,
        *,
        command_registry,
        evidence_profiles,
        lifecycle_kind_profiles,
        raw_descriptor_builder,
    ) -> None:
        self._commands = command_registry
        self._evidence = evidence_profiles
        self._lifecycle_kinds = lifecycle_kind_profiles
        self._raw_descriptors = raw_descriptor_builder

    def accepts(self, descriptor: object) -> bool:
        if type(descriptor) is not CommandFeedbackDescriptor:
            return False
        try:
            spec = self._commands.spec(descriptor.command_name)
            evidence = self._evidence.profile(descriptor.command_name)
            lifecycle = self._lifecycle_kinds.profile(descriptor.command_name)
            family = COMMAND_FEEDBACK_FAMILIES[descriptor.command_name]
        except KeyError:
            return False
        identity_matches = bool(
            descriptor.command_source == descriptor.input_source
            and descriptor.lifecycle_kind == spec.lifecycle_kind
            and descriptor.phrase_profile_id
            == f"{descriptor.command_name}_phrase_v1"
            and descriptor.evidence_profile_id == evidence.profile_id
            and descriptor.requested_family == family
            and descriptor.response_lifecycle_kind
            == lifecycle.response_lifecycle_kind
        )
        if not identity_matches:
            return False
        if descriptor.command_source == "lavi_gui":
            expected = self._raw_descriptors.build(
                descriptor.command,
                command_source=descriptor.command_source,
                event_id=descriptor.event_id,
                provider_id=descriptor.provider_id,
                event_kind=descriptor.event_kind,
            )
            return descriptor == expected
        return bool(
            self._commands.is_public_korean_enabled(descriptor.command_name)
            and descriptor.command_source in spec.allowed_input_sources
            and descriptor.rollout_state == evidence.rollout_state
        )


__all__ = ("CommandFeedbackDescriptorAcceptanceValidator",)
