package lavi.minecraft.diagnostics.container.store.deposit.terminal;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.binding.StoreDepositBindingRegistry;
import lavi.minecraft.diagnostics.container.store.deposit.budget.StoreDepositBoundedEventLogger;
import lavi.minecraft.diagnostics.container.store.deposit.budget.StoreDepositEmissionGate;
import lavi.minecraft.diagnostics.container.store.deposit.candidate.StoreContainerCandidateEventFields;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationState;
import lavi.minecraft.diagnostics.container.store.deposit.event.StoreDepositEventFields;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.accounting.StoreDepositTerminalAccountingToken;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.accounting.StoreDepositTerminalClassification;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.accounting.StoreDepositTerminalDispatchObserver;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.accounting.StoreDepositTerminalLedger;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.accounting.StoreDepositTerminalLedgerEventFields;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.accounting.StoreDepositTerminalLedgerSnapshot;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.accounting.StoreDepositTerminalScope;
import lavi.minecraft.diagnostics.session.admission.DiagnosticEventFamily;

//20260829_kpopmodder: Split terminal summary emission from the StoreDepositDiagnostics facade without changing store behavior.
public final class StoreDepositTerminalSummaryEmitter {
    private final StoreDepositBindingRegistry bindings;
    private final StoreDepositEmissionGate emissionGate;
    private final StoreDepositAutomaticTerminalDiagnostics automaticTerminals;
    private final StoreDepositTerminalLedger ledger;

    public StoreDepositTerminalSummaryEmitter(StoreDepositBindingRegistry bindings,
                                              StoreDepositEmissionGate emissionGate) {
        this(bindings, emissionGate, null, new StoreDepositTerminalLedger());
    }

    public StoreDepositTerminalSummaryEmitter(StoreDepositBindingRegistry bindings,
                                              StoreDepositEmissionGate emissionGate,
                                              StoreDepositAutomaticTerminalDiagnostics automaticTerminals) {
        this(bindings, emissionGate, automaticTerminals, new StoreDepositTerminalLedger());
    }

    public StoreDepositTerminalSummaryEmitter(StoreDepositBindingRegistry bindings,
                                              StoreDepositEmissionGate emissionGate,
                                              StoreDepositAutomaticTerminalDiagnostics automaticTerminals,
                                              StoreDepositTerminalLedger ledger) {
        this.bindings = bindings;
        this.emissionGate = emissionGate;
        this.automaticTerminals = automaticTerminals;
        this.ledger = ledger;
    }

    public void emitAndPurge(Task rootTask,
                             StoreDepositOperationState state,
                             String terminalTrigger,
                             String diagnosticClassification) {
        if (state == null) {
            ChatClefDiagnostics.runIfDiagnosticsEligible(
                    ledger::recordTerminalBoundaryWithoutStableContext
            );
            return;
        }
        ChatClefDiagnostics.runIfDiagnosticsEligible(() -> emitEligibleAndPurge(
                rootTask,
                state,
                terminalTrigger,
                diagnosticClassification
        ));
    }

    public StoreDepositTerminalLedgerSnapshot ledgerSnapshot() {
        return ledger.snapshot();
    }

    private void emitEligibleAndPurge(Task rootTask,
                                      StoreDepositOperationState state,
                                      String terminalTrigger,
                                      String diagnosticClassification) {
        String operationId = "unavailable";
        StoreDepositTerminalAccountingToken accountingToken = null;
        boolean ownsAcceptedFinalization = false;
        try {
            operationId = StoreDepositEventFields.operationId(state);
            if (!state.markTerminalFinalized()) {
                ledger.recordDuplicateFinalizationAttempt();
                return;
            }
            ownsAcceptedFinalization = true;
            StoreDepositTerminalClassification classification =
                    StoreDepositTerminalClassification.fromDiagnosticValue(diagnosticClassification);
            StoreDepositTerminalScope scope = StoreDepositTerminalScope.fromRequestSource(
                    state.context() == null ? null : state.context().requestSource()
            );
            accountingToken = ledger.recordAuthoritativeTerminal(
                    operationId,
                    classification,
                    scope,
                    state.context() != null,
                    ChatClefDiagnostics.currentClientTickId()
            );
            if (accountingToken == null) {
                return;
            }
            recordAutomaticTerminalSafely(state, rootTask, terminalTrigger);
            Object[] budgetFields = emissionGate.budgetSummaryFields(operationId);
            Object[] terminalFields = StoreDepositEventFields.terminalSummaryFields(
                    state,
                    terminalTrigger,
                    diagnosticClassification,
                    budgetFields
            );
            if (state.context() != null && state.context().isDepositAllOperation()) {
                terminalFields = StoreDepositEventFields.merge(
                        terminalFields,
                        StoreContainerCandidateEventFields.routeSummaryFields(state)
                );
            }
            Object[] finalTerminalFields = terminalFields;
            DiagnosticEventFamily family = classification.abnormal()
                    ? DiagnosticEventFamily.ABNORMAL_STORE_TERMINAL
                    : DiagnosticEventFamily.ROUTINE_STORE_TERMINAL;
            ChatClefDiagnostics.emitCriticalBoundedGroup(
                    family,
                    "STORE_DEPOSIT_TERMINAL_GROUP",
                    emitter -> emitTerminalGroup(
                            emitter,
                            rootTask,
                            state,
                            terminalTrigger,
                            budgetFields,
                            finalTerminalFields
                    ),
                    new StoreDepositTerminalDispatchObserver(ledger, accountingToken)
            );
            emitSuppressionSnapshotIfDue(rootTask);
        } catch (RuntimeException | LinkageError ignored) {
            if (accountingToken != null) {
                ledger.recordSuppressedBeforeAdmission(accountingToken);
            }
            ledger.recordDiagnosticCoverageGap();
        } finally {
            if (ownsAcceptedFinalization) {
                purge(rootTask, operationId);
            }
        }
    }

