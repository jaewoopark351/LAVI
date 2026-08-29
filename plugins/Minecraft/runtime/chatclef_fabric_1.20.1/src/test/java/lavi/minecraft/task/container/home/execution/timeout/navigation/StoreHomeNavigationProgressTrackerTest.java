package lavi.minecraft.task.container.home.execution.timeout.navigation;

import lavi.minecraft.task.container.home.execution.timeout.StoreHomeTimeoutPolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260828_kpopmodder: Verify bounded movement and best-distance progress without path-state signals.
class StoreHomeNavigationProgressTrackerTest {
    @Test
    void cumulativeMovementUsesLastAcceptedAnchor() {
        StoreHomeNavigationProgressTracker tracker = trackerAt(100.0);
        StoreHomeTimeoutPolicy policy = policy(0.5, 1.0);

        assertFalse(observe(tracker, 0.0, policy).semanticProgress());
        assertFalse(observe(tracker, 0.2, policy).semanticProgress());
        assertFalse(observe(tracker, 0.4, policy).semanticProgress());
        assertTrue(observe(tracker, 0.6, policy).playerMovementProgress());
    }

    @Test
    void subThresholdJitterDoesNotBecomeProgress() {
        StoreHomeNavigationProgressTracker tracker = trackerAt(100.0);
        StoreHomeTimeoutPolicy policy = policy(0.5, 1.0);

        observe(tracker, 0.0, policy);
        assertFalse(observe(tracker, 0.49, policy).semanticProgress());
        assertFalse(observe(tracker, 0.5, policy).semanticProgress());
        assertTrue(observe(tracker, 0.51, policy).semanticProgress());
    }

    @Test
    void cumulativeBestDistanceImprovementUsesEpsilonBoundary() {
        StoreHomeNavigationProgressTracker tracker = trackerAt(10.0);
        StoreHomeTimeoutPolicy policy = policy(10.0, 1.0);

        observe(tracker, 0.0, policy);
        assertFalse(observe(tracker, 0.4, policy).semanticProgress());
        assertFalse(observe(tracker, 0.9, policy).semanticProgress());
        assertFalse(observe(tracker, 1.0, policy).bestDistanceProgress());
        assertTrue(observe(tracker, 1.01, policy).bestDistanceProgress());
    }

    @Test
    void realDetourMovementCountsWhileTargetDistanceIncreases() {
        StoreHomeNavigationProgressTracker tracker = trackerAt(10.0);
        StoreHomeTimeoutPolicy policy = policy(0.5, 1.0);

        observe(tracker, 0.0, policy);
        StoreHomeNavigationProgressObservation detour =
                observe(tracker, -0.51, policy);

        assertTrue(detour.playerMovementProgress());
        assertFalse(detour.bestDistanceProgress());
    }

    @Test
    void unavailablePositionNeverFabricatesProgress() {
        StoreHomeNavigationProgressTracker tracker = trackerAt(10.0);

        assertFalse(tracker.observe(
                false, 0.0, 0.0, 0.0, policy(0.5, 1.0)
        ).semanticProgress());
    }

    private static StoreHomeNavigationProgressTracker trackerAt(double targetX) {
        return new StoreHomeNavigationProgressTracker(targetX, 0.0, 0.0);
    }

    private static StoreHomeNavigationProgressObservation observe(
            StoreHomeNavigationProgressTracker tracker,
            double playerX,
            StoreHomeTimeoutPolicy policy) {
        return tracker.observe(true, playerX, 0.0, 0.0, policy);
    }

    private static StoreHomeTimeoutPolicy policy(
            double movementThreshold,
            double distanceEpsilon) {
        return new StoreHomeTimeoutPolicy(
                3, 3, 10, 20, movementThreshold, distanceEpsilon
        );
    }
}
