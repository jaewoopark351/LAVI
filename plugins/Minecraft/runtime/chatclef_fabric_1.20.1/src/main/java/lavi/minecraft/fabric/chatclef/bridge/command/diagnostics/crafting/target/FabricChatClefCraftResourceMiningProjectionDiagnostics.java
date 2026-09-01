package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.target;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.resources.MineAndCollectTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.MiningRequirement;
import lavi.minecraft.diagnostics.crafting.acquisition.association.CraftResourceAssociationStatus;
import lavi.minecraft.diagnostics.crafting.acquisition.event.CraftResourceAcquisitionEventEmitter;
import lavi.minecraft.diagnostics.crafting.acquisition.event.CraftResourceSourceEventName;
import lavi.minecraft.diagnostics.crafting.acquisition.scope.IronPickaxeAcquisitionScopeBinding;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceMismatchDecision;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceMismatchObservation;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceStage;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetAttemptDecision;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetDiagnosticsRegistry;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetObservation;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetObservationKind;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetRole;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetTuple;
import lavi.minecraft.diagnostics.crafting.acquisition.target.failure.CraftResourceFailureKind;
import lavi.minecraft.diagnostics.crafting.acquisition.target.failure.CraftResourceFailureObservation;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.association.FabricChatClefCraftResourceAssociationReader;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.association.FabricChatClefCraftResourceAssociationScopeDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.association.FabricChatClefCraftResourceAssociationSnapshot;
import lavi.minecraft.integration.mining.MiningToolReadiness;
import lavi.minecraft.diagnostics.session.runtime.DiagnosticDispatchResult;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Map;
import java.util.Optional;

//20260901_kpopmodder: Project existing mining evidence without changing target or retry behavior.
public final class FabricChatClefCraftResourceMiningProjectionDiagnostics {
    private static final CraftResourceAcquisitionEventEmitter EMITTER =
            new CraftResourceAcquisitionEventEmitter();

    private FabricChatClefCraftResourceMiningProjectionDiagnostics() {
    }

    public static void observeTaskChildReconciliation(
            Task parent,
            Task activeChildBefore,
            Task candidateChild,
            boolean isEqualResult,
            boolean canInterruptEvaluated,
            boolean canInterruptPreviousChild,
            boolean replacementApplied,
            boolean previousChildStopCalled,
            Task activeChildAfter,
            boolean candidateDiscardedBecauseEqual,
            boolean sourceEmissionCompleted) {
        try {
            observeTaskChildReconciliationEligible(
                    parent,
                    activeChildBefore,
                    candidateChild,
                    isEqualResult,
                    canInterruptEvaluated,
                    canInterruptPreviousChild,
                    replacementApplied,
                    previousChildStopCalled,
                    activeChildAfter,
                    candidateDiscardedBecauseEqual,
                    sourceEmissionCompleted
            );
        } catch (RuntimeException | LinkageError error) {
            FabricChatClefCraftResourceAssociationReader.observeProjectionFailure(
                    "TASK_CHILD_RECONCILIATION_PROJECTION",
                    error
            );
        }
    }

