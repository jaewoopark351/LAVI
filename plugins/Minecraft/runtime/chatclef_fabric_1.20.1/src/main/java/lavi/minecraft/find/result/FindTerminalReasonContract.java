//#if MC == 12001
//$$ package lavi.minecraft.find.result;

//$$ import java.util.Set;
//$$ import lavi.minecraft.find.result.FindTerminalPhaseEvidence.Phase;

//$$ //20260914_kpopmodder: Closed owner reasons validate internal phase facts before projecting the unchanged FIND wire.
//$$ public final class FindTerminalReasonContract {
//$$     private FindTerminalReasonContract() { }
//$$     private static final Set<String> BOUNDS = Set.of("local_observation_deadline_exhausted", "local_entity_visit_limit_exhausted",
//$$             "cumulative_entity_visit_limit_exhausted", "scan_count_limit_exhausted", "exploration_displacement_limit_exhausted",
//$$             "exploration_sampled_movement_limit_exhausted", "exploration_start_limit_exhausted", "exploration_route_work_limit_exhausted");
//$$     private static final Set<String> LEGACY_UNREACHABLE = Set.of("unsupported_player_movement_state", "target_inside_minimum_standoff",
//$$             "preexisting_movement_input_contended", "safe_range_not_satisfied_at_native_path_end", "native_step_no_longer_safe_or_bound",
//$$             "native_cached_or_world_footprint_not_live_proven", "native_step_requires_world_mutation", "native_step_requested_forbidden_input",
//$$             "external_movement_input_contended", "invalid_native_rotation", "forced_input_lease_rejected_or_superseded",
//$$             "native_slow_path_setting_unsupported", "start_not_in_supported_loaded_flat_scope", "no_supported_safe_standoff_cell",
//$$             "native_planner_read_limit", "native_finite_plan_unreachable", "unsupported_native_movement_kind", "native_path_bounds_exhausted",
//$$             "native_step_unreachable", "native_step_failed", "native_step_canceled");
//$$     public static boolean validates(FindOutcome outcome, Object root) {
//$$         if (!Set.of("report", "approach").contains(outcome.request().mode())
//$$                 || !Set.of("entity", "block", "player", "item").contains(outcome.request().kind())) return false;
//$$         var evidence = outcome.terminalEvidence();
//$$         if (evidence == null) return legacyValid(outcome);
//$$         if (!outcome.request().kind().equals("entity") || !evidence.boundTo(outcome, root) || evidence.phase() == Phase.TERMINAL) return false;
//$$         boolean approach = outcome.request().mode().equals("approach");
//$$         boolean handoff = evidence.approachHandedOff();
//$$         boolean discoveryPhase = evidence.phase() != Phase.APPROACHING && !handoff;
//$$         boolean cleanup = evidence.explorationQuiet() && evidence.inputQuiet();
//$$         boolean discovered = evidence.discoveryComplete();
//$$         if (handoff && (!approach || !discovered || !evidence.explorationQuiet())) return false;
//$$         if (evidence.phase() == Phase.APPROACHING && !handoff || evidence.finalRevalidated() && !discovered) return false;
//$$         if (evidence.approachArrived() && !handoff) return false;
//$$         String reason = outcome.reason(), result = outcome.findResult();
//$$         if (BOUNDS.contains(reason)) return discoveryPhase && !outcome.satisfied() && result.equals("OBSERVATION_BOUNDS_EXHAUSTED");
//$$         return switch (reason) {
//$$             case "discovery_target_revalidated_and_exploration_quiet" -> !approach && evidence.phase() == Phase.STOPPING_EXPLORATION && discovered && evidence.finalRevalidated()
//$$                     && cleanup && outcome.satisfied() && outcome.scopeComplete() && result.equals("FOUND_AND_REPORTED");
//$$             case "same_target_safe_range_and_owned_cleanup" -> approach && evidence.phase() == Phase.APPROACHING && discovered && handoff && evidence.approachArrived()
//$$                     && evidence.finalRevalidated() && cleanup && outcome.satisfied() && outcome.scopeComplete()
//$$                     && Set.of("FOUND_AND_IN_SAFE_RANGE", "ALREADY_IN_SAFE_RANGE").contains(result);
//$$             case "finite_search_plan_completed_no_match" -> discoveryPhase && !discovered && cleanup && !outcome.satisfied()
//$$                     && outcome.scopeComplete() && result.equals("NOT_OBSERVED_IN_LOADED_SCOPE");
//$$             case "discovery_deadline_exhausted", "exploration_step_deadline_exhausted" -> discoveryPhase && failure(outcome, "TIMEOUT");
//$$             case "approach_deadline_exhausted", "native_approach_step_deadline_exhausted" -> approach && handoff && evidence.phase() == Phase.APPROACHING && failure(outcome, "TIMEOUT");
//$$             case "parent_deadline_exhausted" -> failure(outcome, "TIMEOUT");
//$$             case "exploration_route_unavailable", "exploration_no_progress" -> discoveryPhase && !discovered && failure(outcome, "UNREACHABLE");
//$$             case "post_discovery_approach_unreachable" -> approach && handoff && discovered && evidence.phase() == Phase.APPROACHING
//$$                     && failure(outcome, "UNREACHABLE");
//$$             case "selected_target_lost" -> failure(outcome, "TARGET_LOST");
//$$             case "selected_identity_unverifiable" -> failure(outcome, "CANDIDATE_NOT_REVALIDATABLE");
//$$             case "world_player_or_catalog_binding_changed", "root_ownership_lost" -> failure(outcome, "INTERRUPTED");
//$$             case "observation_read_failed", "owned_cleanup_failed", "movement_sample_unavailable" -> failure(outcome, "INTERNAL_ERROR");
//$$             case "invalid_target" -> discoveryPhase && !discovered && failure(outcome, "INVALID_TARGET");
//$$             default -> false;
//$$         };
//$$     }
//$$     private static boolean failure(FindOutcome outcome, String result) {
//$$         return !outcome.satisfied() && outcome.findResult().equals(result);
//$$     }
//$$     public static boolean legacyValid(FindOutcome outcome) {
//$$         String result = outcome.findResult(), reason = outcome.reason();
//$$         if (BOUNDS.contains(reason) || Set.of("discovery_target_revalidated_and_exploration_quiet", "finite_search_plan_completed_no_match",
//$$                 "discovery_deadline_exhausted", "exploration_step_deadline_exhausted", "approach_deadline_exhausted",
//$$                 "native_approach_step_deadline_exhausted", "parent_deadline_exhausted", "exploration_route_unavailable",
//$$                 "exploration_no_progress", "post_discovery_approach_unreachable", "selected_identity_unverifiable",
//$$                 "world_player_or_catalog_binding_changed", "root_ownership_lost", "observation_read_failed",
//$$                 "owned_cleanup_failed", "movement_sample_unavailable", "invalid_target").contains(reason)) return false;
//$$         boolean approach = outcome.request().mode().equals("approach");
//$$         if (Set.of("unsupported_target_safety_profile", "forced_input_lease_capability_unavailable").contains(reason)) return approach && failure(outcome, "UNREACHABLE");
//$$         if (LEGACY_UNREACHABLE.contains(reason)) return approach && failure(outcome, "UNREACHABLE");
//$$         return switch (reason) {
//$$             case "complete_loaded_scope_candidate_revalidated" -> !approach && outcome.satisfied() && result.equals("FOUND_AND_REPORTED");
//$$             case "same_target_safe_range_and_owned_cleanup" -> approach && outcome.satisfied()
//$$                     && Set.of("FOUND_AND_IN_SAFE_RANGE", "ALREADY_IN_SAFE_RANGE").contains(result);
//$$             case "complete_loaded_scope_no_match" -> failure(outcome, "NOT_OBSERVED_IN_LOADED_SCOPE");
//$$             case "elapsed_budget_exhausted", "entity_visit_limit_exhausted", "block_visit_limit_exhausted" -> failure(outcome, "OBSERVATION_BOUNDS_EXHAUSTED");
//$$             case "selected_candidate_not_revalidated", "terminal_candidate_not_revalidated", "selected_target_lost" -> failure(outcome, "TARGET_LOST");
//$$             case "catalog_resource_binding_changed", "world_or_player_binding_changed", "terminal_world_or_player_binding_changed",
//$$                     "user_root_replaced" -> failure(outcome, "INTERRUPTED");
//$$             case "observation_initialization_failed", "world_or_player_unavailable", "observation_read_or_revalidation_failed",
//$$                     "entity_read_failed", "IllegalStateException", "IllegalArgumentException", "NullPointerException" -> failure(outcome, "INTERNAL_ERROR");
//$$             case "item_approach_not_supported" -> failure(outcome, "INVALID_TARGET");
//$$             case "original_approach_deadline_exhausted", "native_step_no_progress_timeout" -> approach && failure(outcome, "TIMEOUT");
//$$             default -> failure(outcome, "INTERNAL_ERROR") && legacyExceptionName(reason);
//$$         };
//$$     }
//$$     private static boolean legacyExceptionName(String reason) {
//$$         if (reason.isEmpty() || !Character.isJavaIdentifierStart(reason.charAt(0))) return false;
//$$         for (int index = 1; index < reason.length(); index++) if (!Character.isJavaIdentifierPart(reason.charAt(index))) return false;
//$$         return true; // Legacy error class names are only cautious failures, never phase/discovery assertions.
//$$     }
//$$ }

//#endif
