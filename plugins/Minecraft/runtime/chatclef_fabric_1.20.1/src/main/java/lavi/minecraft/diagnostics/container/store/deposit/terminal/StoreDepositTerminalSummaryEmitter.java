package lavi.minecraft.diagnostics.container.store.deposit.terminal;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.binding.StoreDepositBindingRegistry;
import lavi.minecraft.diagnostics.container.store.deposit.budget.StoreDepositBoundedEventLogger;
import lavi.minecraft.diagnostics.container.store.deposit.budget.StoreDepositEmissionGate;
import lavi.minecraft.diagnostics.container.store.deposit.budget.StoreDepositTerminalReservation;
import lavi.minecraft.diagnostics.container.store.deposit.candidate.StoreContainerCandidateEventFields;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationState;
import lavi.minecraft.diagnostics.container.store.deposit.event.StoreDepositEventFields;

//20260829_kpopmodder: Split terminal summary emission from the StoreDepositDiagnostics facade without changing store behavior.
public final class StoreDepositTerminalSummaryEmitter {
    private final StoreDepositBindingRegistry bindings;
    private final StoreDepositEmissionGate emissionGate;
    private final StoreDepositAutomaticTerminalDiagnostics automaticTerminals;

    public StoreDepositTerminalSummaryEmitter(StoreDepositBindingRegistry bindings,
                                              StoreDepositEmissionGate emissionGate) {
        this(bindings, emissionGate, null);
    }

    public StoreDepositTerminalSummaryEmitter(StoreDepositBindingRegistry bindings,
                                              StoreDepositEmissionGate emissionGate,
                                              StoreDepositAutomaticTerminalDiagnostics automaticTerminals) {
        this.bindings = bindings;
        this.emissionGate = emissionGate;
        this.automaticTerminals = automaticTerminals;
    }

    public void emitAndPurge(Task rootTask,
                             StoreDepositOperationState state,
                             String terminalTrigger,
                             String diagnosticClassification) {
        if (state == null || !state.markTerminalFinalized()) {
            return;
        }
        if (automaticTerminals != null) {
            automaticTerminals.recordPerItemRoot(state, rootTask, terminalTrigger);
        }
        String operationId = StoreDepositEventFields.operationId(state);
        StoreDepositTerminalReservation reservation = emissionGate.reserveTerminalGroup(operationId);
        if (!reservation.reserved()) {
            if (reservation.exhausted()
                    && emissionGate.shouldEmitControl(operationId, "STORE_DEPOSIT_TERMINAL_GROUP_RESERVE_EXHAUSTED")) {
                StoreDepositBoundedEventLogger.log("STORE_DEPOSIT_TERMINAL_GROUP_RESERVE_EXHAUSTED",
                        "store_deposit_terminal_group_reserve_exhausted",
                        rootTask,
                        ChatClefDiagnostics.withCommandContextFields(
                                StoreDepositEventFields.terminalReserveExhaustedFields(
                                        state,
                                        terminalTrigger,
                                        emissionGate.budgetSummaryFields(operationId)
                                )
                        ));
            }
            emissionGate.purgeOperation(operationId);
            bindings.purgeOperation(rootTask);
            return;
        }
        Object[] budgetFields = emissionGate.budgetSummaryFields(operationId);
        Object[] terminalFields = StoreDepositEventFields.terminalSummaryFields(
                state,
                terminalTrigger,
                diagnosticClassification,
                budgetFields
        );
        if (state.context().isDepositAllOperation()) {
            terminalFields = StoreDepositEventFields.merge(
                    terminalFields,
                    StoreContainerCandidateEventFields.routeSummaryFields(state)
            );
        }
        StoreDepositBoundedEventLogger.log("STORE_DEPOSIT_TERMINAL_SUMMARY",
                "store_deposit_terminal_summary",
                rootTask,
                ChatClefDiagnostics.withCommandContextFields(terminalFields));
        StoreDepositBoundedEventLogger.log("STORE_DEPOSIT_EFFECT_SUMMARY",
                "store_deposit_effect_summary",
                rootTask,
                ChatClefDiagnostics.withCommandContextFields(
                        StoreDepositEventFields.effectSummaryFields(state, terminalTrigger)
                ));
        StoreDepositBoundedEventLogger.log("STORE_BARITONE_OPERATION_SUMMARY",
                "store_baritone_operation_summary",
                rootTask,
                ChatClefDiagnostics.withCommandContextFields(
                        StoreDepositEventFields.baritoneSummaryFields(state, terminalTrigger)
                ));
        StoreDepositBoundedEventLogger.log("STORE_DEPOSIT_DIAGNOSTIC_COVERAGE_SUMMARY",
                "store_deposit_diagnostic_coverage_summary",
                rootTask,
                ChatClefDiagnostics.withCommandContextFields(
                        StoreDepositEventFields.coverageSummaryFields(state, terminalTrigger, budgetFields)
                ));
        emissionGate.purgeOperation(operationId);
        bindings.purgeOperation(rootTask);
    }
}
