package lavi.minecraft.diagnostics.mining.baritone.calculation;

import baritone.api.pathing.goals.Goal;
import baritone.api.process.PathingCommand;
import baritone.api.utils.BetterBlockPos;
import baritone.behavior.PathingBehavior;
import baritone.pathing.calc.AbstractNodeCostSearch;
import baritone.pathing.path.PathExecutor;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.mining.MiningDiagnosticEmitter;
import lavi.minecraft.diagnostics.mining.baritone.BaritoneDiagnosticEmitter;
import lavi.minecraft.diagnostics.mining.baritone.BaritonePathObjectFormatters;

//20260730_kpopmodder: Added diagnostic logging to prove the Carry On interaction failure boundary.
public final class BaritoneGoalRequestDecisionDiagnostics {
    private BaritoneGoalRequestDecisionDiagnostics() {
    }

    public static void log(PathingBehavior behavior,
                           PathingCommand command,
                           boolean accepted,
                           PathExecutor current,
                           PathExecutor next,
                           AbstractNodeCostSearch inProgress,
                           Goal activeGoal,
                           BetterBlockPos expectedSegmentStart,
                           boolean cancelRequested,
                           boolean calcFailedLastTick) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        String behaviorIdentity = BaritonePathObjectFormatters.identity(behavior);
        String rejectReason = deriveRejectReason(accepted, command, current, inProgress);
        String fingerprint = MiningDiagnosticEmitter.joinFingerprint(
                "BARITONE_GOAL_REQUEST_DECISION",
                behaviorIdentity,
                BaritonePathObjectFormatters.commandType(command),
                Boolean.toString(accepted),
                rejectReason,
                BaritonePathObjectFormatters.identity(current),
                BaritonePathObjectFormatters.identity(next),
                BaritonePathObjectFormatters.identity(inProgress),
                Boolean.toString(cancelRequested),
                Boolean.toString(calcFailedLastTick)
        );
        BaritoneDiagnosticEmitter.emitLazy(
                "BARITONE_GOAL_REQUEST_DECISION",
                "baritone_goal_request_decision",
                "baritone_pathing_behavior_observer",
                "secret_internal_set_goal_and_path_returned",
                "baritone_goal_request|" + behaviorIdentity,
                fingerprint,
                () -> new Object[]{
                        "requestAccepted", accepted,
                        "requestRejectedReason", rejectReason,
                        "commandType", BaritonePathObjectFormatters.commandType(command),
                        "commandGoalType", BaritonePathObjectFormatters.commandGoalType(command),
                        "commandGoalSummary", BaritonePathObjectFormatters.commandGoalSummary(command),
                        "pathingGoalType", BaritonePathObjectFormatters.className(activeGoal),
                        "pathingGoalSummary", BaritonePathObjectFormatters.summarizeGoal(activeGoal),
                        "currentExecutorSummary", BaritonePathObjectFormatters.summarizeExecutor(current),
                        "nextExecutorSummary", BaritonePathObjectFormatters.summarizeExecutor(next),
                        "inProgressSummary", BaritonePathObjectFormatters.summarizeFinder(inProgress),
                        "expectedSegmentStart", BaritonePathObjectFormatters.safeValue(expectedSegmentStart),
                        "cancelRequested", cancelRequested,
                        "calcFailedLastTick", calcFailedLastTick
                }
        );
    }

    private static String deriveRejectReason(boolean accepted,
                                             PathingCommand command,
                                             PathExecutor current,
                                             AbstractNodeCostSearch inProgress) {
        if (accepted) {
            return "ACCEPTED";
        }
        if (command == null) {
            return "COMMAND_NULL";
        }
        if (command.goal == null) {
            return "GOAL_NULL";
        }
        if (current != null) {
            return "CURRENT_PATH_PRESENT";
        }
        if (inProgress != null) {
            return "IN_PROGRESS_PRESENT";
        }
        return "UNKNOWN_REJECTED_OR_ALREADY_IN_GOAL";
    }
}
