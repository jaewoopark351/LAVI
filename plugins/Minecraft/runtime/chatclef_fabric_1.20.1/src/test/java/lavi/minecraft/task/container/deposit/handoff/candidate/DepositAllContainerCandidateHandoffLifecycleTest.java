package lavi.minecraft.task.container.deposit.handoff.candidate;

import lavi.minecraft.testsupport.HeadlessMinecraftClientSession;
import lavi.minecraft.testsupport.TestObjects;
import net.minecraft.block.Block;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260831_kpopmodder: Verify container-candidate changes across the post-place scheduler handoff.
class DepositAllContainerCandidateHandoffLifecycleTest {
    @Test
    void completedChestPlacementReevaluatesBarrelOnTheNextGeneration() {
        try (HeadlessMinecraftClientSession ignored = HeadlessMinecraftClientSession.outOfGame()) {
            Block chestCandidate = TestObjects.allocateBootstrapped(Block.class);
            Block barrelCandidate = TestObjects.allocateBootstrapped(Block.class);
            PlacementTaskProbe chestPlacement = PlacementTaskProbe.create();
            PlacementTaskProbe barrelPlacement = PlacementTaskProbe.create();
            ContainerRouteProbe route = new ContainerRouteProbe();
            ContainerCandidateTaskFactory taskFactory = new ContainerCandidateTaskFactory(
                    chestCandidate,
                    barrelCandidate,
                    chestPlacement,
                    barrelPlacement,
                    new OpenTaskProbe(chestCandidate));
            ContainerCandidateHandoffParent parent =
                    new ContainerCandidateHandoffParent(route, taskFactory);
            HandoffSchedulerChain chain = new HandoffSchedulerChain();

            try {
                route.selectPlacement(chestCandidate);
                chain.setTask(parent);
                chain.tick();

                assertSame(chestPlacement, TaskLifecycleAccess.actualChild(parent));
                assertEquals(1, route.evaluationCalls());
                assertEquals(1, taskFactory.chestFactoryCalls());

                route.selectPlacement(barrelCandidate);
                chestPlacement.finish();
                chain.tick();

                assertNull(TaskLifecycleAccess.actualChild(parent));
                assertNull(parent.ownedPlacement());
                assertEquals(1, chestPlacement.stopCalls());
                assertEquals(1, parent.barrierCalls());
                assertEquals(1, route.evaluationCalls());
                assertEquals(0, taskFactory.barrelFactoryCalls());
                assertEquals(0, barrelPlacement.tickCalls());

                chain.tick();

                assertSame(barrelPlacement, TaskLifecycleAccess.actualChild(parent));
                assertSame(barrelPlacement, parent.ownedPlacement());
                assertTrue(barrelPlacement.isActive());
                assertEquals(2, route.evaluationCalls());
                assertEquals(1, taskFactory.barrelFactoryCalls());
                assertEquals(1, barrelPlacement.tickCalls());
                assertEquals(1, parent.barrierCalls());
            } finally {
                chain.setTask(null);
            }
        }
    }

    @Test
    void completedBarrelPlacementReevaluatesChestOnTheNextGeneration() {
        try (HeadlessMinecraftClientSession ignored = HeadlessMinecraftClientSession.outOfGame()) {
            Block chestCandidate = TestObjects.allocateBootstrapped(Block.class);
            Block barrelCandidate = TestObjects.allocateBootstrapped(Block.class);
            PlacementTaskProbe chestPlacement = PlacementTaskProbe.create();
            PlacementTaskProbe barrelPlacement = PlacementTaskProbe.create();
            ContainerRouteProbe route = new ContainerRouteProbe();
            ContainerCandidateTaskFactory taskFactory = new ContainerCandidateTaskFactory(
                    chestCandidate,
                    barrelCandidate,
                    chestPlacement,
                    barrelPlacement,
                    new OpenTaskProbe(chestCandidate));
            ContainerCandidateHandoffParent parent =
                    new ContainerCandidateHandoffParent(route, taskFactory);
            HandoffSchedulerChain chain = new HandoffSchedulerChain();

            try {
                route.selectPlacement(barrelCandidate);
                chain.setTask(parent);
                chain.tick();

                assertSame(barrelPlacement, TaskLifecycleAccess.actualChild(parent));
                assertEquals(1, route.evaluationCalls());
                assertEquals(1, taskFactory.barrelFactoryCalls());

                route.selectPlacement(chestCandidate);
                barrelPlacement.finish();
                chain.tick();

                assertNull(TaskLifecycleAccess.actualChild(parent));
                assertNull(parent.ownedPlacement());
                assertEquals(1, barrelPlacement.stopCalls());
                assertEquals(1, parent.barrierCalls());
                assertEquals(1, route.evaluationCalls());
                assertEquals(0, taskFactory.chestFactoryCalls());
                assertEquals(0, chestPlacement.tickCalls());

                chain.tick();

                assertSame(chestPlacement, TaskLifecycleAccess.actualChild(parent));
                assertSame(chestPlacement, parent.ownedPlacement());
                assertTrue(chestPlacement.isActive());
                assertEquals(2, route.evaluationCalls());
                assertEquals(1, taskFactory.chestFactoryCalls());
                assertEquals(1, chestPlacement.tickCalls());
                assertEquals(1, parent.barrierCalls());
            } finally {
                chain.setTask(null);
            }
        }
    }

