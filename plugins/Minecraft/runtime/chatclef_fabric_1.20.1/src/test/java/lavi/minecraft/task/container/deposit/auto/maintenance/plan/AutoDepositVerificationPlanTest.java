package lavi.minecraft.task.container.deposit.auto.maintenance.plan;

import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.ItemTarget;
import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositContextSnapshot;
import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositPlan;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestination;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationCandidate;
import lavi.minecraft.task.container.deposit.auto.working.WorkingSetSnapshot;
import lavi.minecraft.testsupport.TestObjects;
import lavi.minecraft.testsupport.auto.AutoDepositPlanFixtureFactory;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

//20260914_kpopmodder: A new READY plan cannot add storage targets to a previously completed purpose's verification root.
class AutoDepositVerificationPlanTest {
    @Test
    void removesEveryStorageActionButRetainsCurrentContextReservationAndPressure() {
        Task root = new Task() {
            @Override protected void onStart() { }
            @Override protected Task onTick() { return null; }
            @Override protected void onStop(Task interruptTask) { }
            @Override protected boolean isEqual(Task other) { return this == other; }
            @Override protected String toDebugString() { return "verification-plan-fixture"; }
        };
        Object world = new Object();
        WorkingSetSnapshot working = new WorkingSetSnapshot(root, List.of(root), world,
                Dimension.OVERWORLD, 1L, Map.of(), Map.of(), Map.of());
        AutoDepositContextSnapshot context = new AutoDepositContextSnapshot(world, Dimension.OVERWORLD,
                "verification-plan", 1L, root, working, List.of("fixture"));
        AutoDepositPlan source = AutoDepositPlanFixtureFactory.generalPlan(context, root, 32, 5, 3);
        TestObjects.setField(source, AutoDepositPlan.class, "trustedTargets", source.generalTargets());
        AutoDepositTrustedDestinationCandidate candidate = new AutoDepositTrustedDestinationCandidate(
                TestObjects.allocate(AutoDepositTrustedDestination.class), 3, 2.0, "test-candidate");
        TestObjects.setField(source, AutoDepositPlan.class, "trustedCandidates", List.of(candidate));

        AutoDepositPlan verification = AutoDepositVerificationPlan.from(source);

        assertNotSame(source, verification);
        assertSame(context, verification.context());
        assertSame(working, verification.context().workingSet());
        assertEquals(source.protectedCounts(), verification.protectedCounts());
        assertEquals(32, verification.startingOccupiedSlots());
        assertEquals(5, verification.targetReliefSlots());
        assertEquals(0, verification.expectedFreedSlots());
        assertFalse(verification.hasTargets());
        assertEquals(0, verification.allTargets().length);
        assertTrue(verification.trustedCandidates().isEmpty());
        assertTrue(verification.trustedDestination().isEmpty());
        assertTrue(verification.dispositions().isEmpty());
        assertTrue(verification.policyItemDecisions().isEmpty());
        assertSame(source.fingerprint(), verification.fingerprint());
        assertTrue(source.hasTargets());
        assertEquals(1, source.trustedTargets().length);
        assertEquals(List.of(candidate), source.trustedCandidates());
        assertThrows(UnsupportedOperationException.class,
                () -> verification.trustedCandidates().add(candidate));
        ItemTarget[] originalCopy = source.generalTargets();
        originalCopy[0] = null;
        assertNotNull(source.generalTargets()[0]);
    }
}
