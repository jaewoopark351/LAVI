package lavi.minecraft.diagnostics.container.store.deposit.route;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.budget.StoreDepositBoundedEventLogger;
import lavi.minecraft.diagnostics.container.store.deposit.budget.StoreDepositEmissionGate;
import lavi.minecraft.diagnostics.container.store.deposit.candidate.StoreContainerCandidateEventFields;
import lavi.minecraft.diagnostics.container.store.deposit.candidate.StoreContainerRouteCheckpoint;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationState;
import lavi.minecraft.diagnostics.container.store.deposit.event.StoreDepositEventFields;

import java.util.Optional;

//20260902_kpopmodder: Isolate changed-state checkpoint emission while the route aggregate retains all mutable state.
public final class StoreDepositCheckpointDiagnostics {
    private final StoreDepositEmissionGate emissionGate;

    public StoreDepositCheckpointDiagnostics(StoreDepositEmissionGate emissionGate) {
        this.emissionGate = emissionGate;
    }

    public void emitIfDue(Task task, StoreDepositOperationState state) {
        if (state == null || !state.context().isDepositAllOperation()) {
            return;
        }
        Optional<StoreContainerRouteCheckpoint> checkpoint = state.routeState().checkpoint(
                ChatClefDiagnostics.currentClientTickId()
        );
        if (checkpoint.isEmpty()) {
            return;
        }
        StoreContainerRouteCheckpoint value = checkpoint.get();
        String operationId = StoreDepositEventFields.operationId(state);
        String key = operationId + "|" + value.checkpointSequence();
        if (!emissionGate.shouldEmitDetail(
                operationId,
                "STORE_DEPOSIT_CHECKPOINT_SUMMARY",
                key
        )) {
            return;
        }
        StoreDepositBoundedEventLogger.log(
                "STORE_DEPOSIT_CHECKPOINT_SUMMARY",
                "store_deposit_checkpoint_summary",
                task,
                ChatClefDiagnostics.withCommandContextFields(
                        StoreDepositEventFields.merge(
                                StoreContainerCandidateEventFields.checkpointFields(state, value),
                                emissionGate.budgetSummaryFields(operationId)
                        )
                )
        );
        state.routeState().acknowledgeCheckpointEmission(value.checkpointSequence());
    }
}