    private static void observeTaskChildReconciliationEligible(
            Task parent,
            Task activeChildBefore,
            Task candidateChild,
            boolean isEqualResult,
            boolean canInterruptEvaluated,
            boolean canInterruptPreviousChild,
            boolean replacementApplied,
            boolean previousChildStopCalled,
            Task activeChildAfter,
            boolean candidateDiscardedBecauseEqual,
            boolean sourceEmissionCompleted) {
        FabricChatClefCraftResourceAssociationSnapshot association =
                FabricChatClefCraftResourceAssociationReader.capture(parent);
        if (association.binding().isEmpty()) {
            return;
        }
        IronPickaxeAcquisitionScopeBinding binding = association.binding().get();
        CraftResourceTargetDiagnosticsRegistry registry = registry();
        Optional<CraftResourceTargetTuple> previousTuple = registry.currentTuple(binding.key());
        Optional<CraftResourceTargetTuple> observedTuple =
                FabricChatClefCraftResourceMiningTargetAdapter.reconciliationTuple(
                        activeChildAfter,
                        previousTuple
                );
        Optional<CraftResourceTargetTuple> previousChildTuple =
                FabricChatClefCraftResourceMiningTargetAdapter.reconciliationTuple(
                        activeChildBefore,
                        previousTuple
                );
        boolean previousActiveTargetProven = previousTuple.isPresent()
                && previousChildTuple.filter(previousTuple.get()::equals).isPresent();
        if (replacementApplied
                && previousChildStopCalled
                && previousActiveTargetProven
                && observedTuple.isEmpty()) {
            registry.observeTarget(
                    binding.key(),
                    new CraftResourceTargetObservation(
                            association.decision().status(),
                            CraftResourceTargetObservationKind.TARGET_ABANDONED,
                            Optional.empty()
                    )
            );
        }
        CraftResourceTargetObservationKind observationKind = replacementApplied
                && observedTuple.isPresent()
                        ? CraftResourceTargetObservationKind.ACTIVE_TARGET_PROVEN
                        : isEqualResult || candidateDiscardedBecauseEqual
                                ? CraftResourceTargetObservationKind.EQUAL_RECONCILIATION
                                : CraftResourceTargetObservationKind.CANDIDATE_RETURN;
        CraftResourceTargetAttemptDecision decision = registry.observeTarget(
                binding.key(),
                new CraftResourceTargetObservation(
                        association.decision().status(),
                        observationKind,
                        observedTuple
                )
        );
        if (sourceEmissionCompleted
                && decision.startedNewAttempt()
                && decision.detailEligible()
                && observedTuple.isPresent()) {
            EMITTER.emit(
                    "CRAFT_RESOURCE_TARGET_ROLE_TRANSITION",
                    "craft_resource_target_role_transition",
                    FabricChatClefCraftResourceMiningEventFields.targetTransition(
                            association,
                            CraftResourceSourceEventName.TASK_CHILD_RECONCILIATION,
                            previousTuple,
                            observedTuple.get(),
                            decision,
                            "TASK_CHILD_RECONCILIATION_REPLACEMENT_APPLIED",
                            FabricChatClefCraftResourceMiningTargetAdapter.taskClass(parent),
                            FabricChatClefCraftResourceMiningTargetAdapter.taskClass(activeChildAfter),
                            true
                    ),
                    Map.of(
                            "replacementApplied", replacementApplied,
                            "isEqualResult", isEqualResult,
                            "candidateDiscardedBecauseEqual", candidateDiscardedBecauseEqual,
                            "canInterruptEvaluated", canInterruptEvaluated,
                            "canInterruptPreviousChild", canInterruptPreviousChild,
                            "previousChildStopCalled", previousChildStopCalled
                    )
            );
        }
    }

    public static void observeMineTargetSelection(
            AltoClef mod,
            MineAndCollectTask.MineOrCollectTask task,
            Pair<Double, Optional<BlockPos>> closestBlock,
            Pair<Double, Optional<ItemEntity>> closestDrop,
            Optional<Object> selected,
            String selectionReason,
            boolean targetChanged,
            boolean localBlacklistContains,
            Block[] requestedBlocks,
            int localBlacklistSize,
            BlockPos currentMiningPos,
            boolean sourceEmissionCompleted) {
        try {
            observeMineTargetSelectionEligible(
                    mod,
                    task,
                    closestBlock,
                    closestDrop,
                    selected,
                    selectionReason,
                    targetChanged,
                    localBlacklistContains,
                    requestedBlocks,
                    localBlacklistSize,
                    currentMiningPos,
                    sourceEmissionCompleted
            );
        } catch (RuntimeException | LinkageError error) {
            FabricChatClefCraftResourceAssociationReader.observeProjectionFailure(
                    "MINE_TARGET_SELECTION_TRANSITION_PROJECTION",
                    error
            );
        }
    }

