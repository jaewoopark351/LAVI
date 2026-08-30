package lavi.minecraft.diagnostics.mining.baritone.executor;

//20260830_kpopmodder: Compute only pure deltas between already captured cheap observations.
final class BaritoneExecutorSnapshotMetrics {
    private BaritoneExecutorSnapshotMetrics() {
    }

    static double playerDisplacement(BaritoneExecutorProgressObservation previous,
                                     BaritoneExecutorProgressObservation current) {
        if (previous == null || current == null
                || !finitePlayer(previous) || !finitePlayer(current)) {
            return Double.NaN;
        }
        double dx = current.playerX() - previous.playerX();
        double dy = current.playerY() - previous.playerY();
        double dz = current.playerZ() - previous.playerZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    static String executorPositionDelta(BaritoneExecutorProgressObservation previous,
                                        BaritoneExecutorProgressObservation current) {
        if (previous == null || current == null
                || previous.currentExecutorPosition() == BaritoneExecutorSnapshotValues.UNAVAILABLE_INT
                || current.currentExecutorPosition() == BaritoneExecutorSnapshotValues.UNAVAILABLE_INT) {
            return "unavailable";
        }
        return Integer.toString(current.currentExecutorPosition() - previous.currentExecutorPosition());
    }

    static String targetDistanceDelta(BaritoneExecutorProgressObservation previous,
                                      BaritoneExecutorProgressObservation current) {
        if (previous == null || current == null
                || !previous.targetDistanceAvailable() || !current.targetDistanceAvailable()) {
            return "unavailable";
        }
        return BaritoneExecutorSnapshotValues.formatDouble(
                current.targetDistanceSq() - previous.targetDistanceSq());
    }

    private static boolean finitePlayer(BaritoneExecutorProgressObservation observation) {
        return Double.isFinite(observation.playerX())
                && Double.isFinite(observation.playerY())
                && Double.isFinite(observation.playerZ());
    }
}
