package lavi.minecraft.diagnostics.container.store.deposit.terminal;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationContext;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationState;
import lavi.minecraft.diagnostics.container.store.deposit.event.StoreDepositEventFields;
import lavi.minecraft.diagnostics.container.store.deposit.budget.StoreDepositBoundedEventLogger;
import lavi.minecraft.diagnostics.container.store.deposit.budget.StoreDepositEmissionGate;

//20260830_kpopmodder: Emit each automatic-deposit terminal scope independently from gameplay lifecycle state.
public final class StoreDepositAutomaticTerminalDiagnostics {
    private final StoreDepositAutomaticLifecycleLedger ledger;
    private final StoreDepositEmissionGate emissionGate;

    public StoreDepositAutomaticTerminalDiagnostics(StoreDepositAutomaticLifecycleLedger ledger,
                                                     StoreDepositEmissionGate emissionGate) {
        this.ledger = ledger;
        this.emissionGate = emissionGate;
    }

    public StoreDepositAutomaticContext beginRun(Task maintenanceTask,
                                                 Task userTaskRoot,
                                                 long policyContextEpoch) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return StoreDepositAutomaticContext.unavailable();
        }
        StoreDepositAutomaticContext context = ledger.beginRun(
                maintenanceTask,
                userTaskRoot,
                policyContextEpoch
        );
        StoreDepositAutomaticLifecycleLedger.TerminalRecord eviction;
        while ((eviction = ledger.takePendingEviction()).available()) {
            StoreDepositAutomaticLifecycleLedger.Snapshot snapshot = eviction.snapshot();
            emit(
                    eviction,
                    null,
                    snapshot.userTaskRootIdentity(),
                    snapshot.maintenanceIdentity()
            );
        }
        return context;
    }

    public StoreDepositAutomaticContext registerChild(Task maintenanceTask,
                                                      Task childTask,
                                                      int childIndex) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return StoreDepositAutomaticContext.unavailable();
        }
        return ledger.registerChild(maintenanceTask, childTask, childIndex);
    }

    public StoreDepositAutomaticContext contextForChild(Task childTask) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return StoreDepositAutomaticContext.unavailable();
        }
        return ledger.contextForChild(childTask);
    }

    public void recordPerItemRoot(StoreDepositOperationState state,
                                  Task rootTask,
                                  String terminalReason) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()
                || state == null
                || !state.context().isAutomaticDepositOperation()) {
            return;
        }
        emit(
                ledger.recordScope(
                        state.automaticContext(),
                        "PER_ITEM_ROOT",
                        StoreDepositOperationContext.identity(rootTask),
                        terminalReason
                ),
                rootTask,
                state.context().rootTaskIdentity(),
                "UNAVAILABLE",
                StoreDepositEventFields.operationFields(state)
        );
    }

    public void recordScope(StoreDepositAutomaticContext context,
                            String terminalScope,
                            Task closingTask,
                            Task parentTask,
                            String terminalReason) {
        recordScopeIdentity(
                context,
                terminalScope,
                StoreDepositOperationContext.identity(closingTask),
                closingTask,
                StoreDepositOperationContext.identity(parentTask),
                StoreDepositOperationContext.identity(closingTask),
                terminalReason
        );
    }

    public void recordScopeIdentity(StoreDepositAutomaticContext context,
                                    String terminalScope,
                                    String closingIdentity,
                                    Task logTask,
                                    String parentIdentity,
                                    String childIdentity,
                                    String terminalReason) {
        recordScopeIdentity(
                context,
                terminalScope,
                closingIdentity,
                logTask,
                parentIdentity,
                childIdentity,
                terminalReason,
                new Object[0]
        );
    }

    public void expectScopeIdentity(StoreDepositAutomaticContext context,
                                    String terminalScope,
                                    String closingIdentity) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        ledger.expectTerminalScopeIdentity(context, terminalScope, closingIdentity);
    }

    public void recordCoverageGap(StoreDepositAutomaticContext context,
                                  String gapScope,
                                  String affectedIdentity,
                                  Task logTask,
                                  String reason,
                                  Object[] correlationFields) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        StoreDepositAutomaticLifecycleLedger.Snapshot snapshot =
                ledger.recordDiagnosticCoverageGap(
                        context,
                        gapScope,
                        affectedIdentity,
                        reason
                );
        if (!snapshot.available()
                || !emissionGate.shouldEmitDetail(
                        context.autoOperationId(),
                        "STORE_DEPOSIT_AUTOMATIC_COVERAGE_GAP",
                        gapScope + "|" + affectedIdentity + "|" + reason
                )) {
            return;
        }
        StoreDepositBoundedEventLogger.log(
                "STORE_DEPOSIT_AUTOMATIC_COVERAGE_GAP",
                "automatic_diagnostic_coverage_gap",
                logTask,
                ChatClefDiagnostics.withCommandContextFields(
                        StoreDepositEventFields.merge(
                                StoreDepositEventFields.automaticIdentityFields(
                                        context,
                                        "UNAVAILABLE",
                                        "UNAVAILABLE",
                                        "UNAVAILABLE",
                                        "UNAVAILABLE",
                                        "UNAVAILABLE",
                                        "UNAVAILABLE",
                                        "UNAVAILABLE"
                                ),
                                StoreDepositEventFields.merge(
                                        correlationFields,
                                        new Object[]{
                                                "coverageGapScope", gapScope,
                                                "affectedIdentity", affectedIdentity,
                                                "coverageGapReason", reason,
                                                "observationComplete", false,
                                                "missingBoundaries", "DIAGNOSTIC_BOOKKEEPING_EVICTED",
                                                "behavior_effect", "none"
                                        }
                                )
                        )
                )
        );
    }

    public void recordScopeIdentity(StoreDepositAutomaticContext context,
                                    String terminalScope,
                                    String closingIdentity,
                                    Task logTask,
                                    String parentIdentity,
                                    String childIdentity,
                                    String terminalReason,
                                    Object[] correlationFields) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        emit(
                ledger.recordScope(
                        context,
                        terminalScope,
                        closingIdentity,
                        terminalReason
                ),
                logTask,
                parentIdentity,
                childIdentity,
                correlationFields
        );
    }

    public void recordMaintenanceTerminal(Task maintenanceTask,
                                          String terminalReason,
                                          String nextLifecycleState) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        emit(
                ledger.recordMaintenanceTerminal(maintenanceTask, terminalReason, nextLifecycleState),
                maintenanceTask,
                "UNAVAILABLE",
                StoreDepositOperationContext.identity(maintenanceTask)
        );
    }

    public void recordPressureRunClosed(Task maintenanceTask, String terminalReason) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        emit(
                ledger.recordPressureOwnedRunClose(maintenanceTask, terminalReason),
                maintenanceTask,
                "UNAVAILABLE",
                StoreDepositOperationContext.identity(maintenanceTask)
        );
    }

    public void recordRunToWait(Task maintenanceTask,
                                String terminalReason,
                                String nextLifecycleState) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        emit(
                ledger.recordRunToWait(maintenanceTask, terminalReason, nextLifecycleState),
                maintenanceTask,
                "UNAVAILABLE",
                StoreDepositOperationContext.identity(maintenanceTask)
        );
    }

    public void recordCoverageClosed(Task maintenanceTask, String terminalReason) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        emit(
                ledger.recordCoverageClose(maintenanceTask, terminalReason),
                maintenanceTask,
                "UNAVAILABLE",
                StoreDepositOperationContext.identity(maintenanceTask)
        );
    }

    public void recordCoverageClosedIfNoUserTask(Task maintenanceTask, String terminalReason) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        emit(
                ledger.recordCoverageCloseIfNoUserTask(maintenanceTask, terminalReason),
                maintenanceTask,
                "UNAVAILABLE",
                StoreDepositOperationContext.identity(maintenanceTask)
        );
    }

    public void observeUserTaskResume(Task task) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        emit(
                ledger.observeUserTaskResume(task),
                task,
                "UNAVAILABLE",
                StoreDepositOperationContext.identity(task)
        );
    }

    public void observeUserTaskNaturalCompletion(Task task) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        for (StoreDepositAutomaticLifecycleLedger.TerminalRecord record
                : ledger.observeUserTaskNaturalCompletions(task)) {
            emit(
                    record,
                    task,
                    "UNAVAILABLE",
                    StoreDepositOperationContext.identity(task)
            );
        }
        for (StoreDepositAutomaticLifecycleLedger.TerminalRecord record
                : ledger.recordCoverageClosesForUserTask(
                        task,
                        "USER_TASK_NATURAL_COMPLETION_OBSERVED"
                )) {
            emit(
                    record,
                    task,
                    StoreDepositOperationContext.identity(task),
                    StoreDepositOperationContext.identity(task)
            );
        }
    }

    StoreDepositAutomaticLifecycleLedger.Snapshot snapshotFor(Task maintenanceTask) {
        return ledger.snapshotFor(maintenanceTask);
    }

    private void emit(StoreDepositAutomaticLifecycleLedger.TerminalRecord record,
                      Task task,
                      String parentIdentity,
                      String childIdentity) {
        emit(record, task, parentIdentity, childIdentity, new Object[0]);
    }

    private void emit(StoreDepositAutomaticLifecycleLedger.TerminalRecord record,
                      Task task,
                      String parentIdentity,
                      String childIdentity,
                      Object[] correlationFields) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()
                || record == null
                || !record.available()
                || record.duplicate()) {
            return;
        }
        String automaticOperationId = record.context().autoOperationId();
        boolean coverageClose = "AUTOMATIC_DIAGNOSTIC_COVERAGE".equals(record.terminalScope());
        boolean emitAllowed = coverageClose
                ? emissionGate.shouldEmitLateSummary(automaticOperationId)
                : emissionGate.shouldEmitDetail(
                        automaticOperationId,
                        "STORE_DEPOSIT_AUTOMATIC_TERMINAL",
                        record.terminalScope() + "|" + record.closingIdentity()
                );
        if (!emitAllowed) {
            StoreDepositAutomaticLifecycleLedger.Snapshot suppressed =
                    ledger.recordTerminalEmissionSuppressed(record);
            if (coverageClose
                    && suppressed.available()
                    && emissionGate.shouldEmitCoverageSuppressionSummary()) {
                StoreDepositBoundedEventLogger.log(
                        "STORE_DEPOSIT_AUTOMATIC_COVERAGE_SUPPRESSION_SUMMARY",
                        "automatic_coverage_terminal_suppressed",
                        task,
                        ChatClefDiagnostics.withCommandContextFields(
                                StoreDepositEventFields.merge(
                                        StoreDepositEventFields.automaticIdentityFields(
                                                suppressed.context(),
                                                "UNAVAILABLE",
                                                "UNAVAILABLE",
                                                "UNAVAILABLE",
                                                "UNAVAILABLE",
                                                "UNAVAILABLE",
                                                "UNAVAILABLE",
                                                "UNAVAILABLE"
                                        ),
                                        new Object[]{
                                                "terminalScope", record.terminalScope(),
                                                "terminalReason", record.terminalReason(),
                                                "closingIdentity", record.closingIdentity(),
                                                "activeRunLedgerEvictionCount", suppressed.activeRunLedgerEvictionCount(),
                                                "activeRunEvictionQueueOverflowCount", suppressed.activeRunEvictionQueueOverflowCount(),
                                                "activeRunEvictionEmissionSuppressedCount", suppressed.activeRunEvictionEmissionSuppressedCount(),
                                                "activeRunLedgerEvictionReason", suppressed.activeRunLedgerEvictionReason(),
                                                "terminalEmissionSuppressedCount", suppressed.terminalEmissionSuppressedCount(),
                                                "terminalEmissionSuppressedCounts", suppressed.terminalEmissionSuppressedCounts(),
                                                "terminalEmissionSuppressedReasons", suppressed.terminalEmissionSuppressedReasons(),
                                                "expectedTerminalScopeCounts", suppressed.expectedTerminalCounts(),
                                                "expectedTerminalIdentityOverflowCount",
                                                suppressed.expectedTerminalIdentityOverflowCount(),
                                                "diagnosticCoverageGaps", suppressed.diagnosticCoverageGaps(),
                                                "diagnosticCoverageGapOverflowCount",
                                                suppressed.diagnosticCoverageGapOverflowCount(),
                                                "registeredAutomaticChildCount", suppressed.registeredChildCount(),
                                                "observedPerItemRootTerminalCount", suppressed.terminalCount("PER_ITEM_ROOT"),
                                                "lifecycleCoverageComplete", suppressed.lifecycleCoverageComplete(),
                                                "missingLifecycleBoundaries", suppressed.missingLifecycleBoundaries(),
                                                "observationComplete", false,
                                                "missingBoundaries", suppressed.activeRunLedgerEvictionCount() > 0
                                                        ? "AUTOMATIC_DIAGNOSTIC_COVERAGE_EMISSION_SUPPRESSED,ACTIVE_RUN_EVICTION_TERMINAL_SUPPRESSED"
                                                        : "AUTOMATIC_DIAGNOSTIC_COVERAGE_EMISSION_SUPPRESSED",
                                                "additionalSuppressionMayOccur", true,
                                                "behavior_effect", "none"
                                        }
                                )
                        )
                );
            }
            if (coverageClose) {
                emissionGate.purgeOperation(automaticOperationId);
                ledger.purgeClosedRun(record.context());
            }
            return;
        }
        StoreDepositAutomaticLifecycleLedger.Snapshot snapshot = record.snapshot();
        boolean lifecycleCoverageComplete = !coverageClose || snapshot.lifecycleCoverageComplete();
        boolean observationComplete = snapshot.terminalIdentityCoverageComplete()
                && lifecycleCoverageComplete;
        Object[] fields = StoreDepositEventFields.merge(
                StoreDepositEventFields.automaticIdentityFields(
                        record.context(),
                        "UNAVAILABLE",
                        "UNAVAILABLE",
                        "UNAVAILABLE",
                        "UNAVAILABLE",
                        "UNAVAILABLE",
                        "UNAVAILABLE",
                        "UNAVAILABLE"
                ),
                StoreDepositEventFields.merge(
                        correlationFields,
                        new Object[]{
                                "terminalScope", record.terminalScope(),
                                "terminalReason", record.terminalReason(),
                                "closingIdentity", record.closingIdentity(),
                                "parentIdentity", parentIdentity,
                                "childIdentity", childIdentity,
                                "nextLifecycleState", snapshot.nextLifecycleState(),
                                "ownedRunClosed", snapshot.pressureOwnedRunClosed(),
                                "maintenanceLogicalTerminal", snapshot.maintenanceLogicalTerminal(),
                                "diagnosticCoverageClosed", snapshot.diagnosticCoverageClosed(),
                                "userTaskResumeObserved", snapshot.userTaskResumeObserved(),
                                "userTaskNaturalCompletionObserved", snapshot.userTaskNaturalCompletionObserved(),
                                "terminalScopeCounts", snapshot.terminalCounts(),
                                "expectedTerminalScopeCounts", snapshot.expectedTerminalCounts(),
                                "expectedTerminalIdentityOverflowCount",
                                snapshot.expectedTerminalIdentityOverflowCount(),
                                "diagnosticCoverageGaps", snapshot.diagnosticCoverageGaps(),
                                "diagnosticCoverageGapOverflowCount",
                                snapshot.diagnosticCoverageGapOverflowCount(),
                                "registeredAutomaticChildCount", snapshot.registeredChildCount(),
                                "observedPerItemRootTerminalCount", snapshot.terminalCount("PER_ITEM_ROOT"),
                                "terminalIdentityCoverageComplete", snapshot.terminalIdentityCoverageComplete(),
                                "lifecycleCoverageComplete", lifecycleCoverageComplete,
                                "missingLifecycleBoundaries", coverageClose
                                        ? snapshot.missingLifecycleBoundaries()
                                        : "NOT_FINAL_COVERAGE_BOUNDARY",
                                "terminalDedupeEvictionCount", snapshot.terminalDedupeEvictionCount(),
                                "terminalScopeOverflowCount", snapshot.terminalScopeOverflowCount(),
                                "terminalEmissionSuppressedCount", snapshot.terminalEmissionSuppressedCount(),
                                "terminalEmissionSuppressedCounts", snapshot.terminalEmissionSuppressedCounts(),
                                "terminalEmissionSuppressedReasons", snapshot.terminalEmissionSuppressedReasons(),
                                "activeRunLedgerEvictionCount", snapshot.activeRunLedgerEvictionCount(),
                                "activeRunEvictionQueueOverflowCount", snapshot.activeRunEvictionQueueOverflowCount(),
                                "activeRunEvictionEmissionSuppressedCount", snapshot.activeRunEvictionEmissionSuppressedCount(),
                                "activeRunLedgerEvictionReason", snapshot.activeRunLedgerEvictionReason(),
                                "suppressedEventCount", field(
                                        emissionGate.budgetSummaryFields(automaticOperationId),
                                        "storeBudgetOperationSuppressedCounts",
                                        "{}"
                                ),
                                "observationComplete", observationComplete,
                                "missingBoundaries", terminalMissingBoundaries(snapshot, coverageClose),
                                "behavior_effect", "none"
                        }
                )
        );
        StoreDepositBoundedEventLogger.log(
                "STORE_DEPOSIT_AUTOMATIC_TERMINAL",
                "store_deposit_automatic_terminal",
                task,
                ChatClefDiagnostics.withCommandContextFields(fields)
        );
        if (coverageClose) {
            emissionGate.purgeOperation(automaticOperationId);
            ledger.purgeClosedRun(record.context());
        }
    }

    private static Object field(Object[] fields, String name, Object fallback) {
        if (fields == null) {
            return fallback;
        }
        for (int i = 0; i + 1 < fields.length; i += 2) {
            if (name.equals(fields[i])) {
                return fields[i + 1];
            }
        }
        return fallback;
    }

    private static String terminalMissingBoundaries(
            StoreDepositAutomaticLifecycleLedger.Snapshot snapshot,
            boolean coverageClose) {
        String identityGap = snapshot.terminalIdentityCoverageComplete()
                ? "NONE"
                : "TERMINAL_IDENTITY_DEDUPE_SCOPE_OR_ACTIVE_RUN_EVICTION_OR_EMISSION_SUPPRESSION";
        String lifecycleGap = coverageClose ? snapshot.missingLifecycleBoundaries() : "NONE";
        if ("NONE".equals(identityGap)) {
            return lifecycleGap;
        }
        if ("NONE".equals(lifecycleGap)) {
            return identityGap;
        }
        return identityGap + "," + lifecycleGap;
    }
}
