package lavi.minecraft.diagnostics.container.store.deposit.event;

import adris.altoclef.tasks.container.ContainerStoredTracker;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.slots.Slot;
import lavi.minecraft.diagnostics.container.store.deposit.binding.StoreDepositBindingRegistry.TrackerBinding;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationState;
import lavi.minecraft.diagnostics.container.store.deposit.event.common.StoreDepositOperationCorrelationFields;
import lavi.minecraft.diagnostics.container.store.deposit.event.common.StoreDepositOrderedFieldSupport;
import lavi.minecraft.diagnostics.container.store.deposit.event.effect.StoreDepositEffectEventFields;
import lavi.minecraft.diagnostics.container.store.deposit.event.exception.StoreDepositExceptionEventFields;
import lavi.minecraft.diagnostics.container.store.deposit.event.lifecycle.StoreDepositLifecycleEventFields;
import lavi.minecraft.diagnostics.container.store.deposit.event.route.StoreDepositRouteEventFields;
import lavi.minecraft.diagnostics.container.store.deposit.event.terminal.StoreDepositTerminalEventFields;
import lavi.minecraft.diagnostics.container.store.deposit.event.transfer.StoreDepositTransferEventFields;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

public final class StoreDepositEventFields {
    private StoreDepositEventFields() {
    }

    //20260902_kpopmodder: Preserve operation and correlation public entry points through common delegates.
    public static Object[] operationFields(StoreDepositOperationState state) {
        return StoreDepositOperationCorrelationFields.operationFields(state);
    }

    public static Object[] automaticIdentityFields(
            lavi.minecraft.diagnostics.container.store.deposit.terminal.StoreDepositAutomaticContext context,
            String storeOperationId,
            String selectedCandidateGenerationId,
            String storeAttemptId,
            String routeChildLifecycleId,
            String transferAttemptId,
            String slotActionId,
            String slotMutationId) {
        return StoreDepositOperationCorrelationFields.automaticIdentityFields(
                context,
                storeOperationId,
                selectedCandidateGenerationId,
                storeAttemptId,
                routeChildLifecycleId,
                transferAttemptId,
                slotActionId,
                slotMutationId
        );
    }

    public static Object[] activeRouteIdentityFields(StoreDepositOperationState state) {
        return StoreDepositOperationCorrelationFields.activeRouteIdentityFields(state);
    }

    //20260902_kpopmodder: Preserve lifecycle event public entry points through a narrow delegate.
    public static Object[] rootActivationFields(StoreDepositOperationState state, ItemTarget[] targets) {
        return StoreDepositLifecycleEventFields.rootActivationFields(state, targets);
    }

    public static Object[] lifecycleFields(StoreDepositOperationState state,
                                           Task task,
                                           Task interruptTask,
                                           String action,
                                           String phase,
                                           String lifecycleTaskRole,
                                           boolean activeBefore,
                                           boolean terminalPending,
                                           boolean operationFinalized) {
        return StoreDepositLifecycleEventFields.lifecycleFields(
                state,
                task,
                interruptTask,
                action,
                phase,
                lifecycleTaskRole,
                activeBefore,
                terminalPending,
                operationFinalized
        );
    }

    public static Object[] childReconciliationFields(StoreDepositOperationState state,
                                                     Task parent,
                                                     Task activeChildBefore,
                                                     Task candidateChild,
                                                     Task activeChildAfter,
                                                     boolean subTasksEqual,
                                                     boolean canInterruptEvaluated,
                                                     boolean canInterrupt,
                                                     boolean replacementApplied,
                                                     boolean previousChildStopCalled,
                                                     String lifecycleTaskRole,
                                                     String reconciliationRole) {
        return StoreDepositLifecycleEventFields.childReconciliationFields(
                state,
                parent,
                activeChildBefore,
                candidateChild,
                activeChildAfter,
                subTasksEqual,
                canInterruptEvaluated,
                canInterrupt,
                replacementApplied,
                previousChildStopCalled,
                lifecycleTaskRole,
                reconciliationRole
        );
    }

