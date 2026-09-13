package lavi.minecraft.diagnostics.baritone.builder;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.observation.ObservationDiagnostics;
import lavi.minecraft.diagnostics.baritone.correlation.BuilderTraceRegistry;
import lavi.minecraft.diagnostics.baritone.correlation.BuilderTraceState;

//20260913_kpopmodder: Attach process request/lost-control observations to the same path owner.
public final class BuilderProcessObserver {
    private BuilderProcessObserver() { }
    public static void boundary(Object process, String reason) {
        try {
            if (!ChatClefDiagnostics.isBoundaryEnabled()) return;
            if (!(process instanceof BuilderProcessOwnerView owner)) {
                ObservationDiagnostics.captureFailed("builder", "PROCESS_OWNER_VIEW_UNAVAILABLE");
                return;
            }
            BuilderTraceState state = BuilderTraceRegistry.observeOwner(owner.lavi$ownerBaritone().getPathingBehavior());
            if (state == null) return;
            state.event("BARITONE_BUILDER_CONTROL", reason, BuilderPathSnapshot.id(process),
                    "processIdentity", BuilderPathSnapshot.id(process),
                    "commandAtBoundary", BuilderTraceRegistry.commandOrigin());
        } catch (RuntimeException | LinkageError failure) {
            ObservationDiagnostics.captureFailed("builder", failure.getClass().getSimpleName());
        }
    }
}
