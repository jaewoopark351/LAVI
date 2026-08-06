package lavi.minecraft.diagnostics.mining;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import net.minecraft.util.math.BlockPos;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

//20260806_kpopmodder: Track only diagnostic goal observations for DestroyBlockTask instances.
final class SubmittedGoalDiagnosticState {
    private static final Map<Task, SubmittedGoal> LAST_SUBMITTED_GOAL =
            Collections.synchronizedMap(new WeakHashMap<>());

    private SubmittedGoalDiagnosticState() {
    }

    static synchronized void remember(Task task, BlockPos target, Object goal, String transition, String reason) {
        if (task != null && target != null && goal != null) {
            LAST_SUBMITTED_GOAL.put(task, new SubmittedGoal(
                    target.toImmutable(),
                    goal,
                    ChatClefDiagnostics.className(goal),
                    ChatClefDiagnostics.safeValue(() -> goal),
                    Integer.toHexString(System.identityHashCode(goal)),
                    ChatClefDiagnostics.currentClientTickId(),
                    System.nanoTime(),
                    nextSubmissionSequence(),
                    transition,
                    reason
            ));
        }
    }

    static SubmittedGoal get(Task task) {
        return task == null ? null : LAST_SUBMITTED_GOAL.get(task);
    }

    static String matchesTarget(SubmittedGoal submittedGoal, BlockPos target) {
        return submittedGoal == null ? "unavailable" : Boolean.toString(target != null && target.equals(submittedGoal.target));
    }

    static Object[] fields(SubmittedGoal submittedGoal, BlockPos currentTarget) {
        if (submittedGoal == null) {
            return new Object[]{
                    "submittedGoalPresent", false,
                    "submittedGoalTargetPosition", "unavailable",
                    "submittedGoalMatchesCurrentTarget", "unavailable",
                    "submittedGoalType", "unavailable",
                    "submittedGoalSummary", "unavailable",
                    "submittedGoalIdentity", "unavailable",
                    "submittedGoalClientTickId", "unavailable",
                    "submittedGoalAgeTicks", "unavailable",
                    "submittedGoalAgeMillis", "unavailable",
                    "submittedGoalSequence", "unavailable",
                    "submittedGoalTransition", "unavailable",
                    "submittedGoalReason", "unavailable"
            };
        }
        return new Object[]{
                "submittedGoalPresent", true,
                "submittedGoalTargetPosition", ChatClefDiagnostics.blockPos(submittedGoal.target),
                "submittedGoalMatchesCurrentTarget", matchesTarget(submittedGoal, currentTarget),
                "submittedGoalType", submittedGoal.goalType,
                "submittedGoalSummary", submittedGoal.goalSummary,
                "submittedGoalIdentity", submittedGoal.goalIdentity,
                "submittedGoalClientTickId", submittedGoal.submittedClientTickId,
                "submittedGoalAgeTicks", ChatClefDiagnostics.currentClientTickId() - submittedGoal.submittedClientTickId,
                "submittedGoalAgeMillis", (System.nanoTime() - submittedGoal.submittedNanoTime) / 1_000_000L,
                "submittedGoalSequence", submittedGoal.submissionSequence,
                "submittedGoalTransition", submittedGoal.transition,
                "submittedGoalReason", submittedGoal.reason
        };
    }

    private static long nextSubmissionSequence() {
        return ++submissionSequence;
    }

    private static long submissionSequence;

    static final class SubmittedGoal {
        final BlockPos target;
        final Object goal;
        final String goalType;
        final String goalSummary;
        final String goalIdentity;
        final long submittedClientTickId;
        final long submittedNanoTime;
        final long submissionSequence;
        final String transition;
        final String reason;

        SubmittedGoal(BlockPos target,
                      Object goal,
                      String goalType,
                      String goalSummary,
                      String goalIdentity,
                      long submittedClientTickId,
                      long submittedNanoTime,
                      long submissionSequence,
                      String transition,
                      String reason) {
            this.target = target;
            this.goal = goal;
            this.goalType = goalType;
            this.goalSummary = goalSummary;
            this.goalIdentity = goalIdentity;
            this.submittedClientTickId = submittedClientTickId;
            this.submittedNanoTime = submittedNanoTime;
            this.submissionSequence = submissionSequence;
            this.transition = transition;
            this.reason = reason;
        }
    }
}
