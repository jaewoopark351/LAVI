//#if MC == 12001
//$$ package lavi.minecraft.find.diagnostics;

//$$ import java.util.function.BiConsumer;

//$$ //20260914_kpopmodder: FIND diagnostic failures cannot change observation, cleanup, or terminal decisions.
//$$ public final class FindLog {
//$$     private final String operationId;
//$$     private final FindTraceReservation reservation;
//$$     private final FindLogEmitter emitter;
//$$     private final FindLog parent;
//$$     private final String phaseRole;
//$$     private Object[] terminalSnapshot = new Object[0];

//$$     public FindLog(String operationId) {
//$$         parent = null; phaseRole = "";
//$$         this.operationId = operationId;
//$$         reservation = new FindTraceReservation(false);
//$$         emitter = new FindLogEmitter(null);
//$$     }
//$$     public FindLog(String operationId, BiConsumer<String, Object[]> sink) {
//$$         parent = null; phaseRole = "";
//$$         this.operationId = operationId;
//$$         reservation = new FindTraceReservation(true);
//$$         emitter = new FindLogEmitter(sink);
//$$     }
//$$     private FindLog(FindLog parent, String phaseRole) {
//$$         this.parent = parent; this.phaseRole = phaseRole;
//$$         operationId = parent.operationId; reservation = parent.reservation; emitter = parent.emitter;
//$$     }
//$$     public FindLog inPhase(String role) {
//$$         if (!"EXPLORATION_MOVEMENT".equals(role) && !"APPROACH_MOVEMENT".equals(role)) {
//$$             reservation.markOutputFailure(); return this;
//$$         }
//$$         return new FindLog(parent == null ? this : parent, role);
//$$     }
//$$     public void event(String event, Object... values) {
//$$         if (parent != null) { parent.eventInPhase(phaseRole, event, values); return; }
//$$         emit("", event, values);
//$$     }
//$$     public void eventInPhase(String phaseRole, String event, Object... values) {
//$$         if (parent != null) { parent.eventInPhase(phaseRole, event, values); return; }
//$$         try {
//$$             String phaseSignature = switch (phaseRole) {
//$$                 case "INITIAL_OBSERVATION" -> "I";
//$$                 case "DISCOVERY_LOOP_OBSERVATION" -> "D";
//$$                 case "HANDOFF_REVALIDATION" -> "H";
//$$                 case "INITIAL_CANDIDATE_REVALIDATION" -> "C";
//$$                 case "ARRIVAL_REVALIDATION" -> "A";
//$$                 case "EXPLORATION_MOVEMENT" -> "E";
//$$                 case "APPROACH_MOVEMENT" -> "M";
//$$                 default -> throw new IllegalArgumentException("unknown_find_diagnostic_phase");
//$$             };
//$$             Object[] phased = new Object[values.length + 2];
//$$             System.arraycopy(values, 0, phased, 0, values.length);
//$$             phased[values.length] = "phaseRole"; phased[values.length + 1] = phaseRole;
//$$             emit(phaseSignature, event, phased);
//$$         } catch (RuntimeException | LinkageError | AssertionError ignored) { reservation.markOutputFailure(); }
//$$     }
//$$     private void emit(String phaseSignature, String event, Object[] values) {
//$$         try {
//$$             if (event.equals("REPORT_TERMINAL") || event.equals("APPROACH_TERMINAL") || event.equals("PARENT_OUTCOME")) terminalSnapshot = values.clone();
//$$             else if (event.equals("PARENT_RETIRED") && terminalSnapshot.length == 0) terminalSnapshot = values.clone();
//$$             String exclusion = reservation.observeTraceExclusion();
//$$             if (exclusion != null) emitter.traceExclusion(operationId, exclusion, reservation.status());
//$$             String owner = event.startsWith("INPUT_") ? "FindOwnedMovementInputs"
//$$                     : event.equals("ROOT_LIFETIME_BINDING_RESULT") ? "FindExplorationTask"
//$$                     : event.startsWith("ENTITY_QUERY_") || event.equals("ENTITY_REVALIDATION_RESULT") ? "MinecraftFindEntityObservation"
//$$                     : event.equals("APPROACH_MOVEMENT_RESULT") ? "FindExplorationOperation"
//$$                     : event.startsWith("APPROACH_PLAN_") || event.equals("APPROACH_CAPTURE_COMPLETE") ? "FindFlatPathPlanner"
//$$                     : event.startsWith("APPROACH_STEP_") || event.equals("APPROACH_STARTED") ? "MinecraftFindNativeMovement"
//$$                     : event.startsWith("APPROACH_") ? "FindApproachOperation"
//$$                     : event.startsWith("EXPLORATION_PLAN_") || event.startsWith("EXPLORATION_STEP_") ? "MinecraftFindExplorationNativeSession"
//$$                     : event.equals("EXPLORATION_ROUTE_STARTED") || event.startsWith("EXPLORATION_WAYPOINT_")
//$$                       || event.startsWith("EXPLORATION_MOVEMENT_") ? "MinecraftFindExplorationMovement"
//$$                     : event.startsWith("EXPLORATION_") ? "FindExplorationOperation"
//$$                     : event.equals("DISCOVERY_MOVEMENT_SAMPLED") || event.equals("DISCOVERY_PROGRESS_CLOCK_CHANGED")
//$$                       || event.equals("DISCOVERY_WAYPOINT_PROGRESS") ? "FindDiscoveryProgress"
//$$                     : event.startsWith("DISCOVERY_") || event.startsWith("PARENT_") ? "FindExplorationOperation"
//$$                     : event.equals("PHASE_CHANGED") || event.equals("CANDIDATE_LATCHED") || event.equals("HANDOFF_VALIDATED")
//$$                       || event.equals("BOUNDS_EXHAUSTED") || event.equals("DEADLINE_EXHAUSTED") ? "FindExplorationOperation"
//$$                     : event.equals("RETIRED") || event.equals("TRACE_TERMINAL") ? "FindLog.close" : "FindObservationOperation";
//$$             String signature = owner + "|" + event + "|" + value(values, "findResult") + "|" + value(values, "reason")
//$$                     + "|" + value(values, "input") + "|" + value(values, "claimed") + "|" + value(values, "released")
//$$                     + "|" + value(values, "nativeKind") + (phaseSignature.isEmpty() ? "" : "|" + phaseSignature)
//$$                     + (event.equals("ENTITY_QUERY_COMPLETED") || event.equals("DISCOVERY_SCAN_COMPLETED") ? "|" + queryResultRole(values) : "")
//$$                     + (event.equals("PHASE_CHANGED") ? "|" + value(values, "phaseBefore") + "|" + value(values, "phaseAfter") : "")
//$$                     + (event.equals("EXPLORATION_SUSPENDED") || event.equals("EXPLORATION_RESUMED") ? "|" + value(values, "phase") : "");
//$$             if (event.equals("APPROACH_PLAN_RESULT")) signature += "|" + value(values, "nativeResult");
//$$             if (event.equals("INPUT_RELEASE")) signature += "|" + value(values, "leaseStillOwned") + "|" + value(values, "supersededWriterPreserved");
//$$             var claim = reservation.claim(event, signature);
//$$             if (claim.emit() && !emitter.emit(claim, event, operationId, signature, values)) reservation.markOutputFailure();
//$$         } catch (RuntimeException | LinkageError | AssertionError ignored) {
//$$             reservation.markOutputFailure();
//$$             // A sink failure remains an evidence gap; no gameplay fallback or retry.
//$$         }
//$$     }
//$$     public void terminalFailure(String reason, int visited, int matched, boolean complete) {
//$$         if (parent != null) { parent.terminalFailure(reason, visited, matched, complete); return; }
//$$         emitter.terminalFailure(operationId, reason, visited, matched, complete, reservation.status(), reservation.exclusionReason(), terminalSnapshot);
//$$     }
//$$     public void close(String reason) {
//$$         if (parent != null) { parent.close(reason); return; }
//$$         if (reservation.closed()) return;
//$$         event("RETIRED", "reason", reason);
//$$         event("TRACE_TERMINAL", "reason", reason);
//$$         emitter.retirement(operationId, reason, terminalSnapshot, reservation.status(), reservation.exclusionReason());
//$$         reservation.close();
//$$     }
//$$     private static String value(Object[] fields, String key) {
//$$         for (int index = 0; index + 1 < fields.length; index += 2) if (key.equals(fields[index])) return String.valueOf(fields[index + 1]);
//$$         return "";
//$$     }
//$$     private static String queryResultRole(Object[] values) {
//$$         return switch (value(values, "scopeResult")) { case "MATCH" -> "F"; case "MISS" -> "M"; case "INCOMPLETE" -> "X"; default -> ""; };
//$$     }
//$$ }

//#endif
