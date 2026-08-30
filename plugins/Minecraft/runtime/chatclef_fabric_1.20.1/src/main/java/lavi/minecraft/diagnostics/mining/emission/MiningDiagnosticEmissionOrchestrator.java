package lavi.minecraft.diagnostics.mining.emission;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.mining.budget.MiningDiagnosticCapEventEmitter;
import lavi.minecraft.diagnostics.mining.budget.MiningDiagnosticSuppressionEventEmitter;
import lavi.minecraft.diagnostics.mining.correlation.MiningDiagnosticCorrelation;
import lavi.minecraft.diagnostics.mining.formatting.MiningDiagnosticFieldArrays;
import lavi.minecraft.diagnostics.mining.gate.MiningDiagnosticGateDecision;
import lavi.minecraft.diagnostics.mining.gate.MiningDiagnosticEventGate;

import java.util.function.Supplier;

//20260830_kpopmodder: Coordinate one bounded mining event after correlation and gate collaborators decide.
public final class MiningDiagnosticEmissionOrchestrator {
    private MiningDiagnosticEmissionOrchestrator() {
    }

    public static void emitLazy(String eventName,
                                String reason,
                                Task task,
                                String bucket,
                                String fingerprint,
                                boolean terminal,
                                String fallbackCorrelationKey,
                                String fallbackCorrelationSource,
                                Supplier<Object[]> eventFieldsSupplier) {
        MiningDiagnosticCorrelation correlation = MiningDiagnosticCorrelation.capture(
                task, fallbackCorrelationKey, fallbackCorrelationSource);
        MiningDiagnosticGateDecision decision = MiningDiagnosticEventGate.evaluate(
                bucket,
                fingerprint,
                correlation.key(),
                terminal
        );
        if (!decision.emit) {
            if (decision.reportSessionCap && decision.admission != null) {
                MiningDiagnosticCapEventEmitter.emit(eventName, reason, bucket, fingerprint,
                        decision.suppressedCount, decision.firstObservedTick, decision.lastObservedTick,
                        decision.admission, correlation.commandContextFields());
            } else if (decision.reportCorrelationCap && decision.admission != null) {
                MiningDiagnosticSuppressionEventEmitter.emit(eventName, reason, bucket, fingerprint,
                        decision.suppressedCount, decision.firstObservedTick, decision.lastObservedTick,
                        decision.admission, correlation.commandContextFields());
            }
            return;
        }

        Object[] eventFields = eventFieldsSupplier == null ? new Object[0] : eventFieldsSupplier.get();
        Object[] contractFields = new Object[]{
                "mode", "BOUNDARY",
                "dedupe_key", fingerprint,
                "max_emission", "correlation_detail=256,session_total=5000,reserved_critical=32,summary_ticks=200,summary_seconds=10",
                "terminal", terminal,
                "behavior_effect", "none",
                "summary", decision.summary,
                "suppressedCount", decision.suppressedCount,
                "firstObservedTick", decision.firstObservedTick,
                "lastObservedTick", decision.lastObservedTick,
                "diagnosticCorrelationKey", correlation.key(),
                "diagnosticCorrelationSource", correlation.source()
        };
        ChatClefDiagnostics.logBoundary(eventName, reason, task,
                MiningDiagnosticFieldArrays.merge(
                        contractFields,
                        eventFields,
                        correlation.commandContextFields()
                ));
    }
}
