package lavi.minecraft.diagnostics.container.store.deposit.terminal.accounting;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.EnumSet;

//20260831_kpopmodder: Account for every token-proven Store-root terminal independently of raw log admission.
public final class StoreDepositTerminalLedger {
    public static final int MAX_ABNORMAL_SAMPLES = 8;
    public static final int MAX_ACTIVE_ACCOUNTING_TOKENS =
            StoreDepositTerminalActiveTokenRegistry.MAX_ACTIVE_TOKENS;
    private static final int MAX_OPERATION_ID_LENGTH = 160;
    private static final String NO_OPERATION_ID = "NONE";
    private static final long NO_TICK = -1L;

    private final EnumMap<StoreDepositTerminalClassification, Long> classificationCounts =
            zeroed(StoreDepositTerminalClassification.class);
    private final EnumMap<StoreDepositTerminalScope, Long> scopeCounts =
            zeroed(StoreDepositTerminalScope.class);
    private final Deque<StoreDepositAbnormalTerminalSample> abnormalSamples = new ArrayDeque<>();
    private final StoreDepositTerminalActiveTokenRegistry activeTokens =
            new StoreDepositTerminalActiveTokenRegistry();
    private final StoreDepositTerminalSnapshotProvenance snapshotProvenance;
    private final EnumSet<StoreDepositTerminalSaturationFlag> saturatedCounterFlags =
            EnumSet.noneOf(StoreDepositTerminalSaturationFlag.class);
    private final Object ownerIdentity = new Object();

    private long totalTerminalObserved;
    private long terminalSequence;
    private boolean sequenceAvailable = true;
    private long fullTerminalGroupAdmissionPending;
    private long fullTerminalGroupAdmissionGranted;
    private long fullTerminalGroupSuppressedBeforeAdmission;
    private long fullTerminalGroupEmissionPending;
    private long fullTerminalGroupEmissionCompleted;
    private long fullTerminalGroupEmissionFailedAfterAdmission;
    private long duplicateFinalizationAttempts;
    private long terminalContextAvailableCount;
    private long terminalContextUnavailableCount;
    private long terminalBoundaryWithoutContextCount;
    private long partialModeDisabledCoverageGapCount;
    private long modeDisabledInvalidatedContextCount;
    private long ledgerCoverageGapCount;
    private long ledgerRevision;
    private long omittedAbnormalSampleCount;
    private long activeTokenRegistryOverflowCount;
    private String firstSuppressedOperationId = NO_OPERATION_ID;
    private long firstSuppressedTick = NO_TICK;
    private String lastSuppressedOperationId = NO_OPERATION_ID;
    private long lastSuppressedTick = NO_TICK;

    public StoreDepositTerminalLedger() {
        this(0L, 0L, 0L);
    }

    StoreDepositTerminalLedger(long terminalSequence, long totalTerminalObserved) {
        this(terminalSequence, totalTerminalObserved, 0L);
    }

    StoreDepositTerminalLedger(long terminalSequence,
                               long totalTerminalObserved,
                               long snapshotSequence) {
        this.terminalSequence = Math.max(0, terminalSequence);
        this.totalTerminalObserved = Math.max(0, totalTerminalObserved);
        this.snapshotProvenance = new StoreDepositTerminalSnapshotProvenance(snapshotSequence);
        if (this.terminalSequence == Long.MAX_VALUE) {
            sequenceAvailable = false;
            saturatedCounterFlags.add(StoreDepositTerminalSaturationFlag.TERMINAL_SEQUENCE);
        }
        if (this.totalTerminalObserved == Long.MAX_VALUE) {
            saturatedCounterFlags.add(StoreDepositTerminalSaturationFlag.TOTAL_TERMINAL_OBSERVED);
        }
        if (snapshotSequence == Long.MAX_VALUE) {
            saturatedCounterFlags.add(StoreDepositTerminalSaturationFlag.SNAPSHOT_SEQUENCE);
        }
    }

    public synchronized StoreDepositTerminalAccountingToken recordAuthoritativeTerminal(
            String operationId,
            StoreDepositTerminalClassification classification,
            StoreDepositTerminalScope scope,
            boolean contextAvailable,
            long observedTick) {
        return registerAuthoritativeTerminal(
                operationId,
                classification,
                scope,
                contextAvailable,
                observedTick
        ).token();
    }

