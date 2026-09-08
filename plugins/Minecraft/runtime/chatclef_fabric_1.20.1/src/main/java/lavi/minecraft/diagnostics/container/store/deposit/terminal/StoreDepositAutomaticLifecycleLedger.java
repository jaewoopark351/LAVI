package lavi.minecraft.diagnostics.container.store.deposit.terminal;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.automatic.StoreDepositAutomaticLedgerComponent;

import java.util.List;
import java.util.Map;
import java.util.Set;

//20260830_kpopmodder: Keep automatic-run correlation alive after each per-item store ledger is purged.
//20260907_kpopmodder: Retain one synchronized public facade while focused collaborators own ledger internals.
public final class StoreDepositAutomaticLifecycleLedger {
    private final StoreDepositAutomaticLedgerComponent component =
            new StoreDepositAutomaticLedgerComponent();

    public synchronized StoreDepositAutomaticContext beginRun(Task maintenanceTask,
                                                               Task userTaskRoot,
                                                               long policyContextEpoch) {
        return component.beginRun(maintenanceTask, userTaskRoot, policyContextEpoch);
    }

    public synchronized StoreDepositAutomaticContext registerChild(Task maintenanceTask,
                                                                    Task childTask,
                                                                    int childIndex) {
        return component.registerChild(maintenanceTask, childTask, childIndex);
    }

    public synchronized StoreDepositAutomaticContext contextForChild(Task childTask) {
        return component.contextForChild(childTask);
    }

    public synchronized StoreDepositAutomaticContext contextForMaintenance(Task maintenanceTask) {
        return component.contextForMaintenance(maintenanceTask);
    }

    public synchronized TerminalRecord recordScope(StoreDepositAutomaticContext context,
                                                   String terminalScope,
                                                   String closingIdentity,
                                                   String terminalReason) {
        return component.recordScope(context, terminalScope, closingIdentity, terminalReason);
    }

    public synchronized void expectTerminalScopeIdentity(StoreDepositAutomaticContext context,
                                                         String terminalScope,
                                                         String closingIdentity) {
        component.expectTerminalScopeIdentity(context, terminalScope, closingIdentity);
    }

    public synchronized Snapshot recordDiagnosticCoverageGap(
            StoreDepositAutomaticContext context,
            String gapScope,
            String affectedIdentity,
            String reason) {
        return component.recordDiagnosticCoverageGap(context, gapScope, affectedIdentity, reason);
    }

    public synchronized TerminalRecord recordMaintenanceTerminal(Task maintenanceTask,
                                                                 String terminalReason,
                                                                 String nextLifecycleState) {
        return component.recordMaintenanceTerminal(maintenanceTask, terminalReason, nextLifecycleState);
    }

    public synchronized TerminalRecord recordPressureOwnedRunClose(Task maintenanceTask,
                                                                   String terminalReason) {
        return component.recordPressureOwnedRunClose(maintenanceTask, terminalReason);
    }

    public synchronized TerminalRecord recordRunToWait(Task maintenanceTask,
                                                       String terminalReason,
                                                       String nextLifecycleState) {
        return component.recordRunToWait(maintenanceTask, terminalReason, nextLifecycleState);
    }

    public synchronized TerminalRecord recordCoverageClose(Task maintenanceTask,
                                                           String terminalReason) {
        return component.recordCoverageClose(maintenanceTask, terminalReason);
    }

    public synchronized TerminalRecord recordCoverageCloseIfNoUserTask(Task maintenanceTask,
                                                                       String terminalReason) {
        return component.recordCoverageCloseIfNoUserTask(maintenanceTask, terminalReason);
    }

    public synchronized TerminalRecord recordCoverageCloseForUserTask(Task userTaskRoot,
                                                                      String terminalReason) {
        return component.recordCoverageCloseForUserTask(userTaskRoot, terminalReason);
    }

    public synchronized TerminalRecord observeUserTaskResume(Task task) {
        return component.observeUserTaskResume(task);
    }

    public synchronized TerminalRecord observeUserTaskNaturalCompletion(Task task) {
        return component.observeUserTaskNaturalCompletion(task);
    }

    public synchronized List<TerminalRecord> observeUserTaskNaturalCompletions(Task task) {
        return component.observeUserTaskNaturalCompletions(task);
    }

    public synchronized List<TerminalRecord> recordCoverageClosesForUserTask(
            Task userTaskRoot,
            String terminalReason) {
        return component.recordCoverageClosesForUserTask(userTaskRoot, terminalReason);
    }

