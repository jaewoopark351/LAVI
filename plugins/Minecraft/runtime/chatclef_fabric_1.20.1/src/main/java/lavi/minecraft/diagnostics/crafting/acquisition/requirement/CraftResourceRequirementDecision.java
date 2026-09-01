package lavi.minecraft.diagnostics.crafting.acquisition.requirement;

import java.util.Objects;
import java.util.OptionalInt;

/**
 * Immutable projection of values captured at the authoritative quantity decision.
 *
 * <p>The factory never reads inventory or resolves a recipe. Missing captured values stay
 * unavailable, and the additional-acquisition label is fail-closed unless the current runtime
 * artifact and the captured arithmetic are both proven.</p>
 */
public record CraftResourceRequirementDecision(
        String requestedItem,
        OptionalInt requestedCount,
        OptionalInt currentItemCount,
        OptionalInt targetItemCount,
        OptionalInt deltaNeeded,
        CraftResourceQuantityContract quantityContract,
        CraftResourceArtifactProof artifactProof,
        OptionalInt recipeOutputCount,
        OptionalInt recipeCraftCount,
        String activeRequirementItem,
        OptionalInt activeRequirementCount) {

    public CraftResourceRequirementDecision {
        Objects.requireNonNull(requestedItem, "requestedItem");
        Objects.requireNonNull(requestedCount, "requestedCount");
        Objects.requireNonNull(currentItemCount, "currentItemCount");
        Objects.requireNonNull(targetItemCount, "targetItemCount");
        Objects.requireNonNull(deltaNeeded, "deltaNeeded");
        Objects.requireNonNull(quantityContract, "quantityContract");
        Objects.requireNonNull(artifactProof, "artifactProof");
        Objects.requireNonNull(recipeOutputCount, "recipeOutputCount");
        Objects.requireNonNull(recipeCraftCount, "recipeCraftCount");
        Objects.requireNonNull(activeRequirementItem, "activeRequirementItem");
        Objects.requireNonNull(activeRequirementCount, "activeRequirementCount");
    }

    public static CraftResourceRequirementDecision fromCapturedDecision(
            String requestedItem,
            OptionalInt requestedCount,
            OptionalInt currentItemCount,
            OptionalInt targetItemCount,
            OptionalInt deltaNeeded,
            CraftResourceArtifactProof artifactProof,
            OptionalInt recipeOutputCount,
            OptionalInt recipeCraftCount,
            String activeRequirementItem,
            OptionalInt activeRequirementCount) {
        Objects.requireNonNull(artifactProof, "artifactProof");

        CraftResourceQuantityContract quantityContract =
                provesAdditionalAcquisition(
                        requestedCount,
                        currentItemCount,
                        targetItemCount,
                        deltaNeeded,
                        artifactProof)
                        ? CraftResourceQuantityContract.ADDITIONAL_ACQUISITION
                        : CraftResourceQuantityContract.UNAVAILABLE;

        return new CraftResourceRequirementDecision(
                requestedItem,
                requestedCount,
                currentItemCount,
                targetItemCount,
                deltaNeeded,
                quantityContract,
                artifactProof,
                recipeOutputCount,
                recipeCraftCount,
                activeRequirementItem,
                activeRequirementCount
        );
    }

    private static boolean provesAdditionalAcquisition(
            OptionalInt requestedCount,
            OptionalInt currentItemCount,
            OptionalInt targetItemCount,
            OptionalInt deltaNeeded,
            CraftResourceArtifactProof artifactProof) {
        Objects.requireNonNull(requestedCount, "requestedCount");
        Objects.requireNonNull(currentItemCount, "currentItemCount");
        Objects.requireNonNull(targetItemCount, "targetItemCount");
        Objects.requireNonNull(deltaNeeded, "deltaNeeded");

        if (artifactProof != CraftResourceArtifactProof.PROVEN_CURRENT_RUNTIME
                || requestedCount.isEmpty()
                || currentItemCount.isEmpty()
                || targetItemCount.isEmpty()
                || deltaNeeded.isEmpty()) {
            return false;
        }

        int requested = requestedCount.getAsInt();
        int current = currentItemCount.getAsInt();
        int target = targetItemCount.getAsInt();
        int delta = deltaNeeded.getAsInt();
        if (requested < 0 || current < 0 || target < 0 || delta < 0) {
            return false;
        }

        return (long) current + requested == target && delta == requested;
    }
}