    public synchronized StoreDepositTerminalRegistrationResult registerAuthoritativeTerminal(
            String operationId,
            StoreDepositTerminalClassification classification,
            StoreDepositTerminalScope scope,
            boolean contextAvailable,
            long observedTick) {
        if (!sequenceAvailable) {
            recordCoverageGapMutation();
            return StoreDepositTerminalRegistrationResult.rejected(
                    StoreDepositTerminalRegistrationResult.Status.SEQUENCE_UNAVAILABLE
            );
        }
        if (activeTokens.full()) {
            activeTokenRegistryOverflowCount = increment(
                    activeTokenRegistryOverflowCount,
                    StoreDepositTerminalSaturationFlag.ACTIVE_TOKEN_REGISTRY_OVERFLOW
            );
            recordCoverageGapMutation();
            return StoreDepositTerminalRegistrationResult.rejected(
                    StoreDepositTerminalRegistrationResult.Status.ACTIVE_TOKEN_REGISTRY_FULL
            );
        }
        long nextSequence = increment(
                terminalSequence,
                StoreDepositTerminalSaturationFlag.TERMINAL_SEQUENCE
        );
        if (nextSequence == terminalSequence && terminalSequence == Long.MAX_VALUE) {
            sequenceAvailable = false;
            recordCoverageGapMutation();
            return StoreDepositTerminalRegistrationResult.rejected(
                    StoreDepositTerminalRegistrationResult.Status.SEQUENCE_UNAVAILABLE
            );
        }
        terminalSequence = nextSequence;
        if (terminalSequence == Long.MAX_VALUE) {
            sequenceAvailable = false;
            saturatedCounterFlags.add(StoreDepositTerminalSaturationFlag.TERMINAL_SEQUENCE);
        }

        StoreDepositTerminalClassification resolvedClassification = classification == null
                ? StoreDepositTerminalClassification.UNAVAILABLE
                : classification;
        StoreDepositTerminalScope resolvedScope = scope == null
                ? StoreDepositTerminalScope.UNAVAILABLE
                : scope;
        totalTerminalObserved = increment(
                totalTerminalObserved,
                StoreDepositTerminalSaturationFlag.TOTAL_TERMINAL_OBSERVED
        );
        fullTerminalGroupAdmissionPending = increment(
                fullTerminalGroupAdmissionPending,
                StoreDepositTerminalSaturationFlag.ADMISSION_PENDING
        );
        increment(
                classificationCounts,
                resolvedClassification,
                StoreDepositTerminalSaturationFlag.CLASSIFICATION_COUNT
        );
        increment(
                scopeCounts,
                resolvedScope,
                StoreDepositTerminalSaturationFlag.SCOPE_COUNT
        );
        if (contextAvailable) {
            terminalContextAvailableCount = increment(
                    terminalContextAvailableCount,
                    StoreDepositTerminalSaturationFlag.TERMINAL_CONTEXT_AVAILABLE
            );
        } else {
            terminalContextUnavailableCount = increment(
                    terminalContextUnavailableCount,
                    StoreDepositTerminalSaturationFlag.TERMINAL_CONTEXT_UNAVAILABLE
            );
        }
        if (resolvedClassification.abnormal()) {
            retainAbnormalSample(new StoreDepositAbnormalTerminalSample(
                    terminalSequence,
                    bounded(operationId),
                    resolvedClassification,
                    resolvedScope,
                    observedTick
            ));
        }
        ledgerRevision = increment(
                ledgerRevision,
                StoreDepositTerminalSaturationFlag.LEDGER_REVISION
        );
        StoreDepositTerminalAccountingToken token =
                new StoreDepositTerminalAccountingToken(ownerIdentity, terminalSequence);
        activeTokens.register(token, bounded(operationId), normalizeTick(observedTick));
        return StoreDepositTerminalRegistrationResult.accepted(token);
    }