    private static void observeMineTargetSelectionEligible(
            AltoClef mod,
            MineAndCollectTask.MineOrCollectTask task,
            Pair<Double, Optional<BlockPos>> closestBlock,
            Pair<Double, Optional<ItemEntity>> closestDrop,
            Optional<Object> selected,
            String selectionReason,
            boolean targetChanged,
            boolean localBlacklistContains,
            Block[] requestedBlocks,
            int localBlacklistSize,
            BlockPos currentMiningPos,
            boolean sourceEmissionCompleted) {
        FabricChatClefCraftResourceAssociationSnapshot association =
                FabricChatClefCraftResourceAssociationReader.capture(task);
        if (association.binding().isEmpty()) {
            return;
        }
        IronPickaxeAcquisitionScopeBinding binding = association.binding().get();
        CraftResourceTargetDiagnosticsRegistry registry = registry();
        if (!FabricChatClefCraftResourceMiningTargetAdapter
                .expectedBlockIdsInputComplete(requestedBlocks)) {
            FabricChatClefCraftResourceAssociationScopeDiagnostics.observeObservationGap(
                    binding.key(),
                    "MINE_TARGET_SELECTION_TRANSITION",
                    "EXPECTED_BLOCK_ID_INPUT_LIMIT_EXCEEDED"
            );
        }
        Optional<BlockPos> selectedBlock = selected == null
                ? Optional.empty()
                : selected.filter(BlockPos.class::isInstance).map(BlockPos.class::cast);
        Optional<CraftResourceTargetTuple> candidateTuple = selectedBlock.flatMap(position ->
                FabricChatClefCraftResourceMiningTargetAdapter.targetTuple(
                        position,
                        requestedBlocks
                ));
        registry.observeTarget(
                binding.key(),
                new CraftResourceTargetObservation(
                        association.decision().status(),
                        CraftResourceTargetObservationKind.CANDIDATE_RETURN,
                        candidateTuple
                )
        );
    }

    public static void observeMineTargetGoalRequest(
            AltoClef mod,
            MineAndCollectTask.MineOrCollectTask task,
            BlockPos target,
            BlockPos previousMiningPos,
            BlockPos miningPosAfterDecision,
            boolean localBlacklistContainsBefore,
            int localBlacklistSize,
            Block[] requestedBlocks,
            MiningRequirement requestedRequirement,
            BlockState targetState,
            MiningToolReadiness.Readiness readiness,
            String decisionOutcome,
            Task returnedTask,
            boolean sourceEmissionCompleted) {
        try {
            observeMineTargetGoalRequestEligible(
                    mod,
                    task,
                    target,
                    previousMiningPos,
                    miningPosAfterDecision,
                    localBlacklistContainsBefore,
                    localBlacklistSize,
                    requestedBlocks,
                    requestedRequirement,
                    targetState,
                    readiness,
                    decisionOutcome,
                    returnedTask,
                    sourceEmissionCompleted
            );
        } catch (RuntimeException | LinkageError error) {
            FabricChatClefCraftResourceAssociationReader.observeProjectionFailure(
                    "MINE_TARGET_GOAL_REQUEST_PROJECTION",
                    error
            );
        }
    }

