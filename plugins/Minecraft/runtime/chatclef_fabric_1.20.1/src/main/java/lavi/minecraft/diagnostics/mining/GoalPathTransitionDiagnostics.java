package lavi.minecraft.diagnostics.mining;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import net.minecraft.util.math.BlockPos;

//20260806_kpopmodder: Observe submitted goals and visible Baritone path state without owning pathing.
final class GoalPathTransitionDiagnostics {
    private GoalPathTransitionDiagnostics() {
    }

    static void log(AltoClef mod,
                    Task task,
                    BlockPos target,
                    String transition,
                    Object goal,
                    String reason) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        if ("GOAL_SUBMITTED".equals(transition)) {
            SubmittedGoalDiagnosticState.remember(task, target, goal, transition, reason);
        }
        SubmittedGoalDiagnosticState.SubmittedGoal submittedGoal = SubmittedGoalDiagnosticState.get(task);
        Object observedGoal = goal == null && submittedGoal != null ? submittedGoal.goal : goal;
        String goalMatchesTarget = SubmittedGoalDiagnosticState.matchesTarget(submittedGoal, target);
        String fingerprint = MiningDiagnosticEmitter.joinFingerprint(
                "BARITONE_GOAL_PATH_TRANSITION",
                transition,
                ChatClefDiagnostics.blockPos(target),
                ChatClefDiagnostics.className(observedGoal),
                goalMatchesTarget
        );
        MiningDiagnosticEmitter.emitLazy("BARITONE_GOAL_PATH_TRANSITION", reason, task,
                "baritone_goal_path|" + System.identityHashCode(task),
                fingerprint,
                () -> {
                    BaritonePathDiagnosticSnapshot snapshot = BaritonePathDiagnosticSnapshot.capture(
                            mod, target, observedGoal, goalMatchesTarget);
                    return MiningDiagnosticEmitter.merge(new Object[]{
                        "owner", "baritone_path_observer",
                        "trigger", transition,
                        "transition", transition,
                        "targetPosition", ChatClefDiagnostics.blockPos(target),
                        "planningStartTick", "unavailable_public_api",
                        "planningElapsedTicks", "unavailable_public_api",
                        "existingCancellationReason", "unavailable_public_api"
                    }, SubmittedGoalDiagnosticState.fields(submittedGoal, target), snapshot.fields());
                });
    }
}