    public synchronized boolean recordAdmissionGranted(StoreDepositTerminalAccountingToken token) {
        if (!ownsActive(token) || !token.transition(
                StoreDepositTerminalAccountingStage.ADMISSION_PENDING,
                StoreDepositTerminalAccountingStage.EMISSION_PENDING)) {
            return false;
        }
        fullTerminalGroupAdmissionPending = decrement(fullTerminalGroupAdmissionPending);
        fullTerminalGroupAdmissionGranted = increment(
                fullTerminalGroupAdmissionGranted,
                StoreDepositTerminalSaturationFlag.ADMISSION_GRANTED
        );
        fullTerminalGroupEmissionPending = increment(
                fullTerminalGroupEmissionPending,
                StoreDepositTerminalSaturationFlag.EMISSION_PENDING
        );
        ledgerRevision = increment(
                ledgerRevision,
                StoreDepositTerminalSaturationFlag.LEDGER_REVISION
        );
        return true;
    }

    public synchronized boolean recordSuppressedBeforeAdmission(StoreDepositTerminalAccountingToken token) {
        StoreDepositTerminalActiveTokenRegistry.Metadata activeToken =
                activeTokens.metadata(token);
        if (!ownsActive(token) || !token.transition(
                StoreDepositTerminalAccountingStage.ADMISSION_PENDING,
                StoreDepositTerminalAccountingStage.SUPPRESSED_BEFORE_ADMISSION)) {
            return false;
        }
        activeTokens.remove(token);
        fullTerminalGroupAdmissionPending = decrement(fullTerminalGroupAdmissionPending);
        if (fullTerminalGroupSuppressedBeforeAdmission == 0L && activeToken != null) {
            firstSuppressedOperationId = activeToken.operationId();
            firstSuppressedTick = activeToken.observedTick();
        }
        if (activeToken != null) {
            lastSuppressedOperationId = activeToken.operationId();
            lastSuppressedTick = activeToken.observedTick();
        }
        fullTerminalGroupSuppressedBeforeAdmission = increment(
                fullTerminalGroupSuppressedBeforeAdmission,
                StoreDepositTerminalSaturationFlag.SUPPRESSED_BEFORE_ADMISSION
        );
        ledgerRevision = increment(
                ledgerRevision,
                StoreDepositTerminalSaturationFlag.LEDGER_REVISION
        );
        return true;
    }

    public synchronized boolean recordEmissionCompleted(StoreDepositTerminalAccountingToken token) {
        if (!ownsActive(token) || !token.transition(
                StoreDepositTerminalAccountingStage.EMISSION_PENDING,
                StoreDepositTerminalAccountingStage.EMISSION_COMPLETED)) {
            return false;
        }
        activeTokens.remove(token);
        fullTerminalGroupEmissionPending = decrement(fullTerminalGroupEmissionPending);
        fullTerminalGroupEmissionCompleted = increment(
                fullTerminalGroupEmissionCompleted,
                StoreDepositTerminalSaturationFlag.EMISSION_COMPLETED
        );
        ledgerRevision = increment(
                ledgerRevision,
                StoreDepositTerminalSaturationFlag.LEDGER_REVISION
        );
        return true;
    }

    public synchronized boolean recordEmissionFailedAfterAdmission(StoreDepositTerminalAccountingToken token) {
        if (!ownsActive(token) || !token.transition(
                StoreDepositTerminalAccountingStage.EMISSION_PENDING,
                StoreDepositTerminalAccountingStage.EMISSION_FAILED_AFTER_ADMISSION)) {
            return false;
        }
        activeTokens.remove(token);
        fullTerminalGroupEmissionPending = decrement(fullTerminalGroupEmissionPending);
        fullTerminalGroupEmissionFailedAfterAdmission = increment(
                fullTerminalGroupEmissionFailedAfterAdmission,
                StoreDepositTerminalSaturationFlag.EMISSION_FAILED_AFTER_ADMISSION
        );
        ledgerCoverageGapCount = increment(
                ledgerCoverageGapCount,
                StoreDepositTerminalSaturationFlag.LEDGER_COVERAGE_GAP
        );
        ledgerRevision = increment(
                ledgerRevision,
                StoreDepositTerminalSaturationFlag.LEDGER_REVISION
        );
        return true;
    }

    public synchronized void recordDuplicateFinalizationAttempt() {
        duplicateFinalizationAttempts = increment(
                duplicateFinalizationAttempts,
                StoreDepositTerminalSaturationFlag.DUPLICATE_FINALIZATION_ATTEMPTS
        );
        ledgerRevision = increment(
                ledgerRevision,
                StoreDepositTerminalSaturationFlag.LEDGER_REVISION
        );
    }

