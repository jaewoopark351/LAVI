#20260907_kpopmodder: Project only admission-validated command slots into UI detail.
from __future__ import annotations

import json
import re

from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle.descriptor.command_feedback_descriptor import (
    CommandFeedbackDescriptor,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle.descriptor.command_feedback_target import (
    CommandFeedbackTarget,
)

from .command_lifecycle_presentation_detail_log_policy import (
    CommandLifecyclePresentationDetailLogPolicy,
)


class CommandLifecyclePresentationDetailProjector:
    _COMMAND_NAME = re.compile(
        r"(?:[a-z][a-z0-9_]*|자동보관등록)\Z",
        re.ASCII,
    )
    _FORM_KIND = re.compile(r"[a-z][a-z0-9_]{0,63}\Z", re.ASCII)
    _CANONICAL_SLOT = re.compile(r"[a-z0-9_.:/+-]{1,128}\Z", re.ASCII)
    _PLAYER = re.compile(r"[A-Za-z0-9_]{1,16}\Z", re.ASCII)
    _MAX_INTEGER = 2**31 - 1

    def project(self, descriptor: object) -> str:
        if type(descriptor) is not CommandFeedbackDescriptor:
            return ""
        command_name = self._match(
            descriptor.command_name,
            self._COMMAND_NAME,
        )
        form_kind = self._match(descriptor.form_kind, self._FORM_KIND)
        if command_name is None or form_kind is None:
            return ""
        values = {
            "command_name": command_name,
            "form_kind": form_kind,
        }
        self._add_targets(values, descriptor)
        self._add_player(values, descriptor.player_name)
        self._add_coordinates(values, descriptor)
        for key, value in (
            ("dimension", descriptor.dimension),
            ("setting_value", descriptor.setting_value),
            ("structure", descriptor.structure_name),
            ("destination", descriptor.destination_id),
            ("operation_target", descriptor.operation_target),
        ):
            matched = self._match(value, self._CANONICAL_SLOT)
            if matched is not None:
                self._add_if_bounded(values, key, matched)
        return CommandLifecyclePresentationDetailLogPolicy.validate(
            self._encode(values)
        )

    def _add_targets(self, values: dict, descriptor) -> None:
        targets = descriptor.targets
        if targets:
            projected = []
            for target in targets:
                if type(target) is not CommandFeedbackTarget:
                    continue
                canonical_target = self._match(
                    target.canonical_target,
                    self._CANONICAL_SLOT,
                )
                requested_count = self._positive_integer(
                    target.requested_count
                )
                if canonical_target is None or requested_count is None:
                    continue
                candidate = projected + [
                    {"count": requested_count, "id": canonical_target}
                ]
                candidate_values = dict(values)
                candidate_values["target_entry_count"] = len(targets)
                candidate_values["targets"] = candidate
                if len(candidate) != len(targets):
                    candidate_values["targets_truncated"] = True
                if not self._fits(candidate_values):
                    break
                projected = candidate
            if projected:
                values["target_entry_count"] = len(targets)
                values["targets"] = projected
                if len(projected) != len(targets):
                    values["targets_truncated"] = True
            return
        target_item = self._match(
            descriptor.target_item,
            self._CANONICAL_SLOT,
        )
        if target_item is None:
            return
        self._add_if_bounded(values, "target", target_item)
        requested_count = self._positive_integer(descriptor.requested_count)
        if requested_count is not None:
            self._add_if_bounded(values, "requested_count", requested_count)

    def _add_player(self, values: dict, player_name: object) -> None:
        matched = self._match(player_name, self._PLAYER)
        if matched is not None:
            self._add_if_bounded(values, "player", matched)

    def _add_coordinates(self, values: dict, descriptor) -> None:
        coordinates = (
            descriptor.coordinate_values
            if descriptor.coordinate_values
            else descriptor.coordinates
        )
        if type(coordinates) is not tuple or not coordinates:
            return
        if any(
            type(value) is not int
            or value < -self._MAX_INTEGER - 1
            or value > self._MAX_INTEGER
            for value in coordinates
        ):
            return
        self._add_if_bounded(values, "coordinates", list(coordinates))

    def _add_if_bounded(self, values: dict, key: str, value: object) -> bool:
        candidate = dict(values)
        candidate[key] = value
        if not self._fits(candidate):
            return False
        values[key] = value
        return True

    @classmethod
    def _fits(cls, values: dict) -> bool:
        return len(cls._encode(values)) <= (
            CommandLifecyclePresentationDetailLogPolicy.MAX_LENGTH
        )

    @staticmethod
    def _encode(values: dict) -> str:
        return json.dumps(
            values,
            ensure_ascii=True,
            separators=(",", ":"),
            sort_keys=True,
        )

    @staticmethod
    def _match(value: object, pattern) -> str | None:
        if type(value) is not str or pattern.fullmatch(value) is None:
            return None
        return value

    @classmethod
    def _positive_integer(cls, value: object) -> int | None:
        if type(value) is not int or value < 1 or value > cls._MAX_INTEGER:
            return None
        return value


__all__ = ("CommandLifecyclePresentationDetailProjector",)
