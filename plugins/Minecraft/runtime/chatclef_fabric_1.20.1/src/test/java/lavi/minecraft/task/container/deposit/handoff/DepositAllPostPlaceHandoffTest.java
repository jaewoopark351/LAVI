package lavi.minecraft.task.container.deposit.handoff;

import adris.altoclef.tasks.construction.PlaceBlockNearbyTask;
import lavi.minecraft.testsupport.TestObjects;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DepositAllPostPlaceHandoffTest {
    @Test
    void disabledHandoffNeverDefers() {
        DepositAllPostPlaceHandoff handoff = DepositAllPostPlaceHandoff.disabled();

        assertFalse(handoff.enabled());
        assertFalse(handoff.shouldDefer(placementTask(), true, true));
        assertFalse(handoff.shouldDefer(placementTask(), true, true));
    }

    @Test
    void enabledHandoffRejectsMissingInactiveAndUnfinishedPlacements() {
        DepositAllPostPlaceHandoff handoff = DepositAllPostPlaceHandoff.singleTick();
        PlaceBlockNearbyTask placement = placementTask();

        assertTrue(handoff.enabled());
        assertFalse(handoff.shouldDefer(null, true, true));
        assertFalse(handoff.shouldDefer(placement, false, true));
        assertFalse(handoff.shouldDefer(placement, true, false));
        assertTrue(handoff.shouldDefer(placement, true, true));
    }

    @Test
    void activeFinishedPlacementDefersExactlyOnce() {
        DepositAllPostPlaceHandoff handoff = DepositAllPostPlaceHandoff.singleTick();
        PlaceBlockNearbyTask placement = placementTask();

        assertTrue(handoff.shouldDefer(placement, true, true));
        assertFalse(handoff.shouldDefer(placement, true, true));
        assertFalse(handoff.shouldDefer(placement, true, true));
    }

    @Test
    void aDifferentCompletedPlacementCanDeferIndependently() {
        DepositAllPostPlaceHandoff handoff = DepositAllPostPlaceHandoff.singleTick();
        PlaceBlockNearbyTask first = placementTask();
        PlaceBlockNearbyTask second = placementTask();

        assertTrue(handoff.shouldDefer(first, true, true));
        assertFalse(handoff.shouldDefer(first, true, true));
        assertTrue(handoff.shouldDefer(second, true, true));
        assertFalse(handoff.shouldDefer(second, true, true));
    }

    private static PlaceBlockNearbyTask placementTask() {
        return TestObjects.allocate(PlaceBlockNearbyTask.class);
    }
}
