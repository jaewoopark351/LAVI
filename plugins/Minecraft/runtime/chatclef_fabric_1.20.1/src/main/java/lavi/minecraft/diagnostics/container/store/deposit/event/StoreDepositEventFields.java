package lavi.minecraft.diagnostics.container.store.deposit.event;

import adris.altoclef.tasks.container.ContainerStoredTracker;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.slots.Slot;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.binding.StoreDepositBindingRegistry.TrackerBinding;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationContext;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationState;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class StoreDepositEventFields {
    private StoreDepositEventFields() {
    }

    public static Object[] operationFields(StoreDepositOperationState state) {
        if (state == null) {
            return new Object[]{
                    "storeContextAvailable", false
            };
        }
        StoreDepositOperationContext context = state.context();
        Object[] fields = new Object[]{
                "storeContextAvailable", true,
                "storeOperationId", context.operationId(),
                "requestSource", context.requestSource(),
                "storeRootTaskIdentity", context.rootTaskIdentity(),
                "storeRootTaskClass", context.rootTaskClass(),
                "storeOperationStartTick", context.startTick(),
                "storeOperationStartEventSequence", context.startEventSequence()
        };
        if (!context.isAutomaticDepositOperation() || !state.automaticContext().available()) {
            return fields;
        }
        long selectedCandidateGeneration = state.routeState().selectedCandidateGeneration();
        if (selectedCandidateGeneration <= 0) {
            selectedCandidateGeneration = state.routeState().activeRouteCandidateGeneration();
        }
        return merge(fields, automaticIdentityFields(
                        state.automaticContext(),
                        context.operationId(),
                        candidateId(context.operationId(), selectedCandidateGeneration),
                        storeAttemptId(
                                context.operationId(),
                                state.routeState().activeStoreAttemptSequence()
                        ),
                        routeChildId(
                                context.operationId(),
                                state.routeState().activeStoreAttemptRouteChildLifecycleSequence()
                        ),
                        "UNAVAILABLE",
                        "UNAVAILABLE",
                        "UNAVAILABLE"
                ));
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
        Object[] automatic = context == null
                ? lavi.minecraft.diagnostics.container.store.deposit.terminal.StoreDepositAutomaticContext
                        .unavailable()
                        .fields()
                : context.fields();
        return merge(automatic, new Object[]{
                "storeOperationId", normalizeIdentity(storeOperationId),
                "selectedCandidateGenerationId", normalizeIdentity(selectedCandidateGenerationId),
                "storeAttemptId", normalizeIdentity(storeAttemptId),
                "routeChildLifecycleId", normalizeIdentity(routeChildLifecycleId),
                "transferAttemptId", normalizeIdentity(transferAttemptId),
                "slotActionId", normalizeIdentity(slotActionId),
                "slotMutationId", normalizeIdentity(slotMutationId)
        });
    }

    public static Object[] activeRouteIdentityFields(StoreDepositOperationState state) {
        if (state == null
                || state.context() == null
                || !state.context().isAutomaticDepositOperation()
                || !state.automaticContext().available()) {
            return new Object[0];
        }
        String operationId = state.context().operationId();
        return merge(
                operationFields(state),
                automaticIdentityFields(
                        state.automaticContext(),
                        operationId,
                        candidateId(operationId, state.routeState().activeRouteCandidateGeneration()),
                        storeAttemptId(operationId, state.routeState().activeStoreAttemptSequence()),
                        routeChildId(
                                operationId,
                                state.routeState().activeStoreAttemptRouteChildLifecycleSequence()
                        ),
                        "UNAVAILABLE",
                        "UNAVAILABLE",
                        "UNAVAILABLE"
                )
        );
    }

    public static Object[] rootActivationFields(StoreDepositOperationState state, ItemTarget[] targets) {
        Object[] fields = merge(operationFields(state), new Object[]{
                "storeActivationEpoch", state == null ? "unavailable" : state.activationCount(),
                "activationKind", state == null || state.activationCount() <= 1 ? "INITIAL" : "RESUME_AFTER_INTERRUPT",
                "requestedTargetItems", ChatClefDiagnostics.itemTargets(targets)
        });
        return fields;
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
        Object[] fields = merge(operationFields(state), new Object[]{
                "diagnosticScope", "store_deposit_lifecycle",
                "owner", "store_deposit_lifecycle_observer",
                "mode", "BOUNDARY",
                "trigger", action + "_" + phase,
                "dedupe_key", "store_task_lifecycle|" + operationId(state) + "|" + identity(task) + "|" + action + "|" + phase,
                "max_emission", "state_change_only,session_cap=5000",
                "correlation", "storeOperationId=" + operationId(state) + ",taskIdentity=" + identity(task),
                "payload", "flat_fields",
                "terminal", operationFinalized,
                "behavior_effect", "none",
                "lifecycleAction", action,
                "lifecyclePhase", phase,
                "taskActiveBefore", activeBefore,
                "lifecycleTaskRole", lifecycleTaskRole,
                "interruptTaskInstanceId", identity(interruptTask),
                "interruptTaskClass", interruptTask == null ? "none" : interruptTask.getClass().getName(),
                "onStopCallbackExpected", !"END".equals(phase),
                "terminalPending", terminalPending,
                "operationFinalized", operationFinalized,
                "taskSummary", ChatClefDiagnostics.taskSummaryForDiagnosticLog(task)
        });
        if (state != null
                && state.context() != null
                && state.context().isAutomaticDepositOperation()
                && "STOP".equals(action)
                && state.routeState().isCurrentRouteChild(task)) {
            return merge(fields, activeRouteIdentityFields(state));
        }
        return fields;
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
        String outcome = replacementApplied
                && candidateChild == null
                ? "ACTIVE_CHILD_CLEARED"
                : replacementApplied
                ? "CHILD_REPLACED"
                : subTasksEqual ? "ACTIVE_CHILD_RETAINED" : candidateChild == null ? "NULL_CHILD_RESULT" : "CANDIDATE_NOT_INSTALLED";
        return merge(operationFields(state), new Object[]{
                "diagnosticScope", "store_deposit_child_reconciliation",
                "owner", "store_deposit_child_reconciliation_observer",
                "mode", "BOUNDARY",
                "trigger", "task_tick_child_reconciliation",
                "dedupe_key", "store_child_reconciliation|" + operationId(state) + "|" + identity(parent) + "|" + outcome + "|" + className(candidateChild),
                "max_emission", "state_change_only,per_operation=256",
                "correlation", "storeOperationId=" + operationId(state) + ",parentTaskIdentity=" + identity(parent),
                "payload", "flat_fields",
                "terminal", false,
                "behavior_effect", "none",
                "lifecycleTaskRole", lifecycleTaskRole,
                "reconciliationRole", reconciliationRole,
                "reconciliationOutcome", outcome,
                "parentTask", ChatClefDiagnostics.taskSummaryForDiagnosticLog(parent),
                "activeChildBefore", ChatClefDiagnostics.taskSummaryForDiagnosticLog(activeChildBefore),
                "candidateChild", ChatClefDiagnostics.taskSummaryForDiagnosticLog(candidateChild),
                "activeChildAfter", ChatClefDiagnostics.taskSummaryForDiagnosticLog(activeChildAfter),
                "subTasksEqual", subTasksEqual,
                "canInterruptEvaluated", canInterruptEvaluated,
                "canInterruptPreviousChild", canInterrupt,
                "replacementApplied", replacementApplied,
                "previousChildStopCalled", previousChildStopCalled
        });
    }

    public static Object[] parentCandidateDecisionFields(StoreDepositOperationState state,
                                                         Task task,
                                                         String selectedBranch,
                                                         BlockPos rawClosest,
                                                         boolean closestWithinRange,
                                                         boolean currentTryWithinExtraRange,
                                                         BlockPos currentChestTry,
                                                         ItemTarget[] notStored,
                                                         Object[] extraFields) {
        return merge(merge(operationFields(state), new Object[]{
                "diagnosticScope", "store_deposit_parent_candidate",
                "owner", "store_deposit_parent_candidate_observer",
                "mode", "BOUNDARY",
                "trigger", selectedBranch,
                "dedupe_key", "store_parent_candidate|" + operationId(state) + "|" + selectedBranch + "|" + ChatClefDiagnostics.blockPos(rawClosest),
                "max_emission", "state_change_only,per_operation=256,total_session=4936",
                "correlation", "storeOperationId=" + operationId(state) + ",storeRootTaskIdentity=" + identity(task),
                "payload", "flat_fields",
                "terminal", false,
                "behavior_effect", "none",
                "selectedBranch", selectedBranch,
                "rawClosestContainerPresent", rawClosest != null,
                "rawClosestContainerPosition", ChatClefDiagnostics.blockPos(rawClosest),
                "closestWithinRange", closestWithinRange,
                "currentTryWithinExtraRange", currentTryWithinExtraRange,
                "currentChestTry", ChatClefDiagnostics.blockPos(currentChestTry),
                "notStoredTargets", ChatClefDiagnostics.itemTargets(notStored),
                "taskSummary", ChatClefDiagnostics.taskSummaryForDiagnosticLog(task)
        }), extraFields);
    }

    public static Object[] filteredSearchResultFields(StoreDepositOperationState state,
                                                      Task task,
                                                      Optional<BlockPos> result,
                                                      Block[] targetBlocks) {
        boolean present = result != null && result.isPresent();
        String resultPosition = result == null ? "unavailable" : result.map(BlockPos::toShortString).orElse("none");
        return merge(operationFields(state), new Object[]{
                "diagnosticScope", "store_deposit_filtered_search",
                "owner", "store_deposit_filtered_search_observer",
                "mode", "BOUNDARY",
                "trigger", present ? "FILTERED_TARGET_PRESENT" : "FILTERED_TARGET_ABSENT",
                "dedupe_key", "store_filtered_search|" + operationId(state) + "|" + identity(task) + "|" + resultPosition,
                "max_emission", "state_change_only,per_operation=256,total_session=4936",
                "correlation", "storeOperationId=" + operationId(state) + ",routeChildIdentity=" + identity(task),
                "payload", "flat_fields",
                "terminal", false,
                "behavior_effect", "none",
                "filteredResultPresent", present,
                "filteredResultPosition", result == null ? "unavailable" : result.map(ChatClefDiagnostics::blockPos).orElse("none"),
                "targetBlocks", Arrays.toString(targetBlocks),
                "taskSummary", ChatClefDiagnostics.taskSummaryForDiagnosticLog(task)
        });
    }

    public static Object[] pursuitDecisionFields(StoreDepositOperationState state,
                                                 Task task,
                                                 Object currentPursuit,
                                                 Object candidate,
                                                 String returnedAction) {
        return merge(operationFields(state), new Object[]{
                "diagnosticScope", "store_deposit_pursuit",
                "owner", "store_deposit_pursuit_observer",
                "mode", "BOUNDARY",
                "trigger", returnedAction,
                "dedupe_key", "store_pursuit|" + operationId(state) + "|" + identity(task) + "|" + returnedAction + "|" + String.valueOf(candidate),
                "max_emission", "state_change_only,per_operation=256,total_session=4936",
                "correlation", "storeOperationId=" + operationId(state) + ",routeChildIdentity=" + identity(task),
                "payload", "flat_fields",
                "terminal", false,
                "behavior_effect", "none",
                "currentPursuit", diagnosticValue(currentPursuit),
                "candidate", diagnosticValue(candidate),
                "returnedAction", returnedAction,
                "taskSummary", ChatClefDiagnostics.taskSummaryForDiagnosticLog(task)
        });
    }

    public static Object[] targetCallbackDecisionFields(StoreDepositOperationState state,
                                                        Task task,
                                                        BlockPos callbackTarget,
                                                        BlockPos currentChestTryBefore,
                                                        boolean sameReference,
                                                        boolean progressResetBecauseReferenceChanged,
                                                        ItemTarget[] boundNotStored) {
        return merge(operationFields(state), new Object[]{
                "diagnosticScope", "store_deposit_target_callback",
                "owner", "store_deposit_target_callback_observer",
                "mode", "BOUNDARY",
                "trigger", progressResetBecauseReferenceChanged ? "REFERENCE_CHANGED_PROGRESS_RESET" : "REFERENCE_RETAINED",
                "dedupe_key", "store_target_callback|" + operationId(state) + "|" + ChatClefDiagnostics.blockPos(callbackTarget) + "|" + progressResetBecauseReferenceChanged,
                "max_emission", "state_change_only,per_operation=256,total_session=4936",
                "correlation", "storeOperationId=" + operationId(state) + ",storeRootTaskIdentity=" + identity(task),
                "payload", "flat_fields",
                "terminal", false,
                "behavior_effect", "none",
                "callbackTarget", ChatClefDiagnostics.blockPos(callbackTarget),
                "currentChestTryBefore", ChatClefDiagnostics.blockPos(currentChestTryBefore),
                "filteredTargetSameReferenceAsCurrentTry", sameReference,
                "progressResetBecauseReferenceChanged", progressResetBecauseReferenceChanged,
                "activeChildBoundNotStoredTargets", ChatClefDiagnostics.itemTargets(boundNotStored),
                "taskSummary", ChatClefDiagnostics.taskSummaryForDiagnosticLog(task)
        });
    }

    public static Object[] craftRouteEventFields(StoreDepositOperationState state,
                                                 Task task,
                                                 String observedEventName,
                                                 String observedReason,
                                                 Object[] branchFields) {
        return merge(operationFields(state), new Object[]{
                "diagnosticScope", "store_deposit_craft_route",
                "owner", "store_deposit_craft_route_observer",
                "mode", "BOUNDARY",
                "trigger", observedEventName + ":" + observedReason,
                "dedupe_key", "store_craft_route|" + operationId(state) + "|" + identity(task) + "|" + observedEventName + "|" + observedReason,
                "max_emission", "state_change_only,per_operation=256,total_session=4936",
                "correlation", "storeOperationId=" + operationId(state) + ",containerTaskIdentity=" + identity(task),
                "payload", "flat_fields",
                "terminal", false,
                "behavior_effect", "none",
                "observedEventName", observedEventName,
                "observedReason", observedReason,
                "decision", diagnosticValue(field(branchFields, "decision")),
                "costToWalk", diagnosticValue(field(branchFields, "costToWalk")),
                "costToMakeNew", diagnosticValue(field(branchFields, "costToMakeNew")),
                "nearestPresent", diagnosticValue(field(branchFields, "nearestPresent")),
                "nearestPosition", diagnosticValue(field(branchFields, "nearestPosition")),
                "cachedContainerPosition", diagnosticValue(field(branchFields, "cachedContainerPosition")),
                "openTableTask", diagnosticValue(field(branchFields, "openTableTask")),
                "taskSummary", ChatClefDiagnostics.taskSummaryForDiagnosticLog(task)
        });
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
        return merge(operationFields(state), new Object[]{
                "diagnosticScope", "store_deposit_transfer",
                "owner", "store_deposit_transfer_observer",
                "mode", "BOUNDARY",
                "trigger", action,
                "dedupe_key", "store_container_transfer|" + operationId(state) + "|" + ChatClefDiagnostics.blockPos(targetContainer) + "|" + action,
                "max_emission", "state_change_only,per_operation=256",
                "correlation", "storeOperationId=" + operationId(state) + ",storeInContainerTaskIdentity=" + identity(task),
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
        });
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
                                                   String observationOutcome) {
        return effectObservationFields(
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
        Delta delta = Delta.from(before, after);
        Object[] fields = merge(operationFields(state), new Object[]{
                "diagnosticScope", "store_deposit_effect",
                "owner", "store_deposit_effect_observer",
                "mode", "BOUNDARY",
                "trigger", observationOutcome,
                "dedupe_key", "store_container_effect|" + operationId(state) + "|" + trackerRole(binding) + "|" + observationOutcome + "|" + delta.component1Item,
                "max_emission", "state_change_or_first_reason,per_operation=256",
                "correlation", "storeOperationId=" + operationId(state) + ",trackerIdentity=" + identity(tracker),
                "payload", "flat_fields",
                "terminal", false,
                "behavior_effect", "none",
                "trackerInstanceId", identity(tracker),
                "trackerRole", trackerRole(binding),
                "expectedContainerPosition", binding == null ? "unavailable" : ChatClefDiagnostics.blockPos(binding.targetContainer()),
                "slotIdentity", identity(slot),
                "slotSummary", ChatClefDiagnostics.slotSummary(slot),
                "slotStackBefore", ChatClefDiagnostics.itemStackSummary(before),
                "slotStackAfter", ChatClefDiagnostics.itemStackSummary(after),
                "slotInPlayerInventory", playerInventorySlot,
                "acceptPredicateEvaluated", acceptPredicateEvaluated,
                "acceptPredicateResult", acceptPredicateResult,
                "predicateMatchReason", predicateMatchReason,
                "predicateLastInteractionPresent", predicateLastInteractionPresent,
                "predicateLastInteractionPosition", ChatClefDiagnostics.blockPos(predicateLastInteractionPosition),
                "observationOutcome", observationOutcome,
                "beforeItem", itemName(before),
                "beforeCount", count(before),
                "afterItem", itemName(after),
                "afterCount", count(after),
                "deltaComponentCount", delta.componentCount,
                "deltaComponent1Item", delta.component1Item,
                "deltaComponent1Amount", delta.component1Amount,
                "deltaComponent2Item", delta.component2Item,
                "deltaComponent2Amount", delta.component2Amount
        });
        if (!includeAutomaticSliceFields) {
            return fields;
        }
        return merge(fields, new Object[]{
                "trackerIdentity", identity(tracker),
                "subscriptionGeneration", binding == null ? "UNAVAILABLE" : binding.subscriptionGeneration(),
                "subscriptionActiveAtMutation", binding == null
                        ? "UNAVAILABLE"
                        : binding.subscriptionActive(),
                "trackerTargetBinding", binding == null
                        ? "UNAVAILABLE"
                        : ChatClefDiagnostics.blockPos(binding.targetContainer()),
                "lastBlockPosInteractionAtEvent", ChatClefDiagnostics.blockPos(predicateLastInteractionPosition),
                "predicateEvaluated", acceptPredicateEvaluated,
                "predicateResult", acceptPredicateResult,
                "signedDelta", delta.componentCount == 1
                        ? delta.component1Amount
                        : delta.component1Amount + "," + delta.component2Amount
        });
    }

    public static Object[] terminalSummaryFields(StoreDepositOperationState state,
                                                 String terminalTrigger,
                                                 String diagnosticClassification,
                                                 Object[] budgetFields) {
        return merge(merge(operationFields(state), new Object[]{
                "diagnosticScope", "store_deposit_terminal",
                "owner", "store_deposit_terminal_summary_factory",
                "mode", "BOUNDARY",
                "trigger", terminalTrigger,
                "dedupe_key", "store_deposit_terminal_summary|" + operationId(state),
                "max_emission", "one_per_operation",
                "correlation", "storeOperationId=" + operationId(state),
                "payload", "flat_fields",
                "terminal", true,
                "behavior_effect", "none",
                "terminalTrigger", terminalTrigger,
                "durationMillis", state == null ? "unavailable" : state.elapsedMillis(),
                "activationCount", state == null ? "unavailable" : state.activationCount(),
                "interruptCount", state == null ? "unavailable" : state.interruptCount(),
                "resumeCount", state == null ? "unavailable" : state.resumeCount(),
                "trueStopObserved", state != null && state.trueStopObserved(),
                "naturalFinishObserved", state != null && state.naturalFinishObserved(),
                "explicitStopCorrelated", state != null && state.explicitStopCorrelated(),
                "lifecycleEventCount", state == null ? "unavailable" : state.lifecycleEventCount(),
                "childReconciliationCount", state == null ? "unavailable" : state.childReconciliationCount(),
                "parentCandidateDecisionCount", state == null ? "unavailable" : state.parentCandidateDecisionCount(),
                "filteredSearchResultCount", state == null ? "unavailable" : state.filteredSearchResultCount(),
                "pursuitDecisionCount", state == null ? "unavailable" : state.pursuitDecisionCount(),
                "targetCallbackDecisionCount", state == null ? "unavailable" : state.targetCallbackDecisionCount(),
                "craftRouteEventCount", state == null ? "unavailable" : state.craftRouteEventCount(),
                "transferDecisionCount", state == null ? "unavailable" : state.transferDecisionCount(),
                "effectObservationCount", state == null ? "unavailable" : state.effectObservationCount(),
                "expectedPositiveEffectCount", state == null ? "unavailable" : state.expectedPositiveEffectCount(),
                "exceptionObservationCount", state == null ? "unavailable" : state.exceptionObservationCount(),
                "boundTaskCount", state == null ? "unavailable" : state.boundTaskCount(),
                "droppedTaskBindingCount", state == null ? "unavailable" : state.droppedTaskBindingCount(),
                "trackerBindingCount", state == null ? "unavailable" : state.trackerBindingCount(),
                "droppedTrackerBindingCount", state == null ? "unavailable" : state.droppedTrackerBindingCount(),
                "diagnosticBindingEvictionCount", state == null ? "unavailable" : state.evictedOperationCount(),
                "lastLifecycleAction", state == null ? "unavailable" : state.lastLifecycleAction(),
                "lastLifecyclePhase", state == null ? "unavailable" : state.lastLifecyclePhase(),
                "finalActiveChildInstanceId", state == null ? "unavailable" : state.lastActiveChildIdentity(),
                "finalActiveChildClass", state == null ? "unavailable" : state.lastActiveChildClass(),
                "lifecycleCounts", state == null ? "unavailable" : state.lifecycleCounts(),
                "reconciliationOutcomeCountsByRole", state == null ? "unavailable" : state.reconciliationCounts(),
                "parentCandidateCounts", state == null ? "unavailable" : state.parentCandidateCounts(),
                "filteredSearchCounts", state == null ? "unavailable" : state.filteredSearchCounts(),
                "pursuitCounts", state == null ? "unavailable" : state.pursuitCounts(),
                "targetCallbackCounts", state == null ? "unavailable" : state.targetCallbackCounts(),
                "craftRouteCounts", state == null ? "unavailable" : state.craftRouteCounts(),
                "transferActionCounts", state == null ? "unavailable" : state.transferCounts(),
                "effectObservationCounts", state == null ? "unavailable" : state.effectCounts(),
                "exceptionCounts", state == null ? "unavailable" : state.exceptionCounts(),
                "effectObservationComplete", false,
                "effectObservationCompletenessAuthority", "PARTIAL_SLOT_CALLBACKS_ONLY",
                "effectVerified", false,
                "diagnosticClassification", diagnosticClassification,
                "coverageComplete", false
        }), budgetFields);
    }

    public static Object[] effectSummaryFields(StoreDepositOperationState state,
                                               String terminalTrigger) {
        return merge(operationFields(state), new Object[]{
                "diagnosticScope", "store_deposit_effect_summary",
                "owner", "store_deposit_summary_factory",
                "mode", "BOUNDARY",
                "trigger", terminalTrigger,
                "dedupe_key", "store_deposit_effect_summary|" + operationId(state),
                "max_emission", "one_per_operation",
                "correlation", "storeOperationId=" + operationId(state),
                "payload", "flat_fields",
                "terminal", true,
                "behavior_effect", "none",
                "effectObservationCount", state == null ? "unavailable" : state.effectObservationCount(),
                "expectedPositiveEffectCount", state == null ? "unavailable" : state.expectedPositiveEffectCount(),
                "effectObservationCounts", state == null ? "unavailable" : state.effectCounts(),
                "effectObservationComplete", false,
                "effectObservationCompletenessAuthority", "PARTIAL_SLOT_CALLBACKS_ONLY",
                "effectVerified", false,
                "effectVerificationAuthority", "NOT_VERIFIED_NO_DURABLE_ORACLE"
        });
    }

    public static Object[] baritoneSummaryFields(StoreDepositOperationState state,
                                                 String terminalTrigger) {
        return merge(operationFields(state), new Object[]{
                "diagnosticScope", "store_deposit_baritone_summary",
                "owner", "store_deposit_summary_factory",
                "mode", "BOUNDARY",
                "trigger", terminalTrigger,
                "dedupe_key", "store_deposit_baritone_summary|" + operationId(state),
                "max_emission", "one_per_operation",
                "correlation", "storeOperationId=" + operationId(state),
                "payload", "flat_fields",
                "terminal", true,
                "behavior_effect", "none",
                "baritoneContextImplemented", false,
                "baritoneContextCoverage", "UNOBSERVED_IN_FIRST_DIAGNOSTIC_PASS",
                "coverageComplete", false
        });
    }

    public static Object[] coverageSummaryFields(StoreDepositOperationState state,
                                                 String terminalTrigger,
                                                 Object[] budgetFields) {
        boolean depositAll = state != null && state.context().isDepositAllOperation();
        String implementedFamilies = "lifecycle,child_reconciliation,parent_candidate,filtered_search,pursuit,"
                + "target_callback,craft_route,transfer,effect,user_block_null_input"
                + (depositAll ? ",predicate_rejection_aggregate,branch_epoch,operation_budget,checkpoint" : "");
        String unimplementedFamilies = "baritone_generation_context,container_access_context,durable_effect_oracle"
                + (depositAll ? ",block_scanner_internal_filter" : "");
        return merge(merge(operationFields(state), new Object[]{
                "diagnosticScope", "store_deposit_diagnostic_coverage",
                "owner", "store_deposit_summary_factory",
                "mode", "BOUNDARY",
                "trigger", terminalTrigger,
                "dedupe_key", "store_deposit_coverage_summary|" + operationId(state),
                "max_emission", "one_per_operation",
                "correlation", "storeOperationId=" + operationId(state),
                "payload", "flat_fields",
                "terminal", true,
                "behavior_effect", "none",
                "implementedFamilies", implementedFamilies,
                "unimplementedFamilies", unimplementedFamilies,
                "lifecycleEventCount", state == null ? "unavailable" : state.lifecycleEventCount(),
                "childReconciliationCount", state == null ? "unavailable" : state.childReconciliationCount(),
                "parentCandidateDecisionCount", state == null ? "unavailable" : state.parentCandidateDecisionCount(),
                "filteredSearchResultCount", state == null ? "unavailable" : state.filteredSearchResultCount(),
                "pursuitDecisionCount", state == null ? "unavailable" : state.pursuitDecisionCount(),
                "targetCallbackDecisionCount", state == null ? "unavailable" : state.targetCallbackDecisionCount(),
                "craftRouteEventCount", state == null ? "unavailable" : state.craftRouteEventCount(),
                "transferDecisionCount", state == null ? "unavailable" : state.transferDecisionCount(),
                "effectObservationCount", state == null ? "unavailable" : state.effectObservationCount(),
                "exceptionObservationCount", state == null ? "unavailable" : state.exceptionObservationCount(),
                "coverageComplete", false
        }), budgetFields);
    }

    public static Object[] terminalReserveExhaustedFields(StoreDepositOperationState state,
                                                          String terminalTrigger,
                                                          Object[] budgetFields) {
        return merge(merge(operationFields(state), new Object[]{
                "diagnosticScope", "store_deposit_terminal_control",
                "owner", "store_deposit_critical_reserve_budget",
                "mode", "BOUNDARY",
                "trigger", terminalTrigger,
                "dedupe_key", "store_deposit_terminal_reserve_exhausted|" + operationId(state),
                "max_emission", "one_per_operation,session_control_reserve=8",
                "correlation", "storeOperationId=" + operationId(state),
                "payload", "flat_fields",
                "terminal", true,
                "behavior_effect", "none",
                "terminalGroupEmitted", false,
                "reservedTerminalSummarySlots", 0,
                "coverageComplete", false
        }), budgetFields);
    }

    public static Object[] exceptionFields(StoreDepositOperationState state,
                                           Object owner,
                                           BlockPos observedPosition,
                                           String signature) {
        return merge(operationFields(state), new Object[]{
                "diagnosticScope", "store_deposit_exception",
                "owner", "store_deposit_exception_observer",
                "mode", "BOUNDARY",
                "trigger", "null_user_block_position",
                "dedupe_key", "user_block_range_null_input|" + signature,
                "max_emission", "first_signature_only,session=16",
                "correlation", "storeOperationId=" + operationId(state),
                "payload", "flat_fields",
                "terminal", false,
                "behavior_effect", "none",
                "exceptionBoundary", owner == null ? "unavailable" : owner.getClass().getName(),
                "observedPosition", ChatClefDiagnostics.blockPos(observedPosition),
                "exceptionSignature", signature,
                "threadName", Thread.currentThread().getName()
        });
    }

    public static Object[] merge(Object[] first, Object[] second) {
        if (first == null || first.length == 0) {
            return second == null ? new Object[0] : second;
        }
        if (second == null || second.length == 0) {
            return first;
        }
        if ((first.length & 1) != 0 || (second.length & 1) != 0) {
            Object[] concatenated = new Object[first.length + second.length];
            System.arraycopy(first, 0, concatenated, 0, first.length);
            System.arraycopy(second, 0, concatenated, first.length, second.length);
            return concatenated;
        }
        Map<Object, Object> fields = new LinkedHashMap<>();
        putFields(fields, first);
        putFields(fields, second);
        Object[] merged = new Object[fields.size() * 2];
        int index = 0;
        for (Map.Entry<Object, Object> entry : fields.entrySet()) {
            merged[index++] = entry.getKey();
            merged[index++] = entry.getValue();
        }
        return merged;
    }

    private static void putFields(Map<Object, Object> target, Object[] fields) {
        for (int index = 0; index + 1 < fields.length; index += 2) {
            target.put(fields[index], fields[index + 1]);
        }
    }

    private static String normalizeIdentity(String value) {
        return value == null || value.isBlank() ? "UNAVAILABLE" : value;
    }

    private static String candidateId(String operationId, long generation) {
        return generation <= 0 ? "UNAVAILABLE" : operationId + "-candidate-" + generation;
    }

    private static String storeAttemptId(String operationId, long sequence) {
        return sequence <= 0 ? "UNAVAILABLE" : operationId + "-attempt-" + sequence;
    }

    private static String routeChildId(String operationId, long sequence) {
        return sequence <= 0 ? "UNAVAILABLE" : operationId + "-route-child-" + sequence;
    }

    public static String operationId(StoreDepositOperationState state) {
        return state == null ? "unavailable" : state.context().operationId();
    }

    public static String identity(Object value) {
        return StoreDepositOperationContext.identity(value);
    }

    private static String className(Object value) {
        return value == null ? "none" : value.getClass().getName();
    }

    private static String trackerRole(TrackerBinding binding) {
        return binding == null ? "UNBOUND" : binding.trackerRole();
    }

    private static String itemName(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "empty";
        }
        return String.valueOf(stack.getItem());
    }

    private static int count(ItemStack stack) {
        return stack == null ? 0 : stack.getCount();
    }

    private static Object field(Object[] fields, String name) {
        if (fields == null || name == null) {
            return null;
        }
        for (int i = 0; i + 1 < fields.length; i += 2) {
            if (name.equals(fields[i])) {
                return fields[i + 1];
            }
        }
        return null;
    }

    private static String diagnosticValue(Object value) {
        return value == null ? "unavailable" : String.valueOf(value);
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

    private record Delta(int componentCount,
                         String component1Item,
                         int component1Amount,
                         String component2Item,
                         int component2Amount) {
        private static Delta from(ItemStack before, ItemStack after) {
            boolean beforeEmpty = before == null || before.isEmpty();
            boolean afterEmpty = after == null || after.isEmpty();
            if (beforeEmpty && afterEmpty) {
                return new Delta(0, "UNAVAILABLE", 0, "UNAVAILABLE", 0);
            }
            if (!beforeEmpty && !afterEmpty && before.getItem() != after.getItem()) {
                return new Delta(2, itemName(before), -count(before), itemName(after), count(after));
            }
            ItemStack reference = afterEmpty ? before : after;
            int amount = count(after) - count(before);
            return new Delta(1, itemName(reference), amount, "UNAVAILABLE", 0);
        }
    }
}
