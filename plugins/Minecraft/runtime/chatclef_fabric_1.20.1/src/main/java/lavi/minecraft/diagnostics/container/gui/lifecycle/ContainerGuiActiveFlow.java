package lavi.minecraft.diagnostics.container.gui.lifecycle;

import lavi.minecraft.diagnostics.container.gui.budget.ContainerGuiDiagnosticAggregateSnapshot;
import lavi.minecraft.diagnostics.container.gui.budget.ContainerGuiDiagnosticLimiter;
import lavi.minecraft.diagnostics.container.gui.emission.ContainerGuiDiagnosticEmitter;
import lavi.minecraft.diagnostics.container.gui.screen.ContainerScreenEventSnapshot;

//20260904_kpopmodder: Keep one flow's bounded diagnostics independent from every later reproduction.
public final class ContainerGuiActiveFlow {
    private final ContainerGuiFlowTrace trace;
    private final ContainerGuiDiagnosticLimiter limiter;
    private final ContainerGuiDiagnosticEmitter emitter;

    public ContainerGuiActiveFlow(
            ContainerScreenEventSnapshot source,
            long gameTick,
            String diagnosticBoundaryActivationId) {
        trace = new ContainerGuiFlowTrace(source, gameTick);
        limiter = new ContainerGuiDiagnosticLimiter(diagnosticBoundaryActivationId);
        emitter = new ContainerGuiDiagnosticEmitter(
                limiter,
                "CONTAINER_GUI_FLOW_DIAGNOSTIC_SUPPRESSION_SUMMARY"
        );
    }

    public ContainerGuiFlowTrace trace() {
        return trace;
    }

    public ContainerGuiDiagnosticEmitter emitter() {
        return emitter;
    }

    public ContainerGuiDiagnosticAggregateSnapshot aggregateSnapshot() {
        return limiter.snapshot();
    }

    public boolean rootAssignmentMatches(String currentRootAssignmentId) {
        return trace.rootAssignmentMatches(currentRootAssignmentId);
    }
}
