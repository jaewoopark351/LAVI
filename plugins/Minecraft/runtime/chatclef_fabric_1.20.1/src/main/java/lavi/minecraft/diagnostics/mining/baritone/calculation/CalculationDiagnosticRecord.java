package lavi.minecraft.diagnostics.mining.baritone.calculation;

import baritone.api.pathing.calc.IPath;
import baritone.pathing.calc.AbstractNodeCostSearch;

//20260730_kpopmodder: Added diagnostic logging to prove the Carry On interaction failure boundary.
final class CalculationDiagnosticRecord {
    final long generationId;
    final int pathfinderIdentity;
    final AbstractNodeCostSearch pathfinder;
    String pathingBehaviorId = "unavailable";
    String requestedGoalType = "unavailable";
    String requestedGoalSummary = "unavailable";
    String pathfinderGoalType = "unavailable";
    String pathfinderGoalSummary = "unavailable";
    String pathStartSummary = "unavailable";
    String pathfinderStartSummary = "unavailable";
    boolean firstSegment;
    long primaryTimeoutMs = -1;
    long failureTimeoutMs = -1;
    long calculateStartNanos = -1;
    long calculate0StartNanos = -1;
    long pathBuildStartNanos = -1;
    long postProcessStartNanos = -1;
    String resultType = "unobserved";
    boolean resultPathPresent;
    String resultPathId = "none";
    String resultPathSummary = "none";
    IPath resultPath;
    boolean rawPathPresent;
    String rawPathId = "none";
    String rawPathSummary = "none";
    IPath rawPath;
    boolean postProcessedPathPresent;
    String postProcessedPathId = "none";
    String postProcessedPathSummary = "none";
    IPath postProcessedPath;
    long elapsedMillis = -1;
    boolean cancelRequestedAfterCalculate;

    CalculationDiagnosticRecord(long generationId, AbstractNodeCostSearch pathfinder) {
        this.generationId = generationId;
        this.pathfinderIdentity = System.identityHashCode(pathfinder);
        this.pathfinder = pathfinder;
    }

    IPath resultPath() {
        try {
            if (!resultPathPresent) {
                return null;
            }
            return resultPath;
        } catch (RuntimeException | LinkageError error) {
            return null;
        }
    }
}
