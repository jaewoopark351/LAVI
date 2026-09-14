//#if MC == 12001
//$$ package lavi.minecraft.find.observation;

//$$ import java.util.function.LongSupplier;
//$$ import lavi.minecraft.find.diagnostics.FindLog;
//$$ import lavi.minecraft.find.model.FindCandidate;
//$$ import lavi.minecraft.find.model.FindRequest;
//$$ import lavi.minecraft.find.result.FindOutcome;

//$$ //20260914_kpopmodder: The finite operation owns one frozen origin, monotonic deadline and assigned-once outcome.
//$$ public final class FindObservationOperation {
//$$     public static final long DEADLINE_NANOS = 5_000_000_000L;
//$$     public static final int ENTITY_VISIT_LIMIT = 4096, BLOCK_VISIT_LIMIT = 300000, BLOCK_TICK_LIMIT = 4096;
//$$     public static final long BLOCK_TICK_SOFT_NANOS = 2_000_000L;
//$$     private final FindRequest request;
//$$     private final String operationId;
//$$     private final FindObservationPort port;
//$$     private final LongSupplier clock;
//$$     private final FindLog log;
//$$     private FindObservationPort.Binding binding;
//$$     private FindBlockObservationCursor cursor;
//$$     private long started;
//$$     private boolean initialized;
//$$     private boolean observationComplete;
//$$     private int visited, matched;
//$$     private FindCandidate nearest;
//$$     private FindOutcome outcome;

