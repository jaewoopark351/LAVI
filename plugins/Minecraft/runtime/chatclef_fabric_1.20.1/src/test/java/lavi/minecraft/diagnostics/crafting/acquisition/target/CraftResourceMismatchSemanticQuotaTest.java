package lavi.minecraft.diagnostics.crafting.acquisition.target;

import lavi.minecraft.diagnostics.crafting.acquisition.association.CraftResourceAssociationStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260901_kpopmodder: Keep mismatch uniqueness semantic and correlation state bounded.
class CraftResourceMismatchSemanticQuotaTest {
    @Test
    void identicalSemanticMismatchAcrossCorrelationsConsumesOneSessionSignature() {
        CraftResourceMismatchAdmissionTracker tracker =
                new CraftResourceMismatchAdmissionTracker();
        assertTrue(tracker.activateCorrelation("correlation-a"));
        assertTrue(tracker.activateCorrelation("correlation-b"));

        CraftResourceMismatchDecision first = tracker.evaluate(mismatch("correlation-a"));
        CraftResourceMismatchDecision second = tracker.evaluate(mismatch("correlation-b"));

        assertTrue(first.emissionRequested());
        assertFalse(second.emissionRequested());
        assertEquals(
                CraftResourceMismatchDisposition.DUPLICATE_SUPPRESSED,
                second.disposition()
        );
        assertEquals(first.fingerprint(), second.fingerprint());
        assertFalse(first.fingerprint().contains("correlation-a"));
        assertEquals(1, tracker.snapshot().globalRetainedSignatureCount());
        assertEquals(1, tracker.snapshot().retainedSignatureCount("correlation-a"));
        assertEquals(1, tracker.snapshot().retainedSignatureCount("correlation-b"));
    }

    @Test
    void activeCorrelationStateRefusesNinthAndRetirementMakesOneSlotAvailable() {
        CraftResourceMismatchAdmissionTracker tracker =
                new CraftResourceMismatchAdmissionTracker();
        for (int index = 0; index < 8; index++) {
            assertTrue(tracker.activateCorrelation("correlation-" + index));
        }

        assertFalse(tracker.activateCorrelation("correlation-8"));
        assertEquals(8, tracker.snapshot().activeCorrelationCount());
        assertTrue(tracker.retireCorrelation("correlation-0"));
        assertTrue(tracker.activateCorrelation("correlation-8"));
        assertEquals(8, tracker.snapshot().activeCorrelationCount());
        assertEquals(0, tracker.snapshot().retainedSignatureCount("correlation-0"));
    }

    private static CraftResourceMismatchObservation mismatch(String correlationId) {
        return new CraftResourceMismatchObservation(
                correlationId,
                CraftResourceAssociationStatus.COMMAND_ROOT_DESCENDANT,
                CraftResourceStage.IRON_INPUT_ACQUISITION,
                CraftResourceTargetRole.IRON_ORE_BLOCK,
                "0,64,0",
                Optional.of(List.of(
                        "minecraft:iron_ore",
                        "minecraft:deepslate_iron_ore"
                )),
                Optional.of("minecraft:chest"),
                "MINE_TARGET_SELECTION_TRANSITION",
                10L,
                "task-instance"
        );
    }
}