    private void recordAutomaticTerminalSafely(StoreDepositOperationState state,
                                                Task rootTask,
                                                String terminalTrigger) {
        if (automaticTerminals == null) {
            return;
        }
        try {
            automaticTerminals.recordPerItemRoot(state, rootTask, terminalTrigger);
        } catch (RuntimeException | LinkageError ignored) {
            ledger.recordDiagnosticCoverageGap();
        }
    }

    private static void emitTerminalGroup(
            lavi.minecraft.diagnostics.session.runtime.DiagnosticBoundedGroupEmitter emitter,
            Task rootTask,
            StoreDepositOperationState state,
            String terminalTrigger,
            Object[] budgetFields,
            Object[] finalTerminalFields) {
        StoreDepositBoundedEventLogger.logPreAdmitted(
                emitter,
                "STORE_DEPOSIT_TERMINAL_SUMMARY",
                "store_deposit_terminal_summary",
                rootTask,
                ChatClefDiagnostics.withCommandContextFields(finalTerminalFields)
        );
        StoreDepositBoundedEventLogger.logPreAdmitted(
                emitter,
                "STORE_DEPOSIT_EFFECT_SUMMARY",
                "store_deposit_effect_summary",
                rootTask,
                ChatClefDiagnostics.withCommandContextFields(
                        StoreDepositEventFields.effectSummaryFields(state, terminalTrigger)
                )
        );
        StoreDepositBoundedEventLogger.logPreAdmitted(
                emitter,
                "STORE_BARITONE_OPERATION_SUMMARY",
                "store_baritone_operation_summary",
                rootTask,
                ChatClefDiagnostics.withCommandContextFields(
                        StoreDepositEventFields.baritoneSummaryFields(state, terminalTrigger)
                )
        );
        StoreDepositBoundedEventLogger.logPreAdmitted(
                emitter,
                "STORE_DEPOSIT_DIAGNOSTIC_COVERAGE_SUMMARY",
                "store_deposit_diagnostic_coverage_summary",
                rootTask,
                ChatClefDiagnostics.withCommandContextFields(
                        StoreDepositEventFields.coverageSummaryFields(
                                state,
                                terminalTrigger,
                                budgetFields
                        )
                )
        );
    }

    private void emitSuppressionSnapshotIfDue(Task rootTask) {
        StoreDepositTerminalLedgerSnapshot snapshot = ledger.snapshot();
        long suppressed = snapshot.fullTerminalGroupSuppressedBeforeAdmission();
        if (suppressed != 1L && (suppressed <= 0L || (suppressed & (suppressed - 1L)) != 0L)) {
            return;
        }
        StoreDepositBoundedEventLogger.log(
                "STORE_DEPOSIT_TERMINAL_LEDGER_SNAPSHOT",
                "store_deposit_terminal_group_suppressed",
                rootTask,
                StoreDepositTerminalLedgerEventFields.snapshotFields(snapshot, "SUPPRESSION_MILESTONE")
        );
    }

    private void purge(Task rootTask, String operationId) {
        try {
            emissionGate.purgeOperation(operationId);
        } catch (RuntimeException | LinkageError ignored) {
            ledger.recordDiagnosticCoverageGap();
        }
        try {
            bindings.purgeOperation(rootTask);
        } catch (RuntimeException | LinkageError ignored) {
            ledger.recordDiagnosticCoverageGap();
        }
    }
}
