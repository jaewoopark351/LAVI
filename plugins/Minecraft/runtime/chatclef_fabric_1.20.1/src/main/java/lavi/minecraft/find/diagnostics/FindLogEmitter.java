//#if MC == 12001
//$$ package lavi.minecraft.find.diagnostics;

//$$ import java.util.function.BiConsumer;
//$$ import adris.altoclef.Debug;
//$$ import lavi.minecraft.diagnostics.ChatClefDiagnostics;
//$$ import lavi.minecraft.diagnostics.formatting.DiagnosticBoundedEventFormatter;

//$$ //20260914_kpopmodder: Emission consumes immutable owner snapshots without selecting FIND behavior.
//$$ final class FindLogEmitter {
//$$     private final BiConsumer<String, Object[]> injectedSink;
//$$     private Object[] originalContext;
//$$     FindLogEmitter(BiConsumer<String, Object[]> injectedSink) { this.injectedSink = injectedSink; }
//$$     boolean emit(FindTraceReservation.Claim claim, String event, String operationId, String signature, Object[] values) {
//$$         try {
//$$             if (originalContext == null) originalContext = FindCommandLogContext.capture();
//$$             Object[] fields = new Object[values.length + originalContext.length + 6];
//$$             fields[0] = "operationId"; fields[1] = operationId;
//$$             fields[2] = "findTraceReservation"; fields[3] = claim.status();
//$$             fields[4] = "requiredBoundarySignature"; fields[5] = signature;
//$$             System.arraycopy(values, 0, fields, 6, values.length);
//$$             System.arraycopy(originalContext, 0, fields, 6 + values.length, originalContext.length);
//$$             if (injectedSink != null) { injectedSink.accept(event, fields); return true; }
//$$             if (claim.token() == null) {
//$$                 return ChatClefDiagnostics.logBoundedBoundaryWithDispatchResult("FIND_" + event, "find_ordinary_detail", null,
//$$                         2048, fields, new Object[0]).emissionCompleted();
//$$             }
//$$             var output = ChatClefDiagnostics.emitReservedBoundary(claim.token(), "FIND_" + event, "find_owner_observation", null, fields);
//$$             return output != null && output.completed();
//$$         } catch (RuntimeException | LinkageError | AssertionError ignored) { return false; }
//$$     }
//$$     void traceExclusion(String operationId, String reason, String traceStatus) {
//$$         try {
//$$             ChatClefDiagnostics.logBoundedBoundaryWithDispatchResult("FIND_TRACE_EXCLUDED", reason, null, 2048,
//$$                     new Object[] { "operationId", operationId, "findTraceReservation", traceStatus,
//$$                             "reservationRetry", false, "fullTraceEmissionVerified", false }, new Object[0]);
//$$         } catch (RuntimeException | LinkageError | AssertionError ignored) { }
//$$     }
//$$     void retirement(String operationId, String reason, Object[] snapshot, String traceStatus, String exclusionReason) {
//$$         try {
//$$             normal("LAVI FIND operation terminal ", operationId, "retirementReason", reason, snapshot,
//$$                     traceStatus, exclusionReason);
//$$         } catch (RuntimeException | LinkageError | AssertionError ignored) { }
//$$     }
//$$     void terminalFailure(String operationId, String reason, int visited, int matched, boolean complete, String traceStatus, String exclusionReason,
//$$                          Object[] snapshot) {
//$$         try {
//$$             normal("LAVI FIND terminal ", operationId, "terminalReason", reason,
//$$                     snapshot.length == 0 ? new Object[]{"visited", visited, "matched", matched, "scopeComplete", complete} : snapshot,
//$$                     traceStatus, exclusionReason);
//$$         } catch (RuntimeException | LinkageError | AssertionError ignored) { }
//$$     }
//$$     private void normal(String prefix, String operationId, String reasonKey, String reason, Object[] snapshot,
//$$                         String traceStatus, String exclusionReason) {
//$$         // Reuse captured values/context only; OFF failure reporting performs no clock, world, progress or command getters.
//$$         Object[] context = originalContext == null || originalContext.length == 0
//$$                 ? new Object[]{"commandContextAvailable", false, "commandContextError", "not_captured"} : originalContext;
//$$         Object[] required = new Object[snapshot.length + context.length];
//$$         System.arraycopy(snapshot, 0, required, 0, snapshot.length);
//$$         System.arraycopy(context, 0, required, snapshot.length, context.length);
//$$         var line = DiagnosticBoundedEventFormatter.format(prefix,
//$$                 new Object[]{"operationId", operationId, reasonKey, reason, "detailedTrace", traceStatus,
//$$                         "traceExclusionReason", exclusionReason}, required, new Object[0], 2048 - "ALTO CLEF: WARNING: ".length());
//$$         Debug.logWarning(line.text());
//$$     }
//$$ }

//#endif