    private static void observeMineTargetGoalRequestEligible(
            AltoClef mod,
            MineAndCollectTask.MineOrCollectTask task,
            BlockPos target,
            BlockPos previousMiningPos,
            BlockPos miningPosAfterDecision,
            boolean localBlacklistContainsBefore,
            int localBlacklistSize,
            Block[] requestedBlocks,
            MiningRequirement requestedRequirement,
            BlockState targetState,
            MiningToolReadiness.Readiness readiness,
            String decisionOutcome,
            Task returnedTask,
            boolean sourceEmissionCompleted) {
        FabricChatClefCraftResourceAssociationSnapshot association =
                FabricChatClefCraftResourceAssociationReader.capture(task);
        if (association.binding().isEmpty()) {
            return;
        }
        IronPickaxeAcquisitionScopeBinding binding = association.binding().get();
        CraftResourceTargetDiagnosticsRegistry registry = registry();
        if (!FabricChatClefCraftResourceMiningTargetAdapter
                .expectedBlockIdsInputComplete(requestedBlocks)) {
            FabricChatClefCraftResourceAssociationScopeDiagnostics.observeObservationGap(
                    binding.key(),
                    "MINE_TARGET_GOAL_REQUEST",
                    "EXPECTED_BLOCK_ID_INPUT_LIMIT_EXCEEDED"
            );
        }
        Optional<CraftResourceTargetTuple> previousTuple = registry.currentTuple(binding.key());
        Optional<CraftResourceTargetTuple> goalTuple =
                FabricChatClefCraftResourceMiningTargetAdapter.targetTuple(
                        target == null ? miningPosAfterDecision : target,
                        requestedBlocks
                );
        boolean activeMiningTupleProven = "RETURN_DESTROY_BLOCK_TASK".equals(decisionOutcome)
                && target != null
                && miningPosAfterDecision != null
                && miningPosAfterDecision.equals(target)
                && goalTuple.isPresent();
        CraftResourceTargetAttemptDecision targetDecision = registry.observeTarget(
                binding.key(),
                new CraftResourceTargetObservation(
                        association.decision().status(),
                        activeMiningTupleProven
                                ? CraftResourceTargetObservationKind.ACTIVE_TARGET_PROVEN
                                : CraftResourceTargetObservationKind.GOAL_SUBMISSION,
                        goalTuple
                )
        );
        if (sourceEmissionCompleted
                && activeMiningTupleProven
                && targetDecision.startedNewAttempt()
                && targetDecision.detailEligible()) {
            EMITTER.emit(
                    "CRAFT_RESOURCE_TARGET_ROLE_TRANSITION",
                    "craft_resource_target_role_transition",
                    FabricChatClefCraftResourceMiningEventFields.targetTransition(
                            association,
                            CraftResourceSourceEventName.MINE_TARGET_GOAL_REQUEST,
                            previousTuple,
                            goalTuple.orElseThrow(),
                            targetDecision,
                            "MINE_TARGET_GOAL_REQUEST_ACTIVE_DESTROY_BLOCK_TASK",
                            FabricChatClefCraftResourceMiningTargetAdapter.taskClass(task),
                            FabricChatClefCraftResourceMiningTargetAdapter.taskClass(returnedTask),
                            true
                    ),
                    Map.of(
                            "decisionOutcome", decisionOutcome,
                            "miningPositionAfterDecisionMatchesTarget", true
                    )
            );
        }

        if (!activeMiningTupleProven) {
            return;
        }

        List<String> expectedBlockIds =
                FabricChatClefCraftResourceMiningTargetAdapter.expectedBlockIds(requestedBlocks);
        boolean ironInput = expectedBlockIds.contains("minecraft:iron_ore")
                || expectedBlockIds.contains("minecraft:deepslate_iron_ore");
        CraftResourceMismatchObservation mismatchObservation =
                new CraftResourceMismatchObservation(
                        binding.key().commandCorrelationId(),
                        association.decision().status(),
                        ironInput
                                ? CraftResourceStage.IRON_INPUT_ACQUISITION
                                : CraftResourceStage.UNKNOWN,
                        ironInput
                                ? CraftResourceTargetRole.IRON_ORE_BLOCK
                                : CraftResourceTargetRole.UNKNOWN,
                        FabricChatClefCraftResourceMiningTargetAdapter.position(target),
                        requestedBlocks == null
                                ? Optional.empty()
                                : Optional.of(expectedBlockIds),
                        FabricChatClefCraftResourceMiningTargetAdapter.observedBlockId(targetState),
                        CraftResourceSourceEventName.MINE_TARGET_GOAL_REQUEST.name(),
                        association.captureClientTick(),
                        association.sourceTaskInstanceId()
                );
        CraftResourceMismatchDecision mismatch = registry.evaluateMismatch(
                binding.key(),
                mismatchObservation
        );
        if (mismatch.emissionRequested()) {
            DiagnosticDispatchResult mismatchDispatchResult = EMITTER.emitWithDispatchResult(
                    "CRAFT_RESOURCE_EXPECTED_OBSERVED_MISMATCH",
                    "craft_resource_expected_observed_mismatch",
                    FabricChatClefCraftResourceMiningEventFields.mismatch(
                            association,
                            mismatchObservation,
                            registry,
                            goalTuple.orElseThrow(),
                            FabricChatClefCraftResourceMiningTargetAdapter.blockStateSummary(
                                    targetState
                            ),
                            FabricChatClefCraftResourceMiningTargetAdapter.taskClass(task),
                            FabricChatClefCraftResourceMiningTargetAdapter.taskClass(returnedTask),
                            sourceEmissionCompleted
                    ),
                    Map.of(
                            "decisionOutcome", decisionOutcome == null
                                    ? "UNAVAILABLE"
                                    : decisionOutcome,
                            "localBlacklistContainsBefore", localBlacklistContainsBefore,
                            "localBlacklistSize", localBlacklistSize,
                            "previousMiningPosition",
                            FabricChatClefCraftResourceMiningTargetAdapter.position(
                                    previousMiningPos
                            ),
                            "miningPositionAfterDecision",
                            FabricChatClefCraftResourceMiningTargetAdapter.position(
                                    miningPosAfterDecision
                            ),
                            "requestedMiningRequirement", requestedRequirement == null
                                    ? "UNAVAILABLE"
                                    : requestedRequirement,
                            "readinessAvailable", readiness != null
                    )
            );
            if (!mismatchDispatchResult.admitted()) {
                registry.recordMismatchAdmissionDenied(
                        binding.key(),
                        mismatch.fingerprint()
                );
            } else if (!mismatchDispatchResult.emissionCompleted()) {
                registry.recordMismatchEmissionFailed(
                        binding.key(),
                        mismatch.fingerprint()
                );
            }
        }
    }

