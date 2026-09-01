package lavi.minecraft.diagnostics.crafting.acquisition.target;

import java.util.Objects;

//20260901_kpopmodder: Make local mismatch admission explicit before shared family admission.
public record CraftResourceMismatchDecision(
        CraftResourceMismatchDisposition disposition,
        boolean emissionRequested,
        String fingerprint) {

    public CraftResourceMismatchDecision {
        disposition = Objects.requireNonNull(disposition, "disposition");
        fingerprint = Objects.requireNonNull(fingerprint, "fingerprint");
    }
}
