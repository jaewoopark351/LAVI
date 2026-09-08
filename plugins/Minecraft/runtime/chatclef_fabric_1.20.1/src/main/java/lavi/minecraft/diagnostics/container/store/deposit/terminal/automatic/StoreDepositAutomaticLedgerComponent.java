package lavi.minecraft.diagnostics.container.store.deposit.terminal.automatic;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationContext;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.StoreDepositAutomaticContext;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.StoreDepositAutomaticLifecycleLedger.ClearResult;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.StoreDepositAutomaticLifecycleLedger.Snapshot;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.StoreDepositAutomaticLifecycleLedger.TerminalRecord;

import java.util.ArrayList;
import java.util.List;

//20260907_kpopmodder: Provide the sole facade-to-component port for automatic lifecycle ledger state.
public final class StoreDepositAutomaticLedgerComponent {
    private final StoreDepositAutomaticRunRegistry registry = new StoreDepositAutomaticRunRegistry();

    public StoreDepositAutomaticContext beginRun(Task maintenanceTask,
                                                  Task userTaskRoot,
                                                  long policyContextEpoch) {
        StoreDepositAutomaticRunState existing = registry.stateForMaintenance(maintenanceTask);
        if (existing != null) {
            return existing.baseContext();
        }
        evictOldestIfNeeded();
        return registry.createRun(maintenanceTask, userTaskRoot, policyContextEpoch).baseContext();
    }

    public StoreDepositAutomaticContext registerChild(Task maintenanceTask,
                                                       Task childTask,
                                                       int childIndex) {
        if (maintenanceTask == null || childTask == null) {
            return StoreDepositAutomaticContext.unavailable();
        }
        StoreDepositAutomaticRunState state = registry.stateForMaintenance(maintenanceTask);
        if (state == null) {
            return StoreDepositAutomaticContext.unavailable();
        }
        StoreDepositAutomaticRunRegistry.ChildBinding existing = registry.existingChildBinding(childTask);
        if (existing != null) {
            return existing.context();
        }
        StoreDepositAutomaticContext context = state.registerChild(childIndex);
        registry.bindChild(childTask, state, context);
        expectTerminalScopeIdentity(
                context,
                "PER_ITEM_ROOT",
                StoreDepositOperationContext.identity(childTask)
        );
        return context;
    }

    public StoreDepositAutomaticContext contextForChild(Task childTask) {
        StoreDepositAutomaticRunRegistry.ChildBinding binding = registry.childBinding(childTask);
        return binding == null ? StoreDepositAutomaticContext.unavailable() : binding.context();
    }

    public StoreDepositAutomaticContext contextForMaintenance(Task maintenanceTask) {
        StoreDepositAutomaticRunState state = registry.stateForMaintenance(maintenanceTask);
        return state == null ? StoreDepositAutomaticContext.unavailable() : state.baseContext();
    }

    public TerminalRecord recordScope(StoreDepositAutomaticContext context,
                                      String terminalScope,
                                      String closingIdentity,
                                      String terminalReason) {
        if (context == null || !context.available()) {
            return TerminalRecord.unavailable();
        }
        StoreDepositAutomaticRunState state = registry.runForId(context.autoOperationId());
        return recordScope(state, context, terminalScope, closingIdentity, terminalReason);
    }

    public void expectTerminalScopeIdentity(StoreDepositAutomaticContext context,
                                            String terminalScope,
                                            String closingIdentity) {
        if (context == null || !context.available()) {
            return;
        }
        StoreDepositAutomaticRunState state = registry.runForId(context.autoOperationId());
        if (state == null) {
            return;
        }
        state.terminalCoverage().expect(terminalScope, closingIdentity);
    }

    public Snapshot recordDiagnosticCoverageGap(StoreDepositAutomaticContext context,
                                                String gapScope,
                                                String affectedIdentity,
                                                String reason) {
        if (context == null || !context.available()) {
            return Snapshot.unavailable();
        }
        StoreDepositAutomaticRunState state = registry.runForId(context.autoOperationId());
        if (state == null) {
            return Snapshot.unavailable();
        }
        state.terminalCoverage().recordDiagnosticGap(gapScope, affectedIdentity, reason);
        return snapshot(state);
    }

    public TerminalRecord recordMaintenanceTerminal(Task maintenanceTask,
                                                    String terminalReason,
                                                    String nextLifecycleState) {
        StoreDepositAutomaticRunState state = registry.stateForMaintenance(maintenanceTask);
        if (state == null) {
            return TerminalRecord.unavailable();
        }
        state.recordMaintenanceTerminal(nextLifecycleState);
        return recordScope(
                state,
                state.baseContext(),
                "MAINTENANCE_LOGICAL_TERMINAL",
                StoreDepositOperationContext.identity(maintenanceTask),
                terminalReason
        );
    }

