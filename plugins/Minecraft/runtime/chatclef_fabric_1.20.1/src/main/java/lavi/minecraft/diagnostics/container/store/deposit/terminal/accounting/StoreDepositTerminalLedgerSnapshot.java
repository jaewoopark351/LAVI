package lavi.minecraft.diagnostics.container.store.deposit.terminal.accounting;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

//20260831_kpopmodder: Publish one immutable cumulative Store terminal accounting view.
public record StoreDepositTerminalLedgerSnapshot(
        long totalTerminalObserved,
        long terminalSequence,
        boolean sequenceAvailable,
        long fullTerminalGroupAdmissionPending,
        long fullTerminalGroupAdmissionGranted,
        long fullTerminalGroupSuppressedBeforeAdmission,
        long fullTerminalGroupEmissionPending,
        long fullTerminalGroupEmissionCompleted,
        long fullTerminalGroupEmissionFailedAfterAdmission,
        long duplicateFinalizationAttempts,
        Map<StoreDepositTerminalClassification, Long> classificationCounts,
        Map<StoreDepositTerminalScope, Long> scopeCounts,
        long terminalContextAvailableCount,
        long terminalContextUnavailableCount,
        long terminalBoundaryWithoutContextCount,
        long partialModeDisabledCoverageGapCount,
        long modeDisabledInvalidatedContextCount,
        long ledgerCoverageGapCount,
        long ledgerRevision,
        boolean counterSaturated,
        List<StoreDepositAbnormalTerminalSample> recentAbnormalSamples,
        long omittedAbnormalSampleCount,
        StoreDepositTerminalReconciliationStatus reconciliationStatus,
        long snapshotSequence,
        boolean snapshotSequenceAvailable,
        long previousCreatedSnapshotSequence,
        long previousEmissionCallsReturnedSnapshotSequence,
        long coveredThroughTerminalSequence,
        String accountingMode,
        String snapshotSelfAccounting,
        Set<StoreDepositTerminalSaturationFlag> saturatedCounterFlags,
        StoreDepositSequenceUnavailableReason sequenceUnavailableReason,
        String firstSuppressedOperationId,
        long firstSuppressedTick,
        String lastSuppressedOperationId,
        long lastSuppressedTick,
        int activeAccountingTokenCount,
        int activeAccountingTokenLimit,
        long activeTokenRegistryOverflowCount) {

    public StoreDepositTerminalLedgerSnapshot {
        EnumMap<StoreDepositTerminalClassification, Long> classifications =
                new EnumMap<>(StoreDepositTerminalClassification.class);
        classifications.putAll(classificationCounts);
        classificationCounts = Collections.unmodifiableMap(classifications);
        EnumMap<StoreDepositTerminalScope, Long> scopes =
                new EnumMap<>(StoreDepositTerminalScope.class);
        scopes.putAll(scopeCounts);
        scopeCounts = Collections.unmodifiableMap(scopes);
        recentAbnormalSamples = List.copyOf(recentAbnormalSamples);
        EnumSet<StoreDepositTerminalSaturationFlag> saturationFlags =
                EnumSet.noneOf(StoreDepositTerminalSaturationFlag.class);
        saturationFlags.addAll(Objects.requireNonNull(
                saturatedCounterFlags,
                "saturatedCounterFlags"
        ));
        saturatedCounterFlags = Collections.unmodifiableSet(saturationFlags);
        sequenceUnavailableReason = Objects.requireNonNull(
                sequenceUnavailableReason,
                "sequenceUnavailableReason"
        );
        accountingMode = Objects.requireNonNull(accountingMode, "accountingMode");
        snapshotSelfAccounting = Objects.requireNonNull(
                snapshotSelfAccounting,
                "snapshotSelfAccounting"
        );
        firstSuppressedOperationId = Objects.requireNonNull(
                firstSuppressedOperationId,
                "firstSuppressedOperationId"
        );
        lastSuppressedOperationId = Objects.requireNonNull(
                lastSuppressedOperationId,
                "lastSuppressedOperationId"
        );
    }

    public boolean settled() {
        return fullTerminalGroupAdmissionPending == 0
                && fullTerminalGroupEmissionPending == 0;
    }

    public boolean accountingEquationsHold() {
        if (counterSaturated) {
            return false;
        }
        return totalTerminalObserved == sum(
                fullTerminalGroupAdmissionPending,
                fullTerminalGroupAdmissionGranted,
                fullTerminalGroupSuppressedBeforeAdmission)
                && fullTerminalGroupAdmissionGranted == sum(
                fullTerminalGroupEmissionPending,
                fullTerminalGroupEmissionCompleted,
                fullTerminalGroupEmissionFailedAfterAdmission)
                && totalTerminalObserved == sum(classificationCounts)
                && totalTerminalObserved == sum(scopeCounts)
                && totalTerminalObserved == sum(
                terminalContextAvailableCount,
                terminalContextUnavailableCount)
                && activeAccountingTokenCount == sum(
                fullTerminalGroupAdmissionPending,
                fullTerminalGroupEmissionPending);
    }

    private static long sum(Map<?, Long> values) {
        long total = 0;
        for (long value : values.values()) {
            if (Long.MAX_VALUE - total < value) {
                return Long.MAX_VALUE;
            }
            total += value;
        }
        return total;
    }

    private static long sum(long... values) {
        long total = 0;
        for (long value : values) {
            if (Long.MAX_VALUE - total < value) {
                return Long.MAX_VALUE;
            }
            total += value;
        }
        return total;
    }
}
