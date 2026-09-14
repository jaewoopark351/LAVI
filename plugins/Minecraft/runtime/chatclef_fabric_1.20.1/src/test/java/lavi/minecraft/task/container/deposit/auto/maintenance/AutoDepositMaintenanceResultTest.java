package lavi.minecraft.task.container.deposit.auto.maintenance;

import adris.altoclef.chains.UserTaskChain;
import adris.altoclef.util.ItemTarget;
import lavi.minecraft.task.container.deposit.auto.DepositAllInventoryPressureSnapshot;
import lavi.minecraft.task.container.deposit.auto.maintenance.result.AutoDepositRunReason;
import lavi.minecraft.task.container.deposit.auto.maintenance.result.AutoDepositRunResult;
import lavi.minecraft.task.container.deposit.auto.maintenance.result.AutoDepositWorkingSetStatus;
import lavi.minecraft.task.container.deposit.auto.maintenance.support.AutoDepositMaintenanceFixture;
import lavi.minecraft.task.container.deposit.auto.maintenance.support.ObservedDepositChild;
import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositPlan;
import lavi.minecraft.task.container.deposit.auto.pressure.AutoDepositInventoryPressureSource;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationRepository;
import lavi.minecraft.task.container.deposit.auto.trusted.interaction.AutoDepositExactOpenContainerBinding;
import lavi.minecraft.testsupport.TestObjects;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

//20260914_kpopmodder: Verify maintenance parent evidence, real tick/stop boundaries, and cleanup settlement.
class AutoDepositMaintenanceResultTest {
    @Test
    void genericFinishedWithoutStoredProofAndStoppedChildCannotBecomeNormal() {
        try (AutoDepositMaintenanceFixture fixture = new AutoDepositMaintenanceFixture()) {
            assertChildFailure(fixture, new ObservedDepositChild(true, false, false, 0),
                    AutoDepositRunReason.GENERAL_CHILD_UNCONFIRMED);
            assertChildFailure(fixture, new ObservedDepositChild(false, true, false, 0),
                    AutoDepositRunReason.CHILD_STOPPED);
        }
    }

    @Test
    void verifiedChildrenWorkingSetAndPressureSettleOnlyAfterCleanup() {
        try (AutoDepositMaintenanceFixture fixture = new AutoDepositMaintenanceFixture()) {
            AutoDepositMaintenanceTask task = task(fixture.plan(33), pressure(30));
            ObservedDepositChild child = new ObservedDepositChild(true, false, true, 5);
            children(task, child);
            UserTaskChain chain = fixture.chain();
            task.tick(chain);
            task.tick(chain);
            task.tick(chain);

            AutoDepositRunResult candidate = task.completionCandidate().orElseThrow();
            assertEquals(AutoDepositRunReason.NORMAL, candidate.reason());
            assertTrue(candidate.childrenComplete());
            assertEquals(3, candidate.signedFreedSlotDelta().orElseThrow());
            assertEquals(5, task.confirmedStoredCount());
            assertFalse(candidate.cleanupComplete());
            assertTrue(task.result().isEmpty());
            assertEquals(0, child.ticks());

            task.stop(null);
            AutoDepositRunResult result = task.finalizeAfterCleanup(true).orElseThrow();
            assertTrue(result.normalValidated());
            task.terminate(AutoDepositRunReason.STOPPED);
            assertSame(result, task.finalizeAfterCleanup(false).orElseThrow());
            assertEquals(AutoDepositRunReason.NORMAL, task.completionCandidate().orElseThrow().reason());
        }
    }

    @Test
    void missingTerminalPressureDoesNotInventZeroReliefOrNormalSuccess() {
        try (AutoDepositMaintenanceFixture fixture = new AutoDepositMaintenanceFixture()) {
            AtomicInteger reads = new AtomicInteger();
            AutoDepositMaintenanceTask task = task(fixture.plan(33), ignored -> {
                reads.incrementAndGet();
                return Optional.empty();
            });
            children(task, new ObservedDepositChild(true, false, true, 5));
            UserTaskChain chain = fixture.chain();
            for (int tick = 0; tick < 3; tick++) task.tick(chain);
            assertEquals(1, reads.get());
            assertEquals(AutoDepositMaintenanceOutcome.UNAVAILABLE, task.outcome());
            task.stop(null);
            AutoDepositRunResult result = task.finalizeAfterCleanup(true).orElseThrow();
            assertEquals(AutoDepositRunReason.PRESSURE_UNAVAILABLE, result.reason());
            assertTrue(result.endingPressure().isEmpty());
            assertTrue(result.signedFreedSlotDelta().isEmpty());
            assertFalse(result.normalValidated());
        }
    }

    @Test
    void budgetObserverRunsOnceAndCanStopBeforeTheChildTick() {
        try (AutoDepositMaintenanceFixture fixture = new AutoDepositMaintenanceFixture()) {
            AtomicInteger observerCalls = new AtomicInteger();
            AutoDepositMaintenanceTask task = new AutoDepositMaintenanceTask(fixture.plan(36),
                    AutoDepositTrustedDestinationRepository.inMemoryEmpty(),
                    AutoDepositExactOpenContainerBinding.UNAVAILABLE, pressure(36), current -> {
                observerCalls.incrementAndGet();
                current.terminate(AutoDepositRunReason.BUDGET_EXHAUSTED);
            });
            ObservedDepositChild child = new ObservedDepositChild(false, false, false, 0);
            children(task, child);
            task.tick(fixture.chain());
            assertEquals(1, observerCalls.get());
            assertEquals(0, child.ticks());
            assertEquals(AutoDepositRunReason.BUDGET_EXHAUSTED,
                    task.completionCandidate().orElseThrow().reason());
            task.stop(null);
        }
    }