//$$     public FindObservationOperation(FindRequest request, String operationId, FindObservationPort port,
//$$                                     LongSupplier clock, FindLog log) {
//$$         this.request = request; this.operationId = operationId; this.port = port; this.clock = clock; this.log = log;
//$$     }
//$$     public void start() {
//$$         try { startOwned(); }
//$$         catch (RuntimeException failure) { commit("INTERNAL_ERROR", false, "observation_initialization_failed", null); }
//$$     }
//$$     private void startOwned() {
//$$         if (outcome != null) return;
//$$         if (initialized) {
//$$             log.event("RESUMED", "visited", visited, "matched", matched, "originalDeadlinePreserved", true);
//$$             return;
//$$         }
//$$         initialized = true;
//$$         started = clock.getAsLong();
//$$         if (!port.resourceBindingMatches(request)) { commit("INTERRUPTED", false, "catalog_resource_binding_changed", null); return; }
//$$         binding = port.binding();
//$$         if (binding == null) { commit("INTERNAL_ERROR", false, "world_or_player_unavailable", null); return; }
//$$         if (request.kind().equals("block")) {
//$$             cursor = new FindBlockObservationCursor(binding.x(), binding.y(), binding.z(), binding.bottomY(), binding.topY());
//$$         }
//$$         log.event("STARTED", "targetKind", request.kind(), "mode", request.mode(), "dimension", binding.dimension(),
//$$                 "canonicalTargetId", request.canonicalTargetId(),
//$$                 "playerIdentityDigest", request.kind().equals("player") ? MinecraftFindObservationPort.digest(request.playerName()) : "",
//$$                 "catalogDigest", request.catalogDigest(), "resourceGeneration", request.resourceGeneration(),
//$$                 "radius", request.kind().equals("entity") && request.mode().equals("report") ? "NOT_APPLIED" : request.kind().equals("block") ? 32 : 64,
//$$                 "distancePolicy", request.kind().equals("entity") && request.mode().equals("report") ? "CLIENT_OBSERVABLE_NO_RADIUS" : "FROZEN_ORIGIN_RADIUS",
//$$                 "originX", binding.x(), "originY", binding.y(), "originZ", binding.z(), "elapsedBudgetMillis", 5000,
//$$                 "visitLimit", cursor == null ? ENTITY_VISIT_LIMIT : BLOCK_VISIT_LIMIT,
//$$                 "visitedBefore", 0, "visitedDelta", 0, "visitedAfter", visited,
//$$                 "matchedBefore", 0, "matchedDelta", 0, "matchedAfter", matched);
//$$     }
//$$     public void tick() {
//$$         try { tickOwned(); }
//$$         catch (RuntimeException failure) { commit("INTERNAL_ERROR", false, "observation_read_or_revalidation_failed", null); }
//$$     }
//$$     private void tickOwned() {
//$$         if (!initialized) start();
//$$         if (outcome != null) return;
//$$         if (!port.resourceBindingMatches(request)) { commit("INTERRUPTED", false, "catalog_resource_binding_changed", null); return; }
//$$         if (!port.matches(binding)) { commit("INTERRUPTED", false, "world_or_player_binding_changed", null); return; }
//$$         if (expired()) { commit("OBSERVATION_BOUNDS_EXHAUSTED", false, "elapsed_budget_exhausted", null); return; }
//$$         if (!observationComplete) {
//$$             if (cursor == null) {
//$$                 FindObservationPort.EntityScan scan = port.scanEntities(request, binding, ENTITY_VISIT_LIMIT, () -> !expired(), log);
//$$                 visited = scan.visited(); matched = scan.matched(); nearest = scan.nearest();
//$$                 if (!scan.complete()) {
//$$                     String result = scan.reason().equals("entity_read_failed") ? "INTERNAL_ERROR"
//$$                             : scan.reason().equals("world_or_player_binding_changed") ? "INTERRUPTED" : "OBSERVATION_BOUNDS_EXHAUSTED";
//$$                     commit(result, false, scan.reason(), null); return;
//$$                 }
//$$                 observationComplete = true;
//$$             } else {
//$$                 long tickStarted = clock.getAsLong();
//$$                 int tickVisited = 0;
//$$                 while (!cursor.complete() && tickVisited < BLOCK_TICK_LIMIT) {
//$$                     if (visited >= BLOCK_VISIT_LIMIT) { commit("OBSERVATION_BOUNDS_EXHAUSTED", false, "block_visit_limit_exhausted", null); return; }
//$$                     if (expired()) { commit("OBSERVATION_BOUNDS_EXHAUSTED", false, "elapsed_budget_exhausted", null); return; }
//$$                     FindBlockObservationCursor.Position position = cursor.next();
//$$                     visited++; tickVisited++;
//$$                     FindCandidate candidate = port.readBlock(request, binding, position.x(), position.y(), position.z());
//$$                     if (candidate != null) { matched++; nearest = FindCandidateSelection.nearer(nearest, candidate); }
//$$                     if (clock.getAsLong() - tickStarted >= BLOCK_TICK_SOFT_NANOS) break;
//$$                 }
//$$                 observationComplete = cursor.complete();
//$$                 if (!observationComplete) return;
//$$             }
//$$             log.event("OBSERVATION_COMPLETED", "visited", visited, "matched", matched, "scopeComplete", true,
//$$                     "visitedBefore", 0, "visitedDelta", visited, "visitedAfter", visited,
//$$                     "matchedBefore", 0, "matchedDelta", matched, "matchedAfter", matched);
//$$         }
//$$         if (expired()) { commit("OBSERVATION_BOUNDS_EXHAUSTED", false, "elapsed_budget_exhausted", null); return; }
//$$         if (nearest == null) { commit("NOT_OBSERVED_IN_LOADED_SCOPE", false, "complete_loaded_scope_no_match", null); return; }
//$$         FindCandidate current = port.revalidate(request, binding, nearest, log);
//$$         if (!port.resourceBindingMatches(request)) { commit("INTERRUPTED", false, "catalog_resource_binding_changed", null); return; }
//$$         if (expired()) { commit("OBSERVATION_BOUNDS_EXHAUSTED", false, "elapsed_budget_exhausted", null); return; }
//$$         if (current == null) { commit("TARGET_LOST", false, "selected_candidate_not_revalidated", null); return; }
//$$         commit("FOUND_AND_REPORTED", true, "complete_loaded_scope_candidate_revalidated", current);
//$$     }
//$$     public void suspend() {
//$$         if (outcome == null) log.event("SUSPENDED", "visited", visited, "matched", matched, "originalDeadlinePreserved", true);
//$$     }
//$$     public FindOutcome outcome() { return outcome; }
//$$     public FindRequest request() { return request; }
//$$     public String operationId() { return operationId; }
//$$     public void diagnosticRetired(String reason) { log.close(reason); }
//$$     public FindObservationPort.Binding binding() { return binding; }
//$$     private boolean expired() { return clock.getAsLong() - started >= DEADLINE_NANOS; }
//$$     private void commit(String result, boolean satisfied, String reason, FindCandidate candidate) {
//$$         if (outcome != null) return;
//$$         outcome = new FindOutcome(operationId, request, result, satisfied, binding == null ? "" : binding.dimension(),
//$$                 candidate, visited, matched, observationComplete, reason);
//$$         log.event(request.mode().equals("report") ? "REPORT_TERMINAL" : "OBSERVATION_TERMINAL", "findResult", result, "satisfied", satisfied, "visited", visited,
//$$                 "matched", matched, "scopeComplete", observationComplete, "reason", reason, "ownedResources", "NONE",
//$$                 "visitedBefore", 0, "visitedDelta", visited, "visitedAfter", visited,
//$$                 "matchedBefore", 0, "matchedDelta", matched, "matchedAfter", matched);
//$$         if (!satisfied && request.mode().equals("report")) log.terminalFailure(result + ":" + reason, visited, matched, observationComplete);
//$$     }
//$$ }

//#endif
