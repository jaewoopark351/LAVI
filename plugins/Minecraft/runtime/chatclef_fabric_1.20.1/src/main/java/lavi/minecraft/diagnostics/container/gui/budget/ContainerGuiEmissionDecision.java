package lavi.minecraft.diagnostics.container.gui.budget;

//20260904_kpopmodder: Express only local diagnostic admission; shared admission remains authoritative.
public record ContainerGuiEmissionDecision(
        Outcome outcome,
        int suppressedRepeatCount,
        String diagnosticBoundaryActivationId) {

    public enum Outcome {
        DETAIL,
        REPEAT_SUMMARY,
        SUPPRESSED_DUPLICATE,
        SUPPRESSED_LOCAL_CAP
    }
}
