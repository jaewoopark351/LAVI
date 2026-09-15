#20260907_kpopmodder: Build one immutable descriptor from validated Korean translation slots.
from __future__ import annotations

import re
from typing import Mapping

from ..command_feedback_descriptor import CommandFeedbackDescriptor
from ..command_feedback_family_mapping import COMMAND_FEEDBACK_FAMILIES
from ..command_feedback_quantity_semantics import (
    command_feedback_quantity_semantics,
)
from .command_feedback_trusted_slot_validator import CommandFeedbackTrustedSlotValidator
from .command_feedback_trusted_slot_projector import CommandFeedbackTrustedSlotProjector
from .command_feedback_spoken_label_resolver import CommandFeedbackSpokenLabelResolver


class CommandFeedbackTrustedTranslationDescriptorBuilder:
    _SAFE_KOREAN_LABEL = re.compile(r"[가-힣0-9 ]{1,64}\Z")
    _TARGET = re.compile(r"[a-z0-9_]+\Z", re.ASCII)
    _PLAYER = re.compile(r"[A-Za-z0-9_]{3,16}\Z", re.ASCII)
    _JAVA_INT_MIN = -(2**31)
    _JAVA_INT_MAX = 2**31 - 1

    def __init__(
        self,
        *,
        command_registry,
        evidence_profiles,
        display_names,
        verb_matcher,
        item_phrase_resolver,
        lifecycle_kind_profiles,
    ) -> None:
        self._commands = command_registry
        self._evidence = evidence_profiles
        self._display_names = display_names
        self._verb_matcher = verb_matcher
        self._item_phrases = item_phrase_resolver
        self._lifecycle_kinds = lifecycle_kind_profiles
        self._slot_projector = CommandFeedbackTrustedSlotProjector(display_names)
        self._spoken_labels = CommandFeedbackSpokenLabelResolver(display_names, item_phrase_resolver)

    def build(
        self,
        *,
        event: object,
        translation: object,
    ) -> CommandFeedbackDescriptor | None:
        if not isinstance(translation, Mapping):
            return None
        intent = translation.get("intent")
        command = translation.get("command")
        if (
            not isinstance(intent, Mapping)
            or translation.get("status") != "validated"
            or translation.get("executable") is not True
            or type(command) is not str
            or not command.strip()
            or intent.get("language") != "ko"
        ):
            return None
        command_name = command.strip().split(maxsplit=1)[0].lower()
        try:
            spec = self._commands.spec(command_name)
            evidence = self._evidence.profile(command_name)
            lifecycle = self._lifecycle_kinds.profile(command_name)
        except KeyError:
            return None
        source = getattr(event, "source", None)
        if (
            not self._commands.is_public_korean_enabled(command_name)
            or source not in spec.allowed_input_sources
            or getattr(event, "final", None) is not True
        ):
            return None
        identity = self._event_identity(event)
        if identity is None:
            return None
        intent_kind = str(intent.get("intent_type") or "unknown")
        target_item = self._optional_text(translation.get("resolved_target"))
        requested_count = self._quantity(intent, command_name)
        data = translation.get("data")
        labels = data.get("target_labels") if isinstance(data, Mapping) else None
        def spoken(target, phrase):
            return self._spoken_labels.resolve(target, phrase, labels)
        spoken_label = spoken(target_item, intent.get("item_phrase"))
        projection = self._slot_projector.project(command.strip(), intent, spoken)
        if projection is None:
            return None
        form, slots, targets = projection
        coordinates = slots.coordinate_values if len(slots.coordinate_values) == 3 else None
        if not self._trusted_slots_match(
            command_name=command_name,
            command=command.strip(),
            intent=intent,
            target_item=target_item,
            requested_count=requested_count,
            coordinates=coordinates,
        ):
            return None
        if targets:
            # The compiler may aggregate repeated list entries; freeze its actual resulting quantity.
            target_item = targets[0].canonical_target if len(targets) == 1 else None
            requested_count = targets[0].requested_count if len(targets) == 1 else None
            spoken_label = targets[0].spoken_label if len(targets) == 1 else ""
        verb_class = ""
        if command_name == "get":
            verb_class = self._verb_matcher.classify(
                intent.get("original_text") or getattr(event, "text", "")
            )
            if verb_class not in {"craft", "acquire", "mining"}:
                verb_class = "acquire"
        return CommandFeedbackDescriptor(
            command_name=command_name,
            command=command.strip(),
            command_source=source,
            lifecycle_kind=spec.lifecycle_kind,
            phrase_profile_id=f"{command_name}_phrase_v1",
            evidence_profile_id=evidence.profile_id,
            rollout_state=evidence.rollout_state,
            event_id=identity[0],
            input_source=source,
            provider_id=identity[1],
            event_kind=identity[2],
            requested_family=COMMAND_FEEDBACK_FAMILIES[command_name],
            intent_kind=intent_kind,
            target_item=target_item,
            requested_count=requested_count,
            quantity_semantics=command_feedback_quantity_semantics(command_name),
            acquisition_verb_class=verb_class,
            spoken_target_label=spoken_label,
            player_name=self._optional_text(intent.get("player_name")) or slots.player_name,
            coordinates=coordinates,
            form_kind=("equipment_material_set" if form.form_kind == "equipment_material_set" else "trusted_translation"),
            response_lifecycle_kind=lifecycle.response_lifecycle_kind,
            targets=targets,
            coordinate_values=slots.coordinate_values,
            dimension=slots.dimension,
            setting_value=slots.setting_value,
            structure_name=slots.structure_name,
            destination_id=slots.destination_id,
            operation_target=slots.operation_target,
        )

    @staticmethod
    def _event_identity(event: object):
        values = (
            getattr(event, "event_id", None),
            getattr(event, "provider_id", None),
            getattr(event, "event_kind", None),
        )
        if not all(type(value) is str and bool(value) for value in values):
            return None
        return values

    def _spoken_label(self, target: str | None, item_phrase: object) -> str:
        return self._spoken_labels.resolve(target, item_phrase)

    @staticmethod
    def _quantity(intent: Mapping, command_name: str):
        value = (
            intent.get("food_units")
            if command_name in {"food", "meat"}
            else intent.get("quantity")
        )
        return value if type(value) is int and value >= 1 else None

    @staticmethod
    def _coordinates(intent: Mapping, command_name: str):
        if command_name != "goto":
            return None
        values = (intent.get("x"), intent.get("y"), intent.get("z"))
        if any(type(value) is not int for value in values):
            return False
        return values

    @classmethod
    def _trusted_slots_match(
        cls,
        *,
        command_name: str,
        command: str,
        intent: Mapping,
        target_item: str | None,
        requested_count: int | None,
        coordinates: tuple[int, int, int] | None,
    ) -> bool:
        #20260914_kpopmodder: Bind feedback to the exact admitted FIND slots.
        #20260915_kpopmodder: Recompile original Korean slots in the extracted validator.
        #20260915_kpopmodder: Preserve the compatibility seam while delegating typed validation.
        return CommandFeedbackTrustedSlotValidator().matches(
            command_name=command_name, command=command, intent=intent,
            target_item=target_item, requested_count=requested_count, coordinates=coordinates,
        )

    @staticmethod
    def _optional_text(value: object) -> str | None:
        if type(value) is not str:
            return None
        text = value.strip()
        return text or None


__all__ = ("CommandFeedbackTrustedTranslationDescriptorBuilder",)
