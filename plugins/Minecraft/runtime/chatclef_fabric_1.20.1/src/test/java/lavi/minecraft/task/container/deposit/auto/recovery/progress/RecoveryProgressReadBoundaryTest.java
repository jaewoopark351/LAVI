package lavi.minecraft.task.container.deposit.auto.recovery.progress;

import adris.altoclef.util.Dimension;
import lavi.minecraft.task.container.deposit.auto.maintenance.AutoDepositMaintenanceTask;
import lavi.minecraft.task.container.deposit.auto.recovery.AutoDepositDestinationManifest;
import lavi.minecraft.task.container.deposit.auto.recovery.AutoDepositWorkingSetRecovery;
import lavi.minecraft.task.container.deposit.auto.recovery.ExactPickupFromContainerTask;
import lavi.minecraft.task.container.deposit.auto.recovery.RecoverReservedItemsTask;
import lavi.minecraft.task.container.deposit.auto.working.PlayerInventorySnapshotReader;
import lavi.minecraft.testsupport.TestObjects;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

//20260914_kpopmodder: Read only confirmed recovery records; repeated live/terminal reads must not create progress.
class RecoveryProgressReadBoundaryTest {
    @Test
    void countSaturatesAndARepeatedLiveObservationDoesNotCommitTwice() {
        ConfirmedRecoveredItemCount completed = new ConfirmedRecoveredItemCount();
        completed.add(5);
        assertEquals(8, completed.including(3));
        assertEquals(8, completed.including(3));
        assertEquals(5, completed.total());
        completed.add(3);
        assertEquals(8, completed.including(0));
        completed.add(-5);
        assertEquals(8, completed.total());
        completed.add(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, completed.total());
        assertEquals(Integer.MAX_VALUE, completed.including(1));
    }

    @Test
    void finishedPlusCurrentPickupCountsRemainStableAcrossTimeoutAndChildRemoval() {
        ConfirmedRecoveredItemCount liveCount = new ConfirmedRecoveredItemCount();
        liveCount.add(3);
        ExactPickupFromContainerTask pickup = new ExactPickupFromContainerTask(new BlockPos(1, 2, 3), Map.of(), Map.of());
        TestObjects.setField(pickup, ExactPickupFromContainerTask.class, "recoveredItems", liveCount);
        ConfirmedRecoveredItemCount completed = new ConfirmedRecoveredItemCount();
        completed.add(5);
        RecoverReservedItemsTask recovery = TestObjects.allocate(RecoverReservedItemsTask.class);
        TestObjects.setField(recovery, RecoverReservedItemsTask.class, "completedRecoveredItems", completed);
        TestObjects.setField(recovery, RecoverReservedItemsTask.class, "currentPickup", pickup);

        assertEquals(8, recovery.confirmedRecoveredCount());
        assertEquals(8, recovery.confirmedRecoveredCount());
        TestObjects.setField(pickup, ExactPickupFromContainerTask.class, "result", ExactPickupFromContainerTask.Result.NO_PROGRESS);
        assertEquals(3, pickup.confirmedRecoveredCount(), "A later candidate failure does not erase earlier confirmed transfers");
        completed.add(pickup.confirmedRecoveredCount());
        TestObjects.setField(recovery, RecoverReservedItemsTask.class, "currentPickup", null);
        assertEquals(8, recovery.confirmedRecoveredCount());
        assertEquals(8, recovery.confirmedRecoveredCount());

        AutoDepositMaintenanceTask maintenance = TestObjects.allocate(AutoDepositMaintenanceTask.class);
        assertEquals(0, maintenance.confirmedRecoveredCount());
        AutoDepositWorkingSetRecovery owner = new AutoDepositWorkingSetRecovery(null,
                new AutoDepositDestinationManifest(new Object(), Dimension.OVERWORLD, 1), new PlayerInventorySnapshotReader());
        TestObjects.setField(owner, AutoDepositWorkingSetRecovery.class, "recoveryTask", recovery);
        TestObjects.setField(maintenance, AutoDepositMaintenanceTask.class, "workingSetRecovery", owner);
        assertEquals(8, maintenance.confirmedRecoveredCount());
        assertEquals(0, maintenance.confirmedStoredCount(), "Recovered items are never reported as storage transfers");
    }
}
