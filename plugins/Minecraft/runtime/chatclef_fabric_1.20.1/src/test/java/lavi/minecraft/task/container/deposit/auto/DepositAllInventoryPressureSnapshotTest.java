package lavi.minecraft.task.container.deposit.auto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260826_kpopmodder: Verify the exact occupied-slot boundary for automatic deposit_all.
class DepositAllInventoryPressureSnapshotTest {

    @Test
    void reachesFourFifthsAtTwentyNineOfThirtySixOccupiedSlots() {
        assertFalse(new DepositAllInventoryPressureSnapshot(28, 36).isAtOrAboveThreshold());
        assertTrue(new DepositAllInventoryPressureSnapshot(29, 36).isAtOrAboveThreshold());
        assertTrue(new DepositAllInventoryPressureSnapshot(36, 36).isAtOrAboveThreshold());
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
