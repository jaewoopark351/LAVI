//#if MC == 12001
//$$ package lavi.minecraft.find.exploration.operation;
//$$ 
//$$ import java.util.function.LongSupplier;
//$$ import java.util.function.BooleanSupplier;
//$$ import lavi.minecraft.find.approach.movement.FindApproachMovementPort;
//$$ import lavi.minecraft.find.diagnostics.FindLog;
//$$ import lavi.minecraft.find.exploration.movement.FindExplorationMovementPort;
//$$ import lavi.minecraft.find.model.FindCandidate;
//$$ import lavi.minecraft.find.model.FindRequest;
//$$ import lavi.minecraft.find.observation.FindObservationPort;
//$$ import lavi.minecraft.find.observation.FindObservationPort.Binding;
//$$ import lavi.minecraft.find.result.FindOutcome;
//$$ import lavi.minecraft.find.result.FindTerminalPhaseEvidence;
//$$ import lavi.minecraft.find.result.FindTerminalPhaseEvidence.Phase;
//$$ 
//$$ //20260914_kpopmodder: One parent owns admission clocks, internal misses, discovery handoff and one terminal.
//$$ public final class FindExplorationOperation {
//$$     private final FindRequest request;
//$$     private final String operationId;
//$$     private final Object root;
//$$     private final FindObservationPort observations;
//$$     private final FindExplorationMovementPort exploration;
//$$     private final FindApproachMovementPort approach;
//$$     private final LongSupplier clock;
//$$     private final FindLog log;
//$$     private final FindExplorationLimits limits;
//$$     private final BooleanSupplier freshRootOwnership;
//$$     private final long admitted;
//$$     private final Binding admission;
//$$     private FindDiscoveryProgress progress;
//$$     private Phase phase = Phase.INITIAL_OBSERVATION;
//$$     private FindCandidate selected;
//$$     private Binding handoff;
//$$     private FindOutcome outcome;
//$$     private boolean initialized, active, retired, routeActive, discoveryComplete, finalRevalidated, handedOff, arrived;
//$$     private int scans, visits, matches, routes;
//$$     private long lastScan, approachStarted;
//$$     private long lastDecisionTime;
//$$     private String readFailureType = "NONE";
//$$ 
//$$     public FindExplorationOperation(FindRequest request, String operationId, Object root,
//$$             FindObservationPort observations, FindExplorationMovementPort exploration,
//$$             FindApproachMovementPort approach, LongSupplier clock, FindLog log,
//$$             long admitted, FindExplorationLimits limits) {
//$$         this(request, operationId, root, observations, exploration, approach, clock, log, admitted, limits, () -> true);
//$$     }
//$$     public FindExplorationOperation(FindRequest request, String operationId, Object root,
//$$             FindObservationPort observations, FindExplorationMovementPort exploration,
//$$             FindApproachMovementPort approach, LongSupplier clock, FindLog log,
//$$             long admitted, FindExplorationLimits limits, BooleanSupplier freshRootOwnership) {
//$$         this.request = request; this.operationId = operationId; this.root = root;
//$$         this.observations = observations; this.exploration = exploration; this.approach = approach;
//$$         this.clock = clock; this.log = log; this.admitted = admitted; this.limits = limits;
//$$         this.freshRootOwnership = freshRootOwnership;
//$$         this.lastDecisionTime = admitted;
//$$         this.admission = observations.binding();
//$$         if (admission != null) progress = new FindDiscoveryProgress(admission, admitted, limits.maxSampleGapNanos(), log);
//$$     }
//$$     public void start() {
//$$         if (outcome != null || retired) return;
//$$         if (!initialized) {
//$$             initialized = true;
//$$             log.event("PARENT_STARTED", "kind", request.kind(), "mode", request.mode(),
//$$                 "targetId", request.canonicalTargetId(),
//$$                 "parentNanos", limits.parentNanos(), "discoveryNanos", limits.discoveryNanos(),
//$$                 "localVisitLimit", limits.localVisits(), "cumulativeVisitLimit", limits.cumulativeVisits(),
//$$                 "scanStartLimit", limits.scanStarts(), "routeStartLimit", limits.routeStarts(),
//$$                 "distancePolicy", "client_observed_no_64_cutoff", "admissionClockFrozen", true);
//$$         } else log.event("EXPLORATION_RESUMED", "phase", phase, "routes", routes, "budgetsPreserved", true);
//$$         if (!sample()) return;
//$$         if (retired || outcome != null) return;
//$$         active = true;
//$$         if (progress != null) progress.setActive(phase == Phase.EXPLORING, clock.getAsLong());
//$$     }
//$$     public void tick(boolean rootMatches) {
//$$         if (outcome != null || retired) return;
//$$         try {
//$$             if (!initialized || !active) start();
//$$             if (!checkBinding(rootMatches) || !sample() || expire() || checkDiscoveryBounds() || checkNoProgress()) return;
//$$             switch (phase) {
//$$                 case INITIAL_OBSERVATION -> observe("INITIAL_OBSERVATION");
//$$                 case EXPLORING -> {
//$$                     if (clock.getAsLong() - lastScan >= limits.scanIntervalNanos()) observe("DISCOVERY_LOOP_OBSERVATION");
//$$                     if (outcome != null || phase != Phase.EXPLORING || expire()) return;
//$$                     if (progress.activeWithoutProgress() >= limits.noProgressNanos()) {
//$$                         log.event("EXPLORATION_NO_PROGRESS", "reason", "exploration_no_progress");
//$$                         finish("UNREACHABLE", false, "exploration_no_progress", null, false); return;
//$$                     }
//$$                     if (!routeActive && !beginRoute()) return;
//$$                     var step = exploration.tick();
//$$                     if (!checkBinding(rootMatches) || !sample() || expire() || checkDiscoveryBounds()) return;
//$$                     if (step == null) { finish("INTERNAL_ERROR", false, "observation_read_failed", null, false); return; }
//$$                     if (step.reason() != null) {
//$$                         if (progress.activeWithoutProgress() >= limits.noProgressNanos()) {
//$$                             finish("UNREACHABLE", false, "exploration_no_progress", null, false); return;
//$$                         }
//$$                         log.event("EXPLORATION_ROUTE_RESULT", "reason", step.reason(), "routes", routes);
//$$                         routeFailure(step.reason()); return;
//$$                     }
//$$                     if (step.waypointReached()) {
//$$                         exploration.suspend(); routeActive = false; progress.verifiedProgress();
//$$                         if (!exploration.quiet()) finish("INTERNAL_ERROR", false, "owned_cleanup_failed", null, false);
//$$                     }
//$$                 }
//$$                 case STOPPING_EXPLORATION -> handoff();
//$$                 case APPROACHING -> approachTick(rootMatches);
//$$                 case TERMINAL -> { }
//$$             }
//$$         } catch (RuntimeException failure) {
//$$             readFailureType = failure.getClass().getSimpleName();
//$$             finish("INTERNAL_ERROR", false, "observation_read_failed", null, false);
//$$         }
//$$     }
//$$     private void observe(String role) {
//$$         if (scans >= limits.scanStarts()) { bounds("scan_count_limit_exhausted"); return; }
//$$         if (visits >= limits.cumulativeVisits()) { bounds("cumulative_entity_visit_limit_exhausted"); return; }
//$$         Binding local = observations.binding();
//$$         if (local == null) { finish("INTERNAL_ERROR", false, "movement_sample_unavailable", null, false); return; }
//$$         long started = clock.getAsLong();
//$$         lastScan = started; scans++;
//$$         int budget = Math.min(limits.localVisits(), limits.cumulativeVisits() - visits);
//$$         log.eventInPhase(role, "DISCOVERY_SCAN_STARTED", "scanStartsBefore", scans - 1, "scanStartsDelta", 1,
//$$             "scanStartsAfter", scans, "scanStarts", scans, "visitsBefore", visits,
//$$             "visitLimit", budget, "originX", local.x(), "originY", local.y(), "originZ", local.z());
//$$         var scan = observations.scanEntities(request, local, budget,
//$$             () -> active && !retired && freshRootOwnership.getAsBoolean() && withinDiscoveryTime()
//$$                 && (phase != Phase.EXPLORING || progress.projectedActiveWithoutProgress(clock.getAsLong()) < limits.noProgressNanos())
//$$                 && clock.getAsLong() - started < Math.min(limits.observationNanos(), limits.observationSliceNanos()), log, role);
//$$         if (scan == null || scan.visited() < 0 || scan.visited() > budget || scan.matched() < 0 || scan.matched() > scan.visited()) {
//$$             finish("INTERNAL_ERROR", false, "observation_read_failed", null, false); return;
//$$         }
//$$         visits += scan.visited(); matches += scan.matched();
//$$         log.eventInPhase(role, "DISCOVERY_SCAN_COMPLETED", "scanStarts", scans, "visitsBefore", visits - scan.visited(),
//$$             "visitsDelta", scan.visited(), "visited", scan.visited(), "matchesBefore", matches - scan.matched(),
//$$             "matchesDelta", scan.matched(), "matchesAfter", matches,
//$$             "visitsAfter", visits, "matched", scan.matched(), "complete", scan.complete(),
//$$             "scopeResult", !scan.complete() ? "INCOMPLETE" : scan.nearest() == null ? "MISS" : "MATCH", "reason", scan.reason());
//$$         if (!checkBinding(true)) return;
//$$         if ("entity_read_failed".equals(scan.reason())) { finish("INTERNAL_ERROR", false, "observation_read_failed", null, false); return; }
//$$         if (expire() || !sample() || checkDiscoveryBounds() || checkNoProgress()) return;
//$$         if (!scan.complete() || clock.getAsLong() - started >= Math.min(limits.observationNanos(), limits.observationSliceNanos())) {
//$$             if (clock.getAsLong() - started >= Math.min(limits.observationNanos(), limits.observationSliceNanos())
//$$                     || "elapsed_budget_exhausted".equals(scan.reason())) bounds("local_observation_deadline_exhausted");
//$$             else if (visits >= limits.cumulativeVisits()) bounds("cumulative_entity_visit_limit_exhausted");
//$$             else bounds("local_entity_visit_limit_exhausted");
//$$             return;
//$$         }
//$$         if (scan.nearest() == null) {
//$$             if (visits >= limits.cumulativeVisits()) { bounds("cumulative_entity_visit_limit_exhausted"); return; }
//$$             if (phase == Phase.INITIAL_OBSERVATION) changePhase(Phase.EXPLORING);
//$$             return;
//$$         }
//$$         selected = scan.nearest();
//$$         log.event("CANDIDATE_LATCHED", "scanStarts", scans, "entityId", selected.entityId(),
//$$             "distanceSquared", selected.distanceSquared(), "completeLocalScope", true);
//$$         FindCandidate live = observations.revalidate(request, local, selected, log, "INITIAL_CANDIDATE_REVALIDATION");
//$$         if (!checkBinding(true) || expire()) return;
//$$         if (live == null || !sameIdentity(selected, live)) {
//$$             finish("TARGET_LOST", false, "selected_target_lost", null, true); return;
//$$         }
//$$         selected = live; discoveryComplete = true;
//$$         changePhase(Phase.STOPPING_EXPLORATION);
//$$         log.event("EXPLORATION_STOP_REQUESTED", "reason", "candidate_discovered", "routes", routes);
//$$         handoff();
//$$     }
//$$     private boolean beginRoute() {
//$$         if (routes >= limits.routeStarts()) { bounds("exploration_start_limit_exhausted"); return false; }
//$$         if (!exploration.quiet()) { finish("INTERNAL_ERROR", false, "owned_cleanup_failed", null, false); return false; }
//$$         routes++; routeActive = true;
//$$         exploration.begin(admission, this::movementAllowed);
//$$         if (!checkBinding(true) || expire()) return false;
//$$         log.event("EXPLORATION_STARTED", "routesBefore", routes - 1, "routesDelta", 1, "routesAfter", routes,
//$$             "routes", routes, "budgetsPreserved", true);
//$$         return true;
//$$     }
//$$     private void handoff() {
//$$         exploration.suspend(); routeActive = false;
//$$         boolean quiet = exploration.quiet();
//$$         log.event("EXPLORATION_CLEANUP_RESULT", "reason", quiet ? "owned_resources_quiet" : "owned_cleanup_failed", "quiet", quiet);
//$$         if (!quiet) { finish("INTERNAL_ERROR", false, "owned_cleanup_failed", null, true); return; }
//$$         if (!checkBinding(true) || expire() || !sample() || checkDiscoveryBounds()) return;
//$$         Binding current = observations.binding();
//$$         FindCandidate live = observations.revalidate(request, current, selected, log, "HANDOFF_REVALIDATION");
//$$         if (!checkBinding(true) || expire() || !sample() || checkDiscoveryBounds()) return;
//$$         if (live == null || !sameIdentity(selected, live)) { finish("TARGET_LOST", false, "selected_target_lost", null, true); return; }
//$$         selected = live; finalRevalidated = true;
//$$         if (request.mode().equals("report")) {
//$$             finish("FOUND_AND_REPORTED", true, "discovery_target_revalidated_and_exploration_quiet", selected, true); return;
//$$         }
//$$         handoff = observations.binding();
//$$         if (!checkBinding(true) || expire()) return;
//$$         approach.begin(handoff, request, selected);
//$$         if (!checkBinding(true) || expire() || !sample() || checkDiscoveryBounds()) return;
//$$         handedOff = true; approachStarted = clock.getAsLong();
//$$         changePhase(Phase.APPROACHING);
//$$         log.event("HANDOFF_VALIDATED", "explorationQuiet", true, "sameTarget", true,
//$$             "originX", handoff.x(), "originY", handoff.y(), "originZ", handoff.z(),
//$$             "discoveryMovementFrozen", true, "sampledTravel", progress.travel(), "displacement", progress.displacement());
//$$     }
//$$     private void approachTick(boolean rootMatches) {
//$$         finalRevalidated = false;
//$$         var step = approach.tick();
//$$         if (!checkBinding(rootMatches) || expire()) return;
//$$         if (step == null) { finish("INTERNAL_ERROR", false, "observation_read_failed", null, true); return; }
//$$         if (step.failure() != null || step.arrived()) log.event("APPROACH_MOVEMENT_RESULT", "reason", step.failure(), "arrived", step.arrived());
//$$         if (step.arrived()) {
//$$             approach.suspend();
//$$             if (!approach.quiet()) { finish("INTERNAL_ERROR", false, "owned_cleanup_failed", null, true); return; }
//$$             FindCandidate live = observations.revalidate(request, handoff, selected, log, "ARRIVAL_REVALIDATION");
//$$             if (!checkBinding(rootMatches) || expire()) return;
//$$             if (live == null || !sameIdentity(selected, live) || step.candidate() == null || !sameIdentity(selected, step.candidate())) {
//$$                 finish("TARGET_LOST", false, "selected_target_lost", null, true); return;
//$$             }
//$$             arrived = true; finalRevalidated = true;
//$$             String result = "ALREADY_IN_SAFE_RANGE".equals(step.failure()) ? "ALREADY_IN_SAFE_RANGE" : "FOUND_AND_IN_SAFE_RANGE";
//$$             finish(result, true, "same_target_safe_range_and_owned_cleanup", live, true);
//$$         } else if (step.failure() != null) {
//$$             if ("native_step_no_progress_timeout".equals(step.failure())) {
//$$                 finish("TIMEOUT", false, "native_approach_step_deadline_exhausted", null, true); return;
//$$             }
//$$             finish("selected_target_lost".equals(step.failure()) ? "TARGET_LOST" : "UNREACHABLE", false,
//$$                 "selected_target_lost".equals(step.failure()) ? "selected_target_lost" : "post_discovery_approach_unreachable", null, true);
//$$         }
//$$     }
//$$     public void observeClientTick(boolean rootMatches) {
//$$         if (outcome != null || retired) return;
//$$         try { if (checkBinding(rootMatches) && sample() && !expire() && !checkDiscoveryBounds()) checkNoProgress(); }
//$$         catch (RuntimeException failure) { finish("INTERNAL_ERROR", false, "movement_sample_unavailable", null, false); }
//$$     }
//$$     private boolean sample() {
//$$         if (outcome != null || retired) return false;
//$$         if (handedOff) return true;
//$$         if (progress == null) { finish("INTERNAL_ERROR", false, "movement_sample_unavailable", null, false); return false; }
//$$         try { progress.sample(observations.binding(), clock.getAsLong()); return true; }
//$$         catch (RuntimeException failure) { finish("INTERNAL_ERROR", false, "movement_sample_unavailable", null, false); return false; }
//$$     }
//$$     private boolean checkBinding(boolean rootMatches) {
//$$         if (outcome != null || retired) return false;
//$$         if (!rootMatches || !freshRootOwnership.getAsBoolean()) { retireOwnedResources("root_ownership_lost"); return false; }
//$$         if (admission == null || !observations.matches(admission) || !observations.resourceBindingMatches(request)) {
//$$             finish("INTERRUPTED", false, "world_player_or_catalog_binding_changed", null, false); return false;
//$$         }
//$$         return true;
//$$     }
//$$     private boolean withinDiscoveryTime() {
//$$         long elapsed = clock.getAsLong() - admitted;
//$$         return elapsed >= 0 && elapsed < limits.parentNanos() && elapsed < limits.discoveryNanos();
//$$     }
//$$     private boolean movementAllowed() {
//$$         return active && !retired && outcome == null && phase == Phase.EXPLORING && withinDiscoveryTime()
//$$             && freshRootOwnership.getAsBoolean()
//$$             && observations.matches(admission) && observations.resourceBindingMatches(request)
//$$             && progress != null && progress.displacement() < limits.displacement()
//$$             && progress.travel() < limits.sampledTravel()
//$$             && progress.projectedActiveWithoutProgress(clock.getAsLong()) < limits.noProgressNanos();
//$$     }
//$$     private String deadlineReason() {
//$$         long now = clock.getAsLong();
//$$         lastDecisionTime = now;
//$$         if (now - admitted >= limits.parentNanos()) return "parent_deadline_exhausted";
//$$         if (!handedOff && now - admitted >= limits.discoveryNanos()) return "discovery_deadline_exhausted";
//$$         if (handedOff && now - approachStarted >= limits.approachNanos()) return "approach_deadline_exhausted";
//$$         return null;
//$$     }
//$$     private boolean expire() {
//$$         String reason = deadlineReason();
//$$         if (reason == null) return false;
//$$         log.event("DEADLINE_EXHAUSTED", "reason", reason);
//$$         finish("TIMEOUT", false, reason, null, discoveryComplete); return true;
//$$     }
//$$     private boolean checkDiscoveryBounds() {
//$$         if (handedOff || progress == null) return false;
//$$         if (progress.displacement() >= limits.displacement()) { bounds("exploration_displacement_limit_exhausted"); return true; }
//$$         if (progress.travel() >= limits.sampledTravel()) { bounds("exploration_sampled_movement_limit_exhausted"); return true; }
//$$         return false;
//$$     }
//$$     private void bounds(String reason) {
//$$         log.event("BOUNDS_EXHAUSTED", "reason", reason);
//$$         finish("OBSERVATION_BOUNDS_EXHAUSTED", false, reason, null, false);
//$$     }
//$$     private boolean checkNoProgress() {
//$$         if (phase != Phase.EXPLORING || progress == null
//$$                 || progress.projectedActiveWithoutProgress(clock.getAsLong()) < limits.noProgressNanos()) return false;
//$$         log.event("EXPLORATION_NO_PROGRESS", "reason", "exploration_no_progress", "noProgressAfter", progress.activeWithoutProgress());
//$$         finish("UNREACHABLE", false, "exploration_no_progress", null, false); return true;
//$$     }
//$$     private void routeFailure(String reason) {
//$$         if (expire()) return;
//$$         switch (reason) {
//$$             case "native_step_no_progress_timeout" -> finish("TIMEOUT", false, "exploration_step_deadline_exhausted", null, false);
//$$             case "exploration_displacement_limit_exhausted", "exploration_native_route_outside_admission_scope" -> bounds("exploration_displacement_limit_exhausted");
//$$             case "exploration_route_start_limit_exhausted" -> bounds("exploration_start_limit_exhausted");
//$$             case "native_planner_read_limit", "native_path_bounds_exhausted", "exploration_waypoint_work_limit" -> bounds("exploration_route_work_limit_exhausted");
//$$             case "world_or_player_binding_changed" -> finish("INTERRUPTED", false, "world_player_or_catalog_binding_changed", null, false);
//$$             case "exploration_movement_read_failed" -> finish("INTERNAL_ERROR", false, "observation_read_failed", null, false);
//$$             case "exploration_position_unavailable" -> finish("INTERNAL_ERROR", false, "movement_sample_unavailable", null, false);
//$$             default -> finish("UNREACHABLE", false, "exploration_route_unavailable", null, false);
//$$         }
//$$     }
//$$     public void suspend() {
//$$         if (outcome != null || retired) return;
//$$         sample(); active = false;
//$$         if (progress != null) progress.setActive(false, clock.getAsLong());
//$$         var cleanup = cleanupOwned(); routeActive = false;
//$$         log.event("EXPLORATION_SUSPENDED", "phase", phase, "budgetsPreserved", true,
//$$             "explorationQuiet", cleanup.explorationQuiet(), "inputQuiet", cleanup.approachQuiet());
//$$         if (!cleanup.success()) retireOwnedResources("owned_cleanup_failed");
//$$     }
//$$     public void retireOwnedResources(String reason) {
//$$         if (retired || outcome != null) return;
//$$         retired = true; active = false; routeActive = false;
//$$         var cleanup = cleanupOwned();
//$$         observeRetirement(reason, cleanup.success());
//$$     }
//$$     private void finish(String result, boolean satisfied, String reason, FindCandidate candidate, boolean complete) {
//$$         if (outcome != null || retired) return;
//$$         active = false;
//$$         var cleanup = cleanupOwned(); routeActive = false;
//$$         boolean explorationQuiet = cleanup.explorationQuiet(), approachQuiet = cleanup.approachQuiet();
//$$         if (retired || outcome != null) return;
//$$         if (!cleanup.success()) {
//$$             // No natural-completion profile without cleanup proof; external lifecycle retains UNKNOWN.
//$$             retired = true;
//$$             observeRetirement("owned_cleanup_failed", false); return;
//$$         }
//$$         if (!freshRootOwnership.getAsBoolean()) { retireOwnedResources("root_ownership_lost"); return; }
//$$         if (satisfied) {
//$$             String expired = deadlineReason();
//$$             if (expired != null) { result = "TIMEOUT"; reason = expired; satisfied = false; candidate = null; }
//$$             if (!observations.matches(admission) || !observations.resourceBindingMatches(request)) {
//$$                 result = "INTERRUPTED"; reason = "world_player_or_catalog_binding_changed"; satisfied = false; candidate = null;
//$$             }
//$$         }
//$$         if (retired || outcome != null) return;
//$$         if (!freshRootOwnership.getAsBoolean()) { retireOwnedResources("root_ownership_lost"); return; }
//$$         if (retired || outcome != null) return;
//$$         var evidence = new FindTerminalPhaseEvidence(operationId, request, root, phase, discoveryComplete,
//$$             explorationQuiet, finalRevalidated, handedOff, arrived, explorationQuiet && approachQuiet);
//$$         outcome = new FindOutcome(operationId, request, result, satisfied, admission == null ? "" : admission.dimension(),
//$$             candidate, visits, matches, complete, reason, evidence);
//$$         changePhase(Phase.TERMINAL);
//$$         log.event("PARENT_OUTCOME", "findResult", result, "satisfied", satisfied, "reason", reason,
//$$             "kind", request.kind(), "mode", request.mode(), "targetId", request.canonicalTargetId(),
//$$             "visited", visits, "matched", matches, "scanStarts", scans, "routeStarts", routes,
//$$             "sampledTravel", progress == null ? "unavailable" : progress.travel(),
//$$             "scopeComplete", complete, "explorationQuiet", explorationQuiet, "inputQuiet", approachQuiet,
//$$             "discoveryComplete", discoveryComplete, "handoff", handedOff, "finalRevalidated", finalRevalidated,
//$$             "elapsedParentNanos", lastDecisionTime - admitted, "parentLimitNanos", limits.parentNanos(),
//$$             "discoveryLimitNanos", limits.discoveryNanos(), "elapsedApproachNanos", handedOff ? lastDecisionTime - approachStarted : 0,
//$$             "approachLimitNanos", limits.approachNanos(), "localVisitLimit", limits.localVisits(),
//$$             "cumulativeVisitLimit", limits.cumulativeVisits(), "scanStartLimit", limits.scanStarts(), "routeStartLimit", limits.routeStarts(),
//$$             "displacement", progress == null ? "unavailable" : progress.displacement(), "displacementLimit", limits.displacement(),
//$$             "sampledTravelLimit", limits.sampledTravel(), "noProgressNanos", progress == null ? "unavailable" : progress.activeWithoutProgress(),
//$$             "noProgressLimitNanos", limits.noProgressNanos(), "readFailureType", readFailureType);
//$$         if (!satisfied) log.terminalFailure(result + ":" + reason, visits, matches, complete);
//$$     }
//$$     private void changePhase(Phase next) {
//$$         Phase before = phase; phase = next;
//$$         if (progress != null) progress.setActive(active && next == Phase.EXPLORING, clock.getAsLong());
//$$         log.event("PHASE_CHANGED", "phaseBefore", before, "phaseAfter", next);
//$$     }
//$$     private void observeRetirement(String reason, boolean quiet) {
//$$         log.event("PARENT_RETIRED", "reason", reason, "ownedQuiet", quiet, "profileAssigned", false, "findResult", "UNKNOWN",
//$$             "kind", request.kind(), "mode", request.mode(), "targetId", request.canonicalTargetId(), "phase", phase,
//$$             "visited", visits, "matched", matches, "scanStarts", scans, "routeStarts", routes,
//$$             "elapsedObservedParentNanos", lastDecisionTime - admitted, "parentLimitNanos", limits.parentNanos(),
//$$             "discoveryLimitNanos", limits.discoveryNanos(), "approachLimitNanos", limits.approachNanos(),
//$$             "cumulativeVisitLimit", limits.cumulativeVisits(), "scanStartLimit", limits.scanStarts(), "routeStartLimit", limits.routeStarts(),
//$$             "displacement", progress == null ? "unavailable" : progress.displacement(), "displacementLimit", limits.displacement(),
//$$             "sampledTravel", progress == null ? "unavailable" : progress.travel(), "sampledTravelLimit", limits.sampledTravel(),
//$$             "noProgressNanos", progress == null ? "unavailable" : progress.activeWithoutProgress(), "noProgressLimitNanos", limits.noProgressNanos());
//$$     }
//$$     private static boolean sameIdentity(FindCandidate before, FindCandidate after) {
//$$         return before.entityId() == after.entityId() && before.identityDigest().equals(after.identityDigest());
//$$     }
//$$     public FindRequest request() { return request; }
//$$     public String operationId() { return operationId; }
//$$     public FindOutcome outcome() { return outcome; }
//$$     private record OwnedCleanup(boolean explorationQuiet, boolean approachQuiet, boolean success) { }
//$$     private OwnedCleanup cleanupOwned() {
//$$         boolean failed = false, explorationQuiet = false, approachQuiet = false;
//$$         try { exploration.suspend(); } catch (RuntimeException failure) { failed = true; }
//$$         try { approach.suspend(); } catch (RuntimeException failure) { failed = true; }
//$$         try { explorationQuiet = exploration.quiet(); } catch (RuntimeException failure) { failed = true; }
//$$         try { approachQuiet = approach.quiet(); } catch (RuntimeException failure) { failed = true; }
//$$         return new OwnedCleanup(explorationQuiet, approachQuiet, !failed && explorationQuiet && approachQuiet);
//$$     }
//$$     public boolean ownedQuiet() {
//$$         try { return exploration.quiet() && approach.quiet(); } catch (RuntimeException failure) { return false; }
//$$     }
//$$     public boolean approachMovementAllowed() {
//$$         return active && !retired && outcome == null && handedOff && phase == Phase.APPROACHING
//$$             && freshRootOwnership.getAsBoolean()
//$$             && deadlineReason() == null && observations.matches(admission) && observations.resourceBindingMatches(request);
//$$     }
//$$     public boolean retired() { return retired; }
//$$     public void diagnosticRetired(String reason) { log.close(reason); }
//$$ }
//#endif
