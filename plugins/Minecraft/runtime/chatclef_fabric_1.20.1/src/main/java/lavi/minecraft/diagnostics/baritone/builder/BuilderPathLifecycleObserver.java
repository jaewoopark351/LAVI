package lavi.minecraft.diagnostics.baritone.builder;

import baritone.api.pathing.calc.IPath;
import baritone.api.utils.PathCalculationResult;
import lavi.minecraft.diagnostics.baritone.correlation.BuilderPathProvenance;
import lavi.minecraft.diagnostics.baritone.correlation.BuilderTraceRegistry;
import lavi.minecraft.diagnostics.baritone.correlation.BuilderTraceState;
import lavi.minecraft.diagnostics.mining.MiningDiagnosticEmitter;

//20260913_kpopmodder: Observe raw creation, assembly, result, and two-input transformations separately.
public final class BuilderPathLifecycleObserver {
    private BuilderPathLifecycleObserver() { }
    public static void phase(IPath path, IPath returned, String phase) {
        try {
            BuilderTraceRegistry.WorkerBinding worker = BuilderTraceRegistry.worker();
            BuilderTraceState state = worker != null ? worker.state() : BuilderTraceRegistry.containing(path);
            if (state == null) return;
            BuilderPathProvenance origin = worker != null ? worker.origin() : state.ledger.find(path);
            if (origin == null) return;
            BuilderPathSnapshot snapshot = BuilderPathSnapshot.capture(path);
            BuilderPathProvenance bound = origin.transformed(phase, BuilderPathSnapshot.id(path), "none", snapshot);
            state.ledger.bind(path, bound);
            if (returned != null) state.ledger.bind(returned, bound.transformed(phase,
                    BuilderPathSnapshot.id(path), "none", BuilderPathSnapshot.capture(returned)));
            state.event("BARITONE_BUILDER_PATH_PHASE", phase, snapshot.identity(),
                    MiningDiagnosticEmitter.merge(bound.fields(), snapshot.fields("source"),
                            BuilderPathSnapshot.capture(returned).fields("returned")));
        } catch (RuntimeException | LinkageError failure) {
            lavi.minecraft.diagnostics.observation.ObservationDiagnostics.captureFailed("builder", failure.getClass().getSimpleName());
        }
    }
    public static void transformed(IPath first, IPath second, IPath result, String reason, int from, int to) {
        try {
            BuilderTraceState state = BuilderTraceRegistry.containing(first);
            if (state == null) return;
            BuilderPathProvenance origin = state.ledger.find(first);
            BuilderPathProvenance secondOrigin = state.ledger.find(second);
            BuilderPathSnapshot output = BuilderPathSnapshot.capture(result);
            if (result != null) state.ledger.bind(result, origin.transformed(reason,
                    BuilderPathSnapshot.id(first), BuilderPathSnapshot.id(second), output));
            state.event("BARITONE_BUILDER_PATH_TRANSFORM", reason,
                    BuilderPathSnapshot.id(first) + ">" + output.identity(),
                    MiningDiagnosticEmitter.merge(origin.fields(), BuilderPathSnapshot.capture(first).fields("first"),
                            BuilderPathSnapshot.capture(second).fields("second"), output.fields("result"), new Object[]{
                                    "secondCalculationGeneration", secondOrigin == null ? -1 : secondOrigin.calculation(),
                                    "secondRequestOrigin", secondOrigin == null ? "UNBOUND" : secondOrigin.request(),
                                    "firstPositionIncluded", from, "lastPositionIncluded", to,
                                    "sameObjectReturned", first == result}));
        } catch (RuntimeException | LinkageError failure) {
            lavi.minecraft.diagnostics.observation.ObservationDiagnostics.captureFailed("builder", failure.getClass().getSimpleName());
        }
    }
    public static void calculationResult(PathCalculationResult result) {
        try {
            BuilderTraceRegistry.WorkerBinding worker = BuilderTraceRegistry.worker();
            if (worker == null) return;
            IPath path = result == null ? null : result.getPath().orElse(null);
            String type = result == null ? "NULL" : String.valueOf(result.getType());
            BuilderPathSnapshot snapshot = BuilderPathSnapshot.capture(path);
            BuilderPathProvenance built = worker.state().ledger.find(path);
            BuilderPathProvenance origin = (built == null ? worker.origin() : built).transformed("CALCULATION_RESULT",
                    built == null ? "none" : built.firstInput(), built == null ? "none" : built.secondInput(), snapshot);
            if (path != null) worker.state().ledger.bind(path, origin);
            worker.state().event("BARITONE_BUILDER_CALCULATION_RESULT", type, snapshot.identity(),
                    MiningDiagnosticEmitter.merge(origin.fields(), snapshot.fields("result")));
        } catch (RuntimeException | LinkageError failure) {
            lavi.minecraft.diagnostics.observation.ObservationDiagnostics.captureFailed("builder", failure.getClass().getSimpleName());
        }
    }
}
