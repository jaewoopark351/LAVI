package lavi.minecraft.task.container.home.execution.timeout.navigation;

//20260828_kpopmodder: Added this type file to keep one primary Java type per file.
public record StoreHomeNavigationProgressObservation(
        boolean semanticProgress,
        boolean playerMovementProgress,
        boolean bestDistanceProgress,
        double currentDistanceBlocks) {

    public static StoreHomeNavigationProgressObservation unavailable() {
        return new StoreHomeNavigationProgressObservation(
                false, false, false, Double.NaN
        );
    }
}
