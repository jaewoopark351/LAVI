package lavi.minecraft.diagnostics.mining.baritone.process;

import baritone.api.pathing.goals.Goal;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.mining.MiningDiagnosticEmitter;
import lavi.minecraft.diagnostics.mining.baritone.BaritoneDiagnosticEmitter;
import lavi.minecraft.diagnostics.mining.baritone.BaritonePathObjectFormatters;

//20260730_kpopmodder: Added diagnostic logging to prove the Carry On interaction failure boundary.
public final class BaritoneCustomGoalControlDiagnostics {
    private BaritoneCustomGoalControlDiagnostics() {
    }

    public static void logLostControl(Object process,
                                      String phase,
                                      Goal goal,
                                      Goal mostRecentGoal,
                                      String state,
                                      String active) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        String processIdentity = BaritonePathObjectFormatters.identity(process);
        String fingerprint = MiningDiagnosticEmitter.joinFingerprint(
                "BARITONE_CUSTOM_GOAL_LOST_CONTROL",
                phase,
                state,
                active,
                BaritonePathObjectFormatters.className(goal),
                BaritonePathObjectFormatters.className(mostRecentGoal)
        );
        BaritoneDiagnosticEmitter.emitLazy(
                "BARITONE_CUSTOM_GOAL_LOST_CONTROL",
                "baritone_custom_goal_lost_control",
                "baritone_custom_goal_process_observer",
                "custom_goal_process_on_lost_control_" + phase,
                "baritone_custom_goal|" + processIdentity,
                fingerprint,
                () -> new Object[]{
                        "phase", phase,
                        "customGoalProcessIdentity", processIdentity,
                        "customGoalProcessState", state,
                        "customGoalProcessActive", active,
                        "customGoalType", BaritonePathObjectFormatters.className(goal),
                        "customGoalSummary", BaritonePathObjectFormatters.summarizeGoal(goal),
                        "mostRecentGoalType", BaritonePathObjectFormatters.className(mostRecentGoal),
                        "mostRecentGoalSummary", BaritonePathObjectFormatters.summarizeGoal(mostRecentGoal)
                }
        );
    }
}
