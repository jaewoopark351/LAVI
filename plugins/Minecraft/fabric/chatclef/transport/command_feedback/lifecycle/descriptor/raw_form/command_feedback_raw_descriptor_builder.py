#20260907_kpopmodder: Build one immutable descriptor from an already closed raw form.
from __future__ import annotations

from ...evidence.command_terminal_evidence_profile import (
    CommandTerminalEvidenceProfile,
)
from ..command_feedback_descriptor import CommandFeedbackDescriptor
from ..command_feedback_family_mapping import COMMAND_FEEDBACK_FAMILIES
from ..command_feedback_quantity_semantics import (
    command_feedback_quantity_semantics,
)
from ..command_feedback_target import CommandFeedbackTarget


class CommandFeedbackRawDescriptorBuilder:
    def __init__(
        self,
        *,
        command_registry,
        evidence_profiles,
        display_names,
        raw_form_decoder,
        raw_slot_projector,
        lifecycle_kind_profiles,
    ) -> None:
        self._commands = command_registry
        self._evidence = evidence_profiles
        self._display_names = display_names
        self._raw_forms = raw_form_decoder
        self._raw_slots = raw_slot_projector
        self._lifecycle_kinds = lifecycle_kind_profiles

    def build(
        self,
        command: object,
        *,
        command_source: object,
        event_id: object,
        provider_id: object,
        event_kind: object,
    ) -> CommandFeedbackDescriptor | None:
        command_text = command if type(command) is str else None
        source = self._optional_text(command_source)
        if command_text is None or source != "lavi_gui":
            return None
        raw_form = self._raw_forms.decode(command_text)
        if raw_form is None:
            return None
        raw_slots = self._raw_slots.project(raw_form)
        command_name = raw_form.command_name
        try:
            spec = self._commands.spec(command_name)
            evidence = self._evidence.profile(command_name)
            lifecycle = self._lifecycle_kinds.profile(command_name)
        except KeyError:
            return None
        if evidence.rollout_state == CommandTerminalEvidenceProfile.DISABLED:
            return None
        targets = self._targets(command_name, raw_slots.target_entries)
        single_target = targets[0] if len(targets) == 1 else None
        requested_count = raw_slots.requested_count
        if single_target is not None:
            requested_count = single_target.requested_count
        detail_level = (
            CommandFeedbackDescriptor.COMMAND_NAME_ONLY
            if (
                not raw_slots.slots_recoverable
                or raw_form.form_kind in {"butler_player", "crosshair_target"}
            )
            else CommandFeedbackDescriptor.RAW_TYPED
        )
        try:
            return CommandFeedbackDescriptor(
                command_name=command_name,
                command=command_text,
                command_source=source,
                lifecycle_kind=spec.lifecycle_kind,
                phrase_profile_id=f"{command_name}_phrase_v1",
                evidence_profile_id=evidence.profile_id,
                rollout_state=(
                    evidence.rollout_state
                    if command_name == "get" and single_target is not None
                    else CommandTerminalEvidenceProfile.CAUTIOUS
                ),
                event_id=str(event_id or ""),
                input_source=source,
                provider_id=str(provider_id or ""),
                event_kind=str(event_kind or ""),
                requested_family=COMMAND_FEEDBACK_FAMILIES[command_name],
                target_item=(
                    single_target.canonical_target
                    if single_target is not None
                    and command_name
                    in {"get", "deposit", "deposit_all", "equip", "give"}
                    else None
                ),
                requested_count=requested_count,
                quantity_semantics=command_feedback_quantity_semantics(command_name),
                spoken_target_label=(
                    single_target.spoken_label if single_target is not None else ""
                ),
                player_name=raw_slots.player_name,
                coordinates=(
                    raw_slots.coordinate_values
                    if len(raw_slots.coordinate_values) == 3
                    else None
                ),
                detail_level=detail_level,
                form_kind=raw_form.form_kind,
                response_lifecycle_kind=lifecycle.response_lifecycle_kind,
                targets=targets,
                coordinate_values=raw_slots.coordinate_values,
                dimension=raw_slots.dimension,
                setting_value=raw_slots.setting_value,
                structure_name=raw_slots.structure_name,
                destination_id=raw_slots.destination_id,
                operation_target=raw_slots.operation_target,
            )
        except (KeyError, TypeError, ValueError):
            return None

    def _targets(
        self,
        command_name: str,
        entries: tuple[tuple[str, int], ...],
    ) -> tuple[CommandFeedbackTarget, ...]:
        semantics = command_feedback_quantity_semantics(command_name)
        return tuple(
            CommandFeedbackTarget(
                canonical_target=target,
                requested_count=count,
                quantity_semantics=semantics,
                spoken_label=self._display_names.display_names.get(
                    target,
                    "요청한 아이템",
                ),
            )
            for target, count in entries
        )

    @staticmethod
    def _optional_text(value: object) -> str | None:
        if type(value) is not str:
            return None
        text = value.strip()
        return text or None


__all__ = ("CommandFeedbackRawDescriptorBuilder",)
