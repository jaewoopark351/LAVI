package lavi.minecraft.task.container.deposit.auto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

//20260826_kpopmodder: Verify one-shot automatic deposit_all execution and below-threshold rearming.
class DepositAllInventoryPressureStateMachineTest {
    private static final DepositAllInventoryPressureSnapshot BELOW_THRESHOLD =
            new DepositAllInventoryPressureSnapshot(28, 36);
    private static final DepositAllInventoryPressureSnapshot AT_THRESHOLD =
            new DepositAllInventoryPressureSnapshot(29, 36);

    @Test
    void startsOnlyOnceAndRequiresABelowThresholdObservationBeforeAnotherRun() {
        DepositAllInventoryPressureStateMachine machine = new DepositAllInventoryPressureStateMachine();

        assertEquals(DepositAllInventoryPressureSignal.NONE, machine.observe(BELOW_THRESHOLD));
        assertEquals(DepositAllInventoryPressureSignal.THRESHOLD_REACHED, machine.observe(AT_THRESHOLD));
        assertEquals(DepositAllInventoryPressureSignal.NONE, machine.observe(AT_THRESHOLD));

        machine.markRunStarted();
        assertEquals(DepositAllInventoryPressureState.RUNNING, machine.state());
        assertEquals(DepositAllInventoryPressureSignal.NONE, machine.observe(AT_THRESHOLD));

        machine.markRunTerminated();
        assertEquals(DepositAllInventoryPressureState.WAIT_FOR_REARM, machine.state());
        assertEquals(DepositAllInventoryPressureSignal.NONE, machine.observe(AT_THRESHOLD));
        assertEquals(DepositAllInventoryPressureSignal.REARMED, machine.observe(BELOW_THRESHOLD));
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
        assertEquals(DepositAllInventoryPressureSignal.REARMED, machine.observe(BELOW_THRESHOLD));
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
}
