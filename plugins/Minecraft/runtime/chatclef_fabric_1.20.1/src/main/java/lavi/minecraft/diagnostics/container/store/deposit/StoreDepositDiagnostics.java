package lavi.minecraft.diagnostics.container.store.deposit;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.container.ContainerStoredTracker;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.slots.Slot;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.binding.StoreDepositBindingRegistry;
import lavi.minecraft.diagnostics.container.store.deposit.budget.StoreDepositBoundedEventLogger;
import lavi.minecraft.diagnostics.container.store.deposit.budget.StoreDepositEmissionGate;
import lavi.minecraft.diagnostics.container.store.deposit.budget.StoreDepositSharedBudget;
import lavi.minecraft.diagnostics.container.store.deposit.candidate.StoreContainerCandidateCollector;
import lavi.minecraft.diagnostics.container.store.deposit.candidate.StoreContainerCandidateEventFields;
import lavi.minecraft.diagnostics.container.store.deposit.candidate.StoreContainerCandidateObservation;
import lavi.minecraft.diagnostics.container.store.deposit.candidate.StoreContainerCandidateRejectionReason;
import lavi.minecraft.diagnostics.container.store.deposit.candidate.StoreContainerParentDecision;
import lavi.minecraft.diagnostics.container.store.deposit.candidate.StoreContainerRouteCheckpoint;
import lavi.minecraft.diagnostics.container.store.deposit.candidate.range.StoreContainerRangeEventFields;
import lavi.minecraft.diagnostics.container.store.deposit.candidate.range.StoreDepositUserBlockRangeNullDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationState;
import lavi.minecraft.diagnostics.container.store.deposit.event.StoreDepositEventFields;
import lavi.minecraft.diagnostics.container.store.deposit.effect.StoreDepositEffectDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.interaction.StoreDepositInteractionBindingRegistry;
import lavi.minecraft.diagnostics.container.store.deposit.interaction.StoreDepositInteractionDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.lifecycle.StoreDepositChildReconciliationDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.lifecycle.StoreDepositOperationRegistrationDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.lifecycle.StoreDepositRootLifecycleDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.lifecycle.StoreDepositTaskLifecycleDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.lifecycle.StoreDepositTrackerBindingDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.route.StoreDepositContainerRouteEventDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.route.StoreDepositPursuitDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.route.StoreDepositMovementDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.route.StoreDepositTargetCallbackDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.session.StoreDepositModeStateInvalidator;
import lavi.minecraft.diagnostics.container.store.deposit.session.mode.StoreDepositModeLifecycleObserver;
import lavi.minecraft.diagnostics.container.store.deposit.session.snapshot.StoreDepositSessionSnapshotContributor;
import lavi.minecraft.diagnostics.container.store.deposit.session.snapshot.StoreDepositSessionStateSnapshotReader;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.StoreDepositTerminalSummaryEmitter;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.accounting.StoreDepositTerminalLedger;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.StoreDepositAutomaticLifecycleLedger;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.StoreDepositAutomaticLifecycleState;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.StoreDepositAutomaticContext;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.StoreDepositAutomaticTerminalDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.transfer.StoreDepositTransferDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.transfer.StoreDepositTransferAttemptRegistry;
import lavi.minecraft.diagnostics.container.store.deposit.transfer.StoreDepositTransferSelectionSnapshot;
import lavi.minecraft.diagnostics.container.store.deposit.transfer.StoreDepositSlotActionDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.transfer.StoreDepositNotStoredDiagnostics;
import net.minecraft.screen.slot.SlotActionType;
import lavi.minecraft.diagnostics.interaction.BlockInteractionContext;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;
import java.util.function.Supplier;

