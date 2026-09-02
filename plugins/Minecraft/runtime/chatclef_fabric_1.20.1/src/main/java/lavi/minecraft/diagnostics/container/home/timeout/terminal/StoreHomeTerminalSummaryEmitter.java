package lavi.minecraft.diagnostics.container.home.timeout.terminal;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.container.home.timeout.event.StoreHomeDiagnosticEmitter;
import lavi.minecraft.diagnostics.container.home.timeout.event.StoreHomeEventFields;
import lavi.minecraft.task.container.home.execution.StoreHomeResult;
import lavi.minecraft.task.container.home.execution.operation.StoreHomeOperationProgress;

//20260902_kpopmodder: Isolate STORE_HOME terminal summary assembly and emission.
public final class StoreHomeTerminalSummaryEmitter {
    public void emit(
            StoreHomeDiagnosticEmitter emitter,
            String eventName,
            String reason,
            Task owner,
            Object[] operationFields,
            Object[] candidateEvidence,
            StoreHomeResult result,
            StoreHomeOperationProgress operation) {
        emitter.emitTerminal(
                eventName,
                reason,
                owner,
                StoreHomeDiagnosticEmitter.merge(
                        operationFields,
                        candidateEvidence,
                        StoreHomeEventFields.terminal(result, reason, operation),
                        new Object[]{
                                "budgetSummaryCapturedBeforeTerminalEmission", true
                        },
                        emitter.budgetSummaryFields()
                )
        );
    }
}