    public static void observeBlockUnreachableRequest(
            AltoClef mod,
            Task task,
            BlockPos target,
            int requestedAllowedFailures,
            String requestSource,
            Task activeDestroyTask,
            Task candidateDestroyTask,
            boolean sourceEmissionCompleted) {
        try {
            observeBlockUnreachableRequestEligible(
                    mod,
                    task,
                    target,
                    requestedAllowedFailures,
                    requestSource,
                    activeDestroyTask,
                    candidateDestroyTask,
                    sourceEmissionCompleted
            );
        } catch (RuntimeException | LinkageError error) {
            FabricChatClefCraftResourceAssociationReader.observeProjectionFailure(
                    "BLOCK_UNREACHABLE_REQUEST_PROJECTION",
                    error
            );
        }
    }

    public static void observeMineTargetAbandoned(
            AltoClef mod,
            Task task,
            BlockPos previousMiningPosition,
            BlockPos miningPositionAfterBoundary,
            String closureKind,
            String closureReason,
            boolean sourceEmissionCompleted) {
        try {
            observeMineTargetAbandonedEligible(
                    task,
                    previousMiningPosition,
                    miningPositionAfterBoundary,
                    closureKind,
                    closureReason,
                    sourceEmissionCompleted
            );
        } catch (RuntimeException | LinkageError error) {
            FabricChatClefCraftResourceAssociationReader.observeProjectionFailure(
                    "MINE_TARGET_ABANDONED_PROJECTION",
                    error
            );
        }
    }