    @Test
    void changedCandidateKeepsTheActualPlacementThroughTheBarrierAndReevaluatesTheNextGeneration() {
        try (HeadlessMinecraftClientSession ignored = HeadlessMinecraftClientSession.outOfGame()) {
            Block chestCandidate = TestObjects.allocateBootstrapped(Block.class);
            Block barrelCandidate = TestObjects.allocateBootstrapped(Block.class);
            PlacementTaskProbe chestPlacement = PlacementTaskProbe.create();
            PlacementTaskProbe barrelPlacement = PlacementTaskProbe.create();
            OpenTaskProbe newlyPlacedChestOpen = new OpenTaskProbe(chestCandidate);
            ContainerRouteProbe route = new ContainerRouteProbe();
            ContainerCandidateTaskFactory taskFactory = new ContainerCandidateTaskFactory(
                    chestCandidate,
                    barrelCandidate,
                    chestPlacement,
                    barrelPlacement,
                    newlyPlacedChestOpen);
            ContainerCandidateHandoffParent parent =
                    new ContainerCandidateHandoffParent(route, taskFactory);
            HandoffSchedulerChain chain = new HandoffSchedulerChain();

            try {
                route.selectPlacement(chestCandidate);
                chain.setTask(parent);
                chain.tick();

                assertSame(chestPlacement, TaskLifecycleAccess.actualChild(parent));
                assertSame(chestPlacement, parent.ownedPlacement());
                assertTrue(chestPlacement.isActive());
                assertEquals(1, chestPlacement.tickCalls());
                assertEquals(1, route.evaluationCalls());
                assertEquals(1, taskFactory.chestFactoryCalls());
                assertEquals(0, taskFactory.barrelFactoryCalls());

                route.selectPlacement(barrelCandidate);
                chain.tick();

                assertSame(chestPlacement, TaskLifecycleAccess.actualChild(parent));
                assertSame(chestPlacement, parent.ownedPlacement());
                assertEquals(2, chestPlacement.tickCalls());
                assertEquals(0, chestPlacement.stopCalls());
                assertEquals(2, route.evaluationCalls());
                assertEquals(1, taskFactory.chestFactoryCalls());
                assertEquals(0, taskFactory.barrelFactoryCalls());
                assertEquals(0, barrelPlacement.tickCalls());

                chestPlacement.finish();
                chain.tick();

                assertNull(TaskLifecycleAccess.actualChild(parent));
                assertNull(parent.ownedPlacement());
                assertFalse(chestPlacement.isActive());
                assertEquals(1, chestPlacement.stopCalls());
                assertEquals(1, parent.barrierCalls());
                assertEquals(2, route.evaluationCalls());
                assertEquals(0, newlyPlacedChestOpen.startCalls());
                assertEquals(0, newlyPlacedChestOpen.tickCalls());
                assertEquals(0, taskFactory.barrelFactoryCalls());
                assertEquals(0, barrelPlacement.tickCalls());

                route.selectOpen(chestCandidate);
                chain.tick();

                assertSame(newlyPlacedChestOpen, TaskLifecycleAccess.actualChild(parent));
                assertSame(chestCandidate, newlyPlacedChestOpen.target());
                assertEquals(3, route.evaluationCalls());
                assertEquals(1, newlyPlacedChestOpen.startCalls());
                assertEquals(1, newlyPlacedChestOpen.tickCalls());
                assertEquals(1, parent.barrierCalls());
                assertEquals(0, taskFactory.barrelFactoryCalls());

                route.selectPlacement(barrelCandidate);
                chain.tick();

                assertSame(barrelPlacement, TaskLifecycleAccess.actualChild(parent));
                assertSame(barrelPlacement, parent.ownedPlacement());
                assertTrue(barrelPlacement.isActive());
                assertEquals(4, route.evaluationCalls());
                assertEquals(1, taskFactory.barrelFactoryCalls());
                assertEquals(1, barrelPlacement.tickCalls());
                assertEquals(0, barrelPlacement.stopCalls());
                assertEquals(1, newlyPlacedChestOpen.stopCalls());
                assertEquals(1, parent.barrierCalls());

                barrelPlacement.finish();
                chain.tick();

                assertNull(TaskLifecycleAccess.actualChild(parent));
                assertNull(parent.ownedPlacement());
                assertFalse(barrelPlacement.isActive());
                assertEquals(1, barrelPlacement.stopCalls());
                assertEquals(2, parent.barrierCalls());
                assertEquals(4, route.evaluationCalls());
                assertEquals(1, taskFactory.chestFactoryCalls());
                assertEquals(1, taskFactory.barrelFactoryCalls());
                assertEquals(1, chestPlacement.stopCalls());
            } finally {
                chain.setTask(null);
            }
        }
    }

