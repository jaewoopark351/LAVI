package lavi.minecraft.integration.carryon.container;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.interaction.BlockInteractionContext;
import lavi.minecraft.diagnostics.interaction.BlockInteractionTargetInfo;
import lavi.minecraft.integration.carryon.CarryOnCarryState;
import lavi.minecraft.integration.carryon.CarryOnObservation;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

//20260907_kpopmodder: Keep each migrated Slice A characterization scenario with its owning responsibility.
class CarryOnContainerPickupEvidenceTest {

    @BeforeEach
    void startWithFreshDiagnosticSession() {
        ChatClefDiagnostics.setBoundaryEnabled(false);
        ChatClefDiagnostics.resetDiagnosticSessionForTests();
    }

    @AfterEach
    void disableDiagnostics() {
        ChatClefDiagnostics.setBoundaryEnabled(false);
        ChatClefDiagnostics.resetDiagnosticSessionForTests();
    }

    @Test
    @DisplayName("scenario 13 [assertion 20]: Carry On temporal edge without identity is not exact attribution")
    void carryOnTemporalEdgeWithRetainedTargetAndUnavailableIdentityIsNotExactAttribution() {
        BlockPos target = new BlockPos(1, 64, 2);
        BlockInteractionContext context = new BlockInteractionContext(
                91L,
                10L,
                new BlockInteractionTargetInfo(
                        true, "container", "minecraft:chest", "Chest", "facing=north", target
                ),
                null,
                null,
                null,
                true
        );
        CarryOnObservation before = CarryOnObservation.observed("2.1.2.7", false);
        CarryOnObservation after = CarryOnObservation.observed("2.1.2.7", true);

        CarryOnContainerPickupEvidence evidence =
                CarryOnContainerPickupEvidence.classify(context, after);

        assertEquals(CarryOnCarryState.AVAILABLE_NOT_CARRYING, before.state());
        assertEquals(CarryOnCarryState.AVAILABLE_CARRYING, after.state());
        assertEquals(target, context.targetPosition());
        assertEquals("unavailable", after.carriedBlockId());
        assertEquals(CarryOnContainerPickupEvidence.STRONG_TEMPORAL_ATTRIBUTION, evidence);
        assertNotEquals(CarryOnContainerPickupEvidence.CONFIRMED_TARGET_IDENTITY, evidence);
    }
}
