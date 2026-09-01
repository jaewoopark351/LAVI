package lavi.minecraft.diagnostics.crafting.acquisition.requirement;

/**
 * Provenance of the runtime artifact whose quantity decision was observed.
 */
public enum CraftResourceArtifactProof {
    PROVEN_CURRENT_RUNTIME,
    UNVERIFIED,
    IDENTITY_MISMATCH,
    UNAVAILABLE
}
