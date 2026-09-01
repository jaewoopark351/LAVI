package lavi.minecraft.task.container.deposit.handoff.candidate;

import adris.altoclef.tasks.container.DepositAllTask;
import adris.altoclef.util.ItemTarget;
import lavi.minecraft.task.container.deposit.DepositAllContainerTargetState;
import lavi.minecraft.task.container.deposit.DepositAllStoreTaskGeneration;
import lavi.minecraft.task.container.deposit.auto.maintenance.child.AutoDepositGeneralTaskFactory;
import lavi.minecraft.task.container.deposit.handoff.DepositAllPlacementTaskOwner;
import lavi.minecraft.task.container.deposit.handoff.DepositAllPostPlaceHandoff;
import lavi.minecraft.testsupport.HeadlessMinecraftClientSession;
import lavi.minecraft.testsupport.TestObjects;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260831_kpopmodder: Verify automatic operations receive independent candidate-owner and handoff state.
class AutoDepositContainerCandidateOperationIsolationTest {
    @Test
    void dirtyPreviousOperationDoesNotLeakStateIntoTheNextOperation() {
        try (HeadlessMinecraftClientSession ignored = HeadlessMinecraftClientSession.outOfGame()) {
            AutoDepositGeneralTaskFactory operationFactory = new AutoDepositGeneralTaskFactory();
            DepositAllTask firstOperation = operationFactory.create(targets()).get(0);
            DepositAllPlacementTaskOwner firstOwner =
                    DepositAllTaskHandoffAccess.placementOwner(firstOperation);
            DepositAllPostPlaceHandoff firstHandoff =
                    DepositAllTaskHandoffAccess.postPlaceHandoff(firstOperation);
            DepositAllContainerTargetState firstTargetState =
                    DepositAllTaskOperationStateAccess.targetState(firstOperation);
            DepositAllStoreTaskGeneration firstStoreGeneration =
                    DepositAllTaskOperationStateAccess.storeTaskGeneration(firstOperation);
            Block chestCandidate = TestObjects.allocate(Block.class);
            Block barrelCandidate = TestObjects.allocate(Block.class);
            PlacementTaskProbe sharedPlacementIdentity = PlacementTaskProbe.create();

            assertSame(
                    sharedPlacementIdentity,
                    firstOwner.getOrCreate(chestCandidate, () -> sharedPlacementIdentity));
            assertTrue(firstHandoff.shouldDefer(sharedPlacementIdentity, true, true));
            assertTrue(firstTargetState.select(new BlockPos(8, 64, 12)));

            DepositAllTask secondOperation = operationFactory.create(targets()).get(0);
            DepositAllPlacementTaskOwner secondOwner =
                    DepositAllTaskHandoffAccess.placementOwner(secondOperation);
            DepositAllPostPlaceHandoff secondHandoff =
                    DepositAllTaskHandoffAccess.postPlaceHandoff(secondOperation);
            DepositAllContainerTargetState secondTargetState =
                    DepositAllTaskOperationStateAccess.targetState(secondOperation);
            DepositAllStoreTaskGeneration secondStoreGeneration =
                    DepositAllTaskOperationStateAccess.storeTaskGeneration(secondOperation);

            assertNotSame(firstOperation, secondOperation);
            assertNotSame(firstOwner, secondOwner);
            assertNotSame(firstHandoff, secondHandoff);
            assertNotSame(firstTargetState, secondTargetState);
            assertNotSame(firstStoreGeneration, secondStoreGeneration);
            assertTrue(firstOwner.retainingIdentity());
            assertTrue(secondOwner.retainingIdentity());
            assertTrue(firstHandoff.enabled());
            assertTrue(secondHandoff.enabled());
            assertTrue(firstTargetState.selectedTarget().isPresent());
            assertTrue(secondTargetState.selectedTarget().isEmpty());
            assertNull(secondOwner.currentTask());
            assertSame(
                    sharedPlacementIdentity,
                    secondOwner.getOrCreate(barrelCandidate, () -> sharedPlacementIdentity));
            assertTrue(secondHandoff.shouldDefer(sharedPlacementIdentity, true, true));

            assertSame(sharedPlacementIdentity, firstOwner.currentTask());
            assertSame(sharedPlacementIdentity, secondOwner.currentTask());
        }
    }

    @Test
    void dirtyStoreGenerationDoesNotLeakIntoTheNextOperation() {
        try (HeadlessMinecraftClientSession ignored = HeadlessMinecraftClientSession.outOfGame()) {
            AutoDepositGeneralTaskFactory operationFactory = new AutoDepositGeneralTaskFactory();
            DepositAllTask firstOperation = operationFactory.create(targets()).get(0);
            DepositAllStoreTaskGeneration firstStoreGeneration =
                    DepositAllTaskOperationStateAccess.storeTaskGeneration(firstOperation);
            BlockPos firstStoreTarget = new BlockPos(14, 64, 18);
            ItemTarget firstStoreSnapshot = targets()[0];

            DepositAllStoreGenerationStateAccess.prime(
                    firstStoreGeneration,
                    firstStoreTarget,
                    new ItemTarget[]{firstStoreSnapshot},
                    PlacementTaskProbe.create(),
                    1
            );

            assertEquals(1, firstStoreGeneration.generationId());
            assertEquals(firstStoreTarget, firstStoreGeneration.activeStoreTarget().orElseThrow());
            assertEquals(1, firstStoreGeneration.activeStoreSnapshot().length);
            assertSame(firstStoreSnapshot, firstStoreGeneration.activeStoreSnapshot()[0]);

            DepositAllTask secondOperation = operationFactory.create(targets()).get(0);
            DepositAllStoreTaskGeneration secondStoreGeneration =
                    DepositAllTaskOperationStateAccess.storeTaskGeneration(secondOperation);

            assertNotSame(firstOperation, secondOperation);
            assertNotSame(firstStoreGeneration, secondStoreGeneration);
            assertEquals(0, secondStoreGeneration.generationId());
            assertTrue(secondStoreGeneration.activeStoreTarget().isEmpty());
            assertEquals(0, secondStoreGeneration.activeStoreSnapshot().length);
        }
    }

    private static ItemTarget[] targets() {
        return new ItemTarget[]{new ItemTarget(new Item[0], 1)};
    }
}
