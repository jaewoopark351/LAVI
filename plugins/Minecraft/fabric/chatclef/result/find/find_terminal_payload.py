#20260914_kpopmodder: Validate closed FIND effects and forbid success evidence on failures.
from __future__ import annotations

from dataclasses import dataclass
from types import MappingProxyType
from typing import Mapping

from plugins.Minecraft.fabric.chatclef.transport.find_catalog.find_catalog_snapshot import DIGEST, IDENTIFIER, safe_text

FOUND = frozenset({"FOUND_AND_REPORTED", "FOUND_AND_IN_SAFE_RANGE", "ALREADY_IN_SAFE_RANGE"})
FAILURES = frozenset({"INVALID_TARGET", "OBSERVATION_BOUNDS_EXHAUSTED", "TARGET_LOST",
                     "CANDIDATE_NOT_REVALIDATABLE", "INTERNAL_ERROR", "UNREACHABLE", "TIMEOUT", "INTERRUPTED"})
SCOPES = {"entity": "loaded_entities", "block": "loaded_blocks", "item": "loaded_dropped_items", "player": "loaded_players"}


@dataclass(frozen=True, slots=True)
class FindTerminalPayload:
    target_kind: str
    mode: str
    find_result: str
    find_satisfied: bool
    reason: str
    fields: Mapping

    @classmethod
    def from_data(cls, data: object):
        if not isinstance(data, Mapping) or type(data.get("effect_profile_version")) is not int or data["effect_profile_version"] != 1:
            return None
        payload = data.get("effect_payload")
        if not isinstance(payload, Mapping):
            return None
        profile_id = data.get("effect_profile_id")
        if type(profile_id) is not str:
            return None
        mode = {"fabric_chatclef_find_observation": "report", "fabric_chatclef_find_approach": "approach"}.get(profile_id)
        if mode is None or data.get("effect_kind") != ("find_observation" if mode == "report" else "find_approach"):
            return None
        kind, result = payload.get("target_kind"), payload.get("find_result")
        if type(kind) is not str or type(result) is not str or kind not in SCOPES or (kind == "item" and mode != "report"):
            return None
        if payload.get("completion_mode") != ("LOCATE_AND_REPORT" if mode == "report" else "LOCATE_AND_APPROACH") or payload.get("observation_scope") != SCOPES[kind]:
            return None
        allowed_results = {"FOUND_AND_REPORTED", "NOT_OBSERVED_IN_LOADED_SCOPE", *FAILURES} if mode == "report" else {"FOUND_AND_IN_SAFE_RANGE", "ALREADY_IN_SAFE_RANGE", "NOT_OBSERVED_IN_LOADED_SCOPE", *FAILURES}
        if result not in allowed_results or type(payload.get("find_satisfied")) is not bool or payload["find_satisfied"] != (result in FOUND):
            return None
        reason = payload.get("reason")
        if not safe_text(reason, 256) or reason != reason.strip():
            return None
        expected = {"target_kind", "completion_mode", "observation_scope", "find_result", "find_satisfied", "reason"}
        if kind == "player":
            expected.add("player_identity_digest")
            digest = payload.get("player_identity_digest")
            if type(digest) is not str or DIGEST.fullmatch(digest) is None:
                return None
        else:
            expected.update(("canonical_target_id", "catalog_digest", "resource_generation"))
            identifier, digest, generation = payload.get("canonical_target_id"), payload.get("catalog_digest"), payload.get("resource_generation")
            if (type(identifier) is not str or len(identifier) > 128 or IDENTIFIER.fullmatch(identifier) is None
                    or type(digest) is not str or DIGEST.fullmatch(digest) is None
                    or type(generation) is not int or not 0 <= generation <= 2**63 - 1):
                return None
        if result in FOUND:
            expected.update(("candidate_identity_digest", "dimension", "x", "y", "z"))
            candidate, dimension = payload.get("candidate_identity_digest"), payload.get("dimension")
            if (type(candidate) is not str or DIGEST.fullmatch(candidate) is None
                    or type(dimension) is not str or len(dimension) > 128 or IDENTIFIER.fullmatch(dimension) is None
                    or any(type(payload.get(axis)) is not int or not -(2**31) <= payload[axis] <= 2**31 - 1 for axis in ("x", "y", "z"))):
                return None
            if mode == "approach":
                expected.add("safe_distance_satisfied")
                if payload.get("safe_distance_satisfied") is not True:
                    return None
        if set(payload) != expected:
            return None
        return cls(kind, mode, result, payload["find_satisfied"], reason, MappingProxyType(dict(payload)))
