package lavi.minecraft.diagnostics.mining.baritone;

import baritone.api.pathing.goals.Goal;
import baritone.api.process.IBaritoneProcess;
import baritone.api.process.PathingCommand;
import lavi.minecraft.diagnostics.mining.baritone.process.BaritoneCustomGoalControlDiagnostics;
import lavi.minecraft.diagnostics.mining.baritone.process.BaritoneProcessCancelDiagnostics;
import lavi.minecraft.diagnostics.mining.baritone.process.BaritoneProcessTickDiagnostics;

import java.util.List;

//20260806_kpopmodder: Preserve the established Baritone process diagnostics API as a thin compatibility facade.
public final class BaritoneProcessControlDiagnostics {
    private BaritoneProcessControlDiagnostics() {
    }

    public static void logPreTickReturned(Object manager,
                                          IBaritoneProcess inControlLastTick,
                                          IBaritoneProcess inControlThisTick,
                                          PathingCommand command,
                                          List<IBaritoneProcess> activeProcesses) {
        BaritoneProcessTickDiagnostics.log(
                manager, inControlLastTick, inControlThisTick, command, activeProcesses
        );
    }

    public static void logCancelEverythingBoundary(Object manager,
                                                   String phase,
                                                   IBaritoneProcess inControlLastTick,
                                                   IBaritoneProcess inControlThisTick,
                                                   PathingCommand command,
                                                   List<IBaritoneProcess> activeProcesses) {
        BaritoneProcessCancelDiagnostics.log(
                manager, phase, inControlLastTick, inControlThisTick, command, activeProcesses
        );
    }

    public static void logCustomGoalLostControl(Object process,
                                                String phase,
                                                Goal goal,
                                                Goal mostRecentGoal,
                                                String state,
                                                String active) {
        BaritoneCustomGoalControlDiagnostics.logLostControl(
                process, phase, goal, mostRecentGoal, state, active
        );
    }
}
