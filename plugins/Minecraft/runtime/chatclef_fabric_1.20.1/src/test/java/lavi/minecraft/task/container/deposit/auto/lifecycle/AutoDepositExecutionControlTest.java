package lavi.minecraft.task.container.deposit.auto.lifecycle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

//20260914_kpopmodder: Keep explicit STOP latched across idle observations until the native runner resumes.
class AutoDepositExecutionControlTest {
    @Test
    void initialIdleDoesNotPretendTheUserHasIssuedStop() {
        AutoDepositExecutionControl control = new AutoDepositExecutionControl();
        assertTrue(control.permitsExecution(false));
        assertFalse(control.stopped());
    }

    @Test
    void repeatedIdleAndSafetyWaitObservationsCannotClearStop() {
        AutoDepositExecutionControl control = new AutoDepositExecutionControl();
        control.stop();
        for (int observation = 0; observation < 100; observation++) {
            assertFalse(control.permitsExecution(false));
            assertTrue(control.stopped());
        }
        control.stop();
        assertFalse(control.permitsExecution(false));
    }

    @Test
    void nativeRunnerResumeReleasesControlAndALaterStopLatchesAgain() {
        AutoDepositExecutionControl control = new AutoDepositExecutionControl();
        control.stop();
        assertFalse(control.permitsExecution(false));
        assertTrue(control.permitsExecution(true));
        assertFalse(control.stopped());
        control.stop();
        assertFalse(control.permitsExecution(false));
        assertTrue(control.stopped());
    }
}