    public TerminalRecord recordPressureOwnedRunClose(Task maintenanceTask,
                                                      String terminalReason) {
        StoreDepositAutomaticRunState state = registry.stateForMaintenance(maintenanceTask);
        if (state == null) {
            return TerminalRecord.unavailable();
        }
        state.recordPressureOwnedRunClose();
        return recordScope(
                state,
                state.baseContext(),
                "PRESSURE_OWNED_RUN",
                StoreDepositOperationContext.identity(maintenanceTask),
                terminalReason
        );
    }

    public TerminalRecord recordRunToWait(Task maintenanceTask,
                                          String terminalReason,
                                          String nextLifecycleState) {
        StoreDepositAutomaticRunState state = registry.stateForMaintenance(maintenanceTask);
        if (state == null) {
            return TerminalRecord.unavailable();
        }
        state.recordRunToWait(nextLifecycleState);
        return recordScope(
                state,
                state.baseContext(),
                "RUNNING_TO_WAIT_FOR_REARM",
                StoreDepositOperationContext.identity(maintenanceTask),
                terminalReason
        );
    }

    public TerminalRecord recordCoverageClose(Task maintenanceTask, String terminalReason) {
        StoreDepositAutomaticRunState state = registry.stateForMaintenance(maintenanceTask);
        if (state == null) {
            return TerminalRecord.unavailable();
        }
        return recordCoverageClose(state, StoreDepositOperationContext.identity(maintenanceTask), terminalReason);
    }

    public TerminalRecord recordCoverageCloseIfNoUserTask(Task maintenanceTask,
                                                          String terminalReason) {
        StoreDepositAutomaticRunState state = registry.stateForMaintenance(maintenanceTask);
        if (state == null || state.userTaskRoot() != null) {
            return TerminalRecord.unavailable();
        }
        return recordCoverageClose(state, StoreDepositOperationContext.identity(maintenanceTask), terminalReason);
    }

    public TerminalRecord recordCoverageCloseForUserTask(Task userTaskRoot,
                                                         String terminalReason) {
        StoreDepositAutomaticRunState state = registry.firstUserRun(
                userTaskRoot,
                candidate -> !candidate.diagnosticCoverageClosed()
        );
        if (state == null) {
            return TerminalRecord.unavailable();
        }
        return recordCoverageClose(state, StoreDepositOperationContext.identity(userTaskRoot), terminalReason);
    }

    public TerminalRecord observeUserTaskResume(Task task) {
        StoreDepositAutomaticRunState state = registry.firstUserRun(
                task,
                candidate -> candidate.pressureOwnedRunClosed() && !candidate.userTaskResumeObserved()
        );
        if (state == null) {
            return TerminalRecord.unavailable();
        }
        state.recordUserTaskResume();
        return recordScope(
                state,
                state.baseContext(),
                "USER_TASK_RESUME",
                StoreDepositOperationContext.identity(task),
                "USER_ROOT_RECONCILIATION_OBSERVED"
        );
    }

    public TerminalRecord observeUserTaskNaturalCompletion(Task task) {
        StoreDepositAutomaticRunState state = registry.firstUserRun(
                task,
                candidate -> !candidate.userTaskNaturalCompletionObserved()
        );
        if (state == null) {
            return TerminalRecord.unavailable();
        }
        state.recordUserTaskNaturalCompletion();
        return recordScope(
                state,
                state.baseContext(),
                "USER_TASK_NATURAL_COMPLETION",
                StoreDepositOperationContext.identity(task),
                "NATURAL_FINISH_OBSERVED"
        );
    }

    public List<TerminalRecord> observeUserTaskNaturalCompletions(Task task) {
        List<TerminalRecord> result = new ArrayList<>();
        for (StoreDepositAutomaticRunState state : registry.userRuns(task)) {
            if (state.userTaskNaturalCompletionObserved()) {
                continue;
            }
            registry.touch(state);
            state.recordUserTaskNaturalCompletion();
            result.add(recordScope(
                    state,
                    state.baseContext(),
                    "USER_TASK_NATURAL_COMPLETION",
                    StoreDepositOperationContext.identity(task),
                    "NATURAL_FINISH_OBSERVED"
            ));
        }
        return List.copyOf(result);
    }

