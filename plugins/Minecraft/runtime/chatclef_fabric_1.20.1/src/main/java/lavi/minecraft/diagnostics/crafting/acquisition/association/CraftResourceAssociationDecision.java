package lavi.minecraft.diagnostics.crafting.acquisition.association;

import java.util.Objects;

/**
 * Result of a fail-closed ownership classification.
 */
public record CraftResourceAssociationDecision(
        CraftResourceAssociationStatus status,
        String reason) {

    public CraftResourceAssociationDecision {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(reason, "reason");
    }
}
