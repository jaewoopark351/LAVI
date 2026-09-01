package lavi.minecraft.diagnostics.mining;

import adris.altoclef.AltoClef;
import adris.altoclef.multiversion.blockpos.BlockPosVer;
import adris.altoclef.tasks.resources.MineAndCollectTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.MiningRequirement;
import adris.altoclef.util.helpers.StorageHelper;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.integration.mining.MiningToolReadiness;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

import java.util.Arrays;

//20260809_kpopmodder: Observe MineOrCollect goal requests without changing target selection or Baritone ownership.
final class MineTargetGoalRequestDiagnostics {
    private MineTargetGoalRequestDiagnostics() {
    }

    static boolean log(AltoClef mod,
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
                    Task returnedTask) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return false;
        }
        String targetPosition = ChatClefDiagnostics.blockPos(target);
        String previousPosition = ChatClefDiagnostics.blockPos(previousMiningPos);
        String relation = MineTargetPositionRelation.classify(previousMiningPos, target);
        String scannerUnreachableBefore = ChatClefDiagnostics.safeValue(() ->
                mod == null || target == null
                        ? "unavailable"
                        : mod.getBlockScanner().isUnreachable(target));
        String fingerprint = MiningDiagnosticEmitter.joinFingerprint(
                "MINE_TARGET_GOAL_REQUEST",
                targetPosition,
                previousPosition,
                relation,
                decisionOutcome,
                scannerUnreachableBefore,
                Boolean.toString(localBlacklistContainsBefore),
                MiningDiagnosticEmitter.taskClass(returnedTask)
        );
        return MiningDiagnosticEmitter.emitLazyWithPhysicalOutcome(
                "MINE_TARGET_GOAL_REQUEST", "mine_target_goal_request", task,
                "mine_target_goal_request|" + System.identityHashCode(task),
                fingerprint,
                () -> new Object[]{
                        "owner", "mine_target_goal_request_observer",
                        "trigger", "before_mine_or_collect_goal_task_return",
                        "decisionOutcome", decisionOutcome,
                        "parentTaskClass", MiningDiagnosticEmitter.taskClass(task),
                        "parentTaskInstanceId", MiningDiagnosticEmitter.instanceId(task),
                        "returnedTaskClass", MiningDiagnosticEmitter.taskClass(returnedTask),
                        "returnedTaskInstanceId", MiningDiagnosticEmitter.instanceId(returnedTask),
                        "targetPosition", targetPosition,
                        "previousMiningPosition", previousPosition,
                        "miningPositionAfterDecision", ChatClefDiagnostics.blockPos(miningPosAfterDecision),
                        "targetChangedFromPreviousMiningPosition", previousMiningPos == null || !previousMiningPos.equals(target),
                        "targetRelationToPreviousMiningPosition", relation,
                        "previousToTargetManhattanDistance", MineTargetPositionRelation.manhattanDistance(previousMiningPos, target),
                        "previousToTargetChebyshevDistance", MineTargetPositionRelation.chebyshevDistance(previousMiningPos, target),
                        "previousToTargetSquaredDistance", MineTargetPositionRelation.squaredDistance(previousMiningPos, target),
                        "requestedBlockIds", Arrays.toString(requestedBlocks),
                        "targetBlockId", targetState == null ? "unavailable" : targetState.getBlock(),
                        "targetBlockState", targetState == null ? "unavailable" : targetState,
                        "targetStateCaptureSource", "EXISTING_GET_GOAL_TASK_LOCAL",
                        "blockStillMatchesRequestedType", matchesRequestedBlock(targetState, requestedBlocks),
                        "chunkLoaded", ChatClefDiagnostics.safeValue(() -> mod.getChunkTracker().isChunkLoaded(target)),
                        "worldCanBreak", "NOT_CAPTURED_WITHOUT_BEHAVIOR_REEVALUATION",
                        "scannerUnreachableBefore", scannerUnreachableBefore,
                        "localBlacklistContainsBefore", localBlacklistContainsBefore,
                        "localBlacklistSize", localBlacklistSize,
                        "playerPosition", mod == null ? "unavailable" : ChatClefDiagnostics.playerPosition(mod),
                        "distanceSq", ChatClefDiagnostics.safeValue(() -> BlockPosVer.getSquaredDistance(target, mod.getPlayer().getPos())),
                        "currentMiningRequirement", ChatClefDiagnostics.safeValue(StorageHelper::getCurrentMiningRequirement),
                        "requestedMiningRequirement", requestedRequirement,
                        "readinessRequirement", readiness == null ? "unavailable" : readiness.requirement(),
                        "readinessBroadRequirementMet", readiness == null ? "unavailable" : readiness.broadRequirementMet(),
                        "readinessSelectableToolPresent", readiness == null ? "unavailable" : readiness.selectableToolPresent(),
                        "readinessRejectedBySavePolicy", readiness == null ? "unavailable" : readiness.rejectedBySavePolicy(),
                        "readinessRequiresAcquisition", readiness == null ? "unavailable" : readiness.requiresAcquisition()
                });
    }

    private static Object matchesRequestedBlock(BlockState targetState, Block[] requestedBlocks) {
        if (targetState == null || requestedBlocks == null) {
            return "unavailable";
        }
        Block actual = targetState.getBlock();
        for (Block requested : requestedBlocks) {
            if (actual == requested) {
                return true;
            }
        }
        return false;
    }
}
