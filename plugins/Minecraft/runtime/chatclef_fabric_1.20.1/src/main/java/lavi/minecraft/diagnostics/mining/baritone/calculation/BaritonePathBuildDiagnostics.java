package lavi.minecraft.diagnostics.mining.baritone.calculation;

import baritone.api.pathing.calc.IPath;
import baritone.api.pathing.goals.Goal;
import baritone.api.utils.BetterBlockPos;
import baritone.pathing.calc.PathNode;
import baritone.pathing.movement.CalculationContext;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.mining.MiningDiagnosticEmitter;
import lavi.minecraft.diagnostics.mining.baritone.BaritonePathObjectFormatters;

//20260730_kpopmodder: Added diagnostic logging to prove the Carry On interaction failure boundary.
public final class BaritonePathBuildDiagnostics {
    private BaritonePathBuildDiagnostics() {
    }

    public static void logEnter(Object path,
                                BetterBlockPos realStart,
                                PathNode startNode,
                                PathNode endNode,
                                int numNodes,
                                Goal goal,
                                CalculationContext context) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        CalculationDiagnosticRecord record = CalculationDiagnosticRegistry.activeRecordForPath(path);
        if (record == null || path == null) {
            return;
        }
        String pathIdentity = BaritonePathObjectFormatters.identity(path);
        synchronized (record) {
            record.pathBuildStartNanos = System.nanoTime();
            record.rawPathId = pathIdentity;
        }
        CalculationDiagnosticRegistry.bindPath(path, record);
        BaritoneCalculationPhaseDiagnostics.emit(
                record,
                record.pathfinder,
                "PATH_BUILD_ENTER",
                "baritone_path_constructor_head",
                "path_build|" + pathIdentity,
                -1,
                "unavailable_from_path_phase_mixin",
                () -> BaritonePathBuildPayload.fields(
                        path, realStart, startNode, endNode, numNodes, goal, context, false)
        );
    }

    public static void logReturn(Object path,
                                 BetterBlockPos realStart,
                                 PathNode startNode,
                                 PathNode endNode,
                                 int numNodes,
                                 Goal goal,
                                 CalculationContext context) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        CalculationDiagnosticRecord record = CalculationDiagnosticRegistry.activeRecordForPath(path);
        if (record == null || path == null) {
            return;
        }
        CalculationDiagnosticRegistry.bindPath(path, record);
        IPath rawPath = path instanceof IPath ? (IPath) path : null;
        String pathIdentity = BaritonePathObjectFormatters.identity(path);
        long elapsedMillis;
        synchronized (record) {
            record.rawPathPresent = rawPath != null;
            record.rawPath = rawPath;
            record.rawPathId = pathIdentity;
            elapsedMillis = elapsedMillisSince(record.pathBuildStartNanos);
        }
        BaritoneCalculationPhaseDiagnostics.emit(
                record,
                record.pathfinder,
                "PATH_BUILD_RETURN",
                "baritone_path_constructor_return",
                "path_build|" + pathIdentity,
                elapsedMillis,
                "unavailable_from_path_phase_mixin",
                () -> returnFields(record, path, rawPath, realStart, startNode, endNode, numNodes, goal, context)
        );
    }

    private static Object[] returnFields(CalculationDiagnosticRecord record,
                                         Object path,
                                         IPath rawPath,
                                         BetterBlockPos realStart,
                                         PathNode startNode,
                                         PathNode endNode,
                                         int numNodes,
                                         Goal goal,
                                         CalculationContext context) {
        String rawPathSummary = BaritonePathObjectFormatters.summarizePath(rawPath);
        synchronized (record) {
            record.rawPathSummary = rawPathSummary;
        }
        return MiningDiagnosticEmitter.merge(
                BaritonePathBuildPayload.fields(
                        path, realStart, startNode, endNode, numNodes, goal, context, true),
                new Object[]{
                        "rawResultPathPresent", record.rawPathPresent,
                        "rawResultPathIdentity", record.rawPathId,
                        "rawResultPathSummary", record.rawPathSummary
                }
        );
    }

    private static long elapsedMillisSince(long startedAtNanos) {
        if (startedAtNanos <= 0) {
            return -1;
        }
        return (System.nanoTime() - startedAtNanos) / 1_000_000L;
    }
}
