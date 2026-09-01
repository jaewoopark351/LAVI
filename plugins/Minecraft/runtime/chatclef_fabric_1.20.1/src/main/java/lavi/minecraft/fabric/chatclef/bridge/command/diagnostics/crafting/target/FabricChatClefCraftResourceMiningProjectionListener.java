package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.target;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.resources.MineAndCollectTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.MiningRequirement;
import lavi.minecraft.diagnostics.mining.projection.MiningProjectionObserver;
import lavi.minecraft.integration.mining.MiningToolReadiness;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

//20260901_kpopmodder: Adapt the neutral mining observer seam to the Fabric acquisition projector.
public final class FabricChatClefCraftResourceMiningProjectionListener
        implements MiningProjectionObserver {
    @Override
    public void observeTaskChildReconciliation(
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
        FabricChatClefCraftResourceMiningProjectionDiagnostics
                .observeTaskChildReconciliation(
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
    }

    @Override
    public void observeMineTargetSelection(
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
        FabricChatClefCraftResourceMiningProjectionDiagnostics.observeMineTargetSelection(
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
    }

    @Override
    public void observeMineTargetGoalRequest(
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
        FabricChatClefCraftResourceMiningProjectionDiagnostics.observeMineTargetGoalRequest(
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
    }

    @Override
    public void observeMineTargetAbandoned(
            AltoClef mod,
            Task task,
            BlockPos previousMiningPosition,
            BlockPos miningPositionAfterBoundary,
            String closureKind,
            String closureReason,
            boolean sourceEmissionCompleted) {
        FabricChatClefCraftResourceMiningProjectionDiagnostics.observeMineTargetAbandoned(
                mod,
                task,
                previousMiningPosition,
                miningPositionAfterBoundary,
                closureKind,
                closureReason,
                sourceEmissionCompleted
        );
    }

    @Override
    public void observeBlockUnreachableRequest(
            AltoClef mod,
            Task task,
            BlockPos target,
            int requestedAllowedFailures,
            String requestSource,
            Task activeDestroyTask,
            Task candidateDestroyTask,
            boolean sourceEmissionCompleted) {
        FabricChatClefCraftResourceMiningProjectionDiagnostics.observeBlockUnreachableRequest(
                mod,
                task,
                target,
                requestedAllowedFailures,
                requestSource,
                activeDestroyTask,
                candidateDestroyTask,
                sourceEmissionCompleted
        );
    }

    @Override
    public void observeBlacklistStateChanged(
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
        FabricChatClefCraftResourceMiningProjectionDiagnostics.observeBlacklistStateChanged(
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
    }
}
