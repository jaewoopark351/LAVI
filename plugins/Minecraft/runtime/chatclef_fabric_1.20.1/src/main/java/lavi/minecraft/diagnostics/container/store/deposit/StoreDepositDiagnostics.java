package lavi.minecraft.diagnostics.container.store.deposit;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.container.ContainerStoredTracker;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.slots.Slot;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.binding.StoreDepositBindingRegistry;
import lavi.minecraft.diagnostics.container.store.deposit.binding.StoreDepositBindingRegistry.TrackerBinding;
import lavi.minecraft.diagnostics.container.store.deposit.budget.StoreDepositEmissionGate;
import lavi.minecraft.diagnostics.container.store.deposit.budget.StoreDepositTerminalReservation;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationState;
import lavi.minecraft.diagnostics.container.store.deposit.event.StoreDepositEventFields;
import lavi.minecraft.diagnostics.container.store.deposit.event.StoreDepositEventFields.PredicateSnapshot;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

public final class StoreDepositDiagnostics {
    private static final StoreDepositBindingRegistry BINDINGS = new StoreDepositBindingRegistry();
    private static final StoreDepositEmissionGate EMISSION_GATE = new StoreDepositEmissionGate();
    private static final ThreadLocal<PredicateSnapshot> LAST_PREDICATE_SNAPSHOT = new ThreadLocal<>();

    private StoreDepositDiagnostics() {
    }

