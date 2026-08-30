package lavi.minecraft.diagnostics.mining.baritone.calculation;

import baritone.api.pathing.calc.IPath;
import baritone.api.pathing.goals.Goal;
import baritone.api.utils.BetterBlockPos;
import baritone.pathing.calc.PathNode;
import baritone.pathing.movement.CalculationContext;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.mining.baritone.BaritonePathObjectFormatters;

//20260730_kpopmodder: Added diagnostic logging to prove the Carry On interaction failure boundary.
final class BaritonePathBuildPayload {
    private BaritonePathBuildPayload() {
    }

    static Object[] fields(Object path,
                           BetterBlockPos realStart,
                           PathNode startNode,
                           PathNode endNode,
                           int numNodes,
                           Goal goal,
                           CalculationContext context,
                           boolean includePathSummary) {
        IPath rawPath = includePathSummary && path instanceof IPath ? (IPath) path : null;
        return new Object[]{
                "pathIdentity", BaritonePathObjectFormatters.identity(path),
                "pathType", BaritonePathObjectFormatters.className(path),
                "pathSummary", includePathSummary
                        ? BaritonePathObjectFormatters.summarizePath(rawPath)
                        : "unavailable_before_constructor_return",
                "pathRealStart", BaritonePathObjectFormatters.safeValue(realStart),
                "pathStartNodePresent", startNode != null,
                "pathStartNodeSummary", summarizeNode(startNode),
                "pathEndNodePresent", endNode != null,
                "pathEndNodeSummary", summarizeNode(endNode),
                "pathEndNodePreviousPresent", endNode != null && endNode.previous != null,
                "pathNumNodesConsidered", numNodes,
                "pathGoalType", BaritonePathObjectFormatters.className(goal),
                "pathGoalSummary", BaritonePathObjectFormatters.summarizeGoal(goal),
                "calculationContextType", BaritonePathObjectFormatters.className(context)
        };
    }

    private static String summarizeNode(PathNode node) {
        if (node == null) {
            return "none";
        }
        return "PathNode{x="
                + node.x
                + ",y="
                + node.y
                + ",z="
                + node.z
                + ",cost="
                + ChatClefDiagnostics.safeValueForDiagnosticLog(() -> node.cost)
                + ",estimatedCostToGoal="
                + ChatClefDiagnostics.safeValueForDiagnosticLog(() -> node.estimatedCostToGoal)
                + ",combinedCost="
                + ChatClefDiagnostics.safeValueForDiagnosticLog(() -> node.combinedCost)
                + ",open="
                + ChatClefDiagnostics.safeValueForDiagnosticLog(node::isOpen)
                + "}";
    }
}
