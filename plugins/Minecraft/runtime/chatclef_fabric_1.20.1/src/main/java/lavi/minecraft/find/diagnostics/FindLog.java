//#if MC == 12001
//$$ package lavi.minecraft.find.diagnostics;

//$$ import java.util.function.BiConsumer;

//$$ //20260914_kpopmodder: FIND diagnostic failures cannot change observation, cleanup, or terminal decisions.
//$$ public final class FindLog {
//$$     private final String operationId;
//$$     private final FindTraceReservation reservation;
//$$     private final FindLogEmitter emitter;
//$$     private Object[] terminalSnapshot = new Object[0];

//$$     public FindLog(String operationId) {
//$$         this.operationId = operationId;
//$$         reservation = new FindTraceReservation(false);
//$$         emitter = new FindLogEmitter(null);
//$$     }
//$$     public FindLog(String operationId, BiConsumer<String, Object[]> sink) {
//$$         this.operationId = operationId;
//$$         reservation = new FindTraceReservation(true);
//$$         emitter = new FindLogEmitter(sink);
//$$     }
//$$     public void event(String event, Object... values) {
//$$         try {
//$$             if (event.equals("REPORT_TERMINAL") || event.equals("APPROACH_TERMINAL")) terminalSnapshot = values.clone();
//$$             String exclusion = reservation.observeTraceExclusion();
//$$             if (exclusion != null) emitter.traceExclusion(operationId, exclusion, reservation.status());
//$$             String owner = event.startsWith("INPUT_") ? "FindOwnedMovementInputs"
//$$                     : event.startsWith("APPROACH_PLAN_") || event.equals("APPROACH_CAPTURE_COMPLETE") ? "FindFlatPathPlanner"
//$$                     : event.startsWith("APPROACH_STEP_") || event.equals("APPROACH_STARTED") ? "MinecraftFindNativeMovement"
//$$                     : event.startsWith("APPROACH_") ? "FindApproachOperation"
//$$                     : event.equals("RETIRED") || event.equals("TRACE_TERMINAL") ? "FindLog.close" : "FindObservationOperation";
//$$             String signature = owner + "|" + event + "|" + value(values, "findResult") + "|" + value(values, "reason")
//$$                     + "|" + value(values, "input") + "|" + value(values, "claimed") + "|" + value(values, "released")
//$$                     + "|" + value(values, "nativeKind");
//$$             var claim = reservation.claim(event, signature);
//$$             if (claim.emit() && !emitter.emit(claim, event, operationId, signature, values)) reservation.markOutputFailure();
//$$         } catch (RuntimeException | LinkageError ignored) {
//$$             reservation.markOutputFailure();
//$$             // A sink failure remains an evidence gap; no gameplay fallback or retry.
//$$         }
//$$     }
//$$     public void terminalFailure(String reason, int visited, int matched, boolean complete) {
//$$         emitter.terminalFailure(operationId, reason, visited, matched, complete, reservation.status(), reservation.exclusionReason());
//$$     }
//$$     public void close(String reason) {
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
//$$ }

//#endif
