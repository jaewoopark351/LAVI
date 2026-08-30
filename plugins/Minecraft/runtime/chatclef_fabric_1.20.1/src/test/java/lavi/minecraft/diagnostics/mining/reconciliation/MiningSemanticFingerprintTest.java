package lavi.minecraft.diagnostics.mining.reconciliation;

import adris.altoclef.tasksystem.Task;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

//20260830_kpopmodder: Keep candidate allocation identity out of reconciliation fingerprints.
class MiningSemanticFingerprintTest {
    @Test
    void freshSameTargetCandidateIdentityKeepsTheSameFingerprint() {
        Task parent = new TestTask("parent");
        Task active = new TestTask("active");
        BlockPos target = new BlockPos(8, 64, 12);
        ReconciliationTaskIdentity activeIdentity = identity(active, target);

        String first = allocationNoiseFingerprint(parent, activeIdentity, identity(new TestTask("candidate-b"), target));
        String second = allocationNoiseFingerprint(parent, activeIdentity, identity(new TestTask("candidate-c"), target));

        assertEquals(first, second);
    }

    @Test
    void actualSameTargetReplacementChangesTheSemanticFingerprint() {
        Task parent = new TestTask("parent");
        Task active = new TestTask("active");
        Task replacement = new TestTask("replacement");
        BlockPos target = new BlockPos(8, 64, 12);
        ReconciliationTaskIdentity before = identity(active, target);
        ReconciliationTaskIdentity candidate = identity(replacement, target);
        ReconciliationTaskIdentity after = identity(replacement, target);
        ReconciliationOutcome replacementOutcome = ReconciliationOutcome.classify(
                before, candidate, after, false, true, true, true, false);
        String replacementFingerprint = ReconciliationSemanticFingerprint.create(
                parent, before, candidate, after, replacementOutcome, false, true, false);

        String allocationNoise = allocationNoiseFingerprint(
                parent, before, identity(new TestTask("candidate"), target));
        assertEquals("SAME_TARGET_ACTIVE_CHILD_REPLACED",
                replacementOutcome.reconciliationClassification());
        assertNotEquals(allocationNoise, replacementFingerprint);
    }

    @Test
    void changedCandidateTargetChangesTheSemanticFingerprint() {
        Task parent = new TestTask("parent");
        Task active = new TestTask("active");
        ReconciliationTaskIdentity activeIdentity = identity(active, new BlockPos(8, 64, 12));

        String sameTarget = retainedFingerprint(
                parent, activeIdentity, identity(new TestTask("candidate-a"), new BlockPos(8, 64, 12)));
        String changedTarget = retainedFingerprint(
                parent, activeIdentity, identity(new TestTask("candidate-b"), new BlockPos(9, 64, 12)));

        assertNotEquals(sameTarget, changedTarget);
    }

    private static String allocationNoiseFingerprint(Task parent,
                                                     ReconciliationTaskIdentity active,
                                                     ReconciliationTaskIdentity candidate) {
        ReconciliationOutcome outcome = ReconciliationOutcome.classify(
                active, candidate, active, true, false, false, false, true);
        assertEquals("CANDIDATE_ALLOCATION_NO_ACTIVE_CHURN", outcome.reconciliationClassification());
        return ReconciliationSemanticFingerprint.create(
                parent, active, candidate, active, outcome, true, false, true);
    }

    private static String retainedFingerprint(Task parent,
                                              ReconciliationTaskIdentity active,
                                              ReconciliationTaskIdentity candidate) {
        ReconciliationOutcome outcome = ReconciliationOutcome.classify(
                active, candidate, active, true, false, false, false, true);
        return ReconciliationSemanticFingerprint.create(
                parent, active, candidate, active, outcome, true, false, true);
    }

    private static ReconciliationTaskIdentity identity(Task task, BlockPos target) {
        return new ReconciliationTaskIdentity(
                task,
                task.getClass().getName(),
                Integer.toHexString(System.identityHashCode(task)),
                target,
                target.getX() + "," + target.getY() + "," + target.getZ()
        );
    }

    private static final class TestTask extends Task {
        private final String name;

        private TestTask(String name) {
            this.name = name;
        }

        @Override
        protected void onStart() {
        }

        @Override
        protected Task onTick() {
            return null;
        }

        @Override
        protected void onStop(Task interruptTask) {
        }

        @Override
        protected boolean isEqual(Task other) {
            return this == other;
        }

        @Override
        protected String toDebugString() {
            return name;
        }
    }
}
