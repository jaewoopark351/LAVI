package lavi.minecraft.diagnostics.container.store.deposit.terminal.automatic;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationContext;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.StoreDepositAutomaticContext;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.StoreDepositAutomaticLifecycleLedger.Snapshot;

//20260907_kpopmodder: Own the lifecycle flags and immutable identifiers for one automatic deposit run.
final class StoreDepositAutomaticRunState {
    private final long autoOperationEpoch;
    private final long policyContextEpoch;
    private final String autoOperationId;
    private final String maintenanceGenerationId;
    private final String pressureOwnedRunId;
    private final Task maintenanceTask;
    private final Task userTaskRoot;
    private final StoreDepositAutomaticTerminalCoverage terminalCoverage =
            new StoreDepositAutomaticTerminalCoverage();

    private int nextChildOrdinal = 1;
    private boolean maintenanceLogicalTerminal;
    private boolean pressureOwnedRunClosed;
    private boolean diagnosticCoverageClosed;
    private boolean userTaskResumeObserved;
    private boolean userTaskNaturalCompletionObserved;
    private long activeRunLedgerEvictionCount;
    private long activeRunEvictionQueueOverflowCount;
    private String activeRunLedgerEvictionReason = "NONE";
    private String nextLifecycleState = "UNAVAILABLE";

    StoreDepositAutomaticRunState(long autoOperationEpoch,
                                  long policyContextEpoch,
                                  String autoOperationId,
                                  String maintenanceGenerationId,
                                  String pressureOwnedRunId,
                                  Task maintenanceTask,
                                  Task userTaskRoot) {
        this.autoOperationEpoch = autoOperationEpoch;
        this.policyContextEpoch = policyContextEpoch;
        this.autoOperationId = autoOperationId;
        this.maintenanceGenerationId = maintenanceGenerationId;
        this.pressureOwnedRunId = pressureOwnedRunId;
        this.maintenanceTask = maintenanceTask;
        this.userTaskRoot = userTaskRoot;
    }

    StoreDepositAutomaticContext baseContext() {
        return new StoreDepositAutomaticContext(
                true,
                autoOperationEpoch,
                policyContextEpoch,
                autoOperationId,
                maintenanceGenerationId,
                "UNAVAILABLE",
                -1,
                pressureOwnedRunId
        );
    }

    StoreDepositAutomaticContext registerChild(int childIndex) {
        int ordinal = nextChildOrdinal++;
        return new StoreDepositAutomaticContext(
                true,
                autoOperationEpoch,
                policyContextEpoch,
                autoOperationId,
                maintenanceGenerationId,
                autoOperationId + "-child-" + ordinal,
                childIndex,
                pressureOwnedRunId
        );
    }

    String autoOperationId() {
        return autoOperationId;
    }

    Task maintenanceTask() {
        return maintenanceTask;
    }

    Task userTaskRoot() {
        return userTaskRoot;
    }

    StoreDepositAutomaticTerminalCoverage terminalCoverage() {
        return terminalCoverage;
    }

    boolean pressureOwnedRunClosed() {
        return pressureOwnedRunClosed;
    }

    boolean diagnosticCoverageClosed() {
        return diagnosticCoverageClosed;
    }

    boolean userTaskResumeObserved() {
        return userTaskResumeObserved;
    }

    boolean userTaskNaturalCompletionObserved() {
        return userTaskNaturalCompletionObserved;
    }

    void recordMaintenanceTerminal(String lifecycleState) {
        maintenanceLogicalTerminal = true;
        nextLifecycleState = normalize(lifecycleState);
    }

    void recordPressureOwnedRunClose() {
        pressureOwnedRunClosed = true;
    }

    void recordRunToWait(String lifecycleState) {
        nextLifecycleState = normalize(lifecycleState);
    }

    void recordCoverageClose() {
        diagnosticCoverageClosed = true;
    }

    void recordUserTaskResume() {
        userTaskResumeObserved = true;
    }

    void recordUserTaskNaturalCompletion() {
        userTaskNaturalCompletionObserved = true;
    }

    void recordActiveRunEviction(long queueOverflowCount, String reason) {
        diagnosticCoverageClosed = true;
        terminalCoverage.markIncomplete();
        activeRunLedgerEvictionCount++;
        activeRunEvictionQueueOverflowCount = queueOverflowCount;
        activeRunLedgerEvictionReason = normalize(reason);
    }

    Snapshot snapshot(long activeRunEvictionEmissionSuppressedCount) {
        return new Snapshot(
                true,
                baseContext(),
                terminalCoverage.terminalCounts(),
                terminalCoverage.expectedTerminalCounts(),
                terminalCoverage.expectedTerminalIdentities(),
                terminalCoverage.observedTerminalIdentities(),
                terminalCoverage.expectedTerminalIdentityOverflowCount(),
                terminalCoverage.diagnosticCoverageGaps(),
                terminalCoverage.diagnosticCoverageGapOverflowCount(),
                nextChildOrdinal - 1,
                maintenanceLogicalTerminal,
                pressureOwnedRunClosed,
                diagnosticCoverageClosed,
                userTaskResumeObserved,
                userTaskNaturalCompletionObserved,
                terminalCoverage.terminalIdentityCoverageComplete(),
                terminalCoverage.terminalDedupeEvictionCount(),
                terminalCoverage.terminalScopeOverflowCount(),
                terminalCoverage.terminalEmissionSuppressedCount(),
                terminalCoverage.terminalEmissionSuppressedCounts(),
                terminalCoverage.terminalEmissionSuppressedReasons(),
                activeRunLedgerEvictionCount,
                activeRunEvictionQueueOverflowCount,
                activeRunEvictionEmissionSuppressedCount,
                activeRunLedgerEvictionReason,
                nextLifecycleState,
                StoreDepositOperationContext.identity(maintenanceTask),
                StoreDepositOperationContext.identity(userTaskRoot)
        );
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? "UNAVAILABLE" : value;
    }
}
