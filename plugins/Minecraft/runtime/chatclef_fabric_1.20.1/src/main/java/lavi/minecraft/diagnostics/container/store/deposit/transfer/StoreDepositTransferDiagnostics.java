package lavi.minecraft.diagnostics.container.store.deposit.transfer;

import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.binding.StoreDepositBindingRegistry;
import lavi.minecraft.diagnostics.container.store.deposit.budget.StoreDepositBoundedEventLogger;
import lavi.minecraft.diagnostics.container.store.deposit.budget.StoreDepositEmissionGate;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationState;
import lavi.minecraft.diagnostics.container.store.deposit.event.StoreDepositEventFields;
import net.minecraft.util.math.BlockPos;

//20260829_kpopmodder: Split transfer decision observation from the StoreDepositDiagnostics compatibility facade.
public final class StoreDepositTransferDiagnostics {
    private final StoreDepositBindingRegistry bindings;
    private final StoreDepositEmissionGate emissionGate;
    private final StoreDepositTransferAttemptRegistry attempts;

    public StoreDepositTransferDiagnostics(StoreDepositBindingRegistry bindings,
                                           StoreDepositEmissionGate emissionGate) {
        this(bindings, emissionGate, null);
    }

    public StoreDepositTransferDiagnostics(StoreDepositBindingRegistry bindings,
                                           StoreDepositEmissionGate emissionGate,
                                           StoreDepositTransferAttemptRegistry attempts) {
        this.bindings = bindings;
        this.emissionGate = emissionGate;
        this.attempts = attempts;
    }

    public void stageTransferCandidate(Task parent,
                                       Task candidate,
                                       StoreDepositTransferSelectionSnapshot selection) {
        if (ChatClefDiagnostics.isBoundaryEnabled() && attempts != null) {
            attempts.stage(parent, candidate, selection);
        }
    }

    public void reconcileTransferCandidate(Task parent,
                                           Task activeChildBefore,
                                           Task candidateChild,
                                           boolean subTasksEqual,
                                           boolean replacementApplied,
                                           Task activeChildAfter) {
        if (attempts != null) {
            StoreDepositTransferAttemptRegistry.Reconciliation reconciliation = attempts.reconcile(
                    parent,
                    activeChildBefore,
                    candidateChild,
                    subTasksEqual,
                    replacementApplied,
                    activeChildAfter
            );
            emitTransferBegin(reconciliation.promoted());
        }
    }

    private void emitTransferBegin(StoreDepositTransferAttemptRegistry.ActiveTransfer transfer) {
        if (transfer == null) {
            return;
        }
        String operationId = transfer.state().context().operationId();
        if (!emissionGate.shouldEmitDetail(
                operationId,
                "STORE_DEPOSIT_TRANSFER_BEGIN",
                transfer.transferAttemptId()
        )) {
            return;
        }
        Object[] fields = StoreDepositEventFields.merge(
                StoreDepositEventFields.operationFields(transfer.state()),
                StoreDepositEventFields.merge(
                        transfer.identityFields(),
                        StoreDepositEventFields.merge(
                                transfer.selection().fields(),
                                StoreDepositEventFields.merge(
                                        transfer.sourceFields(),
                                        new Object[]{
                                                "transferBoundary", "BEGIN_AFTER_TASK_RECONCILIATION",
                                                "stableOrServerSnapshotAvailable", false,
                                                "durableEffect", "UNAVAILABLE",
                                                "observationComplete", false,
                                                "missingBoundaries", "PHYSICAL_SOURCE,SLOT_ACTION,SERVER_SLOT_UPDATE,POST_ACTION_STABLE",
                                                "behavior_effect", "none"
                                        }
                                )
                        )
                )
        );
        StoreDepositBoundedEventLogger.log(
                "STORE_DEPOSIT_TRANSFER_BEGIN",
                "store_deposit_transfer_begin",
                transfer.transferTask(),
                ChatClefDiagnostics.withCommandContextFields(fields)
        );
    }

    public void observeTaskLifecycle(Task task, String action, String phase) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()
                || attempts == null
                || !"STOP".equals(action)
                || !"BEGIN".equals(phase)) {
            return;
        }
        StoreDepositTransferAttemptRegistry.ActiveTransfer closed =
                attempts.closeForTaskStop(task, "TASK_STOP_BEGIN");
        if (closed == null) {
            return;
        }
        String operationId = closed.state().context().operationId();
        if (!emissionGate.shouldEmitDetail(
                operationId,
                "STORE_DEPOSIT_TRANSFER_TERMINAL",
                closed.transferAttemptId()
        )) {
            return;
        }
        Object[] fields = StoreDepositEventFields.merge(
                StoreDepositEventFields.operationFields(closed.state()),
                StoreDepositEventFields.merge(
                        closed.identityFields(),
                        StoreDepositEventFields.merge(
                                        closed.selection().fields(),
                                        StoreDepositEventFields.merge(
                                                closed.sourceFields(),
                                                closed.terminalFields("TASK_STOP_BEGIN")
                                        )
                        )
                )
        );
        StoreDepositBoundedEventLogger.log(
                "STORE_DEPOSIT_TRANSFER_TERMINAL",
                "store_deposit_transfer_terminal",
                task,
                ChatClefDiagnostics.withCommandContextFields(fields)
        );
    }

    public void logTransferDecision(Task task,
                                    BlockPos targetContainer,
                                    ItemTarget target,
                                    int potentialSourceSlotCount,
                                    boolean bestSourcePresent,
                                    boolean destinationEvaluated,
                                    boolean destinationPresent,
                                    String action) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        try {
            StoreDepositOperationState state = bindings.stateFor(task);
            if (state == null) {
                return;
            }
            state.recordTransferDecision(action);
            if (state.context().isDepositAllOperation()) {
                state.routeState().recordTransferDecision();
            }
            String operationId = StoreDepositEventFields.operationId(state);
            String key = operationId + "|" + ChatClefDiagnostics.blockPos(targetContainer) + "|" + action + "|" + String.valueOf(target);
            if (!emissionGate.shouldEmitDetail(operationId, "STORE_CONTAINER_TRANSFER_DECISION", key)) {
                return;
            }
            StoreDepositBoundedEventLogger.log("STORE_CONTAINER_TRANSFER_DECISION",
                    "store_container_transfer_decision",
                    task,
                    ChatClefDiagnostics.withCommandContextFields(
                            StoreDepositEventFields.transferDecisionFields(
                                    state,
                                    task,
                                    targetContainer,
                                    target,
                                    potentialSourceSlotCount,
                                    bestSourcePresent,
                                    destinationEvaluated,
                                    destinationPresent,
                                    action
                            )
                    ));
        } catch (RuntimeException | LinkageError ignored) {
        }
    }
}
