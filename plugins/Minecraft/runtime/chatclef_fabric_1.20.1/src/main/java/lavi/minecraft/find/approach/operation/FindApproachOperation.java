//#if MC == 12001
//$$ package lavi.minecraft.find.approach.operation;
//$$
//$$ import java.util.function.LongSupplier;
//$$ import lavi.minecraft.find.approach.movement.FindApproachMovementPort;
//$$ import lavi.minecraft.find.diagnostics.FindLog;
//$$ import lavi.minecraft.find.model.FindCandidate;
//$$ import lavi.minecraft.find.model.FindRequest;
//$$ import lavi.minecraft.find.observation.FindObservationOperation;
//$$ import lavi.minecraft.find.observation.FindObservationPort;
//$$ import lavi.minecraft.find.result.FindOutcome;
//$$
//$$ //20260914_kpopmodder: One operation owns its frozen deadline, observed candidate and assigned-once terminal.
//$$ public final class FindApproachOperation {
//$$     public static final long DEADLINE_NANOS = 120_000_000_000L;
//$$     private final FindObservationOperation observation;
//$$     private final FindObservationPort observations;
//$$     private final FindApproachMovementPort movement;
//$$     private final LongSupplier clock;
//$$     private final FindLog log;
//$$     private boolean initialized, approaching, suspended;
//$$     private long started;
//$$     private FindOutcome outcome;
//$$
//$$     public FindApproachOperation(FindObservationOperation observation, FindObservationPort observations,
//$$                                  FindApproachMovementPort movement, LongSupplier clock, FindLog log) {
//$$         this.observation = observation; this.observations = observations; this.movement = movement;
//$$         this.clock = clock; this.log = log;
//$$     }
//$$     public void start() {
//$$         if (outcome != null) return;
//$$         if (!initialized) { initialized = true; started = clock.getAsLong(); }
//$$         else log.event("APPROACH_RESUMED", "originalDeadlinePreserved", true, "candidatePreserved", approaching);
//$$         suspended = false; observation.start();
//$$     }
//$$     public void tick(boolean userRootMatches) {
//$$         if (outcome != null) return;
//$$         try {
//$$             if (!initialized) start();
//$$             if (!userRootMatches) { commit("INTERRUPTED", false, "user_root_replaced", null); return; }
//$$             if (expire()) return;
//$$             if (!observations.resourceBindingMatches(request())) {
//$$                 commit("INTERRUPTED", false, "catalog_resource_binding_changed", null); return;
//$$             }
//$$             if (!approaching) {
//$$                 observation.tick();
//$$                 if (expire()) return;
//$$                 FindOutcome report = observation.outcome();
//$$                 if (report == null) return;
//$$                 if (!report.satisfied()) { commit(report.findResult(), false, report.reason(), null); return; }
//$$                 if (request().kind().equals("item")) { commit("INVALID_TARGET", false, "item_approach_not_supported", null); return; }
//$$                 movement.begin(observation.binding(), request(), report.candidate()); approaching = true;
//$$             }
//$$             if (!observations.matches(observation.binding())) {
//$$                 commit("INTERRUPTED", false, "world_or_player_binding_changed", null); return;
//$$             }
//$$             FindApproachMovementPort.Step step = movement.tick();
//$$             if (expire()) return;
//$$             if (!observations.resourceBindingMatches(request())) {
//$$                 commit("INTERRUPTED", false, "catalog_resource_binding_changed", null); return;
//$$             }
//$$             if (step.arrived()) {
//$$                 movement.suspend();
//$$                 if (expire()) return;
//$$                 if (!movement.quiet()) return;
//$$                 FindCandidate current = observations.revalidate(request(), observation.binding(), step.candidate());
//$$                 if (expire()) return;
//$$                 if (!observations.resourceBindingMatches(request())) {
//$$                     commit("INTERRUPTED", false, "catalog_resource_binding_changed", null); return;
//$$                 }
//$$                 if (current == null) commit("TARGET_LOST", false, "terminal_candidate_not_revalidated", null);
//$$                 else if (observations.matches(observation.binding())) {
//$$                     commit(step.failure(), true, "same_target_safe_range_and_owned_cleanup", current);
//$$                 } else commit("INTERRUPTED", false, "terminal_world_or_player_binding_changed", null);
//$$             } else if (step.failure() != null) {
//$$                 String result = step.failure().equals("selected_target_lost") ? "TARGET_LOST"
//$$                         : step.failure().contains("timeout") ? "TIMEOUT" : "UNREACHABLE";
//$$                 commit(result, false, step.failure(), null);
//$$             }
//$$         } catch (RuntimeException exception) {
//$$             // A failed cleanup is not success evidence; a repeat failure propagates to the existing bridge.
//$$             commit("INTERNAL_ERROR", false, exception.getClass().getSimpleName(), null);
//$$         }
//$$     }
//$$     public void suspend() {
//$$         movement.suspend();
//$$         if (outcome == null && !suspended) {
//$$             suspended = true; observation.suspend();
//$$             log.event("APPROACH_SUSPENDED", "originalDeadlinePreserved", true, "candidatePreserved", approaching,
//$$                     "ownedMovementQuiet", movement.quiet());
//$$         }
//$$     }
//$$     private boolean expire() {
//$$         if (clock.getAsLong() - started < DEADLINE_NANOS) return false;
//$$         commit("TIMEOUT", false, "original_approach_deadline_exhausted", null); return true;
//$$     }
//$$     private void commit(String result, boolean satisfied, String reason, FindCandidate candidate) {
//$$         if (outcome != null) return;
//$$         movement.suspend();
//$$         if (!movement.quiet()) throw new IllegalStateException("find_owned_movement_not_quiescent");
//$$         // Cleanup or validation may consume the last admitted time; they cannot turn expiry into success.
//$$         if (satisfied && clock.getAsLong() - started >= DEADLINE_NANOS) {
//$$             result = "TIMEOUT"; satisfied = false; reason = "original_approach_deadline_exhausted"; candidate = null;
//$$         }
//$$         FindOutcome report = observation.outcome();
//$$         FindObservationPort.Binding binding = observation.binding();
//$$         outcome = new FindOutcome(operationId(), request(), result, satisfied, binding == null ? "" : binding.dimension(),
//$$                 candidate, report == null ? 0 : report.visited(), report == null ? 0 : report.matched(),
//$$                 report != null && report.scopeComplete(), reason);
//$$         // Counts are diagnostic snapshots after the assigned terminal; failure cannot change that decision.
//$$         try {
//$$             var progress = movement.progress();
//$$             log.event("APPROACH_TERMINAL", "findResult", result, "satisfied", satisfied, "reason", reason,
//$$                     "ownedMovementQuiet", true, "originalDeadlinePreserved", true,
//$$                     "visited", outcome.visited(), "matched", outcome.matched(), "scopeComplete", outcome.scopeComplete(),
//$$                     "stepsBefore", 0, "stepsDelta", progress.completedSteps(), "stepsAfter", progress.completedSteps(),
//$$                     "plannedSteps", progress.plannedSteps(), "captureColumns", progress.captureColumns(),
//$$                     "movementInputIssued", progress.movementInputIssued());
//$$         } catch (RuntimeException | LinkageError ignored) {
//$$             log.event("APPROACH_TERMINAL", "findResult", result, "satisfied", satisfied, "reason", reason,
//$$                     "ownedMovementQuiet", true, "originalDeadlinePreserved", true, "progressUnavailable", true,
//$$                     "visited", outcome.visited(), "matched", outcome.matched(), "scopeComplete", outcome.scopeComplete());
//$$         }
//$$         if (!satisfied) log.terminalFailure(result + ":" + reason, outcome.visited(), outcome.matched(), outcome.scopeComplete());
//$$     }
//$$     public FindOutcome outcome() { return outcome; }
//$$     public FindRequest request() { return observation.request(); }
//$$     public String operationId() { return observation.operationId(); }
//$$     public void diagnosticRetired(String reason) { observation.diagnosticRetired(reason); }
//$$ }
//#endif
