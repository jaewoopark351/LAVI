package lavi.minecraft.integration.carryon.container;

//20260826_kpopmodder: Classify observed postconditions separately from the bounded observation window.
final class CarryOnContainerPostconditionClassifier {
    CarryOnContainerPostconditionOutcome classify(boolean snapshotAvailable,
                                                   int observationOffsetTicks,
                                                   boolean expectedGuiOpened,
                                                   boolean observedPickup,
                                                   CarryOnContainerPickupEvidence pickupEvidence,
                                                   boolean targetRemoved,
                                                   boolean targetStillMatches,
                                                   boolean terminalOffset) {
        if (!snapshotAvailable) {
            return terminalOffset
                    ? CarryOnContainerPostconditionOutcome.OBSERVATION_WINDOW_EXPIRED
                    : CarryOnContainerPostconditionOutcome.OBSERVATION_PENDING;
        }
        if (expectedGuiOpened) {
            return observationOffsetTicks == 0
                    ? CarryOnContainerPostconditionOutcome.GUI_OPENED
                    : CarryOnContainerPostconditionOutcome.GUI_OPEN_DELAYED;
        }
        if (observedPickup && pickupEvidence == CarryOnContainerPickupEvidence.CONFIRMED_TARGET_IDENTITY) {
            return CarryOnContainerPostconditionOutcome.CARRY_ON_PICKUP_CONFIRMED;
        }
        if (observedPickup && pickupEvidence == CarryOnContainerPickupEvidence.STRONG_TEMPORAL_ATTRIBUTION) {
            return CarryOnContainerPostconditionOutcome.CARRY_ON_PICKUP_STRONGLY_ATTRIBUTED;
        }
        if (terminalOffset && targetRemoved) {
            return CarryOnContainerPostconditionOutcome.TARGET_REMOVED_WITHOUT_GUI;
        }
        if (terminalOffset && targetStillMatches) {
            return CarryOnContainerPostconditionOutcome.NO_GUI_TARGET_STILL_PRESENT;
        }
        return terminalOffset
                ? CarryOnContainerPostconditionOutcome.OBSERVATION_WINDOW_EXPIRED
                : CarryOnContainerPostconditionOutcome.OBSERVATION_PENDING;
    }
}
