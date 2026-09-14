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


#20260914_kpopmodder: Defined producer phase assertions use unchanged wire keys; legacy names cannot assert exploration facts.
_NEW_RESULTS = {
    "discovery_target_revalidated_and_exploration_quiet": ("report", {"FOUND_AND_REPORTED"}),
    "same_target_safe_range_and_owned_cleanup": ("approach", {"FOUND_AND_IN_SAFE_RANGE", "ALREADY_IN_SAFE_RANGE"}),
    "finite_search_plan_completed_no_match": (None, {"NOT_OBSERVED_IN_LOADED_SCOPE"}),
    **{reason: (None, {"OBSERVATION_BOUNDS_EXHAUSTED"}) for reason in (
        "local_observation_deadline_exhausted", "local_entity_visit_limit_exhausted",
        "cumulative_entity_visit_limit_exhausted", "scan_count_limit_exhausted",
        "exploration_displacement_limit_exhausted", "exploration_sampled_movement_limit_exhausted", "exploration_start_limit_exhausted", "exploration_route_work_limit_exhausted")},
    "discovery_deadline_exhausted": (None, {"TIMEOUT"}),
    "exploration_step_deadline_exhausted": (None, {"TIMEOUT"}),
    "approach_deadline_exhausted": ("approach", {"TIMEOUT"}),
    "native_approach_step_deadline_exhausted": ("approach", {"TIMEOUT"}),
    "parent_deadline_exhausted": (None, {"TIMEOUT"}),
    "exploration_route_unavailable": (None, {"UNREACHABLE"}),
    "exploration_no_progress": (None, {"UNREACHABLE"}),
    "post_discovery_approach_unreachable": ("approach", {"UNREACHABLE"}),
    "selected_target_lost": (None, {"TARGET_LOST"}),
    "selected_identity_unverifiable": (None, {"CANDIDATE_NOT_REVALIDATABLE"}),
    "world_player_or_catalog_binding_changed": (None, {"INTERRUPTED"}),
    "root_ownership_lost": (None, {"INTERRUPTED"}),
    **{reason: (None, {"INTERNAL_ERROR"}) for reason in ("observation_read_failed", "owned_cleanup_failed", "movement_sample_unavailable")},
    "invalid_target": (None, {"INVALID_TARGET"}),
}
_LEGACY_UNREACHABLE = frozenset((
    "unsupported_player_movement_state", "target_inside_minimum_standoff", "preexisting_movement_input_contended",
    "safe_range_not_satisfied_at_native_path_end", "native_step_no_longer_safe_or_bound",
    "native_cached_or_world_footprint_not_live_proven", "native_step_requires_world_mutation",
    "native_step_requested_forbidden_input", "external_movement_input_contended", "invalid_native_rotation",
    "forced_input_lease_rejected_or_superseded", "native_slow_path_setting_unsupported",
    "start_not_in_supported_loaded_flat_scope", "no_supported_safe_standoff_cell", "native_planner_read_limit",
    "native_finite_plan_unreachable", "unsupported_native_movement_kind", "native_path_bounds_exhausted",
    "native_step_unreachable", "native_step_failed", "native_step_canceled",
    "unsupported_target_safety_profile", "forced_input_lease_capability_unavailable",
))
_LEGACY_RESULTS = {
    "complete_loaded_scope_candidate_revalidated": ("report", {"FOUND_AND_REPORTED"}),
    "same_target_safe_range_and_owned_cleanup": ("approach", {"FOUND_AND_IN_SAFE_RANGE", "ALREADY_IN_SAFE_RANGE"}),
    "complete_loaded_scope_no_match": (None, {"NOT_OBSERVED_IN_LOADED_SCOPE"}),
    **{reason: (None, {"OBSERVATION_BOUNDS_EXHAUSTED"}) for reason in ("elapsed_budget_exhausted", "entity_visit_limit_exhausted", "block_visit_limit_exhausted")},
    **{reason: (None, {"TARGET_LOST"}) for reason in ("selected_candidate_not_revalidated", "terminal_candidate_not_revalidated", "selected_target_lost")},
    **{reason: (None, {"INTERRUPTED"}) for reason in ("catalog_resource_binding_changed", "world_or_player_binding_changed", "terminal_world_or_player_binding_changed", "user_root_replaced")},
    **{reason: (None, {"INTERNAL_ERROR"}) for reason in ("observation_initialization_failed", "world_or_player_unavailable",
        "observation_read_or_revalidation_failed", "entity_read_failed", "IllegalStateException", "IllegalArgumentException", "NullPointerException")},
    "item_approach_not_supported": (None, {"INVALID_TARGET"}),
    **{reason: ("approach", {"TIMEOUT"}) for reason in ("original_approach_deadline_exhausted", "native_step_no_progress_timeout")},
    **{reason: ("approach", {"UNREACHABLE"}) for reason in _LEGACY_UNREACHABLE},
}

def _reason_matches(kind, mode, result, reason):
    # Unsatisfied discovery remains a trusted producer assertion, not recipient world observation.
    rule = _NEW_RESULTS.get(reason) if kind == "entity" else None
    if rule is None:
        rule = _LEGACY_RESULTS.get(reason)
    if rule is None:
        return reason not in _NEW_RESULTS and result == "INTERNAL_ERROR" and reason.isidentifier()  # Legacy exception names carry no phase facts.
    return (rule[0] is None or rule[0] == mode) and result in rule[1]

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
        if not _reason_matches(kind, mode, result, reason):
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
