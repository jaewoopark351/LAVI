package lavi.minecraft.diagnostics.mining.baritone;

import baritone.api.pathing.calc.IPath;
import baritone.api.pathing.goals.Goal;
import baritone.pathing.calc.AbstractNodeCostSearch;
import baritone.pathing.path.PathExecutor;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;

//20260830_kpopmodder: Format path objects without re-evaluating a pathfinder completion predicate.
final class BaritonePathDiagnosticFormatter {
    static final String FINISH_OBSERVATION_UNAVAILABLE = "NOT_CAPTURED_WITHOUT_BEHAVIOR_REEVALUATION";

    private BaritonePathDiagnosticFormatter() {
    }

    static String summarizeObject(Object value) {
        if (value == null) {
            return "none";
        }
        return BaritoneDiagnosticIdentityFormatter.className(value)
                + "#"
                + BaritoneDiagnosticIdentityFormatter.identity(value)
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
        return BaritoneDiagnosticIdentityFormatter.className(finder)
                + "#"
                + BaritoneDiagnosticIdentityFormatter.identity(finder)
                + ",start="
                + ChatClefDiagnostics.safeValueForDiagnosticLog(finder::getStart)
                + ",goal="
                + ChatClefDiagnostics.safeValueForDiagnosticLog(finder::getGoal)
                + ",finished="
                + FINISH_OBSERVATION_UNAVAILABLE;
    }

    static String summarizePath(IPath path) {
        if (path == null) {
            return "none";
        }
        return BaritoneDiagnosticIdentityFormatter.className(path)
                + "#"
                + BaritoneDiagnosticIdentityFormatter.identity(path)
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
        return BaritoneDiagnosticIdentityFormatter.className(executor)
                + "#"
                + BaritoneDiagnosticIdentityFormatter.identity(executor)
                + ",position="
                + ChatClefDiagnostics.safeValueForDiagnosticLog(executor::getPosition)
                + ",failed="
                + ChatClefDiagnostics.safeValueForDiagnosticLog(executor::failed)
                + ",finished="
                + ChatClefDiagnostics.safeValueForDiagnosticLog(executor::finished)
                + ",path="
                + ChatClefDiagnostics.safeValueForDiagnosticLog(() -> summarizePath(executor.getPath()));
    }
}
