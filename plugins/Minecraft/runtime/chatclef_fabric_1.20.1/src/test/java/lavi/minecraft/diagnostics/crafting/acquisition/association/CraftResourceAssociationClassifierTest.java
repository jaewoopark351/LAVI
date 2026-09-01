package lavi.minecraft.diagnostics.crafting.acquisition.association;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

//20260901_kpopmodder: Define fail-closed Task ownership classification before implementation.
class CraftResourceAssociationClassifierTest {
    private final CraftResourceAssociationClassifier classifier =
            new CraftResourceAssociationClassifier();

    @Test
    void matchingAmbientCommandIdentifiersAloneRemainUnknown() {
        CraftResourceAssociationDecision decision = classifier.classify(evidence(
                true,
                false,
                false,
                false,
                CraftResourceSelectedChainKind.USER_TASK_CHAIN,
                false,
                false,
                true,
                false,
                false,
                true,
                false,
                true
        ));

        assertEquals(CraftResourceAssociationStatus.UNKNOWN, decision.status());
        assertFalse(decision.reason().isBlank());
    }

    @Test
    void completeSameBoundaryUserRootLineageIsCommandRootDescendant() {
        CraftResourceAssociationDecision decision = classifier.classify(provenUserLineage(true, false));

        assertEquals(CraftResourceAssociationStatus.COMMAND_ROOT_DESCENDANT, decision.status());
        assertFalse(decision.reason().isBlank());
    }

    @Test
    void everyRootBindingAxisIsRequiredIndependently() {
        CraftResourceAssociationEvidence assignmentMissing = evidence(
                true, false, true, true,
                CraftResourceSelectedChainKind.USER_TASK_CHAIN,
                true, true, false, true, true, true, false, false
        );
        CraftResourceAssociationEvidence generationMissing = evidence(
                true, true, false, true,
                CraftResourceSelectedChainKind.USER_TASK_CHAIN,
                true, true, false, true, true, true, false, false
        );
        CraftResourceAssociationEvidence rootIdentityMissing = evidence(
                true, true, true, false,
                CraftResourceSelectedChainKind.USER_TASK_CHAIN,
                true, true, false, true, true, true, false, false
        );

        assertEquals(CraftResourceAssociationStatus.UNKNOWN,
                classifier.classify(assignmentMissing).status());
        assertEquals(CraftResourceAssociationStatus.UNKNOWN,
                classifier.classify(generationMissing).status());
        assertEquals(CraftResourceAssociationStatus.UNKNOWN,
                classifier.classify(rootIdentityMissing).status());
    }

    @Test
    void actualConcurrentChainMembershipIsUnownedEvenWithMatchingCommandIds() {
        CraftResourceAssociationDecision decision = classifier.classify(evidence(
                true,
                true,
                true,
                true,
                CraftResourceSelectedChainKind.CONCURRENT_NON_USER_CHAIN,
                true,
                false,
                false,
                false,
                true,
                true,
                false,
                false
        ));

        assertEquals(CraftResourceAssociationStatus.CONCURRENT_CHAIN_UNOWNED, decision.status());
        assertFalse(decision.reason().isBlank());
    }

    @Test
    void truncatedRenderedOrClassOnlyLineageCannotUpgradeOwnership() {
        CraftResourceAssociationEvidence truncated = evidence(
                true, true, true, true,
                CraftResourceSelectedChainKind.USER_TASK_CHAIN,
                true, true, true, true, true, true, false, false
        );
        CraftResourceAssociationEvidence renderedClassOnly = evidence(
                true, true, true, true,
                CraftResourceSelectedChainKind.USER_TASK_CHAIN,
                false, false, false, false, false, true, false, true
        );

        assertEquals(CraftResourceAssociationStatus.UNKNOWN,
                classifier.classify(truncated).status());
        assertEquals(CraftResourceAssociationStatus.UNKNOWN,
                classifier.classify(renderedClassOnly).status());
    }

    @Test
    void crossBoundaryEvidenceRequiresAPrecapturedImmutableToken() {
        CraftResourceAssociationDecision withoutToken = classifier.classify(
                provenUserLineage(false, false)
        );
        CraftResourceAssociationDecision withToken = classifier.classify(
                provenUserLineage(false, true)
        );

        assertEquals(CraftResourceAssociationStatus.UNKNOWN, withoutToken.status());
        assertFalse(withoutToken.reason().isBlank());
        assertEquals(CraftResourceAssociationStatus.COMMAND_ROOT_DESCENDANT, withToken.status());
        assertFalse(withToken.reason().isBlank());
    }

    private static CraftResourceAssociationEvidence provenUserLineage(
            boolean sameCaptureBoundary,
            boolean asyncTokenAvailable) {
        return evidence(
                true,
                true,
                true,
                true,
                CraftResourceSelectedChainKind.USER_TASK_CHAIN,
                true,
                true,
                false,
                true,
                true,
                sameCaptureBoundary,
                asyncTokenAvailable,
                false
        );
    }

    private static CraftResourceAssociationEvidence evidence(
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
        return new CraftResourceAssociationEvidence(
                commandIdentifiersMatch,
                rootAssignmentMatches,
                rootGenerationMatches,
                boundRootIdentityMatches,
                selectedChainKind,
                emittingTaskInSelectedChainPath,
                selectedChainPathRootMatchesBoundRoot,
                selectedChainPathTruncated,
                lineageConnectedToBoundRoot,
                lineageStillActive,
                sameCaptureBoundary,
                asyncAssociationTokenAvailable,
                renderedClassOnlyMatch
        );
    }
}
