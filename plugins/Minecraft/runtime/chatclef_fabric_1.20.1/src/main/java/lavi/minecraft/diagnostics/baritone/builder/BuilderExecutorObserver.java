package lavi.minecraft.diagnostics.baritone.builder;

import baritone.api.pathing.calc.IPath;
import baritone.behavior.PathingBehavior;
import baritone.pathing.path.PathExecutor;
import lavi.minecraft.diagnostics.baritone.correlation.BuilderPathProvenance;
import lavi.minecraft.diagnostics.baritone.correlation.BuilderTraceRegistry;
import lavi.minecraft.diagnostics.baritone.correlation.BuilderTraceState;
import lavi.minecraft.diagnostics.mining.MiningDiagnosticEmitter;

//20260913_kpopmodder: Distinguish constructor index from transferred/adopted index without rerunning decisions.
public final class BuilderExecutorObserver {
    private BuilderExecutorObserver() { }
    public static void created(PathingBehavior behavior, PathExecutor executor, IPath path, int position) {
        try {
            BuilderTraceState state = BuilderTraceRegistry.owner(behavior);
            if (state == null) return;
            state.event("BARITONE_BUILDER_EXECUTOR", "CONSTRUCTED", BuilderPathSnapshot.id(executor),
                    MiningDiagnosticEmitter.merge(new Object[]{"executorIdentity", BuilderPathSnapshot.id(executor),
                            "constructorIndex", position}, BuilderPathSnapshot.capture(path).fields("executor")));
        } catch (RuntimeException | LinkageError failure) {
            lavi.minecraft.diagnostics.observation.ObservationDiagnostics.captureFailed("builder", failure.getClass().getSimpleName());
        }
    }
    public static void transformed(PathingBehavior behavior, PathExecutor original, PathExecutor second,
                                    PathExecutor returned, String method) {
        try {
            BuilderTraceState state = BuilderTraceRegistry.owner(behavior);
            if (state == null) return;
            String reason = returned == original ? "SAME_EXECUTOR"
                    : returned == null ? "NULL_EXECUTOR" : "INDEX_TRANSFERRED";
            state.event("BARITONE_BUILDER_EXECUTOR", method + "_" + reason,
                    BuilderPathSnapshot.id(original) + ">" + BuilderPathSnapshot.id(returned),
                    MiningDiagnosticEmitter.merge(executorFields("original", original),
                            executorFields("second", second), executorFields("returned", returned)));
        } catch (RuntimeException | LinkageError failure) {
            lavi.minecraft.diagnostics.observation.ObservationDiagnostics.captureFailed("builder", failure.getClass().getSimpleName());
        }
    }
    public static void adopted(PathingBehavior behavior, PathExecutor current, PathExecutor next, String boundary) {
        try {
            BuilderTraceState state = BuilderTraceRegistry.owner(behavior);
            if (state == null) return;
            IPath currentPath = current == null ? null : current.getPath();
            IPath nextPath = next == null ? null : next.getPath();
            String fingerprint = BuilderPathSnapshot.id(current) + ":" + BuilderPathSnapshot.id(next);
            // Pins are copied before the old calculation registry's complete() and before output admission.
            state.ledger.adopt(currentPath, nextPath);
            if (!state.adoptionChanged(fingerprint)) return;
            String reason = current == null && next == null ? "CLEARED"
                    : next == null ? "CURRENT_ONLY" : current == null ? "NEXT_ONLY" : "CURRENT_AND_NEXT";
            state.event("BARITONE_BUILDER_PATH_ADOPTION", reason, fingerprint,
                    MiningDiagnosticEmitter.merge(new Object[]{"observedBoundary", boundary},
                            executorFields("current", current), executorFields("next", next),
                            originFields("current", state.ledger.find(currentPath)),
                            originFields("next", state.ledger.find(nextPath))));
        } catch (RuntimeException | LinkageError failure) {
            lavi.minecraft.diagnostics.observation.ObservationDiagnostics.captureFailed("builder", failure.getClass().getSimpleName());
        }
    }
    public static Object[] executorFields(String prefix, PathExecutor executor) {
        return MiningDiagnosticEmitter.merge(new Object[]{prefix + "ExecutorIdentity", BuilderPathSnapshot.id(executor),
                prefix + "Position", executor == null ? -1 : executor.getPosition()},
                BuilderPathSnapshot.capture(executor == null ? null : executor.getPath()).fields(prefix));
    }
    private static Object[] originFields(String prefix, BuilderPathProvenance origin) {
        return new Object[]{prefix + "CalculationGeneration", origin == null ? -1 : origin.calculation(),
                prefix + "RequestOrigin", origin == null ? "UNBOUND" : origin.request(),
                prefix + "PathPhase", origin == null ? "UNBOUND" : origin.phase()};
    }
}
