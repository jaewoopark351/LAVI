package lavi.minecraft.diagnostics.container.store.deposit.event.transfer;

import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationState;
import lavi.minecraft.diagnostics.container.store.deposit.event.common.StoreDepositOperationCorrelationFields;
import lavi.minecraft.diagnostics.container.store.deposit.event.common.StoreDepositOrderedFieldSupport;
import net.minecraft.util.math.BlockPos;

//20260902_kpopmodder: Own transfer-family diagnostic payload assembly behind the stable public facade.
public final class StoreDepositTransferEventFields {
    private StoreDepositTransferEventFields() {
    }

    public static Object[] transferDecisionFields(StoreDepositOperationState state,
                                                  Task task,
                                                  BlockPos targetContainer,
                                                  ItemTarget target,
                                                  int potentialSourceSlotCount,
                                                  boolean bestSourcePresent,
                                                  boolean destinationEvaluated,
                                                  boolean destinationPresent,
                                                  String action) {
        return StoreDepositOrderedFieldSupport.merge(
                StoreDepositOperationCorrelationFields.operationFields(state),
                new Object[]{
                        "diagnosticScope", "store_deposit_transfer",
                        "owner", "store_deposit_transfer_observer",
                        "mode", "BOUNDARY",
                        "trigger", action,
                        "dedupe_key", "store_container_transfer|" + StoreDepositOperationCorrelationFields.operationId(state) + "|" + ChatClefDiagnostics.blockPos(targetContainer) + "|" + action,
                        "max_emission", "state_change_only,per_operation=256",
                        "correlation", "storeOperationId=" + StoreDepositOperationCorrelationFields.operationId(state) + ",storeInContainerTaskIdentity=" + StoreDepositOperationCorrelationFields.identity(task),
                        "payload", "flat_fields",
                        "terminal", false,
                        "behavior_effect", "none",
                        "transferAction", action,
                        "targetContainerPosition", ChatClefDiagnostics.blockPos(targetContainer),
                        "targetItem", String.valueOf(target),
                        "potentialSourceSlotCount", potentialSourceSlotCount,
                        "bestSourcePresent", bestSourcePresent,
                        "destinationSlotEvaluated", destinationEvaluated,
                        "destinationSlotPresent", destinationPresent,
                        "taskSummary", ChatClefDiagnostics.taskSummaryForDiagnosticLog(task)
                }
        );
    }
}
