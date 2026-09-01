package lavi.minecraft.task.container.deposit.handoff.candidate;

import adris.altoclef.tasks.container.DepositAllTask;
import adris.altoclef.util.ItemTarget;
import lavi.minecraft.task.container.deposit.auto.maintenance.child.AutoDepositGeneralTaskFactory;
import lavi.minecraft.task.container.deposit.handoff.DepositAllPlacementTaskOwner;
import lavi.minecraft.testsupport.HeadlessMinecraftClientSession;
import lavi.minecraft.testsupport.TestObjects;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260831_kpopmodder: Bind the one-tick handoff contract to DepositAllTask's production boundary.
class DepositAllTaskPostPlaceHandoffBoundaryTest {
    @Test
    void productionBoundaryClearsTheFinishedPlacementAndDefersOnlyOnce() {
        try (HeadlessMinecraftClientSession ignored = HeadlessMinecraftClientSession.outOfGame()) {
            DepositAllTask operation = new AutoDepositGeneralTaskFactory().create(targets()).get(0);
            DepositAllPlacementTaskOwner owner = DepositAllTaskHandoffAccess.placementOwner(operation);
            Block chestCandidate = TestObjects.allocateBootstrapped(Block.class);
            PlacementTaskProbe placement = PlacementTaskProbe.create();

            assertSame(placement, owner.getOrCreate(chestCandidate, () -> placement));
            TaskLifecycleAccess.markActive(placement);
            placement.finish();

            assertTrue(DepositAllTaskHandoffAccess.deferAfterCompletedPlacement(operation));
            assertNull(owner.currentTask());
            assertFalse(DepositAllTaskHandoffAccess.deferAfterCompletedPlacement(operation));
        }
    }

    private static ItemTarget[] targets() {
        return new ItemTarget[]{new ItemTarget(new Item[0], 1)};
    }
}