    private boolean ownsActive(StoreDepositTerminalAccountingToken token) {
        return token != null
                && token.ownedBy(ownerIdentity)
                && activeTokens.contains(token);
    }

    public synchronized void recordTerminalBoundaryWithoutStableContext() {
        terminalBoundaryWithoutContextCount = increment(
                terminalBoundaryWithoutContextCount,
                StoreDepositTerminalSaturationFlag.TERMINAL_BOUNDARY_WITHOUT_CONTEXT
        );
        recordCoverageGapMutation();
    }

    public synchronized void recordDiagnosticCoverageGap() {
        recordCoverageGapMutation();
    }

    public synchronized void recordPartialModeDisabledCoverageGap(long invalidatedContexts) {
        partialModeDisabledCoverageGapCount = increment(
                partialModeDisabledCoverageGapCount,
                StoreDepositTerminalSaturationFlag.PARTIAL_MODE_DISABLED_COVERAGE_GAP
        );
        modeDisabledInvalidatedContextCount = add(
                modeDisabledInvalidatedContextCount,
                Math.max(0L, invalidatedContexts),
                StoreDepositTerminalSaturationFlag.MODE_DISABLED_INVALIDATED_CONTEXT
        );
        recordCoverageGapMutation();
    }

    public synchronized StoreDepositTerminalLedgerSnapshot snapshot() {
        return snapshotUnsafe(snapshotProvenance.observation(terminalSequence));
    }

    public synchronized StoreDepositTerminalLedgerSnapshot captureSnapshot() {
        StoreDepositTerminalSnapshotProvenance.Capture capture =
                snapshotProvenance.capture(terminalSequence);
        if (!capture.created()) {
            recordCoverageGapMutation();
            return snapshotUnsafe(capture.view());
        }
        if (capture.saturatedNow()) {
            saturatedCounterFlags.add(StoreDepositTerminalSaturationFlag.SNAPSHOT_SEQUENCE);
        }
        ledgerRevision = increment(
                ledgerRevision,
                StoreDepositTerminalSaturationFlag.LEDGER_REVISION
        );
        return snapshotUnsafe(capture.view());
    }

    public synchronized boolean recordSnapshotEmissionCallsReturned(long emittedSnapshotSequence) {
        if (!snapshotProvenance.recordEmissionCallsReturned(emittedSnapshotSequence)) {
            return false;
        }
        ledgerRevision = increment(
                ledgerRevision,
                StoreDepositTerminalSaturationFlag.LEDGER_REVISION
        );
        return true;
    }

