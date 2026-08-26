package lavi.minecraft.integration.carryon.container;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CarryOnContainerPostconditionClassifierTest {
    private final CarryOnContainerPostconditionClassifier classifier =
            new CarryOnContainerPostconditionClassifier();

    @Test
    void keepsOffsetZeroPendingWhenNoPostconditionWasObserved() {
        assertEquals(
                CarryOnContainerPostconditionOutcome.OBSERVATION_PENDING,
                classifier.classify(
                        true,
                        0,
                        false,
                        false,
                        CarryOnContainerPickupEvidence.INSUFFICIENT,
                        false,
                        true,
                        false
                )
        );
    }

    @Test
    void preservesImmediateGuiAsAnOffsetZeroTerminalOutcome() {
        assertEquals(
                CarryOnContainerPostconditionOutcome.GUI_OPENED,
                classifier.classify(
                        true,
                        0,
                        true,
                        false,
                        CarryOnContainerPickupEvidence.INSUFFICIENT,
                        false,
                        true,
                        false
                )
        );
    }

    @Test
    void classifiesDelayedGuiAndFinalNoGuiSeparately() {
        assertEquals(
                CarryOnContainerPostconditionOutcome.GUI_OPEN_DELAYED,
                classifier.classify(
                        true,
                        2,
                        true,
                        false,
                        CarryOnContainerPickupEvidence.INSUFFICIENT,
                        false,
                        true,
                        false
                )
        );
        assertEquals(
                CarryOnContainerPostconditionOutcome.NO_GUI_TARGET_STILL_PRESENT,
                classifier.classify(
                        true,
                        5,
                        false,
                        false,
                        CarryOnContainerPickupEvidence.INSUFFICIENT,
                        false,
                        true,
                        true
                )
        );
    }
}
