package lavi.minecraft.task.container.home.execution.timeout;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

//20260828_kpopmodder: Fix the reviewed phase-clock values before runtime verification.
class StoreHomeTimeoutPolicyTest {
    @Test
    void standardPolicyPreservesReviewedBoundaries() {
        StoreHomeTimeoutPolicy policy = StoreHomeTimeoutPolicy.standard();

        assertEquals(2400, policy.candidateNavigationNoProgressTicks());
        assertEquals(2400, policy.candidateLocalInteractionTicks());
        assertEquals(12000, policy.operationNoProgressTicks());
        assertEquals(120000, policy.operationEmergencyHardCapTicks());
        assertEquals(0.5, policy.movementJitterBlocks());
        assertEquals(1.0, policy.bestDistanceImprovementEpsilonBlocks());
    }

    @Test
    void emergencyHardCapMustRemainBeyondNoProgressWindow() {
        assertThrows(IllegalArgumentException.class, () -> new StoreHomeTimeoutPolicy(
                2, 2, 10, 10, 0.5, 1.0
        ));
    }
}
