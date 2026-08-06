package lavi.minecraft.diagnostics.mining;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.construction.DestroyBlockTask;
import adris.altoclef.tasks.resources.MineAndCollectTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.MiningRequirement;
import net.minecraft.block.Block;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

//20260806_kpopmodder: Route mining/path diagnostics through focused emitters without changing task behavior.
public final class MiningPathDiagnostics {
    private MiningPathDiagnostics() {
    }

    public static BaritonePathDiagnosticSnapshot captureBaritoneSnapshot(AltoClef mod,
                                                                         BlockPos target,
                                                                         Object goal,
                                                                         String goalMatchesTarget) {
        return BaritonePathDiagnosticSnapshot.capture(mod, target, goal, goalMatchesTarget);
    }

    public static void logTaskChildReconciliation(Task parent,
                                                  Task activeChildBefore,
                                                  Task candidateChild,
                                                  boolean isEqualResult,
                                                  boolean canInterruptEvaluated,
                                                  boolean canInterruptPreviousChild,
                                                  boolean replacementApplied,
                                                  boolean previousChildStopCalled,
                                                  Task activeChildAfter,
                                                  boolean candidateDiscardedBecauseEqual) {
        TaskChildReconciliationDiagnostics.log(parent, activeChildBefore, candidateChild, isEqualResult,
                canInterruptEvaluated, canInterruptPreviousChild, replacementApplied, previousChildStopCalled,
                activeChildAfter, candidateDiscardedBecauseEqual);
    }

    public static void logMineTargetSelection(AltoClef mod,
                                              MineAndCollectTask.MineOrCollectTask task,
                                              Pair<Double, Optional<BlockPos>> closestBlock,
                                              Pair<Double, Optional<ItemEntity>> closestDrop,
                                              Optional<Object> selected,
                                              String selectionReason,
                                              boolean targetChanged,
                                              boolean localBlacklistContains,
                                              Block[] requestedBlocks,
                                              int localBlacklistSize,
                                              BlockPos currentMiningPos) {
        MineTargetSelectionDiagnostics.log(mod, task, closestBlock, closestDrop, selected, selectionReason,
                targetChanged, localBlacklistContains, requestedBlocks, localBlacklistSize, currentMiningPos);
    }

    public static void logDestroyNavigationState(AltoClef mod,
                                                 DestroyBlockTask task,
                                                 BlockPos target,
                                                 String navigationState,
                                                 boolean reachPresent) {
        DestroyNavigationDiagnostics.log(mod, task, target, navigationState, reachPresent);
    }

    public static void logDestroyLifetimeStart(AltoClef mod,
                                               DestroyBlockTask task,
                                               BlockPos target,
                                               String forceCancelSource,
                                               BaritonePathDiagnosticSnapshot before) {
        DestroyBlockLifetimeDiagnostics.logStart(mod, task, target, forceCancelSource, before);
    }

    public static void logDestroyLifetimeStop(AltoClef mod,
                                              DestroyBlockTask task,
                                              BlockPos target,
                                              Task interruptTask,
                                              String forceCancelSource,
                                              BaritonePathDiagnosticSnapshot before) {
        DestroyBlockLifetimeDiagnostics.logStop(mod, task, target, interruptTask, forceCancelSource, before);
    }

    public static void logDestroyPhaseTransition(AltoClef mod,
                                                 DestroyBlockTask task,
                                                 BlockPos target,
                                                 String currentPhase,
                                                 boolean reachPresent,
                                                 boolean isCloseToMoveBack) {
        DestroyBlockPhaseDiagnostics.log(mod, task, target, currentPhase, reachPresent, isCloseToMoveBack);
    }

    public static void logExistingCancelBoundary(AltoClef mod,
                                                 Task task,
                                                 BlockPos target,
                                                 String cancelSource,
                                                 BaritonePathDiagnosticSnapshot before) {
        ExistingCancelBoundaryDiagnostics.log(mod, task, target, cancelSource, before);
    }

    public static void logGoalPathTransition(AltoClef mod,
                                             Task task,
                                             BlockPos target,
                                             String transition,
                                             Object goal,
                                             String reason) {
        GoalPathTransitionDiagnostics.log(mod, task, target, transition, goal, reason);
    }

    public static void logMovementProgressCheckResult(AltoClef mod,
                                                      Task task,
                                                      BlockPos target,
                                                      String checkerOwner,
                                                      int checkerCallIndex,
                                                      boolean checkEvaluated,
                                                      boolean checkResult,
                                                      String failureTransition,
                                                      boolean moveCheckFirstEvaluated,
                                                      Object moveCheckFirstResult,
                                                      boolean stuckCheckEvaluated,
                                                      Object stuckCheckResult,
                                                      boolean moveCheckSecondEvaluated,
                                                      Object moveCheckSecondResult) {
        MovementProgressDiagnostics.log(mod, task, target, checkerOwner, checkerCallIndex, checkEvaluated,
                checkResult, failureTransition, moveCheckFirstEvaluated, moveCheckFirstResult,
                stuckCheckEvaluated, stuckCheckResult, moveCheckSecondEvaluated, moveCheckSecondResult);
    }

    public static void logBlockUnreachableRequest(AltoClef mod,
                                                  Task task,
                                                  BlockPos target,
                                                  int requestedAllowedFailures,
                                                  String requestSource,
                                                  Task activeDestroyTask,
                                                  Task candidateDestroyTask) {
        BlockUnreachableRequestDiagnostics.log(mod, task, target, requestedAllowedFailures, requestSource,
                activeDestroyTask, candidateDestroyTask);
    }

    public static void logBlacklistStateChanged(AltoClef mod,
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
                                                String resetReason) {
        BlockBlacklistDiagnostics.log(mod, item, entryCreated, failureCountBefore, failureCountAfter,
                allowedFailuresBefore, requestedAllowedFailures, allowedFailuresAfter, unreachableBefore,
                unreachableAfter, currentDistanceSq, bestDistanceSqBefore, bestDistanceSqAfter,
                currentMiningRequirement, bestToolBefore, bestToolAfter, resetApplied, resetReason);
    }
}