    public List<TerminalRecord> recordCoverageClosesForUserTask(Task userTaskRoot,
                                                               String terminalReason) {
        List<TerminalRecord> result = new ArrayList<>();
        for (StoreDepositAutomaticRunState state : registry.userRuns(userTaskRoot)) {
            if (state.diagnosticCoverageClosed()) {
                continue;
            }
            registry.touch(state);
            result.add(recordCoverageClose(
                    state,
                    StoreDepositOperationContext.identity(userTaskRoot),
                    terminalReason
            ));
        }
        return List.copyOf(result);
    }

    public Snapshot snapshotFor(Task maintenanceTask) {
        StoreDepositAutomaticRunState state = registry.stateForMaintenance(maintenanceTask);
        return state == null ? Snapshot.unavailable() : snapshot(state);
    }

    public TerminalRecord takePendingEviction() {
        return registry.takePendingEviction();
    }

    public void purgeClosedRun(StoreDepositAutomaticContext context) {
        if (context == null || !context.available()) {
            return;
        }
        StoreDepositAutomaticRunState state = registry.runForId(context.autoOperationId());
        if (state != null && state.diagnosticCoverageClosed()) {
            registry.remove(state);
        }
    }

    public Snapshot recordTerminalEmissionSuppressed(TerminalRecord record) {
        if (record == null || !record.available() || record.duplicate()) {
            return Snapshot.unavailable();
        }
        StoreDepositAutomaticRunState state = registry.runForId(record.context().autoOperationId());
        if (state == null) {
            Snapshot previous = record.snapshot();
            if (previous.available() && previous.activeRunLedgerEvictionCount() > 0) {
                long suppressedCount = registry.incrementActiveRunEvictionEmissionSuppressedCount();
                return StoreDepositAutomaticTerminalCoverage.withEmissionSuppressed(
                        previous,
                        record.terminalScope(),
                        record.terminalReason(),
                        suppressedCount
                );
            }
            return Snapshot.unavailable();
        }
        state.terminalCoverage().recordEmissionSuppressed(
                record.terminalScope(),
                record.terminalReason()
        );
        return snapshot(state);
    }

    public ClearResult clearForModeTransition() {
        return registry.clearForModeTransition();
    }

    public int activeRunCount() {
        return registry.activeRunCount();
    }

    public static boolean terminalIdentityCoverageComplete(Snapshot snapshot,
                                                           boolean stateComplete) {
        return StoreDepositAutomaticCoverageEvaluator.terminalIdentityCoverageComplete(
                snapshot,
                stateComplete
        );
    }

    public static String missingLifecycleBoundaries(Snapshot snapshot) {
        return StoreDepositAutomaticCoverageEvaluator.missingLifecycleBoundaries(snapshot);
    }

    private TerminalRecord recordCoverageClose(StoreDepositAutomaticRunState state,
                                               String closingIdentity,
                                               String terminalReason) {
        state.recordCoverageClose();
        return recordScope(
                state,
                state.baseContext(),
                "AUTOMATIC_DIAGNOSTIC_COVERAGE",
                closingIdentity,
                terminalReason
        );
    }

    private TerminalRecord recordScope(StoreDepositAutomaticRunState state,
                                       StoreDepositAutomaticContext context,
                                       String terminalScope,
                                       String closingIdentity,
                                       String terminalReason) {
        if (state == null) {
            return TerminalRecord.unavailable();
        }
        StoreDepositAutomaticTerminalCoverage.TerminalScopeResult result =
                state.terminalCoverage().record(terminalScope, closingIdentity, terminalReason);
        Snapshot snapshot = snapshot(state);
        return new TerminalRecord(
                true,
                result.duplicate(),
                context,
                result.scope(),
                result.reason(),
                result.identity(),
                snapshot
        );
    }

    private Snapshot snapshot(StoreDepositAutomaticRunState state) {
        return state.snapshot(registry.activeRunEvictionEmissionSuppressedCount());
    }

    private void evictOldestIfNeeded() {
        if (!registry.activeRunCapacityReached()) {
            return;
        }
        StoreDepositAutomaticRunState oldest = registry.oldestRun();
        if (oldest == null) {
            return;
        }
        long queueOverflowCount = registry.makePendingEvictionRoom();
        oldest.recordActiveRunEviction(queueOverflowCount, "ACTIVE_RUN_LEDGER_CAP_EVICTION");
        TerminalRecord record = recordScope(
                oldest,
                oldest.baseContext(),
                "AUTOMATIC_DIAGNOSTIC_COVERAGE",
                StoreDepositOperationContext.identity(oldest.maintenanceTask()),
                "ACTIVE_RUN_LEDGER_CAP_EVICTION"
        );
        registry.addPendingEviction(record);
        registry.remove(oldest);
    }
}
