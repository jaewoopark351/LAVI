#20260908_kpopmodder: Preserve the existing closed GET acquisition evidence validation behind one focused evaluator.
from __future__ import annotations

import re
from typing import Mapping

from ..command_terminal_evidence_evaluation import (
    CommandTerminalEvidenceEvaluation,
)


class GetAcquisitionTerminalEvidenceEvaluator:
    _PROFILE_MARKERS = frozenset(
        {"effect_profile_id", "effect_profile_version", "effect_payload"}
    )
    _GET_PROFILE_ID = "fabric_chatclef_get_acquire_delta"
    _GET_PROFILE_VERSION = 1
    _GET_EFFECT_KIND = "get_acquisition_delta"
    _GET_PAYLOAD_KEYS = frozenset(
        {
            "target_item",
            "target_match_ids",
            "quantity_semantics",
            "requested_delta",
            "before_target_count",
            "after_target_count",
            "target_count_delta",
            "effect_observation_status",
            "effect_observation_reason",
        }
    )
    _LEGACY_KEYS = frozenset(
        {
            "target_item",
            "requested_count",
            "before_target_count",
            "after_target_count",
            "target_count_delta",
            "effect_observation_status",
            "effect_observation_reason",
        }
    )
    _MINECRAFT_ID = re.compile(r"minecraft:[a-z0-9_./-]+\Z", re.ASCII)
    _MAX_MATCH_IDS = 2048
    _MAX_EFFECT_REASON_LENGTH = 256

    def evaluate(
        self,
        result: object,
        *,
        data: object,
        context: object,
        profile: object,
    ) -> CommandTerminalEvidenceEvaluation:
        del result, profile
        if not isinstance(data, Mapping):
            return CommandTerminalEvidenceEvaluation(False)
        if (
            data.get("request_kind") == "stop_control_v1"
            or data.get("operation") in {"stop_ai", "store_home"}
            or "store_home_result" in data
        ):
            return CommandTerminalEvidenceEvaluation(False)
        if any(marker in data for marker in self._PROFILE_MARKERS):
            verified = self._verified_profile_get(data, context=context)
        else:
            verified = self._verified_legacy_get(data, context=context)
        return CommandTerminalEvidenceEvaluation(verified)

    def _verified_profile_get(self, data: Mapping, *, context: object) -> bool:
        if not self._PROFILE_MARKERS.issubset(data) or "effect_kind" not in data:
            return False
        if (
            data.get("effect_profile_id") != self._GET_PROFILE_ID
            or type(data.get("effect_profile_version")) is not int
            or data.get("effect_profile_version") != self._GET_PROFILE_VERSION
            or data.get("effect_kind") != self._GET_EFFECT_KIND
        ):
            return False
        payload = data.get("effect_payload")
        if not isinstance(payload, Mapping) or frozenset(payload) != self._GET_PAYLOAD_KEYS:
            return False
        target_item, requested_count, semantics = self._request_values(context)
        match_ids = payload.get("target_match_ids")
        if (
            type(target_item) is not str
            or not target_item
            or payload.get("target_item") != target_item
            or type(requested_count) is not int
            or requested_count < 1
            or semantics != "acquire_delta"
            or payload.get("quantity_semantics") != "ACQUIRE_DELTA"
            or type(payload.get("requested_delta")) is not int
            or payload.get("requested_delta") != requested_count
            or type(match_ids) is not list
            or not match_ids
            or len(match_ids) > self._MAX_MATCH_IDS
            or any(
                type(value) is not str
                or self._MINECRAFT_ID.fullmatch(value) is None
                for value in match_ids
            )
            or match_ids != sorted(set(match_ids))
        ):
            return False
        if not self._observed_counts(payload, requested_count, context=context):
            return False
        flat_present = any(key in data for key in self._LEGACY_KEYS)
        if flat_present:
            if not self._legacy_wire_compatible_context(context):
                return False
            if not self._LEGACY_KEYS.issubset(data):
                return False
            comparisons = {
                "target_item": payload.get("target_item"),
                "requested_count": payload.get("requested_delta"),
                "before_target_count": payload.get("before_target_count"),
                "after_target_count": payload.get("after_target_count"),
                "target_count_delta": payload.get("target_count_delta"),
                "effect_observation_status": payload.get(
                    "effect_observation_status"
                ),
                "effect_observation_reason": payload.get(
                    "effect_observation_reason"
                ),
            }
            if any(data.get(key) != value for key, value in comparisons.items()):
                return False
        return self._terminal_identity_valid(data)

    def _verified_legacy_get(self, data: Mapping, *, context: object) -> bool:
        if not self._legacy_exact_context(context):
            return False
        target_item, requested_count, _semantics = self._request_values(context)
        before = data.get("before_target_count")
        after = data.get("after_target_count")
        delta = data.get("target_count_delta")
        context_before = getattr(context, "before_target_count", None)
        return bool(
            self._terminal_identity_valid(data)
            and data.get("effect_kind") == self._GET_EFFECT_KIND
            and data.get("target_item") == target_item
            and type(data.get("requested_count")) is int
            and data.get("requested_count") == requested_count
            and data.get("effect_observation_status") == "authoritative"
            and type(before) is int
            and before >= 0
            and (context_before is None or before == context_before)
            and type(after) is int
            and after >= 0
            and type(delta) is int
            and delta >= requested_count
            and after - before == delta
        )

    @staticmethod
    def _terminal_identity_valid(data: Mapping) -> bool:
        return bool(
            data.get("result_reason") == "matching_task_finished"
            and data.get("result_fidelity")
            == "callback_plus_matching_user_task_event"
        )

    @staticmethod
    def _observed_counts(
        payload: Mapping,
        requested_count: int,
        *,
        context: object,
    ) -> bool:
        before = payload.get("before_target_count")
        after = payload.get("after_target_count")
        delta = payload.get("target_count_delta")
        context_before = getattr(context, "before_target_count", None)
        reason = payload.get("effect_observation_reason")
        return bool(
            payload.get("effect_observation_status") == "authoritative"
            and type(reason) is str
            and bool(reason)
            and len(reason)
            <= GetAcquisitionTerminalEvidenceEvaluator._MAX_EFFECT_REASON_LENGTH
            and type(before) is int
            and before >= 0
            and (context_before is None or context_before == before)
            and type(after) is int
            and after >= 0
            and type(delta) is int
            and delta >= requested_count
            and after - before == delta
        )

    @staticmethod
    def _request_values(context: object):
        descriptor = getattr(context, "descriptor", None)
        return (
            getattr(descriptor, "target_item", None)
            or getattr(context, "target_item", "diamond_pickaxe"),
            getattr(descriptor, "requested_count", None)
            or getattr(context, "requested_count", 1),
            getattr(descriptor, "quantity_semantics", None) or "acquire_delta",
        )

    @classmethod
    def _legacy_exact_context(cls, context: object) -> bool:
        descriptor = getattr(context, "descriptor", None)
        target, requested, semantics = cls._request_values(context)
        if descriptor is None and context is None:
            return True
        return bool(
            getattr(descriptor, "command_name", "get") == "get"
            and getattr(descriptor, "command", "get diamond_pickaxe 1")
            == "get diamond_pickaxe 1"
            and target == "diamond_pickaxe"
            and requested == 1
            and semantics == "acquire_delta"
            and getattr(descriptor, "acquisition_verb_class", "craft") == "craft"
        )

    @classmethod
    def _legacy_wire_compatible_context(cls, context: object) -> bool:
        descriptor = getattr(context, "descriptor", None)
        if descriptor is None:
            return False
        target, requested, semantics = cls._request_values(context)
        command = str(getattr(descriptor, "command", "") or "").strip()
        if command.startswith("@"):
            command = command[1:].lstrip()
        command = " ".join(command.split())
        return bool(
            getattr(descriptor, "command_name", "") == "get"
            and command == "get diamond_pickaxe 1"
            and target == "diamond_pickaxe"
            and requested == 1
            and semantics == "acquire_delta"
        )


__all__ = ("GetAcquisitionTerminalEvidenceEvaluator",)
