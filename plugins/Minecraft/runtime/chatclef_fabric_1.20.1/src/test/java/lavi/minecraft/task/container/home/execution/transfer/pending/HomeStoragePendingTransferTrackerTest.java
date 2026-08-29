package lavi.minecraft.task.container.home.execution.transfer.pending;

import lavi.minecraft.task.container.home.execution.transfer.HomeStoragePendingTransferObservation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260829_kpopmodder: Preserve pending-transfer ownership and active-tick advancement.
class HomeStoragePendingTransferTrackerTest {
    @Test
    void exposesNeutralObservationAndAdvancesOnlyWhenAsked() {
        HomeStoragePendingTransferTracker tracker =
                new HomeStoragePendingTransferTracker();

        tracker.begin("destination-1", 8, 63, 64, 12);
        HomeStoragePendingTransferObservation initial = tracker.observation()
                .orElseThrow();
        assertEquals(0, initial.elapsedTicks());
        assertEquals(8, tracker.logicalSlot().orElseThrow());

        tracker.advanceTick();

        assertEquals(1, tracker.observation().orElseThrow().elapsedTicks());
        tracker.clear();
        assertFalse(tracker.hasPending());
        assertTrue(tracker.observation().isEmpty());
    }
}
