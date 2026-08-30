package lavi.minecraft.diagnostics.mining.baritone;

import baritone.api.pathing.calc.IPath;
import baritone.api.pathing.goals.Goal;
import baritone.api.process.IBaritoneProcess;
import baritone.api.process.PathingCommand;
import baritone.pathing.calc.AbstractNodeCostSearch;
import baritone.pathing.path.PathExecutor;

//20260806_kpopmodder: Keep this public compatibility facade separate from focused Baritone formatters.
public final class BaritonePathObjectFormatters {
    private BaritonePathObjectFormatters() {
    }

    public static String identity(Object value) {
        return BaritoneDiagnosticIdentityFormatter.identity(value);
    }

    public static String className(Object value) {
        return BaritoneDiagnosticIdentityFormatter.className(value);
    }

    public static String summarizeObject(Object value) {
        return BaritonePathDiagnosticFormatter.summarizeObject(value);
    }

    public static String summarizeGoal(Goal goal) {
        return BaritonePathDiagnosticFormatter.summarizeGoal(goal);
    }

    public static String summarizeFinder(AbstractNodeCostSearch finder) {
        return BaritonePathDiagnosticFormatter.summarizeFinder(finder);
    }

    public static String summarizePath(IPath path) {
        return BaritonePathDiagnosticFormatter.summarizePath(path);
    }

    public static String summarizeExecutor(PathExecutor executor) {
        return BaritonePathDiagnosticFormatter.summarizeExecutor(executor);
    }

    public static String summarizeProcess(IBaritoneProcess process) {
        return BaritoneProcessDiagnosticFormatter.summarizeProcess(process);
    }

    public static String commandType(PathingCommand command) {
        return BaritoneProcessDiagnosticFormatter.commandType(command);
    }

    public static String commandGoalType(PathingCommand command) {
        return BaritoneProcessDiagnosticFormatter.commandGoalType(command);
    }

    public static String commandGoalSummary(PathingCommand command) {
        return BaritoneProcessDiagnosticFormatter.commandGoalSummary(command);
    }

    public static String safeValue(Object value) {
        return BaritoneDiagnosticIdentityFormatter.safeValue(value);
    }

    public static boolean executorUsesPath(PathExecutor executor, IPath path) {
        return BaritoneExecutorPathObservation.executorUsesPath(executor, path);
    }
}