    //20260902_kpopmodder: Preserve route event public entry points through a narrow delegate.
    public static Object[] parentCandidateDecisionFields(StoreDepositOperationState state,
                                                         Task task,
                                                         String selectedBranch,
                                                         BlockPos rawClosest,
                                                         boolean closestWithinRange,
                                                         boolean currentTryWithinExtraRange,
                                                         BlockPos currentChestTry,
                                                         ItemTarget[] notStored,
                                                         Object[] extraFields) {
        return StoreDepositRouteEventFields.parentCandidateDecisionFields(
                state,
                task,
                selectedBranch,
                rawClosest,
                closestWithinRange,
                currentTryWithinExtraRange,
                currentChestTry,
                notStored,
                extraFields
        );
    }

    public static Object[] filteredSearchResultFields(StoreDepositOperationState state,
                                                      Task task,
                                                      Optional<BlockPos> result,
                                                      Block[] targetBlocks) {
        return StoreDepositRouteEventFields.filteredSearchResultFields(
                state,
                task,
                result,
                targetBlocks
        );
    }

    public static Object[] pursuitDecisionFields(StoreDepositOperationState state,
                                                 Task task,
                                                 Object currentPursuit,
                                                 Object candidate,
                                                 String returnedAction) {
        return StoreDepositRouteEventFields.pursuitDecisionFields(
                state,
                task,
                currentPursuit,
                candidate,
                returnedAction
        );
    }

    public static Object[] targetCallbackDecisionFields(StoreDepositOperationState state,
                                                        Task task,
                                                        BlockPos callbackTarget,
                                                        BlockPos currentChestTryBefore,
                                                        boolean sameReference,
                                                        boolean progressResetBecauseReferenceChanged,
                                                        ItemTarget[] boundNotStored) {
        return StoreDepositRouteEventFields.targetCallbackDecisionFields(
                state,
                task,
                callbackTarget,
                currentChestTryBefore,
                sameReference,
                progressResetBecauseReferenceChanged,
                boundNotStored
        );
    }

    public static Object[] craftRouteEventFields(StoreDepositOperationState state,
                                                 Task task,
                                                 String observedEventName,
                                                 String observedReason,
                                                 Object[] branchFields) {
        return StoreDepositRouteEventFields.craftRouteEventFields(
                state,
                task,
                observedEventName,
                observedReason,
                branchFields
        );
    }

    //20260902_kpopmodder: Preserve the transfer event public entry point through a narrow delegate.
    public static Object[] transferDecisionFields(StoreDepositOperationState state,
                                                  Task task,
                                                  BlockPos targetContainer,
                                                  ItemTarget target,
                                                  int potentialSourceSlotCount,
                                                  boolean bestSourcePresent,
                                                  boolean destinationEvaluated,
                                                  boolean destinationPresent,
                                                  String action) {
        return StoreDepositTransferEventFields.transferDecisionFields(
                state,
                task,
                targetContainer,
                target,
                potentialSourceSlotCount,
                bestSourcePresent,
                destinationEvaluated,
                destinationPresent,
                action
        );
    }

    //20260902_kpopmodder: Preserve effect event public entry points through a narrow delegate.
    public static Object[] effectObservationFields(StoreDepositOperationState state,
                                                   TrackerBinding binding,
                                                   ContainerStoredTracker tracker,
                                                   Slot slot,
                                                   ItemStack before,
                                                   ItemStack after,
                                                   boolean playerInventorySlot,
                                                   boolean acceptPredicateEvaluated,
                                                   boolean acceptPredicateResult,
                                                   String predicateMatchReason,
                                                   boolean predicateLastInteractionPresent,
                                                   BlockPos predicateLastInteractionPosition,
                                                   String observationOutcome) {
        return StoreDepositEffectEventFields.effectObservationFields(
                state,
                binding,
                tracker,
                slot,
                before,
                after,
                playerInventorySlot,
                acceptPredicateEvaluated,
                acceptPredicateResult,
                predicateMatchReason,
                predicateLastInteractionPresent,
                predicateLastInteractionPosition,
                observationOutcome,
                true
        );
    }

