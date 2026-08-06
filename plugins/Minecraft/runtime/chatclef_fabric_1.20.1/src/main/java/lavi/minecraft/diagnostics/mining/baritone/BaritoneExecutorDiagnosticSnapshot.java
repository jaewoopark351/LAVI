package lavi.minecraft.diagnostics.mining.baritone;

import baritone.pathing.path.PathExecutor;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;

//20260806_kpopmodder: Expose read-only Baritone executor fields for path adoption diagnostics.
final class BaritoneExecutorDiagnosticSnapshot {
    private BaritoneExecutorDiagnosticSnapshot() {
    }

    static Object[] fields(String role, PathExecutor executor) {
        String prefix = role + "Executor";
        return new Object[]{
                prefix + "Present", executor != null,
                prefix + "Identity", BaritonePathObjectFormatters.identity(executor),
                prefix + "Position", executor == null
                        ? "none"
                        : ChatClefDiagnostics.safeValueForDiagnosticLog(executor::getPosition),
                prefix + "Failed", executor == null
                        ? "none"
                        : ChatClefDiagnostics.safeValueForDiagnosticLog(executor::failed),
                prefix + "Finished", executor == null
                        ? "none"
                        : ChatClefDiagnostics.safeValueForDiagnosticLog(executor::finished),
                prefix + "PathSummary", executor == null
                        ? "none"
                        : ChatClefDiagnostics.safeValueForDiagnosticLog(
                                () -> BaritonePathObjectFormatters.summarizePath(executor.getPath()))
        };
    }
}