public final class StoreDepositDiagnostics {
    private static final StoreDepositBindingRegistry BINDINGS = new StoreDepositBindingRegistry();
    private static final StoreDepositEmissionGate EMISSION_GATE = StoreDepositSharedBudget.emissionGate();
    private static final StoreDepositAutomaticLifecycleLedger AUTOMATIC_LEDGER =
            StoreDepositAutomaticLifecycleState.ledger();
    private static final StoreDepositAutomaticTerminalDiagnostics AUTOMATIC_TERMINALS =
            new StoreDepositAutomaticTerminalDiagnostics(AUTOMATIC_LEDGER, EMISSION_GATE);
    private static final StoreDepositTerminalLedger TERMINAL_LEDGER = new StoreDepositTerminalLedger();
    private static final StoreDepositTerminalSummaryEmitter TERMINAL_SUMMARIES =
            new StoreDepositTerminalSummaryEmitter(
                    BINDINGS,
                    EMISSION_GATE,
                    AUTOMATIC_TERMINALS,
                    TERMINAL_LEDGER
            );
    private static final StoreDepositInteractionBindingRegistry INTERACTION_BINDINGS =
            new StoreDepositInteractionBindingRegistry();
    private static final StoreDepositInteractionDiagnostics INTERACTIONS =
            new StoreDepositInteractionDiagnostics(
                    BINDINGS,
                    EMISSION_GATE,
                    INTERACTION_BINDINGS
            );
    private static final StoreDepositOperationRegistrationDiagnostics OPERATION_REGISTRATION =
            new StoreDepositOperationRegistrationDiagnostics(BINDINGS, AUTOMATIC_TERMINALS);
    private static final StoreDepositRootLifecycleDiagnostics ROOT_LIFECYCLE =
            new StoreDepositRootLifecycleDiagnostics(BINDINGS);
    private static final StoreDepositTrackerBindingDiagnostics TRACKER_BINDINGS =
            new StoreDepositTrackerBindingDiagnostics(BINDINGS);
    private static final StoreDepositTaskLifecycleDiagnostics TASK_LIFECYCLE =
            new StoreDepositTaskLifecycleDiagnostics(BINDINGS, EMISSION_GATE, TERMINAL_SUMMARIES);
    private static final StoreDepositMovementDiagnostics MOVEMENTS =
            new StoreDepositMovementDiagnostics(BINDINGS, EMISSION_GATE, AUTOMATIC_TERMINALS);
    private static final StoreDepositTransferAttemptRegistry TRANSFER_ATTEMPTS =
            new StoreDepositTransferAttemptRegistry(BINDINGS, AUTOMATIC_TERMINALS);
    private static final StoreDepositSlotActionDiagnostics SLOT_ACTIONS =
            new StoreDepositSlotActionDiagnostics(
                    TRANSFER_ATTEMPTS,
                    EMISSION_GATE,
                    AUTOMATIC_TERMINALS
            );
    private static final StoreDepositNotStoredDiagnostics NOT_STORED =
            new StoreDepositNotStoredDiagnostics(BINDINGS, EMISSION_GATE);
    private static final StoreDepositTransferDiagnostics TRANSFERS =
            new StoreDepositTransferDiagnostics(BINDINGS, EMISSION_GATE, TRANSFER_ATTEMPTS);
    private static final StoreDepositChildReconciliationDiagnostics CHILD_RECONCILIATIONS =
            new StoreDepositChildReconciliationDiagnostics(BINDINGS, EMISSION_GATE, MOVEMENTS, TRANSFERS);
    private static final StoreDepositEffectDiagnostics EFFECTS =
            new StoreDepositEffectDiagnostics(BINDINGS, EMISSION_GATE, SLOT_ACTIONS);
    private static final StoreDepositPursuitDiagnostics PURSUITS =
            new StoreDepositPursuitDiagnostics(BINDINGS, EMISSION_GATE);
    private static final StoreDepositTargetCallbackDiagnostics TARGET_CALLBACKS =
            new StoreDepositTargetCallbackDiagnostics(BINDINGS, EMISSION_GATE);
    private static final StoreDepositContainerRouteEventDiagnostics ROUTE_EVENTS =
            new StoreDepositContainerRouteEventDiagnostics(BINDINGS, EMISSION_GATE);
    private static final StoreDepositUserBlockRangeNullDiagnostics USER_BLOCK_RANGE_NULL_INPUTS =
            new StoreDepositUserBlockRangeNullDiagnostics(EMISSION_GATE);

    static {
        StoreDepositModeStateInvalidator invalidator =
                new StoreDepositModeStateInvalidator(
                        BINDINGS,
                        INTERACTIONS,
                        TRANSFER_ATTEMPTS,
                        SLOT_ACTIONS,
                        EFFECTS,
                        EMISSION_GATE,
                        AUTOMATIC_LEDGER
                );
        ChatClefDiagnostics.registerSessionLifecycleObserver(
                new StoreDepositModeLifecycleObserver(
                        invalidator,
                        TERMINAL_LEDGER
                )
        );
        ChatClefDiagnostics.registerSessionLifecycleObserver(
                new StoreDepositSessionSnapshotContributor(
                        new StoreDepositSessionStateSnapshotReader(
                                BINDINGS,
                                AUTOMATIC_LEDGER
                        ),
                        TERMINAL_LEDGER
                )
        );
    }

    private StoreDepositDiagnostics() {
    }

    public static void beginAutomaticRun(Task maintenanceTask,
                                         Task userTaskRoot,
                                         long policyContextEpoch,
                                         Task primaryChild) {
        runEligible(() -> {
            try {
                AUTOMATIC_TERMINALS.beginRun(maintenanceTask, userTaskRoot, policyContextEpoch);
                if (primaryChild != null) {
                    AUTOMATIC_TERMINALS.registerChild(maintenanceTask, primaryChild, 0);
                }
            } catch (RuntimeException | LinkageError ignored) {
            }
        });
    }

