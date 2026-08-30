package lavi.minecraft.diagnostics.mining.baritone.calculation;

import baritone.api.pathing.calc.IPath;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.mining.baritone.BaritonePathObjectFormatters;

//20260730_kpopmodder: Added diagnostic logging to prove the Carry On interaction failure boundary.
public final class BaritonePathPostProcessDiagnostics {
    private BaritonePathPostProcessDiagnostics() {
    }

    public static void logEnter(IPath path) {
        if (!ChatClefDiagnostics.isBoundaryEnabled() || path == null) {
            return;
        }
        CalculationDiagnosticRecord record = CalculationDiagnosticRegistry.activeRecordForPath(path);
        if (record == null) {
            return;
        }
        synchronized (record) {
            record.postProcessStartNanos = System.nanoTime();
        }
        String pathIdentity = BaritonePathObjectFormatters.identity(path);
        BaritoneCalculationPhaseDiagnostics.emit(
                record,
                record.pathfinder,
                "POST_PROCESS_ENTER",
                "baritone_path_post_process_head",
                "post_process|" + pathIdentity,
                -1,
                "unavailable_from_path_phase_mixin",
                () -> new Object[]{
                        "rawResultPathPresent", true,
                        "rawResultPathIdentity", pathIdentity,
                        "rawResultPathSummary", BaritonePathObjectFormatters.summarizePath(path)
                }
        );
    }

    public static void logReturn(IPath path, IPath result) {
        if (!ChatClefDiagnostics.isBoundaryEnabled() || path == null) {
            return;
        }
        CalculationDiagnosticRecord record = CalculationDiagnosticRegistry.activeRecordForPath(path);
        if (record == null) {
            return;
        }
        long elapsedMillis;
        synchronized (record) {
            record.postProcessedPathPresent = result != null;
            record.postProcessedPath = result;
            record.postProcessedPathId = BaritonePathObjectFormatters.identity(result);
            elapsedMillis = elapsedMillisSince(record.postProcessStartNanos);
        }
        String pathIdentity = BaritonePathObjectFormatters.identity(path);
        BaritoneCalculationPhaseDiagnostics.emit(
                record,
                record.pathfinder,
                "POST_PROCESS_RETURN",
                "baritone_path_post_process_return",
                "post_process|" + pathIdentity + "|" + record.postProcessedPathId,
                elapsedMillis,
                "unavailable_from_path_phase_mixin",
                () -> returnFields(record, path, result, pathIdentity)
        );
    }

    private static Object[] returnFields(CalculationDiagnosticRecord record,
                                         IPath path,
                                         IPath result,
                                         String pathIdentity) {
        String postProcessedSummary = BaritonePathObjectFormatters.summarizePath(result);
        synchronized (record) {
            record.postProcessedPathSummary = postProcessedSummary;
        }
        return new Object[]{
                "rawResultPathPresent", true,
                "rawResultPathIdentity", pathIdentity,
                "rawResultPathSummary", BaritonePathObjectFormatters.summarizePath(path),
                "postProcessedPathPresent", record.postProcessedPathPresent,
                "postProcessedPathIdentity", record.postProcessedPathId,
                "postProcessedPathSummary", record.postProcessedPathSummary
        };
    }

    private static long elapsedMillisSince(long startedAtNanos) {
        if (startedAtNanos <= 0) {
            return -1;
        }
        return (System.nanoTime() - startedAtNanos) / 1_000_000L;
    }
}
