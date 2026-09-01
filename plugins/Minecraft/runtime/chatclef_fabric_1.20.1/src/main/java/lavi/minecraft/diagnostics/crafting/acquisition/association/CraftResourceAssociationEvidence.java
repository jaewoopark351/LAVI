package lavi.minecraft.diagnostics.crafting.acquisition.association;

import java.util.Objects;

/**
 * Immutable, already-captured evidence used to classify Task ownership.
 *
 * <p>This value intentionally contains no Task or Minecraft types. Object-identity and actual
 * untruncated-path comparisons must happen at the observation boundary and be reduced to these
 * booleans before asynchronous delivery.</p>
 */
public record CraftResourceAssociationEvidence(
        boolean commandIdentifiersMatch,
        boolean rootAssignmentMatches,
        boolean rootGenerationMatches,
        boolean boundRootIdentityMatches,
        CraftResourceSelectedChainKind selectedChainKind,
        boolean emittingTaskInSelectedChainPath,
        boolean selectedChainPathRootMatchesBoundRoot,
        boolean selectedChainPathTruncated,
        boolean lineageConnectedToBoundRoot,
        boolean lineageStillActive,
        boolean sameCaptureBoundary,
        boolean asyncAssociationTokenAvailable,
        boolean renderedClassOnlyMatch) {

    public CraftResourceAssociationEvidence {
        Objects.requireNonNull(selectedChainKind, "selectedChainKind");
    }
}
