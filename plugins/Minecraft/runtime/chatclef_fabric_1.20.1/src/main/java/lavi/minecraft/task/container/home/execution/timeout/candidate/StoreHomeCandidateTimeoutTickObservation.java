package lavi.minecraft.task.container.home.execution.timeout.candidate;

//20260828_kpopmodder: Added this type file to keep one primary Java type per file.
public record StoreHomeCandidateTimeoutTickObservation(
        boolean semanticNavigationProgress,
        boolean playerMovementProgress,
        boolean bestDistanceProgress,
        boolean enteredLocalInteraction) {
}