    public static Object[] effectObservationFields(StoreDepositOperationState state,
                                                   TrackerBinding binding,
                                                   ContainerStoredTracker tracker,
                                                   Slot slot,
                                                   ItemStack before,
                                                   ItemStack after,
                                                   boolean playerInventorySlot,
                                                   boolean acceptPredicateEvaluated,
                                                   boolean acceptPredicateResult,
                                                   String predicateMatchReason,
                                                   boolean predicateLastInteractionPresent,
                                                   BlockPos predicateLastInteractionPosition,
                                                   String observationOutcome,
                                                   boolean includeAutomaticSliceFields) {
        return StoreDepositEffectEventFields.effectObservationFields(
                state,
                binding,
                tracker,
                slot,
                before,
                after,
                playerInventorySlot,
                acceptPredicateEvaluated,
                acceptPredicateResult,
                predicateMatchReason,
                predicateLastInteractionPresent,
                predicateLastInteractionPosition,
                observationOutcome,
                includeAutomaticSliceFields
        );
    }

    //20260902_kpopmodder: Preserve terminal summary public entry points through a narrow delegate.
    public static Object[] terminalSummaryFields(StoreDepositOperationState state,
                                                 String terminalTrigger,
                                                 String diagnosticClassification,
                                                 Object[] budgetFields) {
        return StoreDepositTerminalEventFields.terminalSummaryFields(
                state,
                terminalTrigger,
                diagnosticClassification,
                budgetFields
        );
    }

    public static Object[] effectSummaryFields(StoreDepositOperationState state,
                                               String terminalTrigger) {
        return StoreDepositEffectEventFields.effectSummaryFields(state, terminalTrigger);
    }

    public static Object[] baritoneSummaryFields(StoreDepositOperationState state,
                                                 String terminalTrigger) {
        return StoreDepositTerminalEventFields.baritoneSummaryFields(state, terminalTrigger);
    }

    public static Object[] coverageSummaryFields(StoreDepositOperationState state,
                                                 String terminalTrigger,
                                                 Object[] budgetFields) {
        return StoreDepositTerminalEventFields.coverageSummaryFields(
                state,
                terminalTrigger,
                budgetFields
        );
    }

    public static Object[] terminalReserveExhaustedFields(StoreDepositOperationState state,
                                                          String terminalTrigger,
                                                          Object[] budgetFields) {
        return StoreDepositTerminalEventFields.terminalReserveExhaustedFields(
                state,
                terminalTrigger,
                budgetFields
        );
    }

    //20260902_kpopmodder: Preserve the exception event public entry point through a narrow delegate.
    public static Object[] exceptionFields(StoreDepositOperationState state,
                                           Object owner,
                                           BlockPos observedPosition,
                                           String signature) {
        return StoreDepositExceptionEventFields.exceptionFields(
                state,
                owner,
                observedPosition,
                signature
        );
    }

    //20260902_kpopmodder: Preserve ordered merge and identity public entry points through common delegates.
    public static Object[] merge(Object[] first, Object[] second) {
        return StoreDepositOrderedFieldSupport.merge(first, second);
    }

    public static String operationId(StoreDepositOperationState state) {
        return StoreDepositOperationCorrelationFields.operationId(state);
    }

    public static String identity(Object value) {
        return StoreDepositOperationCorrelationFields.identity(value);
    }

    public record PredicateSnapshot(String matchReason,
                                    boolean lastInteractionPresent,
                                    BlockPos lastInteractionPosition) {
        public static PredicateSnapshot unavailable() {
            return new PredicateSnapshot("UNAVAILABLE", false, null);
        }

        public static PredicateSnapshot from(Optional<BlockPos> lastInteraction,
                                             BlockPos targetContainer,
                                             boolean accepted) {
            if (lastInteraction == null || lastInteraction.isEmpty()) {
                return new PredicateSnapshot("LAST_INTERACTION_ABSENT", false, null);
            }
            BlockPos position = lastInteraction.get();
            return new PredicateSnapshot(accepted ? "MATCH" : "POSITION_MISMATCH", true, position);
        }
    }

}