    public static void registerAutomaticMaintenanceChild(AltoClef mod,
                                                         Task maintenanceTask,
                                                         Task childTask,
                                                         int childIndex,
                                                         ItemTarget[] selectedItems) {
        runEligible(() -> {
            try {
                StoreDepositOperationState existing = BINDINGS.stateFor(childTask);
                if (existing != null
                        && existing.context().isAutomaticDepositOperation()
                        && existing.automaticContext().available()) {
                    return;
                }
                StoreDepositAutomaticContext automaticContext =
                        AUTOMATIC_TERMINALS.registerChild(maintenanceTask, childTask, childIndex);
                OPERATION_REGISTRATION.registerAutomaticChild(
                        automaticContext,
                        selectedItems,
                        childTask
                );
            } catch (RuntimeException | LinkageError ignored) {
            }
        });
    }

    public static void recordAutomaticMaintenanceTerminal(Task maintenanceTask,
                                                          String terminalReason,
                                                          String nextLifecycleState) {
        runEligible(() -> {
            try {
                AUTOMATIC_TERMINALS.recordMaintenanceTerminal(
                        maintenanceTask,
                        terminalReason,
                        nextLifecycleState
                );
            } catch (RuntimeException | LinkageError ignored) {
            }
        });
    }

    public static void recordAutomaticPressureRunClosed(Task maintenanceTask,
                                                       String terminalReason,
                                                       String nextLifecycleState) {
        runEligible(() -> {
            try {
                AUTOMATIC_TERMINALS.recordPressureRunClosed(maintenanceTask, terminalReason);
            } catch (RuntimeException | LinkageError ignored) {
            }
            try {
                AUTOMATIC_TERMINALS.recordRunToWait(
                        maintenanceTask,
                        terminalReason,
                        nextLifecycleState
                );
            } catch (RuntimeException | LinkageError ignored) {
            }
            try {
                AUTOMATIC_TERMINALS.recordCoverageClosedIfNoUserTask(
                        maintenanceTask,
                        terminalReason
                );
            } catch (RuntimeException | LinkageError ignored) {
            }
        });
    }

    public static void bindInteraction(Task activeTask, BlockInteractionContext interaction) {
        runEligible(() -> INTERACTIONS.bindInteraction(activeTask, interaction));
    }

    public static Object[] interactionFields(BlockInteractionContext interaction) {
        return callEligible(
                () -> INTERACTIONS.interactionFields(interaction),
                new Object[]{"storeContextAvailable", false}
        );
    }

    public static String interactionScopeKey(BlockInteractionContext interaction) {
        return callEligible(() -> INTERACTIONS.interactionScopeKey(interaction), "store-unbound");
    }

    public static boolean shouldEmitInteractionDetail(BlockInteractionContext interaction,
                                                      String eventName,
                                                      String semanticKey) {
        return callEligible(
                () -> INTERACTIONS.shouldEmitInteractionDetail(interaction, eventName, semanticKey),
                false
        );
    }

    public static Object[] registerBareDepositInvocation(AltoClef mod,
                                                         boolean explicitItemListProvided,
                                                         ItemTarget[] selectedItems,
                                                         Task taskToRun) {
        return registerBareDepositInvocation(
                mod,
                explicitItemListProvided,
                selectedItems,
                taskToRun,
                "BARE_DEPOSIT_COMMAND"
        );
    }

    public static Object[] registerBareDepositInvocation(AltoClef mod,
                                                         boolean explicitItemListProvided,
                                                         ItemTarget[] selectedItems,
                                                         Task taskToRun,
                                                         String requestSource) {
        return callEligible(
                () -> OPERATION_REGISTRATION.registerBareDepositInvocation(
                        mod,
                        explicitItemListProvided,
                        selectedItems,
                        taskToRun,
                        requestSource
                ),
                new Object[0]
        );
    }

    public static Object[] onStoreRootStart(Task task, boolean getIfNotPresent, ItemTarget[] toStore) {
        return callEligible(
                () -> ROOT_LIFECYCLE.onStoreRootStart(task, getIfNotPresent, toStore),
                new Object[0]
        );
    }

    public static Object[] onStoreRootStopCallback(Task task, Task interruptTask) {
        return callEligible(
                () -> ROOT_LIFECYCLE.onStoreRootStopCallback(task, interruptTask),
                new Object[0]
        );
    }

    public static void bindRootTracker(Task owner, ContainerStoredTracker tracker) {
        runEligible(() -> TRACKER_BINDINGS.bindRootTracker(owner, tracker));
    }

    public static void bindTargetTracker(Task owner, ContainerStoredTracker tracker, BlockPos targetContainer) {
        runEligible(() -> TRACKER_BINDINGS.bindTargetTracker(owner, tracker, targetContainer));
    }

    public static void trackerSubscriptionStarted(ContainerStoredTracker tracker) {
        runEligible(() -> TRACKER_BINDINGS.trackerSubscriptionStarted(tracker));
    }

    public static void trackerSubscriptionStopped(ContainerStoredTracker tracker) {
        runEligible(() -> TRACKER_BINDINGS.trackerSubscriptionStopped(tracker));
    }

