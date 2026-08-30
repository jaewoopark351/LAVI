package lavi.minecraft.diagnostics.container.store.deposit.route;

import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.binding.StoreDepositBindingRegistry;
import lavi.minecraft.diagnostics.container.store.deposit.budget.StoreDepositBoundedEventLogger;
import lavi.minecraft.diagnostics.container.store.deposit.budget.StoreDepositEmissionGate;
import lavi.minecraft.diagnostics.container.store.deposit.candidate.StoreContainerCandidateEventFields;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationState;
import lavi.minecraft.diagnostics.container.store.deposit.event.StoreDepositEventFields;
import net.minecraft.util.math.BlockPos;

//20260829_kpopmodder: Keep target-callback decision observation in one focused route collaborator.
public final class StoreDepositTargetCallbackDiagnostics {
    private final StoreDepositBindingRegistry bindings;
    private final StoreDepositEmissionGate emissionGate;

    public StoreDepositTargetCallbackDiagnostics(StoreDepositBindingRegistry bindings,
                                                 StoreDepositEmissionGate emissionGate) {
        this.bindings = bindings;
        this.emissionGate = emissionGate;
    }

    public void logTargetCallbackDecision(Task rootTask,
                                          BlockPos callbackTarget,
                                          BlockPos currentChestTryBefore,
                                          boolean sameReference,
                                          boolean progressResetBecauseReferenceChanged,
                                          ItemTarget[] boundNotStored) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        try {
            StoreDepositOperationState state = bindings.stateFor(rootTask);
            if (state == null) {
                return;
            }
            String outcome = progressResetBecauseReferenceChanged
                    ? "REFERENCE_CHANGED_PROGRESS_RESET"
                    : "REFERENCE_RETAINED";
            state.recordTargetCallbackDecision(outcome);
            String operationId = StoreDepositEventFields.operationId(state);
            String key = operationId + "|" + ChatClefDiagnostics.blockPos(callbackTarget) + "|" + outcome;
            if (!emissionGate.shouldEmitDetail(operationId, "STORE_CONTAINER_TARGET_CALLBACK_DECISION", key)) {
                return;
            }
            Object[] eventFields = StoreDepositEventFields.targetCallbackDecisionFields(
                    state,
                    rootTask,
                    callbackTarget,
                    currentChestTryBefore,
                    sameReference,
                    progressResetBecauseReferenceChanged,
                    boundNotStored
            );
            if (state.context().isDepositAllOperation()) {
                eventFields = StoreDepositEventFields.merge(
                        eventFields,
                        StoreContainerCandidateEventFields.routeCorrelationFields(state)
                );
            }
            StoreDepositBoundedEventLogger.log("STORE_CONTAINER_TARGET_CALLBACK_DECISION",
                    "store_container_target_callback_decision",
                    rootTask,
                    ChatClefDiagnostics.withCommandContextFields(eventFields));
        } catch (RuntimeException | LinkageError ignored) {
        }
    }
}
