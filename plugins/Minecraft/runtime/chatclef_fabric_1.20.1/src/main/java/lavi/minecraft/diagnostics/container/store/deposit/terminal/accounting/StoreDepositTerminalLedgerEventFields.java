package lavi.minecraft.diagnostics.container.store.deposit.terminal.accounting;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;

//20260831_kpopmodder: Project cumulative Store terminal accounting without owning admission or emission.
public final class StoreDepositTerminalLedgerEventFields {
    private StoreDepositTerminalLedgerEventFields() {
    }

    public static Object[] snapshotFields(StoreDepositTerminalLedgerSnapshot snapshot,
                                          String snapshotReason) {
        return new Object[]{
                "diagnosticSessionId", ChatClefDiagnostics.diagnosticSessionSnapshot().diagnosticSessionId(),
                "snapshotReason", snapshotReason,
                "snapshotKind", snapshotReason,
                "snapshotSequence", sequenceOrNone(snapshot.snapshotSequence()),
                "snapshotSequenceAvailable", snapshot.snapshotSequenceAvailable(),
                "previousCreatedSnapshotSequence",
                sequenceOrNone(snapshot.previousCreatedSnapshotSequence()),
                "previousEmissionCallsReturnedSnapshotSequence",
                sequenceOrNone(snapshot.previousEmissionCallsReturnedSnapshotSequence()),
                "coveredThroughTerminalSequence", snapshot.coveredThroughTerminalSequence(),
                "accountingMode", snapshot.accountingMode(),
                "snapshotSelfAccounting", snapshot.snapshotSelfAccounting(),
                "totalTerminalObserved", snapshot.totalTerminalObserved(),
                "terminalSequence", snapshot.terminalSequence(),
                "sequenceAvailable", snapshot.sequenceAvailable(),
                "sequenceUnavailableReason", snapshot.sequenceUnavailableReason(),
                "fullTerminalGroupAdmissionPending", snapshot.fullTerminalGroupAdmissionPending(),
                "fullTerminalGroupAdmissionGranted", snapshot.fullTerminalGroupAdmissionGranted(),
                "fullTerminalGroupSuppressedBeforeAdmission", snapshot.fullTerminalGroupSuppressedBeforeAdmission(),
                "fullTerminalGroupEmissionPending", snapshot.fullTerminalGroupEmissionPending(),
                "fullTerminalGroupEmissionCompleted", snapshot.fullTerminalGroupEmissionCompleted(),
                "fullTerminalGroupEmissionFailedAfterAdmission", snapshot.fullTerminalGroupEmissionFailedAfterAdmission(),
                "duplicateFinalizationAttempts", snapshot.duplicateFinalizationAttempts(),
                "classificationCounts", snapshot.classificationCounts(),
                "scopeCounts", snapshot.scopeCounts(),
                "terminalContextAvailableCount", snapshot.terminalContextAvailableCount(),
                "terminalContextUnavailableCount", snapshot.terminalContextUnavailableCount(),
                "terminalBoundaryWithoutContextCount", snapshot.terminalBoundaryWithoutContextCount(),
                "partialModeDisabledCoverageGapCount", snapshot.partialModeDisabledCoverageGapCount(),
                "modeDisabledInvalidatedContextCount", snapshot.modeDisabledInvalidatedContextCount(),
                "ledgerCoverageGapCount", snapshot.ledgerCoverageGapCount(),
                "ledgerRevision", snapshot.ledgerRevision(),
                "counterSaturated", snapshot.counterSaturated(),
                "saturatedCounterFlags", snapshot.saturatedCounterFlags(),
                "firstSuppressedOperationId", snapshot.firstSuppressedOperationId(),
                "firstSuppressedTick", snapshot.firstSuppressedTick(),
                "lastSuppressedOperationId", snapshot.lastSuppressedOperationId(),
                "lastSuppressedTick", snapshot.lastSuppressedTick(),
                "activeAccountingTokenCount", snapshot.activeAccountingTokenCount(),
                "activeAccountingTokenLimit", snapshot.activeAccountingTokenLimit(),
                "activeTokenRegistryOverflowCount", snapshot.activeTokenRegistryOverflowCount(),
                "recentAbnormalSampleCount", snapshot.recentAbnormalSamples().size(),
                "omittedAbnormalSampleCount", snapshot.omittedAbnormalSampleCount(),
                "reconciliationStatus", snapshot.reconciliationStatus(),
                "accountingEquationsHold", snapshot.accountingEquationsHold(),
                "behavior_effect", "none"
        };
    }

    private static Object sequenceOrNone(long sequence) {
        return sequence <= 0L ? "NONE" : sequence;
    }
}