    public static boolean hasAutomaticTrackerContext(ContainerStoredTracker tracker) {
        return callEligible(() -> {
            try {
                return EFFECTS.hasAutomaticContext(tracker);
            } catch (RuntimeException | LinkageError ignored) {
                return false;
            }
        }, false);
    }

    public static boolean hasAutomaticTaskContext(Task task) {
        return callEligible(() -> {
            try {
                StoreDepositOperationState state = BINDINGS.stateFor(task);
                return state != null
                        && state.context() != null
                        && state.context().isAutomaticDepositOperation()
                        && state.automaticContext().available();
            } catch (RuntimeException | LinkageError ignored) {
                return false;
            }
        }, false);
    }

    public static void markExplicitCancelCandidate(Task rootTask) {
        runEligible(() -> ROOT_LIFECYCLE.markExplicitCancelCandidate(rootTask));
    }

    public static void logTaskLifecycleBoundary(Task task,
                                                Task interruptTask,
                                                String action,
                                                String phase,
                                                boolean activeBefore) {
        runEligible(() -> {
            TASK_LIFECYCLE.logTaskLifecycleBoundary(
                    task,
                    interruptTask,
                    action,
                    phase,
                    activeBefore
            );
            try {
                TRANSFERS.observeTaskLifecycle(task, action, phase);
            } catch (RuntimeException | LinkageError ignored) {
            }
            try {
                MOVEMENTS.observeTaskLifecycle(task, action, phase);
            } catch (RuntimeException | LinkageError ignored) {
            }
        });
    }

    public static void logNaturalFinish(Task task) {
        runEligible(() -> {
            TASK_LIFECYCLE.logNaturalFinish(task);
            try {
                AUTOMATIC_TERMINALS.observeUserTaskNaturalCompletion(task);
            } catch (RuntimeException | LinkageError ignored) {
            }
        });
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
        runEligible(() -> {
            CHILD_RECONCILIATIONS.logChildReconciliation(
                    parent,
                    activeChildBefore,
                    candidateChild,
                    subTasksEqual,
                    canInterruptEvaluated,
                    canInterrupt,
                    replacementApplied,
                    previousChildStopCalled,
                    activeChildAfter
            );
            try {
                AUTOMATIC_TERMINALS.observeUserTaskResume(parent);
            } catch (RuntimeException | LinkageError ignored) {
            }
        });
    }

    public static void logParentCandidateDecision(Task task,
                                                   String selectedBranch,
                                                   BlockPos rawClosest,
                                                   boolean closestWithinRange,
                                                   boolean currentTryWithinExtraRange,
                                                   BlockPos currentChestTry,
                                                   ItemTarget[] notStored,
                                                   Object... fields) {
        runEligible(() -> logParentCandidateDecisionInternal(
                    task,
                    selectedBranch,
                    true,
                    rawClosest,
                    rawClosest != null,
                    closestWithinRange,
                    true,
                    currentTryWithinExtraRange,
                    currentChestTry,
                    notStored,
                    fields
        ));
    }

    public static void logDepositAllParentCandidateDecision(Task task,
                                                            String selectedBranch,
                                                            boolean closestEvaluated,
                                                            BlockPos rawClosest,
                                                            boolean closestWithinRangeEvaluated,
                                                            boolean closestWithinRange,
                                                            boolean currentTryWithinExtraRangeEvaluated,
                                                            boolean currentTryWithinExtraRange,
                                                            BlockPos currentChestTry,
                                                            ItemTarget[] notStored,
                                                            Object... fields) {
        runEligible(() -> logParentCandidateDecisionInternal(
                task,
                selectedBranch,
                closestEvaluated,
                rawClosest,
                closestWithinRangeEvaluated,
                closestWithinRange,
                currentTryWithinExtraRangeEvaluated,
                currentTryWithinExtraRange,
                currentChestTry,
                notStored,
                fields
        ));
    }

