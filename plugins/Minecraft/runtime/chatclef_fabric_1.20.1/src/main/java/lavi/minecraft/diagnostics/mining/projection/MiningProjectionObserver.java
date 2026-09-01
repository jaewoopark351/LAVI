package lavi.minecraft.diagnostics.mining.projection;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.resources.MineAndCollectTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.MiningRequirement;
import lavi.minecraft.integration.mining.MiningToolReadiness;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

//20260901_kpopmodder: Keep generic mining diagnostics independent from a Fabric projection implementation.
public interface MiningProjectionObserver {
    default void observeTaskChildReconciliation(
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
    }

    default void observeMineTargetSelection(
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
    }

    default void observeMineTargetGoalRequest(
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
    }

    default void observeMineTargetAbandoned(
            AltoClef mod,
            Task task,
            BlockPos previousMiningPosition,
            BlockPos miningPositionAfterBoundary,
            String closureKind,
            String closureReason,
            boolean sourceEmissionCompleted) {
    }

    default void observeBlockUnreachableRequest(
            AltoClef mod,
            Task task,
            BlockPos target,
            int requestedAllowedFailures,
            String requestSource,
            Task activeDestroyTask,
            Task candidateDestroyTask,
            boolean sourceEmissionCompleted) {
    }

    default void observeBlacklistStateChanged(
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
    }
}
