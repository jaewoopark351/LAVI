package lavi.minecraft.diagnostics.mining.baritone;

import baritone.api.pathing.calc.IPath;
import baritone.api.pathing.goals.Goal;
import baritone.api.process.IBaritoneProcess;
import baritone.api.process.PathingCommand;
import baritone.pathing.calc.AbstractNodeCostSearch;
import baritone.pathing.path.PathExecutor;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;

//20260806_kpopmodder: Keep Baritone diagnostic value formatting separate from behavior observation.
final class BaritonePathObjectFormatters {
    private BaritonePathObjectFormatters() {
    }

    static String identity(Object value) {
        if (value == null) {
            return "none";
        }
        return Integer.toHexString(System.identityHashCode(value));
    }

    static String className(Object value) {
        return value == null ? "none" : ChatClefDiagnostics.className(value);
    }

    static String summarizeObject(Object value) {
        if (value == null) {
            return "none";
        }
        return className(value)
                + "#"
                + identity(value)
                + ":"
                + ChatClefDiagnostics.safeValueForDiagnosticLog(() -> value);
    }

    static String summarizeGoal(Goal goal) {
        return goal == null ? "none" : summarizeObject(goal);
    }

    static String summarizeFinder(AbstractNodeCostSearch finder) {
        if (finder == null) {
            return "none";
        }
        return className(finder)
                + "#"
                + identity(finder)
                + ",start="
                + ChatClefDiagnostics.safeValueForDiagnosticLog(finder::getStart)
                + ",goal="
                + ChatClefDiagnostics.safeValueForDiagnosticLog(finder::getGoal)
                + ",finished="
                + ChatClefDiagnostics.safeValueForDiagnosticLog(finder::isFinished);
    }

    static String summarizePath(IPath path) {
        if (path == null) {
            return "none";
        }
        return className(path)
                + "#"
                + identity(path)
                + ",length="
                + ChatClefDiagnostics.safeValueForDiagnosticLog(path::length)
                + ",movements="
                + ChatClefDiagnostics.safeValueForDiagnosticLog(() -> path.movements().size())
                + ",positions="
                + ChatClefDiagnostics.safeValueForDiagnosticLog(() -> path.positions().size())
                + ",src="
                + ChatClefDiagnostics.safeValueForDiagnosticLog(path::getSrc)
                + ",dest="
                + ChatClefDiagnostics.safeValueForDiagnosticLog(path::getDest)
                + ",goal="
                + ChatClefDiagnostics.safeValueForDiagnosticLog(path::getGoal)
                + ",nodesConsidered="
                + ChatClefDiagnostics.safeValueForDiagnosticLog(path::getNumNodesConsidered);
    }

    static String summarizeExecutor(PathExecutor executor) {
        if (executor == null) {
            return "none";
        }
        return className(executor)
                + "#"
                + identity(executor)
                + ",position="
                + ChatClefDiagnostics.safeValueForDiagnosticLog(executor::getPosition)
                + ",failed="
                + ChatClefDiagnostics.safeValueForDiagnosticLog(executor::failed)
                + ",finished="
                + ChatClefDiagnostics.safeValueForDiagnosticLog(executor::finished)
                + ",path="
                + ChatClefDiagnostics.safeValueForDiagnosticLog(() -> summarizePath(executor.getPath()));
    }

    static String summarizeProcess(IBaritoneProcess process) {
        if (process == null) {
            return "none";
        }
        return className(process)
                + "#"
                + identity(process)
                + ",displayName="
                + ChatClefDiagnostics.safeValueForDiagnosticLog(process::displayName)
                + ",active="
                + ChatClefDiagnostics.safeValueForDiagnosticLog(process::isActive)
                + ",temporary="
                + ChatClefDiagnostics.safeValueForDiagnosticLog(process::isTemporary);
    }

    static String commandType(PathingCommand command) {
        return command == null ? "none" : String.valueOf(command.commandType);
    }

    static String commandGoalType(PathingCommand command) {
        return command == null ? "none" : className(command.goal);
    }

    static String commandGoalSummary(PathingCommand command) {
        return command == null ? "none" : summarizeGoal(command.goal);
    }

    static String safeValue(Object value) {
        return ChatClefDiagnostics.safeValueForDiagnosticLog(() -> value);
    }

    static boolean executorUsesPath(PathExecutor executor, IPath path) {
        if (executor == null || path == null) {
            return false;
        }
        try {
            return executor.getPath() == path;
        } catch (RuntimeException | LinkageError error) {
            return false;
        }
    }
}