    private static void logParentCandidateDecisionInternal(Task task,
                                                           String selectedBranch,
                                                           boolean closestEvaluated,
                                                           BlockPos rawClosest,
                                                           boolean closestWithinRangeEvaluated,
                                                           boolean closestWithinRange,
                                                           boolean currentTryWithinExtraRangeEvaluated,
                                                           boolean currentTryWithinExtraRange,
                                                           BlockPos currentChestTry,
                                                           ItemTarget[] notStored,
                                                           Object[] fields) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        try {
            StoreDepositOperationState state = BINDINGS.stateFor(task);
            if (state == null) {
                return;
            }
            state.recordParentCandidateDecision(selectedBranch);
            if (!state.context().isDepositAllOperation()) {
                String legacyKey = StoreDepositEventFields.operationId(state)
                        + "|" + selectedBranch
                        + "|" + ChatClefDiagnostics.blockPos(rawClosest)
                        + "|" + ChatClefDiagnostics.blockPos(currentChestTry);
                if (!EMISSION_GATE.shouldEmitDetail(
                        StoreDepositEventFields.operationId(state),
                        "STORE_CONTAINER_PARENT_CANDIDATE_DECISION",
                        legacyKey
                )) {
                    return;
                }
                StoreDepositBoundedEventLogger.log("STORE_CONTAINER_PARENT_CANDIDATE_DECISION",
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
                return;
            }
            Object rangeDecisionOverride = fieldValue(fields, "candidateDecisionOutcome");
            String rangeDecisionOutcome = "unavailable".equals(rangeDecisionOverride)
                    ? rangeDecisionOutcome(
                            closestEvaluated,
                            rawClosest,
                            closestWithinRange,
                            currentTryWithinExtraRange
                    )
                    : String.valueOf(rangeDecisionOverride);
            StoreContainerParentDecision decision = state.routeState().recordParentDecision(
                    selectedBranch,
                    closestEvaluated,
                    rawClosest,
                    closestWithinRangeEvaluated,
                    closestWithinRange,
                    currentTryWithinExtraRangeEvaluated,
                    currentTryWithinExtraRange,
                    currentChestTry,
                    rangeDecisionOutcome,
                    notStoredStateHash(notStored),
                    null
            );
            String key = StoreDepositEventFields.operationId(state)
                    + "|" + selectedBranch
                    + "|" + ChatClefDiagnostics.blockPos(rawClosest)
                    + "|" + ChatClefDiagnostics.blockPos(currentChestTry)
                    + "|" + rangeDecisionOutcome
                    + "|" + decision.notStoredStateHash();
            if (EMISSION_GATE.shouldEmitDetail(
                    StoreDepositEventFields.operationId(state),
                    "STORE_CONTAINER_PARENT_CANDIDATE_DECISION",
                    key
            )) {
                Object[] eventFields = StoreContainerCandidateEventFields.parentDecisionFields(
                        state,
                        task,
                        decision,
                        notStored,
                        withoutField(fields, "fallbackContainerItemPresent")
                );
                eventFields = StoreDepositEventFields.merge(
                        eventFields,
                        StoreContainerRangeEventFields.parentDecisionFields(
                                state.routeState().currentRangeTransition(),
                                fieldValue(fields, "fallbackContainerItemPresent")
                        )
                );
                StoreDepositBoundedEventLogger.log("STORE_CONTAINER_PARENT_CANDIDATE_DECISION",
                        "store_container_parent_candidate_decision",
                        task,
                        ChatClefDiagnostics.withCommandContextFields(eventFields));
            }
            emitCheckpointIfDue(task, state);
        } catch (RuntimeException | LinkageError ignored) {
        }
    }

    public static void beginFilteredSearchObservation(Task task) {
        runEligible(() -> {
            try {
                StoreContainerCandidateCollector.begin(BINDINGS.stateFor(task), task);
            } catch (RuntimeException | LinkageError ignored) {
            }
        });
    }

    public static void beginDepositAllParentFilteredSearchObservation(Task task, BlockPos rawClosest) {
        runEligible(() -> {
            try {
                StoreContainerCandidateCollector.beginParentSelection(
                        BINDINGS.stateFor(task),
                        task,
                        rawClosest
                );
            } catch (RuntimeException | LinkageError ignored) {
            }
        });
    }

    public static void observeDepositAllContainerEligibility(BlockPos position, String outcome) {
        runEligible(() -> {
            StoreContainerCandidateRejectionReason reason;
            try {
                reason = StoreContainerCandidateRejectionReason.valueOf(outcome);
            } catch (IllegalArgumentException | NullPointerException ignored) {
                reason = StoreContainerCandidateRejectionReason.UNKNOWN;
            }
            StoreContainerCandidateCollector.observe(position, reason);
        });
    }

    public static void endDepositAllParentFilteredSearchObservation(Task task,
                                                                    boolean completedNormally) {
        runEligible(() -> {
            try {
                StoreContainerCandidateCollector.end(task, completedNormally);
            } catch (RuntimeException | LinkageError ignored) {
            }
        });
    }

    public static void endFilteredSearchObservation(Task task,
                                                    boolean completedNormally,
                                                    Block[] targetBlocks) {
        runEligible(() -> {
            try {
                StoreContainerCandidateCollector.end(task, completedNormally);
                if (!completedNormally) {
                    logIncompleteFilteredSearchResult(task, targetBlocks);
                }
            } catch (RuntimeException | LinkageError ignored) {
            }
        });
    }

