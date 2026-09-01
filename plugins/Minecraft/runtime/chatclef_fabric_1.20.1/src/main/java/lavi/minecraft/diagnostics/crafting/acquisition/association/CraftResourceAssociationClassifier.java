package lavi.minecraft.diagnostics.crafting.acquisition.association;

import java.util.Objects;

/**
 * Classifies already-captured Task ownership without consulting live engine state.
 */
public final class CraftResourceAssociationClassifier {

    public CraftResourceAssociationDecision classify(CraftResourceAssociationEvidence evidence) {
        Objects.requireNonNull(evidence, "evidence");

        if (!evidence.commandIdentifiersMatch()) {
            return unknown("COMMAND_IDENTIFIERS_MISMATCH");
        }

        if (isProvenConcurrentMembership(evidence)) {
            if (!hasUsableCaptureBoundary(evidence)) {
                return unknown("CROSS_BOUNDARY_WITHOUT_TOKEN");
            }
            return new CraftResourceAssociationDecision(
                    CraftResourceAssociationStatus.CONCURRENT_CHAIN_UNOWNED,
                    "PROVEN_CONCURRENT_CHAIN_MEMBERSHIP"
            );
        }

        if (!hasCompleteRootBinding(evidence)) {
            if (!evidence.rootAssignmentMatches()
                    && !evidence.rootGenerationMatches()
                    && !evidence.boundRootIdentityMatches()
                    && !evidence.emittingTaskInSelectedChainPath()
                    && !evidence.selectedChainPathRootMatchesBoundRoot()) {
                return unknown("COMMAND_IDS_ARE_NOT_OWNERSHIP");
            }
            return unknown("ROOT_BINDING_INCOMPLETE");
        }

        if (evidence.selectedChainKind() != CraftResourceSelectedChainKind.USER_TASK_CHAIN) {
            return unknown("USER_TASK_CHAIN_NOT_PROVEN");
        }
        if (evidence.selectedChainPathTruncated()) {
            return unknown("SELECTED_CHAIN_PATH_TRUNCATED");
        }
        if (evidence.renderedClassOnlyMatch()) {
            return unknown("RENDERED_CLASS_ONLY_MATCH");
        }
        if (!evidence.emittingTaskInSelectedChainPath()) {
            return unknown("EMITTING_TASK_NOT_IN_SELECTED_CHAIN_PATH");
        }
        if (!evidence.selectedChainPathRootMatchesBoundRoot()) {
            return unknown("SELECTED_CHAIN_ROOT_MISMATCH");
        }
        if (!evidence.lineageConnectedToBoundRoot()) {
            return unknown("LINEAGE_TO_BOUND_ROOT_NOT_PROVEN");
        }
        if (!evidence.lineageStillActive()) {
            return unknown("LINEAGE_NOT_ACTIVE");
        }
        if (!hasUsableCaptureBoundary(evidence)) {
            return unknown("CROSS_BOUNDARY_WITHOUT_TOKEN");
        }

        return new CraftResourceAssociationDecision(
                CraftResourceAssociationStatus.COMMAND_ROOT_DESCENDANT,
                "PROVEN_USER_ROOT_LINEAGE"
        );
    }

    private static boolean hasCompleteRootBinding(CraftResourceAssociationEvidence evidence) {
        return evidence.rootAssignmentMatches()
                && evidence.rootGenerationMatches()
                && evidence.boundRootIdentityMatches();
    }

    private static boolean isProvenConcurrentMembership(CraftResourceAssociationEvidence evidence) {
        return evidence.selectedChainKind()
                == CraftResourceSelectedChainKind.CONCURRENT_NON_USER_CHAIN
                && evidence.emittingTaskInSelectedChainPath()
                && !evidence.selectedChainPathTruncated()
                && !evidence.renderedClassOnlyMatch();
    }

    private static boolean hasUsableCaptureBoundary(CraftResourceAssociationEvidence evidence) {
        return evidence.sameCaptureBoundary() || evidence.asyncAssociationTokenAvailable();
    }

    private static CraftResourceAssociationDecision unknown(String reason) {
        return new CraftResourceAssociationDecision(CraftResourceAssociationStatus.UNKNOWN, reason);
    }
}
