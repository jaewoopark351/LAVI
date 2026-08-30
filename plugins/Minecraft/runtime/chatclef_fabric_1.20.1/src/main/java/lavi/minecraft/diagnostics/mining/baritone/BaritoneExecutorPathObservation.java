package lavi.minecraft.diagnostics.mining.baritone;

import baritone.api.pathing.calc.IPath;
import baritone.pathing.path.PathExecutor;

//20260830_kpopmodder: Own only the passive executor-to-path identity observation.
final class BaritoneExecutorPathObservation {
    private BaritoneExecutorPathObservation() {
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
