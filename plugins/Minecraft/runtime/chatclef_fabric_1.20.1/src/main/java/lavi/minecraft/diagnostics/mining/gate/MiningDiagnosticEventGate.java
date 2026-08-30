package lavi.minecraft.diagnostics.mining.gate;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.mining.budget.MiningDiagnosticSessionBudget;
import lavi.minecraft.diagnostics.mining.budget.MiningDiagnosticAdmission;

import java.util.concurrent.TimeUnit;

//20260806_kpopmodder: Bound mining/path diagnostic events without changing task, pathing, retry, or timeout behavior.
public final class MiningDiagnosticEventGate {
    public static final long SUMMARY_INTERVAL_TICKS = 200;
    public static final long SUMMARY_INTERVAL_NANOS = TimeUnit.SECONDS.toNanos(10);

    private static final MiningDiagnosticEventGate RUNTIME =
            new MiningDiagnosticEventGate(
                    new MiningDiagnosticSessionBudget(),
                    new MiningDiagnosticGateStateStore()
            );

    private final MiningDiagnosticSessionBudget budget;
    private final MiningDiagnosticGateStateStore stateStore;

    public MiningDiagnosticEventGate(MiningDiagnosticSessionBudget budget) {
        this(budget, new MiningDiagnosticGateStateStore());
    }

    public MiningDiagnosticEventGate(MiningDiagnosticSessionBudget budget,
                                     MiningDiagnosticGateStateStore stateStore) {
        this.budget = budget;
        this.stateStore = stateStore;
    }

    public static MiningDiagnosticGateDecision evaluate(String bucket,
                                                        String fingerprint,
                                                        String correlationKey,
                                                        boolean critical) {
        return RUNTIME.evaluateAt(
                bucket,
                fingerprint,
                correlationKey,
                critical,
                ChatClefDiagnostics.currentClientTickId(),
                System.nanoTime()
        );
    }

    public synchronized MiningDiagnosticGateDecision evaluateAt(String bucket,
                                                                String fingerprint,
                                                                String correlationKey,
                                                                boolean critical,
                                                                long tick,
                                                                long monotonicNanos) {
        if (stateStore.resetIfTickRegressed(tick)) {
            budget.reset();
        }
        String normalizedBucket = normalize(bucket);
        String normalizedFingerprint = normalize(fingerprint);
        String normalizedCorrelation = normalize(correlationKey);
        String stateKey = normalizedCorrelation + '\u001f' + normalizedBucket;
        boolean retainNewState = critical
                ? budget.canAdmitCritical()
                : budget.canAdmitDetail(normalizedCorrelation);
        MiningDiagnosticGateState state = stateStore.stateFor(
                stateKey, tick, monotonicNanos, retainNewState);
        boolean stateChanged = !normalizedFingerprint.equals(state.fingerprint);

        if (stateChanged) {
            int suppressed = state.suppressedCount;
            long firstObserved = state.firstObservedTick;
            state.fingerprint = normalizedFingerprint;
            state.suppressedCount = 0;
            state.firstObservedTick = tick;
            state.lastObservedTick = tick;
            state.lastSummaryTick = tick;
            state.lastSummaryNanos = monotonicNanos;
            return admit(state, critical, false, suppressed, firstObserved, tick, normalizedCorrelation);
        }

        state.incrementSuppressed();
        state.lastObservedTick = tick;
        if (critical) {
            return MiningDiagnosticGateDecision.suppressed(state, null);
        }
        boolean tickSummaryDue = tick - state.lastSummaryTick >= SUMMARY_INTERVAL_TICKS;
        boolean timeSummaryDue = monotonicNanos - state.lastSummaryNanos >= SUMMARY_INTERVAL_NANOS;
        if (!tickSummaryDue && !timeSummaryDue) {
            return MiningDiagnosticGateDecision.suppressed(state, null);
        }

        int suppressed = state.suppressedCount;
        long firstObserved = state.firstObservedTick;
        state.suppressedCount = 0;
        state.firstObservedTick = tick;
        state.lastSummaryTick = tick;
        state.lastSummaryNanos = monotonicNanos;
        return admit(state, false, true, suppressed, firstObserved, tick, normalizedCorrelation);
    }

    public static void resetRuntimeSession() {
        RUNTIME.resetSession();
    }

    public synchronized void resetSession() {
        stateStore.reset();
        budget.reset();
    }

    private MiningDiagnosticGateDecision admit(MiningDiagnosticGateState state,
                                               boolean critical,
                                               boolean summary,
                                               int suppressed,
                                               long firstObserved,
                                               long lastObserved,
                                               String correlationKey) {
        MiningDiagnosticAdmission admission = critical
                ? budget.admitCritical(correlationKey)
                : budget.admitDetail(correlationKey);
        if (admission.admitted()) {
            return MiningDiagnosticGateDecision.emitted(
                    summary, suppressed, firstObserved, lastObserved, admission);
        }

        state.suppressedCount = summary ? Math.max(1, suppressed) : 1;
        state.firstObservedTick = summary ? firstObserved : lastObserved;
        boolean reportCorrelationCap = false;
        MiningDiagnosticAdmission summaryAdmission = admission;
        if ("CORRELATION_DETAIL_CAP".equals(admission.reason())
                && stateStore.claimCorrelationCapReport(correlationKey)) {
            MiningDiagnosticAdmission administrative = budget.admitAdministrativeDetail(correlationKey);
            if (administrative.admitted()) {
                reportCorrelationCap = true;
                summaryAdmission = administrative;
            } else if (administrative.reportSessionCap()) {
                summaryAdmission = administrative;
            }
        }
        return MiningDiagnosticGateDecision.suppressed(
                state,
                summaryAdmission,
                reportCorrelationCap,
                admission.reportSessionCap() || summaryAdmission.reportSessionCap()
        );
    }

    private static String normalize(String value) {
        if (value == null || value.isEmpty()) {
            return "none";
        }
        return value.length() <= 360 ? value : value.substring(0, 360) + "...";
    }

}