    private static void logIncompleteFilteredSearchResult(Task task, Block[] targetBlocks) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        StoreDepositOperationState state = BINDINGS.stateFor(task);
        if (state == null
                || !state.context().isDepositAllOperation()
                || !"OPEN_EXISTING".equals(state.routeState().currentBranch())
                || !state.routeState().isCurrentRouteChild(task)) {
            return;
        }
        StoreContainerCandidateObservation observation = StoreContainerCandidateCollector.take(state, task);
        if (!observation.available()) {
            return;
        }
        state.recordFilteredSearchResult("FILTERED_SCAN_DID_NOT_COMPLETE");
        state.routeState().recordFilteredSearch(Optional.empty(), observation);
        String key = StoreDepositEventFields.operationId(state)
                + "|FILTERED_SCAN_DID_NOT_COMPLETE|"
                + ChatClefDiagnostics.blockPos(observation.parentRawClosest())
                + "|" + observation.rawCandidatePredicateOutcome()
                + "|" + observation.rawCandidateRejectionReason();
        if (!EMISSION_GATE.shouldEmitDetail(
                StoreDepositEventFields.operationId(state),
                "STORE_CONTAINER_FILTERED_SEARCH_RESULT",
                key
        )) {
            return;
        }
        StoreDepositBoundedEventLogger.log(
                "STORE_CONTAINER_FILTERED_SEARCH_RESULT",
                "store_container_filtered_scan_did_not_complete",
                task,
                ChatClefDiagnostics.withCommandContextFields(
                        StoreContainerCandidateEventFields.filteredSearchFields(
                                state,
                                task,
                                Optional.empty(),
                                targetBlocks,
                                observation
                        )
                )
        );
    }

    public static void logFilteredSearchResult(Task task,
                                               Optional<BlockPos> result,
                                               Block[] targetBlocks) {
        runEligible(() -> logFilteredSearchResultEligible(task, result, targetBlocks));
    }

    private static void logFilteredSearchResultEligible(Task task,
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
            boolean depositAllParentSelectionSearch = state.context().isDepositAllOperation()
                    && state.context().isRoot(task);
            boolean depositAllContainerRouteSearch = state.context().isDepositAllOperation()
                    && "OPEN_EXISTING".equals(state.routeState().currentBranch())
                    && state.routeState().isCurrentRouteChild(task);
            boolean depositAllCandidateSearch = depositAllParentSelectionSearch || depositAllContainerRouteSearch;
            StoreContainerCandidateObservation observation = depositAllCandidateSearch
                    ? StoreContainerCandidateCollector.take(state, task)
                    : StoreContainerCandidateObservation.unavailable();
            if (depositAllCandidateSearch) {
                state.routeState().recordFilteredSearch(result, observation);
            }
            String position = result == null ? "unavailable" : result.map(BlockPos::toShortString).orElse("none");
            BlockPos originatingRaw = observation.available()
                    ? observation.parentRawClosest()
                    : state.routeState().currentParentDecision().rawClosest();
            String key = depositAllCandidateSearch
                    ? StoreDepositEventFields.operationId(state)
                            + "|" + relation
                            + "|" + ChatClefDiagnostics.blockPos(originatingRaw)
                            + "|" + position
                            + "|" + observation.candidateEvaluationCount()
                             + "|" + observation.rejectionCountsByReason()
                             + "|" + observation.firstRejectedReason()
                             + "|" + observation.lastRejectedReason()
                             + "|" + observation.rawCandidatePredicateOutcome()
                             + "|" + observation.rawCandidateRejectionReason()
                    : StoreDepositEventFields.operationId(state)
                            + "|" + StoreDepositEventFields.identity(task)
                            + "|" + relation
                            + "|" + position;
            if (!EMISSION_GATE.shouldEmitDetail(StoreDepositEventFields.operationId(state), "STORE_CONTAINER_FILTERED_SEARCH_RESULT", key)) {
                return;
            }
            Object[] eventFields = depositAllCandidateSearch
                    ? StoreContainerCandidateEventFields.filteredSearchFields(
                            state,
                            task,
                            result,
                            targetBlocks,
                            observation
                    )
                    : StoreDepositEventFields.filteredSearchResultFields(state, task, result, targetBlocks);
            StoreDepositBoundedEventLogger.log("STORE_CONTAINER_FILTERED_SEARCH_RESULT",
                    "store_container_filtered_search_result",
                    task,
                    ChatClefDiagnostics.withCommandContextFields(eventFields));
        } catch (RuntimeException | LinkageError ignored) {
        }
    }

    public static void logPursuitDecision(Task task,
                                          Object currentPursuit,
                                          Object candidate,
                                          String returnedAction) {
        runEligible(() -> PURSUITS.logPursuitDecision(
                task,
                currentPursuit,
                candidate,
                returnedAction
        ));
    }

    public static void logTargetCallbackDecision(Task rootTask,
                                                 BlockPos callbackTarget,
                                                 BlockPos currentChestTryBefore,
                                                 boolean sameReference,
                                                 boolean progressResetBecauseReferenceChanged,
                                                 ItemTarget[] boundNotStored) {
        runEligible(() -> TARGET_CALLBACKS.logTargetCallbackDecision(
                rootTask,
                callbackTarget,
                currentChestTryBefore,
                sameReference,
                progressResetBecauseReferenceChanged,
                boundNotStored
        ));
    }

    public static void observeContainerRouteEvent(String eventName,
                                                  String reason,
                                                  Task task,
                                                  Object[] branchFields) {
        runEligible(() -> ROUTE_EVENTS.observeContainerRouteEvent(
                eventName,
                reason,
                task,
                branchFields
        ));
    }

    public static void logTransferDecision(Task task,
                                           BlockPos targetContainer,
                                           ItemTarget target,
                                           int potentialSourceSlotCount,
                                           boolean bestSourcePresent,
                                           boolean destinationEvaluated,
                                           boolean destinationPresent,
                                           String action) {
        runEligible(() -> TRANSFERS.logTransferDecision(
                task,
                targetContainer,
                target,
                potentialSourceSlotCount,
                bestSourcePresent,
                destinationEvaluated,
                destinationPresent,
                action
        ));
    }

    public static void stageAutomaticTransferCandidate(
            Task parent,
            Task candidate,
            StoreDepositTransferSelectionSnapshot selection) {
        runEligible(() -> {
            try {
                TRANSFERS.stageTransferCandidate(parent, candidate, selection);
            } catch (RuntimeException | LinkageError ignored) {
            }
        });
    }

    public static void beginAutomaticSlotAction(Task exactLeafTask,
                                                Object handler,
                                                int syncId,
                                                int clickedSlotIndex,
                                                int clickButton,
                                                SlotActionType clickActionType,
                                                ItemStack cursorBefore) {
        runEligible(() -> {
            try {
                SLOT_ACTIONS.beginAction(
                        exactLeafTask,
                        handler,
                        syncId,
                        clickedSlotIndex,
                        clickButton,
                        clickActionType,
                        cursorBefore
                );
            } catch (RuntimeException | LinkageError ignored) {
            }
        });
    }

    public static void observeAutomaticSlotActionReturn(ItemStack cursorAfter) {
        runEligible(() -> {
            try {
                SLOT_ACTIONS.observeLocalClickReturn(cursorAfter);
            } catch (RuntimeException | LinkageError ignored) {
            }
        });
    }

    public static void beginAutomaticSlotMutation(int ordinal,
                                                  Slot slot,
                                                  ItemStack before,
                                                  ItemStack after) {
        runEligible(() -> {
            try {
                SLOT_ACTIONS.beginMutation(ordinal, slot, before, after);
            } catch (RuntimeException | LinkageError ignored) {
            }
        });
    }

    public static void endAutomaticSlotMutation() {
        runEligible(() -> {
            try {
                SLOT_ACTIONS.endMutation();
            } catch (RuntimeException | LinkageError ignored) {
            }
        });
    }

    public static void endAutomaticSlotAction() {
        runEligible(() -> {
            try {
                SLOT_ACTIONS.endAction();
            } catch (RuntimeException | LinkageError ignored) {
            }
        });
    }

    public static void observeAutomaticNotStoredDecision(
            ContainerStoredTracker tracker,
            ItemTarget input,
            int storedCount,
            boolean storedSatisfied,
            boolean hasItemEvaluated,
            boolean hasItem,
            int firstAvailableCount,
            boolean secondAvailableCountEvaluated,
            int secondAvailableCount,
            ItemTarget output) {
        runEligible(() -> {
            try {
                NOT_STORED.observe(
                        tracker,
                        input,
                        storedCount,
                        storedSatisfied,
                        hasItemEvaluated,
                        hasItem,
                        firstAvailableCount,
                        secondAvailableCountEvaluated,
                        secondAvailableCount,
                        output
                );
            } catch (RuntimeException | LinkageError ignored) {
            }
        });
    }

    public static void observeAutomaticMovementResult(Task rootTask,
                                                      BlockPos selectedTargetBefore,
                                                      String invalidationReason,
                                                      boolean progressCheckEvaluated,
                                                      boolean progressCheckOk,
                                                      boolean candidateResetPerformed) {
        runEligible(() -> {
            try {
                MOVEMENTS.observeMovementResult(
                        rootTask,
                        selectedTargetBefore,
                        invalidationReason,
                        progressCheckEvaluated,
                        progressCheckOk,
                        candidateResetPerformed
                );
            } catch (RuntimeException | LinkageError ignored) {
            }
        });
    }

    public static void stageAutomaticRouteCandidate(Task rootTask,
                                                    Task candidateTask,
                                                    BlockPos selectedTarget,
                                                    int behaviorGenerationId,
                                                    boolean selectedNewTarget) {
        runEligible(() -> {
            try {
                MOVEMENTS.stageRouteCandidate(
                        rootTask,
                        candidateTask,
                        selectedTarget,
                        behaviorGenerationId,
                        selectedNewTarget
                );
            } catch (RuntimeException | LinkageError ignored) {
            }
        });
    }

    public static void clearPredicateSnapshot() {
        runEligible(EFFECTS::clearPredicateSnapshot);
    }

    public static void observeTargetContainerPredicate(ContainerStoredTracker tracker,
                                                       Slot slot,
                                                       BlockPos targetContainer,
                                                       Optional<BlockPos> lastInteraction,
                                                       boolean accepted) {
        runEligible(() -> EFFECTS.observeTargetContainerPredicate(
                tracker,
                slot,
                targetContainer,
                lastInteraction,
                accepted
        ));
    }

    public static void logEffectObservation(ContainerStoredTracker tracker,
                                            Slot slot,
                                            ItemStack before,
                                            ItemStack after,
                                            boolean playerInventorySlot,
                                            boolean acceptPredicateEvaluated,
                                            boolean acceptPredicateResult) {
        runEligible(() -> EFFECTS.logEffectObservation(
                tracker,
                slot,
                before,
                after,
                playerInventorySlot,
                acceptPredicateEvaluated,
                acceptPredicateResult
        ));
    }

    public static void logEffectObservation(ContainerStoredTracker tracker,
                                            Slot slot,
                                            ItemStack before,
                                            ItemStack after,
                                            boolean playerInventorySlot,
                                            boolean acceptPredicateEvaluated,
                                            boolean acceptPredicateResult,
                                            String trackerTotalBefore,
                                            String trackerTotalAfter) {
        runEligible(() -> EFFECTS.logEffectObservation(
                tracker,
                slot,
                before,
                after,
                playerInventorySlot,
                acceptPredicateEvaluated,
                acceptPredicateResult,
                trackerTotalBefore,
                trackerTotalAfter
        ));
    }

    public static void logUserBlockRangeNullInput(Object owner, BlockPos observedPosition) {
        runEligible(() -> USER_BLOCK_RANGE_NULL_INPUTS.logNullInput(owner, observedPosition));
    }

    private static void emitCheckpointIfDue(Task task, StoreDepositOperationState state) {
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
        String key = StoreDepositEventFields.operationId(state) + "|" + value.checkpointSequence();
        if (!EMISSION_GATE.shouldEmitDetail(
                StoreDepositEventFields.operationId(state),
                "STORE_DEPOSIT_CHECKPOINT_SUMMARY",
                key
        )) {
            return;
        }
        StoreDepositBoundedEventLogger.log("STORE_DEPOSIT_CHECKPOINT_SUMMARY",
                "store_deposit_checkpoint_summary",
                task,
                ChatClefDiagnostics.withCommandContextFields(
                        StoreDepositEventFields.merge(
                                StoreContainerCandidateEventFields.checkpointFields(state, value),
                                EMISSION_GATE.budgetSummaryFields(StoreDepositEventFields.operationId(state))
                        )
                ));
        state.routeState().acknowledgeCheckpointEmission(value.checkpointSequence());
    }

    private static String rangeDecisionOutcome(boolean closestEvaluated,
                                               BlockPos rawClosest,
                                               boolean closestWithinRange,
                                               boolean currentTryWithinExtraRange) {
        if (!closestEvaluated) {
            return "NOT_EVALUATED_EARLY_GET_MISSING_TARGET";
        }
        if (rawClosest == null) {
            return "NO_RAW_CLOSEST";
        }
        if (closestWithinRange && currentTryWithinExtraRange) {
            return "BOTH_RANGE_CONDITIONS";
        }
        if (closestWithinRange) {
            return "RAW_CLOSEST_WITHIN_50";
        }
        if (currentTryWithinExtraRange) {
            return "CURRENT_TRY_WITHIN_70";
        }
        return "RAW_PRESENT_BUT_OUTSIDE_RANGES";
    }

    private static String notStoredStateHash(ItemTarget[] notStored) {
        return Integer.toHexString(ChatClefDiagnostics.itemTargets(notStored).hashCode());
    }

    private static Object fieldValue(Object[] fields, String key) {
        if (fields == null || key == null) {
            return "unavailable";
        }
        for (int index = 0; index + 1 < fields.length; index += 2) {
            if (key.equals(String.valueOf(fields[index]))) {
                return fields[index + 1];
            }
        }
        return "unavailable";
    }

    private static Object[] withoutField(Object[] fields, String key) {
        if (fields == null || fields.length == 0 || key == null) {
            return fields;
        }
        int retainedLength = 0;
        for (int index = 0; index + 1 < fields.length; index += 2) {
            if (!key.equals(String.valueOf(fields[index]))) {
                retainedLength += 2;
            }
        }
        Object[] retained = new Object[retainedLength];
        int targetIndex = 0;
        for (int index = 0; index + 1 < fields.length; index += 2) {
            if (!key.equals(String.valueOf(fields[index]))) {
                retained[targetIndex++] = fields[index];
                retained[targetIndex++] = fields[index + 1];
            }
        }
        return retained;
    }

    private static void runEligible(Runnable action) {
        ChatClefDiagnostics.runIfDiagnosticsEligible(action);
    }

    private static <T> T callEligible(Supplier<T> action, T ineligibleValue) {
        return ChatClefDiagnostics.callIfDiagnosticsEligible(action, ineligibleValue);
    }

}
