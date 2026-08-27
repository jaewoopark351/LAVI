package lavi.minecraft.task.container.deposit.auto;

import adris.altoclef.util.Dimension;
import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositDecisionFingerprint;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

//20260826_kpopmodder: Verify one-shot automatic deposit_all execution and below-threshold rearming.
class DepositAllInventoryPressureStateMachineTest {
    private static final Object WORLD = new Object();
    private static final DepositAllInventoryPressureSnapshot BETWEEN_WATER_MARKS =
            new DepositAllInventoryPressureSnapshot(32, 36);
    private static final DepositAllInventoryPressureSnapshot AT_LOW_WATER =
            new DepositAllInventoryPressureSnapshot(28, 36);
    private static final DepositAllInventoryPressureSnapshot AT_THRESHOLD =
            new DepositAllInventoryPressureSnapshot(33, 36);

    @Test
    void startsOnlyOnceAndRequiresABelowThresholdObservationBeforeAnotherRun() {
        DepositAllInventoryPressureStateMachine machine = new DepositAllInventoryPressureStateMachine();

        assertEquals(DepositAllInventoryPressureSignal.NONE, machine.observe(BETWEEN_WATER_MARKS));
        assertEquals(DepositAllInventoryPressureSignal.THRESHOLD_REACHED, machine.observe(AT_THRESHOLD));
        assertEquals(DepositAllInventoryPressureSignal.NONE, machine.observe(AT_THRESHOLD));

        machine.markRunStarted();
        assertEquals(DepositAllInventoryPressureState.RUNNING, machine.state());
        assertEquals(DepositAllInventoryPressureSignal.NONE, machine.observe(AT_THRESHOLD));

        machine.markRunTerminated();
        assertEquals(DepositAllInventoryPressureState.WAIT_FOR_REARM, machine.state());
        assertEquals(DepositAllInventoryPressureSignal.NONE, machine.observe(AT_THRESHOLD));
        assertEquals(DepositAllInventoryPressureSignal.NONE, machine.observe(BETWEEN_WATER_MARKS));
        assertEquals(DepositAllInventoryPressureSignal.REARMED, machine.observe(AT_LOW_WATER));
        assertEquals(DepositAllInventoryPressureState.ARMED, machine.state());
        assertEquals(DepositAllInventoryPressureSignal.THRESHOLD_REACHED, machine.observe(AT_THRESHOLD));
    }

    @Test
    void suppressedThresholdDoesNotRetryWhileInventoryRemainsHigh() {
        DepositAllInventoryPressureStateMachine machine = new DepositAllInventoryPressureStateMachine();

        assertEquals(DepositAllInventoryPressureSignal.THRESHOLD_REACHED, machine.observe(AT_THRESHOLD));
        machine.markThresholdSuppressed();

        assertEquals(DepositAllInventoryPressureState.WAIT_FOR_REARM, machine.state());
        assertEquals(DepositAllInventoryPressureSignal.NONE, machine.observe(AT_THRESHOLD));
        assertEquals(DepositAllInventoryPressureSignal.NONE, machine.observe(BETWEEN_WATER_MARKS));
        assertEquals(DepositAllInventoryPressureSignal.REARMED, machine.observe(AT_LOW_WATER));
    }

    @Test
    void explicitTrustedPolicyChangeRearmsACompletedRunExactlyOnce() {
        DepositAllInventoryPressureStateMachine machine = new DepositAllInventoryPressureStateMachine();

        assertEquals(DepositAllInventoryPressureSignal.THRESHOLD_REACHED,
                machine.observe(AT_THRESHOLD));
        machine.markRunStarted();
        machine.markRunTerminated();

        assertEquals(DepositAllInventoryPressureSignal.MEANINGFUL_CHANGE,
                machine.observeExplicitPolicyChange());
        assertEquals(DepositAllInventoryPressureState.ARMED, machine.state());
        assertEquals(DepositAllInventoryPressureSignal.NONE,
                machine.observeExplicitPolicyChange());
        assertEquals(DepositAllInventoryPressureSignal.THRESHOLD_REACHED,
                machine.observe(AT_THRESHOLD));
    }

    @Test
    void noSafeSurplusRetriesOnlyAfterSemanticFingerprintChanges() {
        DepositAllInventoryPressureStateMachine machine = new DepositAllInventoryPressureStateMachine();
        AutoDepositDecisionFingerprint first = fingerprint("first");

        assertEquals(DepositAllInventoryPressureSignal.THRESHOLD_REACHED, machine.observe(AT_THRESHOLD));
        machine.markNoSafeSurplus(first);

        assertEquals(DepositAllInventoryPressureState.NO_SAFE_SURPLUS_WAIT, machine.state());
        assertEquals(DepositAllInventoryPressureSignal.NONE, machine.observeMeaningfulChange(first));
        assertEquals(DepositAllInventoryPressureSignal.NONE, machine.observe(BETWEEN_WATER_MARKS));
        assertEquals(
                DepositAllInventoryPressureSignal.MEANINGFUL_CHANGE,
                machine.observeMeaningfulChange(fingerprint("changed"))
        );
        assertEquals(DepositAllInventoryPressureState.ARMED, machine.state());
        assertEquals(DepositAllInventoryPressureSignal.THRESHOLD_REACHED, machine.observe(AT_THRESHOLD));
    }

    @Test
    void rejectsLifecycleTransitionsFromTheWrongState() {
        DepositAllInventoryPressureStateMachine machine = new DepositAllInventoryPressureStateMachine();

        assertThrows(IllegalStateException.class, machine::markRunTerminated);
        assertThrows(IllegalStateException.class, machine::markRunStarted);
        assertThrows(IllegalStateException.class, machine::markThresholdSuppressed);

        assertEquals(DepositAllInventoryPressureSignal.THRESHOLD_REACHED, machine.observe(AT_THRESHOLD));
        machine.markRunStarted();
        assertThrows(IllegalStateException.class, machine::markRunStarted);
        assertThrows(IllegalStateException.class, machine::markThresholdSuppressed);
    }

    private static AutoDepositDecisionFingerprint fingerprint(String semantic) {
        return new AutoDepositDecisionFingerprint(
                WORLD,
                null,
                Dimension.OVERWORLD,
                "test-world",
                1,
                1L,
                "none",
                List.of(semantic)
        );
    }
}
