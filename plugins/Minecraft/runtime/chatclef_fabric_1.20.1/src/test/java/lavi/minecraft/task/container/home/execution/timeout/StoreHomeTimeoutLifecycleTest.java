package lavi.minecraft.task.container.home.execution.timeout;

import lavi.minecraft.task.container.home.execution.timeout.candidate.StoreHomeCandidateTimeoutPhase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260828_kpopmodder: Verify candidate replacement does not reset operation-owned clocks.
class StoreHomeTimeoutLifecycleTest {
    private static final StoreHomeTimeoutPolicy POLICY =
            new StoreHomeTimeoutPolicy(3, 3, 6, 12, 0.5, 1.0);

    @Test
    void candidateSwitchResetsCandidateStateButPreservesOperationState() {
        StoreHomeTimeoutLifecycle lifecycle = new StoreHomeTimeoutLifecycle(POLICY);
        lifecycle.startCandidate(100.0, 0.0, 0.0);
        activeTick(lifecycle, 0.0, false);
        activeTick(lifecycle, 0.0, false);
        int operationTicksBeforeSwitch =
                lifecycle.observation().operationActiveTicks();
        int operationNoProgressBeforeSwitch =
                lifecycle.observation().operationNoProgressTicks();

        lifecycle.clearCandidate();
        lifecycle.startCandidate(200.0, 0.0, 0.0);

        StoreHomeTimeoutObservation afterSwitch = lifecycle.observation();
        assertEquals(operationTicksBeforeSwitch, afterSwitch.operationActiveTicks());
        assertEquals(operationNoProgressBeforeSwitch,
                afterSwitch.operationNoProgressTicks());
        assertEquals(0, afterSwitch.candidateActiveTicks());
        assertEquals("NAVIGATE_TO_CANDIDATE",
                afterSwitch.candidateTimeoutPhase());
    }

    @Test
    void exactActivationAndConfirmedTransferResetOnlyNoProgress() {
        StoreHomeTimeoutLifecycle lifecycle = new StoreHomeTimeoutLifecycle(POLICY);
        lifecycle.startCandidate(100.0, 0.0, 0.0);
        activeTick(lifecycle, 0.0, true);
        lifecycle.onActiveRootTick();

        lifecycle.markCandidateActivated();
        StoreHomeTimeoutObservation activated = lifecycle.observation();
        assertEquals(StoreHomeCandidateTimeoutPhase.ACTIVATED.name(),
                activated.candidateTimeoutPhase());
        assertEquals(0, activated.operationNoProgressTicks());
        int emergencyElapsed = activated.operationActiveTicks();

        lifecycle.onActiveRootTick();
        lifecycle.recordConfirmedTransfer();

        assertEquals(0, lifecycle.observation().operationNoProgressTicks());
        assertEquals(emergencyElapsed + 1,
                lifecycle.observation().operationActiveTicks());
    }

    @Test
    void safetyPauseLeavesComposedSnapshotUnchanged() {
        StoreHomeTimeoutLifecycle lifecycle = new StoreHomeTimeoutLifecycle(POLICY);
        lifecycle.startCandidate(100.0, 0.0, 0.0);
        activeTick(lifecycle, 0.0, false);
        StoreHomeTimeoutObservation beforePause = lifecycle.observation();

        // No active-root callback occurs during the safety-chain pause.

        assertEquals(beforePause, lifecycle.observation());
        assertTrue(lifecycle.operationTimeoutReason().isEmpty());
    }

    @Test
    void exactActivationOnNoProgressBoundaryResetsBeforeDecision() {
        StoreHomeTimeoutPolicy boundaryPolicy =
                new StoreHomeTimeoutPolicy(20, 20, 6, 12, 0.5, 1.0);
        StoreHomeTimeoutLifecycle lifecycle =
                new StoreHomeTimeoutLifecycle(boundaryPolicy);
        lifecycle.startCandidate(100.0, 0.0, 0.0);
        for (int tick = 0; tick < 5; tick++) {
            lifecycle.onActiveRootTick();
            lifecycle.observeCandidate(true, 0.0, 0.0, 0.0, false);
        }

        lifecycle.onActiveRootTick();
        lifecycle.observeCandidate(true, 0.0, 0.0, 0.0, true);
        lifecycle.markCandidateActivated();

        assertTrue(lifecycle.operationNoProgressTimeoutReason().isEmpty());
        assertEquals(0, lifecycle.observation().operationNoProgressTicks());
    }

    @Test
    void diagnosticAliasTracksOnlyTheActiveCandidatePhaseClock() {
        StoreHomeTimeoutPolicy aliasPolicy =
                new StoreHomeTimeoutPolicy(3, 4, 6, 12, 0.5, 1.0);
        StoreHomeTimeoutLifecycle lifecycle =
                new StoreHomeTimeoutLifecycle(aliasPolicy);
        lifecycle.startCandidate(100.0, 0.0, 0.0);
        activeTick(lifecycle, 0.0, false);
        assertEquals(1, lifecycle.observation().activeCandidateTimeoutTicks());
        assertEquals(3, lifecycle.observation().activeCandidateLimitTicks());

        activeTick(lifecycle, 0.0, true);
        assertEquals(1, lifecycle.observation().activeCandidateTimeoutTicks());
        assertEquals(4, lifecycle.observation().activeCandidateLimitTicks());

        lifecycle.markCandidateActivated();
        assertEquals(0, lifecycle.observation().activeCandidateTimeoutTicks());
        assertEquals(0, lifecycle.observation().activeCandidateLimitTicks());
    }

    private static void activeTick(
            StoreHomeTimeoutLifecycle lifecycle,
            double playerX,
            boolean handoff) {
        lifecycle.onActiveRootTick();
        lifecycle.observeCandidate(
                true, playerX, 0.0, 0.0, handoff
        );
    }
}
