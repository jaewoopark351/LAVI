package lavi.minecraft.task.container.home.execution.timeout.navigation;

import lavi.minecraft.task.container.home.execution.timeout.StoreHomeTimeoutPolicy;

//20260828_kpopmodder: Recognize only bounded player movement or best-distance improvement as navigation progress.
public final class StoreHomeNavigationProgressTracker {
    private final double targetX;
    private final double targetY;
    private final double targetZ;

    private boolean initialized;
    private double acceptedPlayerX;
    private double acceptedPlayerY;
    private double acceptedPlayerZ;
    private double bestObservedDistance;
    private double bestDistanceAtAcceptedProgress;

    public StoreHomeNavigationProgressTracker(
            double targetX,
            double targetY,
            double targetZ) {
        requireFinite(targetX, "targetX");
        requireFinite(targetY, "targetY");
        requireFinite(targetZ, "targetZ");
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetZ = targetZ;
    }

    public StoreHomeNavigationProgressObservation observe(
            boolean playerPositionAvailable,
            double playerX,
            double playerY,
            double playerZ,
            StoreHomeTimeoutPolicy policy) {
        if (!playerPositionAvailable
                || !Double.isFinite(playerX)
                || !Double.isFinite(playerY)
                || !Double.isFinite(playerZ)) {
            return StoreHomeNavigationProgressObservation.unavailable();
        }

        double currentDistance = distance(
                playerX, playerY, playerZ, targetX, targetY, targetZ
        );
        if (!initialized) {
            initialized = true;
            acceptedPlayerX = playerX;
            acceptedPlayerY = playerY;
            acceptedPlayerZ = playerZ;
            bestObservedDistance = currentDistance;
            bestDistanceAtAcceptedProgress = currentDistance;
            return new StoreHomeNavigationProgressObservation(
                    false, false, false, currentDistance
            );
        }

        bestObservedDistance = Math.min(bestObservedDistance, currentDistance);
        boolean moved = distanceSquared(
                playerX,
                playerY,
                playerZ,
                acceptedPlayerX,
                acceptedPlayerY,
                acceptedPlayerZ
        ) > policy.movementJitterSquared();
        boolean bestDistanceImproved =
                bestDistanceAtAcceptedProgress - bestObservedDistance
                        > policy.bestDistanceImprovementEpsilonBlocks();
        boolean semanticProgress = moved || bestDistanceImproved;
        if (semanticProgress) {
            acceptedPlayerX = playerX;
            acceptedPlayerY = playerY;
            acceptedPlayerZ = playerZ;
            bestDistanceAtAcceptedProgress = bestObservedDistance;
        }
        return new StoreHomeNavigationProgressObservation(
                semanticProgress,
                moved,
                bestDistanceImproved,
                currentDistance
        );
    }

    private static double distance(
            double x1,
            double y1,
            double z1,
            double x2,
            double y2,
            double z2) {
        return Math.sqrt(distanceSquared(x1, y1, z1, x2, y2, z2));
    }

    private static double distanceSquared(
            double x1,
            double y1,
            double z1,
            double x2,
            double y2,
            double z2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        double dz = z1 - z2;
        return dx * dx + dy * dy + dz * dz;
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }

}
