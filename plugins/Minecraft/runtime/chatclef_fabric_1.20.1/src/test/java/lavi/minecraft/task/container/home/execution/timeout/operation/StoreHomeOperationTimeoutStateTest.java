package lavi.minecraft.task.container.home.execution.timeout.operation;

import lavi.minecraft.task.container.home.execution.timeout.StoreHomeTimeoutPolicy;
import lavi.minecraft.task.container.home.execution.timeout.StoreHomeTimeoutReason;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260828_kpopmodder: Verify resettable operation progress and the non-resettable emergency ceiling.
class StoreHomeOperationTimeoutStateTest {
    private static final StoreHomeTimeoutPolicy POLICY =
            new StoreHomeTimeoutPolicy(3, 3, 4, 8, 0.5, 1.0);

    @Test
    void operationNoProgressUsesConsecutiveActiveTicks() {
        StoreHomeOperationTimeoutState state = new StoreHomeOperationTimeoutState();

        tick(state, 3);
        assertTrue(state.timeoutReason(POLICY).isEmpty());
        state.tick();
        assertEquals(
                StoreHomeTimeoutReason.OPERATION_NO_PROGRESS,
                state.timeoutReason(POLICY).orElseThrow()
        );
    }

    @Test
    void semanticProgressAllowsLifetimePastOldAbsoluteWindow() {
        StoreHomeOperationTimeoutState state = new StoreHomeOperationTimeoutState();

        for (int tick = 0; tick < 7; tick++) {
            state.tick();
            state.recordSemanticProgress();
            assertTrue(state.timeoutReason(POLICY).isEmpty());
        }
        assertEquals(7, state.activeTicks());
    }

    @Test
    void standardPolicyAllowsProgressBeyondFormerAbsolute12000TickLifetime() {
        StoreHomeTimeoutPolicy standard = StoreHomeTimeoutPolicy.standard();
        StoreHomeOperationTimeoutState state = new StoreHomeOperationTimeoutState();

        for (int tick = 0; tick <= 12000; tick++) {
            state.tick();
            state.recordSemanticProgress();
            assertTrue(state.timeoutReason(standard).isEmpty());
        }

        assertEquals(12001, state.activeTicks());
    }

    @Test
    void emergencyHardCapNeverResetsAndWinsAtBoundary() {
        StoreHomeOperationTimeoutState state = new StoreHomeOperationTimeoutState();

        for (int tick = 0; tick < 8; tick++) {
            state.tick();
            state.recordSemanticProgress();
        }

        assertEquals(
                StoreHomeTimeoutReason.OPERATION_EMERGENCY_HARD_CAP,
                state.timeoutReason(POLICY).orElseThrow()
        );
    }

    @Test
    void candidateSwitchAndSafetyPauseDoNotResetOrConsumeOperationClock() {
        StoreHomeOperationTimeoutState state = new StoreHomeOperationTimeoutState();
        tick(state, 2);
        int activeBeforePause = state.activeTicks();
        int noProgressBeforePause = state.noProgressTicks();

        // Candidate replacement and safety preemption invoke neither operation reset nor tick.

        assertEquals(activeBeforePause, state.activeTicks());
        assertEquals(noProgressBeforePause, state.noProgressTicks());
        state.tick();
        assertEquals(activeBeforePause + 1, state.activeTicks());
        assertEquals(noProgressBeforePause + 1, state.noProgressTicks());
    }

    private static void tick(StoreHomeOperationTimeoutState state, int count) {
        for (int tick = 0; tick < count; tick++) {
            state.tick();
        }
    }
}
