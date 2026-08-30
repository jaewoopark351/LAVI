package lavi.minecraft.diagnostics.mining.baritone.calculation;

import baritone.Baritone;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;

import java.util.function.Supplier;

//20260730_kpopmodder: Added diagnostic logging to prove the Carry On interaction failure boundary.
record BaritoneCalculationTimeoutSnapshot(String slowPath,
                                          long slowPathTimeoutMs,
                                          long effectivePrimaryTimeoutMs,
                                          long effectiveFailureTimeoutMs) {
    static BaritoneCalculationTimeoutSnapshot capture(long primaryTimeoutMs, long failureTimeoutMs) {
        String slowPath = ChatClefDiagnostics.safeValueForDiagnosticLog(() -> Baritone.settings().slowPath.value);
        long slowPathTimeoutMs = safeLongSetting(() -> Baritone.settings().slowPathTimeoutMS.value);
        boolean slowPathEnabled = "true".equals(slowPath);
        long effectivePrimaryTimeoutMs = slowPathEnabled && slowPathTimeoutMs >= 0
                ? slowPathTimeoutMs
                : primaryTimeoutMs;
        long effectiveFailureTimeoutMs = slowPathEnabled && slowPathTimeoutMs >= 0
                ? slowPathTimeoutMs
                : failureTimeoutMs;
        return new BaritoneCalculationTimeoutSnapshot(
                slowPath,
                slowPathTimeoutMs,
                effectivePrimaryTimeoutMs,
                effectiveFailureTimeoutMs
        );
    }

    private static long safeLongSetting(Supplier<?> supplier) {
        try {
            Object value = supplier.get();
            if (value instanceof Number number) {
                return number.longValue();
            }
            return -1;
        } catch (RuntimeException | LinkageError error) {
            return -1;
        }
    }
}
