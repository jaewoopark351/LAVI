package lavi.minecraft.diagnostics.crafting.acquisition.requirement;

import org.junit.jupiter.api.Test;

import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

//20260901_kpopmodder: Keep captured acquisition quantities and provenance distinct before logging.
class CraftResourceRequirementDecisionTest {
    @Test
    void provenAdditionalAcquisitionKeepsRequestedCurrentTargetAndDeltaDistinct() {
        CraftResourceRequirementDecision decision = captured(
                OptionalInt.of(1),
                OptionalInt.of(1),
                OptionalInt.of(2),
                OptionalInt.of(1),
                CraftResourceArtifactProof.PROVEN_CURRENT_RUNTIME
        );

        assertEquals("minecraft:iron_pickaxe", decision.requestedItem());
        assertEquals(1, decision.requestedCount().orElseThrow());
        assertEquals(1, decision.currentItemCount().orElseThrow());
        assertEquals(2, decision.targetItemCount().orElseThrow());
        assertEquals(1, decision.deltaNeeded().orElseThrow());
        assertEquals(CraftResourceQuantityContract.ADDITIONAL_ACQUISITION,
                decision.quantityContract());
        assertEquals(CraftResourceArtifactProof.PROVEN_CURRENT_RUNTIME,
                decision.artifactProof());
        assertEquals(1, decision.recipeOutputCount().orElseThrow());
        assertEquals(1, decision.recipeCraftCount().orElseThrow());
        assertEquals("minecraft:iron_ingot", decision.activeRequirementItem());
        assertEquals(3, decision.activeRequirementCount().orElseThrow());
    }

    @Test
    void unverifiedArtifactNeverClaimsTheAdditionalAcquisitionContract() {
        CraftResourceRequirementDecision decision = captured(
                OptionalInt.of(1),
                OptionalInt.of(1),
                OptionalInt.of(2),
                OptionalInt.of(1),
                CraftResourceArtifactProof.UNVERIFIED
        );

        assertEquals(CraftResourceQuantityContract.UNAVAILABLE, decision.quantityContract());
        assertEquals(CraftResourceArtifactProof.UNVERIFIED, decision.artifactProof());
        assertEquals(1, decision.requestedCount().orElseThrow());
        assertEquals(1, decision.currentItemCount().orElseThrow());
        assertEquals(2, decision.targetItemCount().orElseThrow());
        assertEquals(1, decision.deltaNeeded().orElseThrow());
    }

    @Test
    void unavailableCapturedValuesRemainUnavailableInsteadOfBeingRecomputed() {
        CraftResourceRequirementDecision decision = CraftResourceRequirementDecision.fromCapturedDecision(
                "minecraft:iron_pickaxe",
                OptionalInt.of(1),
                OptionalInt.empty(),
                OptionalInt.empty(),
                OptionalInt.empty(),
                CraftResourceArtifactProof.UNVERIFIED,
                OptionalInt.empty(),
                OptionalInt.empty(),
                "UNAVAILABLE",
                OptionalInt.empty()
        );

        assertEquals(1, decision.requestedCount().orElseThrow());
        assertFalse(decision.currentItemCount().isPresent());
        assertFalse(decision.targetItemCount().isPresent());
        assertFalse(decision.deltaNeeded().isPresent());
        assertFalse(decision.recipeOutputCount().isPresent());
        assertFalse(decision.recipeCraftCount().isPresent());
        assertFalse(decision.activeRequirementCount().isPresent());
        assertEquals("UNAVAILABLE", decision.activeRequirementItem());
        assertEquals(CraftResourceQuantityContract.UNAVAILABLE, decision.quantityContract());
    }

    private static CraftResourceRequirementDecision captured(
            OptionalInt requested,
            OptionalInt current,
            OptionalInt target,
            OptionalInt delta,
            CraftResourceArtifactProof proof) {
        return CraftResourceRequirementDecision.fromCapturedDecision(
                "minecraft:iron_pickaxe",
                requested,
                current,
                target,
                delta,
                proof,
                OptionalInt.of(1),
                OptionalInt.of(1),
                "minecraft:iron_ingot",
                OptionalInt.of(3)
        );
    }
}
