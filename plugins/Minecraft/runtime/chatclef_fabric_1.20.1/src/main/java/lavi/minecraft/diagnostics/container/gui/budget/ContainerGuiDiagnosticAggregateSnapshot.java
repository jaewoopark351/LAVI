package lavi.minecraft.diagnostics.container.gui.budget;

//20260904_kpopmodder: Expose immutable local accounting for terminal evidence.
public record ContainerGuiDiagnosticAggregateSnapshot(
        String diagnosticBoundaryActivationId,
        long diagnosticDetailObservationCount,
        long diagnosticDetailDedupeSuppressedCount,
        long diagnosticDetailLocalAdmissionCount,
        long diagnosticDetailLocalCapSuppressedCount,
        long diagnosticDetailSharedAdmissionRejectedCount,
        long diagnosticDetailPhysicalEmissionCount,
        long omittedCount) {
}
