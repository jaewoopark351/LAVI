package lavi.minecraft.task.container.home.execution.timeout.candidate;

import lavi.minecraft.task.container.home.execution.timeout.StoreHomeTimeoutPolicy;
import lavi.minecraft.task.container.home.execution.timeout.StoreHomeTimeoutReason;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260828_kpopmodder: Verify candidate-local reset scope and sticky phase timeout behavior.
class StoreHomeCandidateTimeoutStateTest {
    private static final StoreHomeTimeoutPolicy POLICY =
            new StoreHomeTimeoutPolicy(3, 3, 10, 20, 0.5, 1.0);

    @Test
    void navigationExpiresOnlyAfterConsecutiveNoProgressTicks() {
        StoreHomeCandidateTimeoutState state = state();

        tick(state, 0.0, false);
        tick(state, 0.0, false);
        assertTrue(state.timeoutReason(POLICY).isEmpty());

        tick(state, 0.0, false);
        assertEquals(
                StoreHomeTimeoutReason.CANDIDATE_NAVIGATION_NO_PROGRESS,
                state.timeoutReason(POLICY).orElseThrow()
        );
    }

    @Test
    void longCandidateLifetimeSurvivesWithPeriodicRealMovement() {
        StoreHomeCandidateTimeoutState state = state();

        for (int tick = 0; tick < 12; tick++) {
            tick(state, tick * 0.51, false);
            assertTrue(state.timeoutReason(POLICY).isEmpty());
        }

        assertEquals(12, state.activeTicks());
    }

    @Test
    void standardPolicyAllowsProgressBeyondFormerAbsolute2400TickLifetime() {
        StoreHomeTimeoutPolicy standard = StoreHomeTimeoutPolicy.standard();
        StoreHomeCandidateTimeoutState state =
                new StoreHomeCandidateTimeoutState(100000.0, 0.0, 0.0);

        for (int tick = 0; tick <= 2400; tick++) {
            state.tick(
                    true,
                    tick * (standard.movementJitterBlocks() + 0.01),
                    0.0,
                    0.0,
                    false,
                    standard
            );
            assertTrue(state.timeoutReason(standard).isEmpty());
        }

        assertEquals(2401, state.activeTicks());
    }

    @Test
    void handoffWinsNavigationBoundaryAndStartsLocalClockOnce() {
        StoreHomeCandidateTimeoutState state = state();

        tick(state, 0.0, false);
        tick(state, 0.0, false);
        StoreHomeCandidateTimeoutTickObservation handoff =
                tick(state, 0.0, true);

        assertTrue(handoff.enteredLocalInteraction());
        assertEquals(StoreHomeCandidateTimeoutPhase.OPEN_AND_BIND_CANDIDATE,
                state.phase());
        assertEquals(1, state.localInteractionTicks());
        assertFalse(state.timeoutReason(POLICY).isPresent());

        StoreHomeCandidateTimeoutTickObservation flicker =
                tick(state, 0.0, false);
        assertFalse(flicker.enteredLocalInteraction());
        assertEquals(2, state.localInteractionTicks());
    }

    @Test
    void localTimeoutIsStickyAndActivationDisablesCandidateTimeout() {
        StoreHomeCandidateTimeoutState state = state();

        tick(state, 0.0, true);
        tick(state, 0.0, false);
        tick(state, 0.0, true);
        assertEquals(
                StoreHomeTimeoutReason.CANDIDATE_LOCAL_INTERACTION_TIMEOUT,
                state.timeoutReason(POLICY).orElseThrow()
        );

        state.markActivated();
        assertTrue(state.timeoutReason(POLICY).isEmpty());
        tick(state, 0.0, false);
        assertEquals(3, state.localInteractionTicks());
    }

    @Test
    void activationOnLocalBoundaryWinsBeforeTimeoutDecision() {
        StoreHomeCandidateTimeoutState state = state();
        tick(state, 0.0, true);
        tick(state, 0.0, false);
        tick(state, 0.0, false);

        state.markActivated();

        assertTrue(state.timeoutReason(POLICY).isEmpty());
    }

    @Test
    void newCandidateResetsOnlyCandidateLocalState() {
        StoreHomeCandidateTimeoutState first = state();
        tick(first, 0.0, true);
        tick(first, 0.0, false);

        StoreHomeCandidateTimeoutState second = state();

        assertEquals(2, first.activeTicks());
        assertEquals(0, second.activeTicks());
        assertEquals(StoreHomeCandidateTimeoutPhase.NAVIGATE_TO_CANDIDATE,
                second.phase());
        assertFalse(second.localInteractionStarted());
    }

    @Test
    void safetyPauseDoesNotConsumeAnyClock() {
        StoreHomeCandidateTimeoutState state = state();
        tick(state, 0.0, false);
        int activeBeforePause = state.activeTicks();
        int navigationBeforePause = state.navigationNoProgressTicks();

        // A safety-chain pause invokes no StoreHomeTask.onTick and therefore no tick here.

        assertEquals(activeBeforePause, state.activeTicks());
        assertEquals(navigationBeforePause, state.navigationNoProgressTicks());
        tick(state, 0.0, false);
        assertEquals(activeBeforePause + 1, state.activeTicks());
    }

    private static StoreHomeCandidateTimeoutState state() {
        return new StoreHomeCandidateTimeoutState(100.0, 0.0, 0.0);
    }

    private static StoreHomeCandidateTimeoutTickObservation tick(
            StoreHomeCandidateTimeoutState state,
            double playerX,
            boolean handoff) {
        return state.tick(true, playerX, 0.0, 0.0, handoff, POLICY);
    }
}