    @Test
    void completedChestPlacementAllowsFreshChestIdentityInALaterGeneration() {
        try (HeadlessMinecraftClientSession ignored = HeadlessMinecraftClientSession.outOfGame()) {
            Block chestCandidate = TestObjects.allocateBootstrapped(Block.class);
            Block barrelCandidate = TestObjects.allocateBootstrapped(Block.class);
            PlacementTaskProbe firstChestPlacement = PlacementTaskProbe.create();
            PlacementTaskProbe secondChestPlacement = PlacementTaskProbe.create();
            PlacementTaskProbe barrelPlacement = PlacementTaskProbe.create();
            OpenTaskProbe newlyPlacedChestOpen = new OpenTaskProbe(chestCandidate);
            ContainerRouteProbe route = new ContainerRouteProbe();
            ContainerCandidateTaskFactory taskFactory = new ContainerCandidateTaskFactory(
                    chestCandidate,
                    barrelCandidate,
                    new PlacementTaskSequence(firstChestPlacement, secondChestPlacement),
                    new PlacementTaskSequence(barrelPlacement),
                    newlyPlacedChestOpen);
            ContainerCandidateHandoffParent parent =
                    new ContainerCandidateHandoffParent(route, taskFactory);
            HandoffSchedulerChain chain = new HandoffSchedulerChain();

            try {
                route.selectPlacement(chestCandidate);
                chain.setTask(parent);
                chain.tick();

                assertSame(firstChestPlacement, TaskLifecycleAccess.actualChild(parent));
                assertSame(firstChestPlacement, parent.ownedPlacement());
                assertEquals(1, taskFactory.chestFactoryCalls());
                assertEquals(1, route.evaluationCalls());

                firstChestPlacement.finish();
                chain.tick();

                assertNull(TaskLifecycleAccess.actualChild(parent));
                assertNull(parent.ownedPlacement());
                assertEquals(1, firstChestPlacement.stopCalls());
                assertEquals(1, parent.barrierCalls());
                assertEquals(1, route.evaluationCalls());

                route.selectPlacement(chestCandidate);
                chain.tick();

                assertNotSame(firstChestPlacement, secondChestPlacement);
                assertSame(secondChestPlacement, TaskLifecycleAccess.actualChild(parent));
                assertSame(secondChestPlacement, parent.ownedPlacement());
                assertTrue(secondChestPlacement.isActive());
                assertEquals(2, taskFactory.chestFactoryCalls());
                assertEquals(2, route.evaluationCalls());

                secondChestPlacement.finish();
                chain.tick();

                assertNull(TaskLifecycleAccess.actualChild(parent));
                assertNull(parent.ownedPlacement());
                assertEquals(1, secondChestPlacement.stopCalls());
                assertEquals(2, parent.barrierCalls());
                assertEquals(2, route.evaluationCalls());

                route.selectOpen(chestCandidate);
                chain.tick();

                assertSame(newlyPlacedChestOpen, TaskLifecycleAccess.actualChild(parent));
                assertEquals(1, newlyPlacedChestOpen.startCalls());
                assertEquals(1, newlyPlacedChestOpen.tickCalls());
                assertEquals(3, route.evaluationCalls());
                assertEquals(2, parent.barrierCalls());
                assertEquals(1, firstChestPlacement.stopCalls());
                assertEquals(1, secondChestPlacement.stopCalls());
                assertEquals(0, taskFactory.barrelFactoryCalls());
            } finally {
                chain.setTask(null);
            }
        }
    }
}
