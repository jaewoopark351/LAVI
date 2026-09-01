package lavi.minecraft.diagnostics.container.store.deposit.terminal.accounting;

import java.util.Map;

//20260831_kpopmodder: Reconcile immutable terminal counts without mutating ledger state during reads.
final class StoreDepositTerminalReconciler {
    private StoreDepositTerminalReconciler() {
    }

    static Result reconcile(
            long totalTerminalObserved,
            long admissionPending,
            long admissionGranted,
            long suppressedBeforeAdmission,
            long emissionPending,
            long emissionCompleted,
            long emissionFailedAfterAdmission,
            Map<?, Long> classificationCounts,
            Map<?, Long> scopeCounts,
            long contextAvailable,
            long contextUnavailable,
            int activeTokenCount) {
        SumResult admission = sum(admissionPending, admissionGranted, suppressedBeforeAdmission);
        SumResult emission = sum(emissionPending, emissionCompleted, emissionFailedAfterAdmission);
        SumResult classifications = sum(classificationCounts);
        SumResult scopes = sum(scopeCounts);
        SumResult contexts = sum(contextAvailable, contextUnavailable);
        SumResult active = sum(admissionPending, emissionPending);
        boolean sumSaturated = admission.saturated()
                || emission.saturated()
                || classifications.saturated()
                || scopes.saturated()
                || contexts.saturated()
                || active.saturated();
        boolean equationsHold = !sumSaturated
                && totalTerminalObserved == admission.value()
                && admissionGranted == emission.value()
                && totalTerminalObserved == classifications.value()
                && totalTerminalObserved == scopes.value()
                && totalTerminalObserved == contexts.value()
                && activeTokenCount == active.value();
        return new Result(equationsHold, sumSaturated);
    }

    private static SumResult sum(Map<?, Long> values) {
        long[] counters = new long[values.size()];
        int index = 0;
        for (long value : values.values()) {
            counters[index++] = value;
        }
        return sum(counters);
    }

    private static SumResult sum(long... values) {
        long result = 0L;
        for (long value : values) {
            if (value < 0L || Long.MAX_VALUE - result < value) {
                return new SumResult(Long.MAX_VALUE, true);
            }
            result += value;
        }
        return new SumResult(result, false);
    }

    record Result(boolean equationsHold, boolean sumSaturated) {
    }

    private record SumResult(long value, boolean saturated) {
    }
}
