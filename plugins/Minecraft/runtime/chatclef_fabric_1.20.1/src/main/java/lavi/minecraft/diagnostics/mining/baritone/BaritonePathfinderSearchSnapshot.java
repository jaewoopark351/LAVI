package lavi.minecraft.diagnostics.mining.baritone;

import baritone.pathing.calc.PathNode;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;

//20260806_kpopmodder: Summarize read-only Baritone pathfinder search state after a calculation completes.
public final class BaritonePathfinderSearchSnapshot {
    private BaritonePathfinderSearchSnapshot() {
    }

    public static Object[] fields(int mapSize,
                                  PathNode startNode,
                                  PathNode mostRecentConsidered,
                                  PathNode[] bestSoFar) {
        BestNodeSummary best = bestNode(bestSoFar);
        return new Object[]{
                "searchMapSizeAfterCalculate", mapSize,
                "searchStartNodePresent", startNode != null,
                "searchStartNodeSummary", summarizeNode(startNode),
                "searchMostRecentNodePresent", mostRecentConsidered != null,
                "searchMostRecentNodeSummary", summarizeNode(mostRecentConsidered),
                "searchMostRecentNodePreviousPresent", mostRecentConsidered != null && mostRecentConsidered.previous != null,
                "searchBestSoFarPresent", best.present,
                "searchBestSoFarCount", best.count,
                "searchBestSoFarIndex", best.index,
                "searchBestSoFarSummary", best.summary,
                "searchBestSoFarPreviousPresent", best.previousPresent,
                "searchBestSoFarEstimatedCostToGoal", best.estimatedCostToGoal,
                "searchBestSoFarCost", best.cost,
                "searchBestSoFarCombinedCost", best.combinedCost
        };
    }

    private static BestNodeSummary bestNode(PathNode[] bestSoFar) {
        if (bestSoFar == null || bestSoFar.length == 0) {
            return BestNodeSummary.empty();
        }
        int count = 0;
        int selectedIndex = -1;
        PathNode selected = null;
        for (int index = 0; index < bestSoFar.length; index++) {
            PathNode node = bestSoFar[index];
            if (node == null) {
                continue;
            }
            count++;
            if (selected == null || node.estimatedCostToGoal < selected.estimatedCostToGoal) {
                selected = node;
                selectedIndex = index;
            }
        }
        if (selected == null) {
            return BestNodeSummary.empty();
        }
        return new BestNodeSummary(
                true,
                count,
                selectedIndex,
                summarizeNode(selected),
                selected.previous != null,
                selected.estimatedCostToGoal,
                selected.cost,
                selected.combinedCost
        );
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

    private record BestNodeSummary(boolean present,
                                   int count,
                                   int index,
                                   String summary,
                                   boolean previousPresent,
                                   double estimatedCostToGoal,
                                   double cost,
                                   double combinedCost) {
        static BestNodeSummary empty() {
            return new BestNodeSummary(false, 0, -1, "none", false,
                    Double.NaN, Double.NaN, Double.NaN);
        }
    }
}
