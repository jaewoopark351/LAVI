package lavi.minecraft.diagnostics.mining.baritone.calculation;

import baritone.pathing.calc.AbstractNodeCostSearch;
import lavi.minecraft.diagnostics.mining.MiningDiagnosticEmitter;
import lavi.minecraft.diagnostics.mining.baritone.BaritoneDiagnosticEmitter;
import lavi.minecraft.diagnostics.mining.baritone.BaritonePathObjectFormatters;

import java.util.function.Supplier;

//20260730_kpopmodder: Added diagnostic logging to prove the Carry On interaction failure boundary.
final class BaritoneCalculationPhaseDiagnostics {
    private BaritoneCalculationPhaseDiagnostics() {
    }

    static void emit(CalculationDiagnosticRecord record,
                     AbstractNodeCostSearch pathfinder,
                     String phase,
                     String trigger,
                     String phaseKey,
                     long elapsedMillis,
                     Object cancelRequested,
                     Supplier<Object[]> phaseFieldsSupplier) {
        String pathfinderIdentity = pathfinder == null
                ? Integer.toHexString(record.pathfinderIdentity)
                : BaritonePathObjectFormatters.identity(pathfinder);
        String fingerprint = MiningDiagnosticEmitter.joinFingerprint(
                "BARITONE_CALCULATION_PHASE_BOUNDARY",
                Long.toString(record.generationId),
                pathfinderIdentity,
                phase,
                phaseKey
        );
        BaritoneDiagnosticEmitter.emitLazy(
                "BARITONE_CALCULATION_PHASE_BOUNDARY",
                "baritone_calculation_phase_boundary",
                "baritone_pathfinder_phase_observer",
                trigger,
                "baritone_calculation|" + record.generationId,
                fingerprint,
                () -> fields(record, pathfinder, phase, pathfinderIdentity, elapsedMillis, cancelRequested,
                        phaseFieldsSupplier)
        );
    }

    private static Object[] fields(CalculationDiagnosticRecord record,
                                   AbstractNodeCostSearch pathfinder,
                                   String phase,
                                   String pathfinderIdentity,
                                   long elapsedMillis,
                                   Object cancelRequested,
                                   Supplier<Object[]> phaseFieldsSupplier) {
        BaritoneCalculationTimeoutSnapshot timeout = BaritoneCalculationTimeoutSnapshot.capture(
                record.primaryTimeoutMs,
                record.failureTimeoutMs
        );
        Object[] phaseFields = phaseFieldsSupplier == null ? null : phaseFieldsSupplier.get();
        return MiningDiagnosticEmitter.merge(new Object[]{
                "calculationGeneration", record.generationId,
                "phase", phase,
                "pathfinderIdentity", pathfinderIdentity,
                "pathfinderType", BaritonePathObjectFormatters.className(pathfinder),
                "pathingBehaviorIdentity", record.pathingBehaviorId,
                "firstSegment", record.firstSegment,
                "pathStart", record.pathStartSummary,
                "pathfinderStart", record.pathfinderStartSummary,
                "requestedGoalType", record.requestedGoalType,
                "requestedGoalSummary", record.requestedGoalSummary,
                "pathfinderGoalType", record.pathfinderGoalType,
                "pathfinderGoalSummary", record.pathfinderGoalSummary,
                "primaryTimeoutMs", record.primaryTimeoutMs,
                "failureTimeoutMs", record.failureTimeoutMs,
                "slowPath", timeout.slowPath(),
                "slowPathTimeoutMs", timeout.slowPathTimeoutMs(),
                "effectivePrimaryTimeoutMs", timeout.effectivePrimaryTimeoutMs(),
                "effectiveFailureTimeoutMs", timeout.effectiveFailureTimeoutMs(),
                "elapsedMillis", elapsedMillis,
                "cancelRequested", cancelRequested,
                "workerThreadName", Thread.currentThread().getName(),
                "workerThreadId", Thread.currentThread().getId()
        }, phaseFields);
    }
}
