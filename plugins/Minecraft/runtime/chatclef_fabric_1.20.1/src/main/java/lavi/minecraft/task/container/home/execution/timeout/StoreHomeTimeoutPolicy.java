package lavi.minecraft.task.container.home.execution.timeout;

//20260828_kpopmodder: Define the bounded STORE_HOME phase-clock thresholds in one LAVI-owned policy.
public record StoreHomeTimeoutPolicy(
        int candidateNavigationNoProgressTicks,
        int candidateLocalInteractionTicks,
        int operationNoProgressTicks,
        int operationEmergencyHardCapTicks,
        double movementJitterBlocks,
        double bestDistanceImprovementEpsilonBlocks) {

    private static final StoreHomeTimeoutPolicy STANDARD =
            new StoreHomeTimeoutPolicy(
                    2400,
                    2400,
                    12000,
                    120000,
                    0.5,
                    1.0
            );

    public StoreHomeTimeoutPolicy {
        requirePositive(candidateNavigationNoProgressTicks,
                "candidateNavigationNoProgressTicks");
        requirePositive(candidateLocalInteractionTicks,
                "candidateLocalInteractionTicks");
        requirePositive(operationNoProgressTicks,
                "operationNoProgressTicks");
        requirePositive(operationEmergencyHardCapTicks,
                "operationEmergencyHardCapTicks");
        if (operationEmergencyHardCapTicks <= operationNoProgressTicks) {
            throw new IllegalArgumentException(
                    "operationEmergencyHardCapTicks must exceed operationNoProgressTicks"
            );
        }
        requirePositiveFinite(movementJitterBlocks, "movementJitterBlocks");
        requirePositiveFinite(
                bestDistanceImprovementEpsilonBlocks,
                "bestDistanceImprovementEpsilonBlocks"
        );
    }

    public static StoreHomeTimeoutPolicy standard() {
        return STANDARD;
    }

    public double movementJitterSquared() {
        return movementJitterBlocks * movementJitterBlocks;
    }

    private static void requirePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }

    private static void requirePositiveFinite(double value, String name) {
        if (!Double.isFinite(value) || value <= 0.0) {
            throw new IllegalArgumentException(name + " must be positive and finite");
        }
    }
}
