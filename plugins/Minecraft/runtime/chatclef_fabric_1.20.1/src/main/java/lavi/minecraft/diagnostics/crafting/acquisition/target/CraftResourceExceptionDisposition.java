package lavi.minecraft.diagnostics.crafting.acquisition.target;

//20260901_kpopmodder: Preserve causal exception evidence separately from coverage accounting.
public enum CraftResourceExceptionDisposition {
    EMISSION_REQUESTED,
    COVERAGE_GAP_REQUESTED,
    DUPLICATE_SUPPRESSED,
    PER_CORRELATION_LIMIT,
    SESSION_LIMIT,
    ASSOCIATION_NOT_OWNED
}