    private StoreDepositTerminalLedgerSnapshot snapshotUnsafe(
            StoreDepositTerminalSnapshotProvenance.View provenance) {
        StoreDepositTerminalReconciler.Result reconciliation =
                StoreDepositTerminalReconciler.reconcile(
                        totalTerminalObserved,
                        fullTerminalGroupAdmissionPending,
                        fullTerminalGroupAdmissionGranted,
                        fullTerminalGroupSuppressedBeforeAdmission,
                        fullTerminalGroupEmissionPending,
                        fullTerminalGroupEmissionCompleted,
                        fullTerminalGroupEmissionFailedAfterAdmission,
                        classificationCounts,
                        scopeCounts,
                        terminalContextAvailableCount,
                        terminalContextUnavailableCount,
                        activeTokens.size()
                );
        EnumSet<StoreDepositTerminalSaturationFlag> snapshotSaturationFlags =
                saturatedCounterFlags.isEmpty()
                        ? EnumSet.noneOf(StoreDepositTerminalSaturationFlag.class)
                        : EnumSet.copyOf(saturatedCounterFlags);
        if (reconciliation.sumSaturated()) {
            snapshotSaturationFlags.add(StoreDepositTerminalSaturationFlag.RECONCILIATION_SUM);
        }
        boolean snapshotCounterSaturated = !snapshotSaturationFlags.isEmpty();
        StoreDepositTerminalReconciliationStatus status;
        if (snapshotCounterSaturated) {
            status = StoreDepositTerminalReconciliationStatus.SATURATED;
        } else if (ledgerCoverageGapCount > 0L || !reconciliation.equationsHold()) {
            status = StoreDepositTerminalReconciliationStatus.COVERAGE_GAP;
        } else {
            status = StoreDepositTerminalReconciliationStatus.EXACT;
        }
        return new StoreDepositTerminalLedgerSnapshot(
                totalTerminalObserved,
                terminalSequence,
                sequenceAvailable,
                fullTerminalGroupAdmissionPending,
                fullTerminalGroupAdmissionGranted,
                fullTerminalGroupSuppressedBeforeAdmission,
                fullTerminalGroupEmissionPending,
                fullTerminalGroupEmissionCompleted,
                fullTerminalGroupEmissionFailedAfterAdmission,
                duplicateFinalizationAttempts,
                classificationCounts,
                scopeCounts,
                terminalContextAvailableCount,
                terminalContextUnavailableCount,
                terminalBoundaryWithoutContextCount,
                partialModeDisabledCoverageGapCount,
                modeDisabledInvalidatedContextCount,
                ledgerCoverageGapCount,
                ledgerRevision,
                snapshotCounterSaturated,
                new ArrayList<>(abnormalSamples),
                omittedAbnormalSampleCount,
                status,
                provenance.snapshotSequence(),
                provenance.snapshotSequenceAvailable(),
                provenance.previousCreatedSnapshotSequence(),
                provenance.previousEmissionCallsReturnedSnapshotSequence(),
                provenance.coveredThroughTerminalSequence(),
                "CUMULATIVE",
                "ADMISSION_INCLUDED_EMISSION_OUTCOME_EXCLUDED",
                snapshotSaturationFlags,
                sequenceAvailable
                        ? StoreDepositSequenceUnavailableReason.NONE
                        : StoreDepositSequenceUnavailableReason.UNAVAILABLE_SATURATED,
                firstSuppressedOperationId,
                firstSuppressedTick,
                lastSuppressedOperationId,
                lastSuppressedTick,
                activeTokens.size(),
                MAX_ACTIVE_ACCOUNTING_TOKENS,
                activeTokenRegistryOverflowCount
        );
    }

    private void retainAbnormalSample(StoreDepositAbnormalTerminalSample sample) {
        if (abnormalSamples.size() == MAX_ABNORMAL_SAMPLES) {
            abnormalSamples.removeFirst();
            omittedAbnormalSampleCount = increment(
                    omittedAbnormalSampleCount,
                    StoreDepositTerminalSaturationFlag.OMITTED_ABNORMAL_SAMPLE
            );
        }
        abnormalSamples.addLast(sample);
    }

    private void recordCoverageGapMutation() {
        ledgerCoverageGapCount = increment(
                ledgerCoverageGapCount,
                StoreDepositTerminalSaturationFlag.LEDGER_COVERAGE_GAP
        );
        ledgerRevision = increment(
                ledgerRevision,
                StoreDepositTerminalSaturationFlag.LEDGER_REVISION
        );
    }

    private long increment(long value, StoreDepositTerminalSaturationFlag flag) {
        if (value == Long.MAX_VALUE) {
            saturatedCounterFlags.add(flag);
            return Long.MAX_VALUE;
        }
        return value + 1;
    }

    private static long decrement(long value) {
        return value <= 0 ? 0 : value - 1;
    }

    private long add(long left,
                     long right,
                     StoreDepositTerminalSaturationFlag flag) {
        if (Long.MAX_VALUE - left < right) {
            saturatedCounterFlags.add(flag);
            return Long.MAX_VALUE;
        }
        return left + right;
    }

    private <K extends Enum<K>> void increment(
            EnumMap<K, Long> counts,
            K key,
            StoreDepositTerminalSaturationFlag flag) {
        counts.put(key, increment(counts.get(key), flag));
    }

    private static <K extends Enum<K>> EnumMap<K, Long> zeroed(Class<K> type) {
        EnumMap<K, Long> values = new EnumMap<>(type);
        for (K value : type.getEnumConstants()) {
            values.put(value, 0L);
        }
        return values;
    }

    private static String bounded(String value) {
        String normalized = value == null || value.isBlank() ? "UNAVAILABLE" : value;
        return normalized.length() <= MAX_OPERATION_ID_LENGTH
                ? normalized
                : normalized.substring(0, MAX_OPERATION_ID_LENGTH);
    }

    private static long normalizeTick(long value) {
        return value < 0L ? NO_TICK : value;
    }

}
