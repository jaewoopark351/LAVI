package lavi.minecraft.diagnostics.mining.baritone;

import baritone.api.pathing.goals.Goal;
import baritone.api.process.IBaritoneProcess;
import baritone.api.process.PathingCommand;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.mining.MiningDiagnosticEmitter;

import java.util.List;

//20260806_kpopmodder: Observe Baritone process control transitions without changing controller ownership.
public final class BaritoneProcessControlDiagnostics {
    private BaritoneProcessControlDiagnostics() {
    }

    public static void logPreTickReturned(Object manager,
                                          IBaritoneProcess inControlLastTick,
                                          IBaritoneProcess inControlThisTick,
                                          PathingCommand command,
                                          List<IBaritoneProcess> activeProcesses) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        String fingerprint = MiningDiagnosticEmitter.joinFingerprint(
                "BARITONE_PROCESS_CONTROL_TICK",
                BaritonePathObjectFormatters.identity(manager),
                BaritonePathObjectFormatters.identity(inControlLastTick),
                BaritonePathObjectFormatters.identity(inControlThisTick),
                BaritonePathObjectFormatters.commandType(command),
                BaritonePathObjectFormatters.commandGoalSummary(command),
                activeProcessSummary(activeProcesses)
        );
        BaritoneDiagnosticEmitter.emit("BARITONE_PROCESS_CONTROL_TICK", "baritone_process_control_tick",
                "baritone_pathing_control_manager_observer", "pathing_control_manager_pre_tick_returned",
                "baritone_process_control|" + BaritonePathObjectFormatters.identity(manager),
                fingerprint,
                new Object[]{
                        "managerIdentity", BaritonePathObjectFormatters.identity(manager),
                        "inControlLastTickSummary", BaritonePathObjectFormatters.summarizeProcess(inControlLastTick),
                        "inControlThisTickSummary", BaritonePathObjectFormatters.summarizeProcess(inControlThisTick),
                        "commandType", BaritonePathObjectFormatters.commandType(command),
                        "commandGoalType", BaritonePathObjectFormatters.commandGoalType(command),
                        "commandGoalSummary", BaritonePathObjectFormatters.commandGoalSummary(command),
                        "activeProcessCount", activeProcessCount(activeProcesses),
                        "activeProcessSummary", activeProcessSummary(activeProcesses)
                });
    }

    public static void logCancelEverythingBoundary(Object manager,
                                                   String phase,
                                                   IBaritoneProcess inControlLastTick,
                                                   IBaritoneProcess inControlThisTick,
                                                   PathingCommand command,
                                                   List<IBaritoneProcess> activeProcesses) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        String fingerprint = MiningDiagnosticEmitter.joinFingerprint(
                "BARITONE_PROCESS_CANCEL_EVERYTHING_BOUNDARY",
                BaritonePathObjectFormatters.identity(manager),
                phase,
                BaritonePathObjectFormatters.identity(inControlLastTick),
                BaritonePathObjectFormatters.identity(inControlThisTick),
                BaritonePathObjectFormatters.commandType(command),
                activeProcessSummary(activeProcesses)
        );
        BaritoneDiagnosticEmitter.emit("BARITONE_PROCESS_CANCEL_EVERYTHING_BOUNDARY", "baritone_process_cancel_everything_boundary",
                "baritone_pathing_control_manager_observer", "pathing_control_manager_cancel_everything_" + phase,
                "baritone_process_cancel|" + BaritonePathObjectFormatters.identity(manager),
                fingerprint,
                new Object[]{
                        "phase", phase,
                        "managerIdentity", BaritonePathObjectFormatters.identity(manager),
                        "inControlLastTickSummary", BaritonePathObjectFormatters.summarizeProcess(inControlLastTick),
                        "inControlThisTickSummary", BaritonePathObjectFormatters.summarizeProcess(inControlThisTick),
                        "commandType", BaritonePathObjectFormatters.commandType(command),
                        "commandGoalType", BaritonePathObjectFormatters.commandGoalType(command),
                        "commandGoalSummary", BaritonePathObjectFormatters.commandGoalSummary(command),
                        "activeProcessCount", activeProcessCount(activeProcesses),
                        "activeProcessSummary", activeProcessSummary(activeProcesses)
                });
    }

    public static void logCustomGoalLostControl(Object process,
                                                String phase,
                                                Goal goal,
                                                Goal mostRecentGoal,
                                                String state,
                                                String active) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        String fingerprint = MiningDiagnosticEmitter.joinFingerprint(
                "BARITONE_CUSTOM_GOAL_LOST_CONTROL",
                BaritonePathObjectFormatters.identity(process),
                phase,
                state,
                active,
                BaritonePathObjectFormatters.summarizeGoal(goal),
                BaritonePathObjectFormatters.summarizeGoal(mostRecentGoal)
        );
        BaritoneDiagnosticEmitter.emit("BARITONE_CUSTOM_GOAL_LOST_CONTROL", "baritone_custom_goal_lost_control",
                "baritone_custom_goal_process_observer", "custom_goal_process_on_lost_control_" + phase,
                "baritone_custom_goal|" + BaritonePathObjectFormatters.identity(process),
                fingerprint,
                new Object[]{
                        "phase", phase,
                        "customGoalProcessIdentity", BaritonePathObjectFormatters.identity(process),
                        "customGoalProcessState", state,
                        "customGoalProcessActive", active,
                        "customGoalType", BaritonePathObjectFormatters.className(goal),
                        "customGoalSummary", BaritonePathObjectFormatters.summarizeGoal(goal),
                        "mostRecentGoalType", BaritonePathObjectFormatters.className(mostRecentGoal),
                        "mostRecentGoalSummary", BaritonePathObjectFormatters.summarizeGoal(mostRecentGoal)
                });
    }

    private static int activeProcessCount(List<IBaritoneProcess> activeProcesses) {
        if (activeProcesses == null) {
            return -1;
        }
        try {
            return activeProcesses.size();
        } catch (RuntimeException | LinkageError error) {
            return -1;
        }
    }

    private static String activeProcessSummary(List<IBaritoneProcess> activeProcesses) {
        if (activeProcesses == null) {
            return "none";
        }
        try {
            StringBuilder builder = new StringBuilder();
            int limit = Math.min(activeProcesses.size(), 6);
            for (int index = 0; index < limit; index++) {
                if (index > 0) {
                    builder.append(",");
                }
                builder.append(BaritonePathObjectFormatters.summarizeProcess(activeProcesses.get(index)));
            }
            if (activeProcesses.size() > limit) {
                builder.append(",...");
            }
            return builder.toString();
        } catch (RuntimeException | LinkageError error) {
            return "error=" + error.getClass().getSimpleName();
        }
    }
}