    private static void observeMineTargetAbandonedEligible(
            Task task,
            BlockPos previousMiningPosition,
            BlockPos miningPositionAfterBoundary,
            String closureKind,
            String closureReason,
            boolean sourceEmissionCompleted) {
        FabricChatClefCraftResourceAssociationSnapshot association =
                FabricChatClefCraftResourceAssociationReader.capture(task);
        if (association.binding().isEmpty()) {
            return;
        }
        IronPickaxeAcquisitionScopeBinding binding = association.binding().get();
        CraftResourceTargetDiagnosticsRegistry registry = registry();
        Optional<CraftResourceTargetTuple> currentTuple = registry.currentTuple(
                binding.key()
        );
        String previousPosition =
                FabricChatClefCraftResourceMiningTargetAdapter.position(
                        previousMiningPosition
                );
        if (currentTuple.isEmpty()
                || previousMiningPosition == null
                || !currentTuple.get().targetPosition().equals(previousPosition)) {
            FabricChatClefCraftResourceAssociationScopeDiagnostics.observeObservationGap(
                    binding.key(),
                    "MINE_TARGET_ABANDONED",
                    "ACTIVE_TARGET_TUPLE_NOT_PROVEN_AT_CLOSURE"
            );
            return;
        }
        CraftResourceTargetObservationKind observationKind = switch (
                closureKind == null ? "" : closureKind
        ) {
            case "OWNER_STOP" -> CraftResourceTargetObservationKind.OWNER_STOP;
            case "OWNER_INTERRUPT" -> CraftResourceTargetObservationKind.OWNER_INTERRUPT;
            case "TARGET_ABANDONED" ->
                    CraftResourceTargetObservationKind.TARGET_ABANDONED;
            default -> null;
        };
        if (observationKind == null
                || (observationKind == CraftResourceTargetObservationKind.TARGET_ABANDONED
                        && miningPositionAfterBoundary != null)) {
            FabricChatClefCraftResourceAssociationScopeDiagnostics.observeObservationGap(
                    binding.key(),
                    "MINE_TARGET_ABANDONED",
                    "CLOSURE_KIND_OR_APPLIED_CLEAR_NOT_PROVEN"
            );
            return;
        }
        CraftResourceTargetAttemptDecision decision = registry.observeTarget(
                binding.key(),
                new CraftResourceTargetObservation(
                        association.decision().status(),
                        observationKind,
                        currentTuple
                )
        );
        if (!sourceEmissionCompleted
                || association.decision().status()
                != CraftResourceAssociationStatus.COMMAND_ROOT_DESCENDANT) {
            return;
        }
        EMITTER.emit(
                "CRAFT_RESOURCE_TARGET_ATTEMPT_CLOSED",
                "craft_resource_target_attempt_closed",
                FabricChatClefCraftResourceMiningEventFields.targetClosure(
                        association,
                        currentTuple.get(),
                        decision,
                        observationKind.name(),
                        closureReason,
                        FabricChatClefCraftResourceMiningTargetAdapter.position(
                                miningPositionAfterBoundary
                        ),
                        true
                ),
                Map.of()
        );
    }

    private static void observeBlockUnreachableRequestEligible(
            AltoClef mod,
            Task task,
            BlockPos target,
            int requestedAllowedFailures,
            String requestSource,
            Task activeDestroyTask,
            Task candidateDestroyTask,
            boolean sourceEmissionCompleted) {
        FabricChatClefCraftResourceAssociationSnapshot association =
                FabricChatClefCraftResourceAssociationReader.capture(task);
        observeOwnedFailure(
                association,
                CraftResourceTargetObservationKind.UNREACHABLE_REQUEST,
                new CraftResourceFailureObservation(
                        association.decision().status(),
                        CraftResourceFailureKind.UNREACHABLE_REQUEST,
                        association.sourceTaskClass(),
                        FabricChatClefCraftResourceMiningTargetAdapter.position(target),
                        -1L,
                        -1L,
                        requestedAllowedFailures,
                        Optional.empty(),
                        Optional.empty(),
                        association.captureClientTick(),
                        sourceEmissionCompleted
                )
        );
    }

