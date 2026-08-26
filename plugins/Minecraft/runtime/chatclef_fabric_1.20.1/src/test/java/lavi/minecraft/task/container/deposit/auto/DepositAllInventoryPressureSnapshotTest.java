package lavi.minecraft.task.container.deposit.auto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260826_kpopmodder: Verify the exact occupied-slot boundary for automatic deposit_all.
class DepositAllInventoryPressureSnapshotTest {

    @Test
    void reachesNineTenthsAtThirtyThreeOfThirtySixOccupiedSlots() {
        assertFalse(new DepositAllInventoryPressureSnapshot(32, 36).isAtOrAboveThreshold());
        assertTrue(new DepositAllInventoryPressureSnapshot(33, 36).isAtOrAboveThreshold());
        assertTrue(new DepositAllInventoryPressureSnapshot(36, 36).isAtOrAboveThreshold());
    }

    @Test
    void reachesLowWaterAtTwentyEightOfThirtySixOccupiedSlots() {
        assertTrue(new DepositAllInventoryPressureSnapshot(28, 36).isAtOrBelowLowWater());
        assertFalse(new DepositAllInventoryPressureSnapshot(29, 36).isAtOrBelowLowWater());
        assertFalse(new DepositAllInventoryPressureSnapshot(32, 36).isAtOrBelowLowWater());
    }

    @Test
    void targetsFiveToEightFreedSlotsAtHighWater() {
        assertEquals(5, new DepositAllInventoryPressureSnapshot(33, 36).requiredReliefSlots());
        assertEquals(8, new DepositAllInventoryPressureSnapshot(36, 36).requiredReliefSlots());
    }

    @Test
    void rejectsImpossibleSlotCounts() {
        assertThrows(IllegalArgumentException.class,
                () -> new DepositAllInventoryPressureSnapshot(-1, 36));
        assertThrows(IllegalArgumentException.class,
                () -> new DepositAllInventoryPressureSnapshot(37, 36));
        assertThrows(IllegalArgumentException.class,
                () -> new DepositAllInventoryPressureSnapshot(0, 0));
    }
}
