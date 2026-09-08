#20260907_kpopmodder: Preserve descriptor factory APIs through focused builders and validation.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.command_registry import (
    KoreanChatClefCommandRegistry,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_korean_display_name_repository import (
    ChatClefKoreanDisplayNameRepository,
)
from plugins.Minecraft.fabric.chatclef.intent.korean_acquisition_verb_matcher import (
    KoreanAcquisitionVerbMatcher,
)
from plugins.Minecraft.fabric.chatclef.intent.korean_item_phrase_resolver import (
    KoreanItemPhraseResolver,
)

from ..evidence.command_terminal_evidence_profile_registry import (
    CommandTerminalEvidenceProfileRegistry,
)
from ..kind import CommandFeedbackLifecycleKindProfileRegistry
from .command_feedback_family_mapping import COMMAND_FEEDBACK_FAMILIES
from .raw_form import CommandFeedbackRawFormDecoder, CommandFeedbackRawSlotProjector
from .raw_form.command_feedback_raw_descriptor_builder import (
    CommandFeedbackRawDescriptorBuilder,
)
from .trusted_translation import CommandFeedbackTrustedTranslationDescriptorBuilder
from .validation import CommandFeedbackDescriptorAcceptanceValidator


class CommandFeedbackDescriptorFactory:
    def __init__(
        self,
        *,
        command_registry=None,
        evidence_profiles=None,
        display_names=None,
        verb_matcher=None,
        item_phrase_resolver=None,
        raw_form_decoder=None,
        raw_slot_projector=None,
        lifecycle_kind_profiles=None,
    ) -> None:
        commands = command_registry or KoreanChatClefCommandRegistry()
        evidence = evidence_profiles or CommandTerminalEvidenceProfileRegistry(
            commands
        )
        lifecycle_kinds = (
            lifecycle_kind_profiles
            or CommandFeedbackLifecycleKindProfileRegistry(commands)
        )
        display = display_names or ChatClefKoreanDisplayNameRepository()
        if set(COMMAND_FEEDBACK_FAMILIES) != set(commands.command_names()):
            raise RuntimeError(
                "command descriptor families must cover the registry exactly"
            )
        self._trusted = CommandFeedbackTrustedTranslationDescriptorBuilder(
            command_registry=commands,
            evidence_profiles=evidence,
            display_names=display,
            verb_matcher=verb_matcher or KoreanAcquisitionVerbMatcher(),
            item_phrase_resolver=item_phrase_resolver or KoreanItemPhraseResolver(),
            lifecycle_kind_profiles=lifecycle_kinds,
        )
        self._raw = CommandFeedbackRawDescriptorBuilder(
            command_registry=commands,
            evidence_profiles=evidence,
            display_names=display,
            raw_form_decoder=raw_form_decoder or CommandFeedbackRawFormDecoder(),
            raw_slot_projector=raw_slot_projector or CommandFeedbackRawSlotProjector(),
            lifecycle_kind_profiles=lifecycle_kinds,
        )
        self._acceptance = CommandFeedbackDescriptorAcceptanceValidator(
            command_registry=commands,
            evidence_profiles=evidence,
            lifecycle_kind_profiles=lifecycle_kinds,
            raw_descriptor_builder=self._raw,
        )

    def from_trusted_translation(self, *, event: object, translation: object):
        return self._trusted.build(event=event, translation=translation)

    def decode_registered_command_name_only(
        self,
        command: object,
        *,
        command_source: object,
        event_id: object,
        provider_id: object,
        event_kind: object,
    ):
        return self._raw.build(
            command,
            command_source=command_source,
            event_id=event_id,
            provider_id=provider_id,
            event_kind=event_kind,
        )

    def accepts_descriptor(self, descriptor: object) -> bool:
        return self._acceptance.accepts(descriptor)


__all__ = ("CommandFeedbackDescriptorFactory",)
