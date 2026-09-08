#20260907_kpopmodder: Build one immutable descriptor from validated Korean translation slots.
from __future__ import annotations

import re
from typing import Mapping

from ..command_feedback_descriptor import CommandFeedbackDescriptor
from ..command_feedback_family_mapping import COMMAND_FEEDBACK_FAMILIES
from ..command_feedback_quantity_semantics import (
    command_feedback_quantity_semantics,
)


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
        spoken_label = self._spoken_label(target_item, intent.get("item_phrase"))
        coordinates = self._coordinates(intent, command_name)
        if coordinates is False or not self._trusted_slots_match(
            command_name=command_name,
            command=command.strip(),
            intent=intent,
            target_item=target_item,
            requested_count=requested_count,
            coordinates=coordinates,
        ):
            return None
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
            player_name=self._optional_text(intent.get("player_name")) or "",
            coordinates=coordinates,
            form_kind="trusted_translation",
            response_lifecycle_kind=lifecycle.response_lifecycle_kind,
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
        phrase = str(item_phrase or "").strip()
        if target and self._SAFE_KOREAN_LABEL.fullmatch(phrase):
            try:
                resolution = self._item_phrases.resolve(phrase)
            except Exception:
                resolution = {}
            if (
                resolution.get("status") == "validated"
                and resolution.get("target") == target
            ):
                return phrase
        if target and target in self._display_names.display_names:
            return self._display_names.display_names[target]
        return "요청한 아이템" if target else ""

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
        expected_intents = {
            "auto_deposit_trust": "auto_deposit_trust_area",
            "deposit": "deposit_item",
            "equip": "equip_item",
            "food": "food",
            "get": "get_item",
            "give": "give_item",
            "goto": "goto",
            "meat": "meat",
            "store_home": "store_home",
        }
        if intent.get("intent_type") != expected_intents.get(command_name):
            return False
        if command_name in {"get", "deposit"}:
            return bool(
                type(target_item) is str
                and cls._TARGET.fullmatch(target_item)
                and type(requested_count) is int
                and requested_count <= cls._JAVA_INT_MAX
                and command == f"{command_name} {target_item} {requested_count}"
            )
        if command_name == "equip":
            return bool(
                type(target_item) is str
                and cls._TARGET.fullmatch(target_item)
                and command == f"equip {target_item}"
            )
        if command_name == "give":
            player = cls._optional_text(intent.get("player_name"))
            return bool(
                type(player) is str
                and cls._PLAYER.fullmatch(player)
                and type(target_item) is str
                and cls._TARGET.fullmatch(target_item)
                and type(requested_count) is int
                and requested_count <= cls._JAVA_INT_MAX
                and command == f"give {player} {target_item} {requested_count}"
            )
        if command_name in {"food", "meat"}:
            return bool(
                type(requested_count) is int
                and requested_count <= cls._JAVA_INT_MAX
                and command == f"{command_name} {requested_count}"
            )
        if command_name == "goto":
            return bool(
                type(coordinates) is tuple
                and all(cls._JAVA_INT_MIN <= value <= cls._JAVA_INT_MAX for value in coordinates)
                and command
                == f"goto {coordinates[0]} {coordinates[1]} {coordinates[2]}"
            )
        if command_name == "auto_deposit_trust":
            return command == "auto_deposit_trust area 16x16"
        if command_name == "store_home":
            return command == "store_home"
        return False

    @staticmethod
    def _optional_text(value: object) -> str | None:
        if type(value) is not str:
            return None
        text = value.strip()
        return text or None


__all__ = ("CommandFeedbackTrustedTranslationDescriptorBuilder",)