    public synchronized Snapshot snapshotFor(Task maintenanceTask) {
        return component.snapshotFor(maintenanceTask);
    }

    public synchronized TerminalRecord takePendingEviction() {
        return component.takePendingEviction();
    }

    synchronized void purgeClosedRun(StoreDepositAutomaticContext context) {
        component.purgeClosedRun(context);
    }

    public synchronized Snapshot recordTerminalEmissionSuppressed(TerminalRecord record) {
        return component.recordTerminalEmissionSuppressed(record);
    }

    public synchronized ClearResult clearForModeTransition() {
        return component.clearForModeTransition();
    }

    public synchronized int activeRunCount() {
        return component.activeRunCount();
    }

    public record Snapshot(boolean available,
                           StoreDepositAutomaticContext context,
                           Map<String, Integer> terminalCounts,
                           Map<String, Integer> expectedTerminalCounts,
                           Set<String> expectedTerminalIdentities,
                           Set<String> observedTerminalIdentities,
                           long expectedTerminalIdentityOverflowCount,
                           Map<String, String> diagnosticCoverageGaps,
                           long diagnosticCoverageGapOverflowCount,
                           int registeredChildCount,
                           boolean maintenanceLogicalTerminal,
                           boolean pressureOwnedRunClosed,
                           boolean diagnosticCoverageClosed,
                           boolean userTaskResumeObserved,
                           boolean userTaskNaturalCompletionObserved,
                           boolean terminalIdentityCoverageComplete,
                           long terminalDedupeEvictionCount,
                           long terminalScopeOverflowCount,
                           long terminalEmissionSuppressedCount,
                           Map<String, Integer> terminalEmissionSuppressedCounts,
                           Map<String, String> terminalEmissionSuppressedReasons,
                           long activeRunLedgerEvictionCount,
                           long activeRunEvictionQueueOverflowCount,
                           long activeRunEvictionEmissionSuppressedCount,
                           String activeRunLedgerEvictionReason,
                           String nextLifecycleState,
                           String maintenanceIdentity,
                           String userTaskRootIdentity) {
        public static Snapshot unavailable() {
            return new Snapshot(
                    false,
                    StoreDepositAutomaticContext.unavailable(),
                    Map.of(),
                    Map.of(),
                    Set.of(),
                    Set.of(),
                    0L,
                    Map.of(),
                    0L,
                    0,
                    false,
                    false,
                    false,
                    false,
                    false,
                    true,
                    0L,
                    0L,
                    0L,
                    Map.of(),
                    Map.of(),
                    0L,
                    0L,
                    0L,
                    "NONE",
                    "UNAVAILABLE",
                    "UNAVAILABLE",
                    "UNAVAILABLE"
            );
        }

        public int terminalCount(String scope) {
            return terminalCounts.getOrDefault(scope, 0);
        }

        public int expectedTerminalCount(String scope) {
            return expectedTerminalCounts.getOrDefault(scope, 0);
        }

        @Override
        public boolean terminalIdentityCoverageComplete() {
            return StoreDepositAutomaticLedgerComponent.terminalIdentityCoverageComplete(
                    this,
                    this.terminalIdentityCoverageComplete
            );
        }

        public boolean lifecycleCoverageComplete() {
            return "NONE".equals(missingLifecycleBoundaries());
        }

        public String missingLifecycleBoundaries() {
            return StoreDepositAutomaticLedgerComponent.missingLifecycleBoundaries(this);
        }
    }

    public record TerminalRecord(boolean available,
                                 boolean duplicate,
                                 StoreDepositAutomaticContext context,
                                 String terminalScope,
                                 String terminalReason,
                                 String closingIdentity,
                                 Snapshot snapshot) {
        public static TerminalRecord unavailable() {
            return new TerminalRecord(
                    false,
                    false,
                    StoreDepositAutomaticContext.unavailable(),
                    "UNAVAILABLE",
                    "UNAVAILABLE",
                    "UNAVAILABLE",
                    Snapshot.unavailable()
            );
        }
    }

    public record ClearResult(int activeRunCount,
                              int pendingEvictionCount,
                              int maintenanceBindingCount,
                              int childBindingCount,
                              int userRootBindingCount) {
        public int totalEntryCount() {
            return activeRunCount
                    + pendingEvictionCount
                    + maintenanceBindingCount
                    + childBindingCount
                    + userRootBindingCount;
        }
    }
}
