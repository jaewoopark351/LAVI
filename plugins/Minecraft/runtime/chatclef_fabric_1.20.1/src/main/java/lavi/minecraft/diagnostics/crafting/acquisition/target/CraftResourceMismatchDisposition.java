package lavi.minecraft.diagnostics.crafting.acquisition.target;

//20260901_kpopmodder: Distinguish mismatch evidence from gaps and bounded suppression.
public enum CraftResourceMismatchDisposition {
    MATCH,
    COVERAGE_GAP,
    EMISSION_REQUESTED,
    DUPLICATE_SUPPRESSED,
    PER_CORRELATION_LIMIT,
    SESSION_LIMIT,
    ASSOCIATION_NOT_OWNED
}