    public static void observeBlacklistStateChanged(
            AltoClef mod,
            Object item,
            boolean entryCreated,
            int failureCountBefore,
            int failureCountAfter,
            int allowedFailuresBefore,
            int requestedAllowedFailures,
            int allowedFailuresAfter,
            boolean unreachableBefore,
            boolean unreachableAfter,
            double currentDistanceSq,
            double bestDistanceSqBefore,
            double bestDistanceSqAfter,
            MiningRequirement currentMiningRequirement,
            MiningRequirement bestToolBefore,
            MiningRequirement bestToolAfter,
            boolean resetApplied,
            String resetReason,
            boolean sourceEmissionCompleted) {
        try {
            observeBlacklistStateChangedEligible(
                    mod,
                    item,
                    entryCreated,
                    failureCountBefore,
                    failureCountAfter,
                    allowedFailuresBefore,
                    requestedAllowedFailures,
                    allowedFailuresAfter,
                    unreachableBefore,
                    unreachableAfter,
                    currentDistanceSq,
                    bestDistanceSqBefore,
                    bestDistanceSqAfter,
                    currentMiningRequirement,
                    bestToolBefore,
                    bestToolAfter,
                    resetApplied,
                    resetReason,
                    sourceEmissionCompleted
            );
        } catch (RuntimeException | LinkageError error) {
            FabricChatClefCraftResourceAssociationReader.observeProjectionFailure(
                    "BLOCK_BLACKLIST_STATE_CHANGED_PROJECTION",
                    error
            );
        }
    }

    private static void observeBlacklistStateChangedEligible(
            AltoClef mod,
            Object item,
            boolean entryCreated,
            int failureCountBefore,
            int failureCountAfter,
            int allowedFailuresBefore,
            int requestedAllowedFailures,
            int allowedFailuresAfter,
            boolean unreachableBefore,
            boolean unreachableAfter,
            double currentDistanceSq,
            double bestDistanceSqBefore,
            double bestDistanceSqAfter,
            MiningRequirement currentMiningRequirement,
            MiningRequirement bestToolBefore,
            MiningRequirement bestToolAfter,
            boolean resetApplied,
            String resetReason,
            boolean sourceEmissionCompleted) {
        if (!(item instanceof BlockPos)) {
            return;
        }
        FabricChatClefCraftResourceAssociationSnapshot association =
                FabricChatClefCraftResourceAssociationReader.capture(null);
        observeOwnedFailure(
                association,
                CraftResourceTargetObservationKind.BLACKLIST_STATE_CHANGED,
                new CraftResourceFailureObservation(
                        association.decision().status(),
                        CraftResourceFailureKind.BLACKLIST_STATE_CHANGED,
                        association.sourceTaskClass(),
                        FabricChatClefCraftResourceMiningTargetAdapter.position(
                                (BlockPos) item
                        ),
                        failureCountBefore,
                        failureCountAfter,
                        allowedFailuresAfter,
                        Optional.of(unreachableBefore),
                        Optional.of(unreachableAfter),
                        association.captureClientTick(),
                        sourceEmissionCompleted
                )
        );
    }

    private static void observeOwnedFailure(
            FabricChatClefCraftResourceAssociationSnapshot association,
            CraftResourceTargetObservationKind observationKind,
            CraftResourceFailureObservation failureObservation) {
        if (association.binding().isEmpty()) {
            return;
        }
        IronPickaxeAcquisitionScopeBinding binding = association.binding().get();
        CraftResourceTargetDiagnosticsRegistry registry = registry();
        registry.observeTarget(
                binding.key(),
                new CraftResourceTargetObservation(
                        association.decision().status(),
                        observationKind,
                        registry.currentTuple(binding.key())
                )
        );
        registry.observeFailure(binding.key(), failureObservation);
    }

    private static CraftResourceTargetDiagnosticsRegistry registry() {
        return FabricChatClefCraftResourceTargetScopeDiagnostics.registry();
    }
}
