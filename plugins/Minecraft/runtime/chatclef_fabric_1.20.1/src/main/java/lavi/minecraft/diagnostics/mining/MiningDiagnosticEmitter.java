package lavi.minecraft.diagnostics.mining;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.mining.emission.MiningDiagnosticEmissionOrchestrator;
import lavi.minecraft.diagnostics.mining.formatting.MiningDiagnosticFieldArrays;
import lavi.minecraft.diagnostics.mining.formatting.MiningDiagnosticTaskFields;
import lavi.minecraft.diagnostics.mining.formatting.MiningSemanticFingerprint;
import lavi.minecraft.diagnostics.mining.gate.MiningDiagnosticEventGate;

import java.util.function.Supplier;

//20260806_kpopmodder: Keep the mining event facade stable while focused collaborators own bounds and correlation.
public final class MiningDiagnosticEmitter {
    private MiningDiagnosticEmitter() {
    }

    public static void emit(String eventName,
                            String reason,
                            Task task,
                            String bucket,
                            String fingerprint,
                            Object[] eventFields) {
        emitLazy(eventName, reason, task, bucket, fingerprint, () -> eventFields);
    }

    public static void emitLazy(String eventName,
                                String reason,
                                Task task,
                                String bucket,
                                String fingerprint,
                                Supplier<Object[]> eventFieldsSupplier) {
        MiningDiagnosticEmissionOrchestrator.emitLazy(
                eventName, reason, task, bucket, fingerprint,
                false, null, null, eventFieldsSupplier);
    }

    public static void emitLazyWithFallback(String eventName,
                                            String reason,
                                            Task task,
                                            String bucket,
                                            String fingerprint,
                                            String fallbackCorrelationKey,
                                            String fallbackCorrelationSource,
                                            Supplier<Object[]> eventFieldsSupplier) {
        MiningDiagnosticEmissionOrchestrator.emitLazy(
                eventName, reason, task, bucket, fingerprint,
                false, fallbackCorrelationKey, fallbackCorrelationSource, eventFieldsSupplier);
    }

    public static void emitReservedLazy(String eventName,
                                        String reason,
                                        Task task,
                                        String bucket,
                                        String fingerprint,
                                        boolean terminal,
                                        Supplier<Object[]> eventFieldsSupplier) {
        MiningDiagnosticEmissionOrchestrator.emitLazy(
                eventName, reason, task, bucket, fingerprint,
                terminal, null, null, eventFieldsSupplier);
    }

    public static void resetSession() {
        MiningDiagnosticEventGate.resetRuntimeSession();
    }

    static String destroyTarget(Task task) {
        return MiningDiagnosticTaskFields.destroyTarget(task);
    }

    static String safeTaskActive(Task task) {
        return MiningDiagnosticTaskFields.safeTaskActive(task);
    }

    static String safeTaskStopped(Task task) {
        return MiningDiagnosticTaskFields.safeTaskStopped(task);
    }

    static String taskClass(Task task) {
        return MiningDiagnosticTaskFields.taskClass(task);
    }

    static String instanceId(Task task) {
        return MiningDiagnosticTaskFields.instanceId(task);
    }

    public static String joinFingerprint(String... values) {
        return MiningSemanticFingerprint.join(values);
    }

    public static Object[] merge(Object[]... arrays) {
        return MiningDiagnosticFieldArrays.merge(arrays);
    }
}