    @Test
    void completedNativeChildrenAreRetainedWhenSafetyPreemptsTheNextParentTick() {
        try (AutoDepositMaintenanceFixture fixture = new AutoDepositMaintenanceFixture()) {
            AutoDepositMaintenanceTask task = task(fixture.plan(36), pressure(32));
            children(task, new ObservedDepositChild(true, false, true, 4));
            task.terminate(AutoDepositRunReason.SAFETY_INTERRUPTED);
            AutoDepositRunResult candidate = task.completionCandidate().orElseThrow();
            assertEquals(AutoDepositRunReason.SAFETY_INTERRUPTED, candidate.reason());
            assertTrue(candidate.childrenComplete());
            assertEquals(AutoDepositWorkingSetStatus.NOT_APPLICABLE, candidate.workingSetStatus());
            assertEquals(4, candidate.signedFreedSlotDelta().orElseThrow());
            assertEquals(4, task.confirmedStoredCount());
            assertFalse(task.finalizeAfterCleanup(true).orElseThrow().normalValidated());
        }
    }

    @Test
    void emptyPlanRequiresExplicitVerifiedResumeAndRootLocalZeroDeltaCanBeNormal() {
        try (AutoDepositMaintenanceFixture fixture = new AutoDepositMaintenanceFixture()) {
            AutoDepositPlan empty = fixture.plan(32);
            TestObjects.setField(empty, AutoDepositPlan.class, "generalTargets", new ItemTarget[0]);
            assertThrows(IllegalArgumentException.class, () -> task(empty, pressure(32)));
            AutoDepositMaintenanceTask resumed = new AutoDepositMaintenanceTask(empty,
                    AutoDepositTrustedDestinationRepository.inMemoryEmpty(),
                    AutoDepositExactOpenContainerBinding.UNAVAILABLE, pressure(32), ignored -> { }, true);
            UserTaskChain chain = fixture.chain();
            resumed.tick(chain);
            resumed.tick(chain);
            resumed.stop(null);
            AutoDepositRunResult result = resumed.finalizeAfterCleanup(true).orElseThrow();
            assertEquals(0, result.signedFreedSlotDelta().orElseThrow());
            assertTrue(result.normalValidated());
        }
    }

    @Test
    void finalResultReadsPostCleanupOccupancyOnceWithoutRewritingTheCandidate() {
        try (AutoDepositMaintenanceFixture fixture = new AutoDepositMaintenanceFixture()) {
            AtomicInteger reads = new AtomicInteger();
            AutoDepositMaintenanceTask task = task(fixture.plan(36), ignored -> Optional.of(
                    new DepositAllInventoryPressureSnapshot(reads.incrementAndGet() == 1 ? 32 : 33, 36)));
            children(task, new ObservedDepositChild(true, false, true, 4));
            UserTaskChain chain = fixture.chain();
            for (int tick = 0; tick < 3; tick++) task.tick(chain);
            assertEquals(32, task.completionCandidate().orElseThrow().endingPressure().orElseThrow().occupiedSlots());
            task.stop(null);
            AutoDepositRunResult settled = task.finalizeAfterCleanup(true).orElseThrow();
            assertEquals(33, settled.endingPressure().orElseThrow().occupiedSlots());
            assertEquals(2, reads.get());
            task.finalizeAfterCleanup(true);
            assertEquals(2, reads.get());
            assertEquals(32, task.completionCandidate().orElseThrow().endingPressure().orElseThrow().occupiedSlots());
        }
    }

    private static void assertChildFailure(AutoDepositMaintenanceFixture fixture,
            ObservedDepositChild child, AutoDepositRunReason reason) {
        AutoDepositMaintenanceTask task = task(fixture.plan(33), pressure(33));
        children(task, child);
        task.tick(fixture.chain());
        assertEquals(reason, task.completionCandidate().orElseThrow().reason());
        assertFalse(task.completionCandidate().orElseThrow().childrenComplete());
        assertEquals(0, child.ticks());
        task.stop(null);
        assertFalse(task.finalizeAfterCleanup(true).orElseThrow().normalValidated());
    }

    private static AutoDepositMaintenanceTask task(AutoDepositPlan plan,
            AutoDepositInventoryPressureSource source) {
        return new AutoDepositMaintenanceTask(plan, AutoDepositTrustedDestinationRepository.inMemoryEmpty(),
                AutoDepositExactOpenContainerBinding.UNAVAILABLE, source);
    }

    private static AutoDepositInventoryPressureSource pressure(int occupied) {
        return ignored -> Optional.of(new DepositAllInventoryPressureSnapshot(occupied, 36));
    }

    private static void children(AutoDepositMaintenanceTask task, ObservedDepositChild child) {
        TestObjects.setField(task, AutoDepositMaintenanceTask.class, "generalTasks", List.of(child));
    }
}