    public static Object[] registerBareDepositInvocation(AltoClef mod,
                                                         boolean explicitItemListProvided,
                                                         ItemTarget[] selectedItems,
                                                         Task taskToRun) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return new Object[]{"storeContextAvailable", false};
        }
        try {
            StoreDepositOperationState state = BINDINGS.registerRoot(taskToRun, "BARE_DEPOSIT_COMMAND");
            state.recordRequestedTargets(selectedItems);
            return StoreDepositEventFields.merge(
                    StoreDepositEventFields.operationFields(state),
                    new Object[]{
                            "storeRequestSource", "BARE_DEPOSIT_COMMAND",
                            "storeExplicitItemListProvided", explicitItemListProvided,
                            "storeSelectedItems", ChatClefDiagnostics.itemTargets(selectedItems),
                            "storeInvocationDimension", ChatClefDiagnostics.safeValue(() -> mod == null || mod.getWorld() == null ? null : mod.getWorld().getRegistryKey().getValue())
                    });
        } catch (RuntimeException | LinkageError ignored) {
            return new Object[]{"storeContextAvailable", "unavailable#error"};
        }
    }

    public static Object[] onStoreRootStart(Task task, boolean getIfNotPresent, ItemTarget[] toStore) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return new Object[]{"storeContextAvailable", false};
        }
        try {
            StoreDepositOperationState state = BINDINGS.activateRoot(task, "STORE_IN_ANY_CONTAINER_TASK");
            state.recordRequestedTargets(toStore);
            return StoreDepositEventFields.rootActivationFields(state, toStore);
        } catch (RuntimeException | LinkageError ignored) {
            return new Object[]{"storeContextAvailable", "unavailable#error"};
        }
    }

    public static Object[] onStoreRootStopCallback(Task task, Task interruptTask) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return new Object[]{"storeContextAvailable", false};
        }
        try {
            return StoreDepositEventFields.merge(
                    StoreDepositEventFields.operationFields(BINDINGS.stateFor(task)),
                    new Object[]{
                            "storeStopCallbackOnly", true,
                            "storeStopCallbackIsTerminalAuthority", false,
                            "storeInterruptTaskInstanceId", StoreDepositEventFields.identity(interruptTask),
                            "storeInterruptTaskClass", interruptTask == null ? "none" : interruptTask.getClass().getName()
                    });
        } catch (RuntimeException | LinkageError ignored) {
            return new Object[]{"storeContextAvailable", "unavailable#error"};
        }
    }

    public static void bindRootTracker(Task owner, ContainerStoredTracker tracker) {
        try {
            BINDINGS.bindTracker(owner, tracker, "ROOT_ANY_CONTAINER", null);
        } catch (RuntimeException | LinkageError ignored) {
        }
    }

    public static void bindTargetTracker(Task owner, ContainerStoredTracker tracker, BlockPos targetContainer) {
        try {
            BINDINGS.bindTracker(owner, tracker, "TARGET_CONTAINER", targetContainer);
        } catch (RuntimeException | LinkageError ignored) {
        }
    }

    public static void markExplicitCancelCandidate(Task rootTask) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        try {
            StoreDepositOperationState state = BINDINGS.stateFor(rootTask);
            if (state != null && state.context().isRoot(rootTask)) {
                state.recordExplicitStopCorrelation();
            }
        } catch (RuntimeException | LinkageError ignored) {
        }
    }

    public static void logTaskLifecycleBoundary(Task task,
                                                Task interruptTask,
                                                String action,
                                                String phase,
                                                boolean activeBefore) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        try {
            StoreDepositOperationState state = BINDINGS.stateFor(task);
            if (state == null) {
                return;
            }
            String role = BINDINGS.roleFor(task);
            boolean rootStop = "ROOT_STORE".equals(role) && "STOP".equals(action);
            state.recordLifecycle(action, phase, rootStop);
            boolean terminalPending = rootStop && "BEGIN".equals(phase);
            boolean operationFinalized = rootStop && "END".equals(phase);
            String key = StoreDepositEventFields.operationId(state) + "|" + StoreDepositEventFields.identity(task) + "|" + action + "|" + phase;
            if (EMISSION_GATE.shouldEmitDetail("STORE_TASK_LIFECYCLE_BOUNDARY", key)) {
                ChatClefDiagnostics.logBoundary("STORE_TASK_LIFECYCLE_BOUNDARY",
                        "store_task_lifecycle_boundary",
                        task,
                        ChatClefDiagnostics.withCommandContextFields(
                                StoreDepositEventFields.lifecycleFields(state, task, interruptTask, action, phase, role, activeBefore, terminalPending, operationFinalized)
                        ));
            }
            if (operationFinalized) {
                emitTerminalSummaryAndPurge(task, state, "ROOT_STOP_END", state.explicitStopCorrelated() ? "EXPLICIT_STOP_CORRELATED" : "UNKNOWN_STOP");
            }
        } catch (RuntimeException | LinkageError ignored) {
        }
    }

    public static void logNaturalFinish(Task task) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        try {
            StoreDepositOperationState state = BINDINGS.stateFor(task);
            if (state == null || !state.context().isRoot(task)) {
                return;
            }
            state.recordNaturalFinish();
            ChatClefDiagnostics.logBoundary("STORE_TASK_LIFECYCLE_BOUNDARY",
                    "store_task_natural_finish_observed",
                    task,
                    ChatClefDiagnostics.withCommandContextFields(
                            StoreDepositEventFields.lifecycleFields(state, task, null, "NATURAL_FINISH", "OBSERVED", "ROOT_STORE", true, false, true)
                    ));
            emitTerminalSummaryAndPurge(task, state, "NATURAL_FINISH", "NATURAL_FINISH_OBSERVED");
        } catch (RuntimeException | LinkageError ignored) {
        }
    }

    public static void logChildReconciliation(Task parent,
                                              Task activeChildBefore,
                                              Task candidateChild,
                                              boolean subTasksEqual,
                                              boolean canInterruptEvaluated,
                                              boolean canInterrupt,
                                              boolean replacementApplied,
                                              boolean previousChildStopCalled,
                                              Task activeChildAfter) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        try {
            StoreDepositOperationState state = BINDINGS.stateFor(parent);
            if (state == null) {
                return;
            }
            if (activeChildAfter != null) {
                BINDINGS.bindChild(parent, activeChildAfter);
            }
            if (candidateChild != null && replacementApplied) {
                BINDINGS.bindChild(parent, candidateChild);
            }
            String outcome = replacementApplied
                    && candidateChild == null
                    ? "ACTIVE_CHILD_CLEARED"
                    : replacementApplied
                    ? "CHILD_REPLACED"
                    : subTasksEqual ? "ACTIVE_CHILD_RETAINED" : candidateChild == null ? "NULL_CHILD_RESULT" : "CANDIDATE_NOT_INSTALLED";
            String lifecycleRole = BINDINGS.roleFor(parent);
            String reconciliationRole = reconciliationRole(parent, lifecycleRole);
            state.recordChildReconciliation(reconciliationRole + ":" + outcome, activeChildAfter);
            String key = StoreDepositEventFields.operationId(state) + "|" + StoreDepositEventFields.identity(parent) + "|" + outcome + "|" + StoreDepositEventFields.identity(activeChildAfter);
            if (!EMISSION_GATE.shouldEmitDetail("STORE_TASK_CHILD_RECONCILIATION", key)) {
                return;
            }
            ChatClefDiagnostics.logBoundary("STORE_TASK_CHILD_RECONCILIATION",
                    "store_task_child_reconciliation",
                    parent,
                    ChatClefDiagnostics.withCommandContextFields(
                            StoreDepositEventFields.childReconciliationFields(
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
                                    lifecycleRole,
                                    reconciliationRole
                            )
                    ));
        } catch (RuntimeException | LinkageError ignored) {
        }
    }

    public static void logParentCandidateDecision(Task task,
                                                  String selectedBranch,
                                                  BlockPos rawClosest,
                                                  boolean closestWithinRange,
                                                  boolean currentTryWithinExtraRange,
                                                  BlockPos currentChestTry,
                                                  ItemTarget[] notStored,
                                                  Object... fields) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        try {
            StoreDepositOperationState state = BINDINGS.stateFor(task);
            if (state == null) {
                return;
            }
            state.recordParentCandidateDecision(selectedBranch);
            String key = StoreDepositEventFields.operationId(state) + "|" + selectedBranch + "|" + ChatClefDiagnostics.blockPos(rawClosest) + "|" + ChatClefDiagnostics.blockPos(currentChestTry);
            if (!EMISSION_GATE.shouldEmitDetail("STORE_CONTAINER_PARENT_CANDIDATE_DECISION", key)) {
                return;
            }
            ChatClefDiagnostics.logBoundary("STORE_CONTAINER_PARENT_CANDIDATE_DECISION",
                    "store_container_parent_candidate_decision",
                    task,
                    ChatClefDiagnostics.withCommandContextFields(
                            StoreDepositEventFields.parentCandidateDecisionFields(
                                    state,
                                    task,
                                    selectedBranch,
                                    rawClosest,
                                    closestWithinRange,
                                    currentTryWithinExtraRange,
                                    currentChestTry,
                                    notStored,
                                    fields
                            )
                    ));
        } catch (RuntimeException | LinkageError ignored) {
        }
    }

    public static void logFilteredSearchResult(Task task,
                                               Optional<BlockPos> result,
                                               Block[] targetBlocks) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        try {
            StoreDepositOperationState state = BINDINGS.stateFor(task);
            if (state == null) {
                return;
            }
            String relation = result != null && result.isPresent() ? "FILTERED_TARGET_PRESENT" : "FILTERED_TARGET_ABSENT";
            state.recordFilteredSearchResult(relation);
            String position = result == null ? "unavailable" : result.map(BlockPos::toShortString).orElse("none");
            String key = StoreDepositEventFields.operationId(state) + "|" + StoreDepositEventFields.identity(task) + "|" + relation + "|" + position;
            if (!EMISSION_GATE.shouldEmitDetail("STORE_CONTAINER_FILTERED_SEARCH_RESULT", key)) {
                return;
            }
            ChatClefDiagnostics.logBoundary("STORE_CONTAINER_FILTERED_SEARCH_RESULT",
                    "store_container_filtered_search_result",
                    task,
                    ChatClefDiagnostics.withCommandContextFields(
                            StoreDepositEventFields.filteredSearchResultFields(state, task, result, targetBlocks)
                    ));
        } catch (RuntimeException | LinkageError ignored) {
        }
    }

    public static void logPursuitDecision(Task task,
                                          Object currentPursuit,
                                          Object candidate,
                                          String returnedAction) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        try {
            StoreDepositOperationState state = BINDINGS.stateFor(task);
            if (state == null) {
                return;
            }
            state.recordPursuitDecision(returnedAction);
            String key = StoreDepositEventFields.operationId(state) + "|" + StoreDepositEventFields.identity(task) + "|" + returnedAction + "|" + String.valueOf(candidate);
            if (!EMISSION_GATE.shouldEmitDetail("STORE_CONTAINER_PURSUIT_DECISION", key)) {
                return;
            }
            ChatClefDiagnostics.logBoundary("STORE_CONTAINER_PURSUIT_DECISION",
                    "store_container_pursuit_decision",
                    task,
                    ChatClefDiagnostics.withCommandContextFields(
                            StoreDepositEventFields.pursuitDecisionFields(state, task, currentPursuit, candidate, returnedAction)
                    ));
        } catch (RuntimeException | LinkageError ignored) {
        }
    }

    public static void logTargetCallbackDecision(Task rootTask,
                                                 BlockPos callbackTarget,
                                                 BlockPos currentChestTryBefore,
                                                 boolean sameReference,
                                                 boolean progressResetBecauseReferenceChanged,
                                                 ItemTarget[] boundNotStored) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        try {
            StoreDepositOperationState state = BINDINGS.stateFor(rootTask);
            if (state == null) {
                return;
            }
            String outcome = progressResetBecauseReferenceChanged ? "REFERENCE_CHANGED_PROGRESS_RESET" : "REFERENCE_RETAINED";
            state.recordTargetCallbackDecision(outcome);
            String key = StoreDepositEventFields.operationId(state) + "|" + ChatClefDiagnostics.blockPos(callbackTarget) + "|" + outcome;
            if (!EMISSION_GATE.shouldEmitDetail("STORE_CONTAINER_TARGET_CALLBACK_DECISION", key)) {
                return;
            }
            ChatClefDiagnostics.logBoundary("STORE_CONTAINER_TARGET_CALLBACK_DECISION",
                    "store_container_target_callback_decision",
                    rootTask,
                    ChatClefDiagnostics.withCommandContextFields(
                            StoreDepositEventFields.targetCallbackDecisionFields(
                                    state,
                                    rootTask,
                                    callbackTarget,
                                    currentChestTryBefore,
                                    sameReference,
                                    progressResetBecauseReferenceChanged,
                                    boundNotStored
                            )
                    ));
        } catch (RuntimeException | LinkageError ignored) {
        }
    }

    public static void observeContainerRouteEvent(String eventName,
                                                  String reason,
                                                  Task task,
                                                  Object[] branchFields) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        try {
            StoreDepositOperationState state = BINDINGS.stateFor(task);
            if (state == null) {
                return;
            }
            state.recordCraftRouteEvent(eventName, reason);
            if (!"CONTAINER_TASK_TARGET_DECISION".equals(eventName)) {
                return;
            }
            String key = StoreDepositEventFields.operationId(state) + "|" + StoreDepositEventFields.identity(task) + "|" + eventName + "|" + reason;
            if (!EMISSION_GATE.shouldEmitDetail("STORE_CRAFT_ROUTE_EVALUATION_ENTERED", key)) {
                return;
            }
            ChatClefDiagnostics.logBoundary("STORE_CRAFT_ROUTE_EVALUATION_ENTERED",
                    "store_craft_route_evaluation_entered",
                    task,
                    ChatClefDiagnostics.withCommandContextFields(
                            StoreDepositEventFields.craftRouteEventFields(state, task, eventName, reason, branchFields)
                    ));
        } catch (RuntimeException | LinkageError ignored) {
        }
    }

    public static void logTransferDecision(Task task,
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
            StoreDepositOperationState state = BINDINGS.stateFor(task);
            if (state == null) {
                return;
            }
            state.recordTransferDecision(action);
            String key = StoreDepositEventFields.operationId(state) + "|" + ChatClefDiagnostics.blockPos(targetContainer) + "|" + action + "|" + String.valueOf(target);
            if (!EMISSION_GATE.shouldEmitDetail("STORE_CONTAINER_TRANSFER_DECISION", key)) {
                return;
            }
            ChatClefDiagnostics.logBoundary("STORE_CONTAINER_TRANSFER_DECISION",
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

    public static void clearPredicateSnapshot() {
        LAST_PREDICATE_SNAPSHOT.remove();
    }

    public static void observeTargetContainerPredicate(ContainerStoredTracker tracker,
                                                       Slot slot,
                                                       BlockPos targetContainer,
                                                       Optional<BlockPos> lastInteraction,
                                                       boolean accepted) {
        try {
            PredicateSnapshot snapshot = PredicateSnapshot.from(lastInteraction, targetContainer, accepted);
            LAST_PREDICATE_SNAPSHOT.set(snapshot);
        } catch (RuntimeException | LinkageError ignored) {
            LAST_PREDICATE_SNAPSHOT.set(PredicateSnapshot.unavailable());
        }
    }

    public static void logEffectObservation(ContainerStoredTracker tracker,
                                            Slot slot,
                                            ItemStack before,
                                            ItemStack after,
                                            boolean playerInventorySlot,
                                            boolean acceptPredicateEvaluated,
                                            boolean acceptPredicateResult) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            LAST_PREDICATE_SNAPSHOT.remove();
            return;
        }
        try {
            StoreDepositOperationState state = BINDINGS.stateFor(tracker);
            TrackerBinding binding = BINDINGS.trackerBinding(tracker);
            if (state == null) {
                LAST_PREDICATE_SNAPSHOT.remove();
                return;
            }
            PredicateSnapshot snapshot = LAST_PREDICATE_SNAPSHOT.get();
            if (snapshot == null) {
                snapshot = PredicateSnapshot.unavailable();
            }
            String outcome = effectOutcome(before, after, playerInventorySlot, acceptPredicateEvaluated, acceptPredicateResult);
            boolean expectedPositiveEffect = "TARGET_CONTAINER".equals(binding == null ? "UNBOUND" : binding.trackerRole())
                    && acceptPredicateResult
                    && positiveDeltaMatchesRequested(state, before, after);
            state.recordEffectObservation(outcome, expectedPositiveEffect);
            String key = StoreDepositEventFields.operationId(state) + "|" + (binding == null ? "UNBOUND" : binding.trackerRole()) + "|" + outcome + "|" + ChatClefDiagnostics.slotSummary(slot);
            if (EMISSION_GATE.shouldEmitDetail("STORE_CONTAINER_EFFECT_OBSERVATION", key)) {
                ChatClefDiagnostics.logBoundary("STORE_CONTAINER_EFFECT_OBSERVATION",
                        "store_container_effect_observation",
                        null,
                        ChatClefDiagnostics.withCommandContextFields(
                                StoreDepositEventFields.effectObservationFields(
                                        state,
                                        binding,
                                        tracker,
                                        slot,
                                        before,
                                        after,
                                        playerInventorySlot,
                                        acceptPredicateEvaluated,
                                        acceptPredicateResult,
                                        snapshot.matchReason(),
                                        snapshot.lastInteractionPresent(),
                                        snapshot.lastInteractionPosition(),
                                        outcome
                                )
                        ));
            }
        } catch (RuntimeException | LinkageError ignored) {
        } finally {
            LAST_PREDICATE_SNAPSHOT.remove();
        }
    }

    public static void logUserBlockRangeNullInput(Object owner, BlockPos observedPosition) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        try {
            String signature = "UserBlockRangeTracker.updateState|null_block_pos";
            StoreDepositOperationState state = null;
            if (!EMISSION_GATE.shouldEmitExceptionSignature(signature)) {
                return;
            }
            ChatClefDiagnostics.logBoundary("USER_BLOCK_RANGE_NULL_INPUT_OBSERVED",
                    "user_block_range_null_input_observed",
                    null,
                    ChatClefDiagnostics.withCommandContextFields(
                            StoreDepositEventFields.exceptionFields(state, owner, observedPosition, signature)
                    ));
        } catch (RuntimeException | LinkageError ignored) {
        }
    }

    private static void emitTerminalSummaryAndPurge(Task rootTask,
                                                    StoreDepositOperationState state,
                                                    String terminalTrigger,
                                                    String diagnosticClassification) {
        if (state == null || !state.markTerminalFinalized()) {
            return;
        }
        StoreDepositTerminalReservation reservation = EMISSION_GATE.reserveTerminalGroup(StoreDepositEventFields.operationId(state));
        if (!reservation.reserved()) {
            if (reservation.exhausted()
                    && EMISSION_GATE.shouldEmitControl(StoreDepositEventFields.operationId(state), "STORE_DEPOSIT_TERMINAL_GROUP_RESERVE_EXHAUSTED")) {
                ChatClefDiagnostics.logBoundary("STORE_DEPOSIT_TERMINAL_GROUP_RESERVE_EXHAUSTED",
                        "store_deposit_terminal_group_reserve_exhausted",
                        rootTask,
                        ChatClefDiagnostics.withCommandContextFields(
                                StoreDepositEventFields.terminalReserveExhaustedFields(state, terminalTrigger, EMISSION_GATE.budgetSummaryFields())
                        ));
            }
            BINDINGS.purgeOperation(rootTask);
            return;
        }
        Object[] budgetFields = EMISSION_GATE.budgetSummaryFields();
        ChatClefDiagnostics.logBoundary("STORE_DEPOSIT_TERMINAL_SUMMARY",
                "store_deposit_terminal_summary",
                rootTask,
                ChatClefDiagnostics.withCommandContextFields(
                        StoreDepositEventFields.terminalSummaryFields(state, terminalTrigger, diagnosticClassification, budgetFields)
                ));
        ChatClefDiagnostics.logBoundary("STORE_DEPOSIT_EFFECT_SUMMARY",
                "store_deposit_effect_summary",
                rootTask,
                ChatClefDiagnostics.withCommandContextFields(
                        StoreDepositEventFields.effectSummaryFields(state, terminalTrigger)
                ));
        ChatClefDiagnostics.logBoundary("STORE_BARITONE_OPERATION_SUMMARY",
                "store_baritone_operation_summary",
                rootTask,
                ChatClefDiagnostics.withCommandContextFields(
                        StoreDepositEventFields.baritoneSummaryFields(state, terminalTrigger)
                ));
        ChatClefDiagnostics.logBoundary("STORE_DEPOSIT_DIAGNOSTIC_COVERAGE_SUMMARY",
                "store_deposit_diagnostic_coverage_summary",
                rootTask,
                ChatClefDiagnostics.withCommandContextFields(
                        StoreDepositEventFields.coverageSummaryFields(state, terminalTrigger, budgetFields)
                ));
        BINDINGS.purgeOperation(rootTask);
    }

    private static String reconciliationRole(Task parent, String lifecycleRole) {
        if ("ROOT_STORE".equals(lifecycleRole)) {
            return "ROOT_ROUTE";
        }
        String className = parent == null ? "" : parent.getClass().getName();
        if (className.endsWith(".DoToClosestBlockTask")) {
            return "TARGET_ACTION";
        }
        if (className.contains(".tasks.container.")) {
            return "CRAFT_OR_TRANSFER_ROUTE";
        }
        return "DESCENDANT";
    }

    private static String effectOutcome(ItemStack before,
                                        ItemStack after,
                                        boolean playerInventorySlot,
                                        boolean acceptPredicateEvaluated,
                                        boolean acceptPredicateResult) {
        if (playerInventorySlot) {
            return "REJECT_PLAYER_INVENTORY_SLOT";
        }
        if (acceptPredicateEvaluated && !acceptPredicateResult) {
            return "REJECT_TARGET_PREDICATE";
        }
        if (!acceptPredicateResult) {
            return "ACCEPT_ZERO_DELTA";
        }
        if (before != null && after != null && before.getItem() != after.getItem()) {
            return "ACCEPT_ITEM_REPLACEMENT";
        }
        int delta = (after == null ? 0 : after.getCount()) - (before == null ? 0 : before.getCount());
        if (delta > 0) {
            return "ACCEPT_POSITIVE_DELTA";
        }
        if (delta < 0) {
            return "ACCEPT_NEGATIVE_DELTA";
        }
        return "ACCEPT_ZERO_DELTA";
    }

    private static boolean positiveDeltaMatchesRequested(StoreDepositOperationState state,
                                                         ItemStack before,
                                                         ItemStack after) {
        if (state == null || after == null || after.isEmpty()) {
            return false;
        }
        if (before != null && !before.isEmpty() && before.getItem() == after.getItem()) {
            return after.getCount() - before.getCount() > 0 && state.matchesRequestedItem(after.getItem());
        }
        return after.getCount() > 0 && state.matchesRequestedItem(after.getItem());
    }
}
