package lavi.minecraft.task.container.deposit.handoff;

import adris.altoclef.tasks.construction.PlaceBlockNearbyTask;

//20260831_kpopmodder: Admit one cleanup-only parent tick after an owned placement completes.
/**
 * Admits one cleanup-only parent tick for each completed placement child.
 */
public final class DepositAllPostPlaceHandoff {
    private final boolean enabled;
    private PlaceBlockNearbyTask lastDeferredTask;

    private DepositAllPostPlaceHandoff(boolean enabled) {
        this.enabled = enabled;
    }

    public static DepositAllPostPlaceHandoff disabled() {
        return new DepositAllPostPlaceHandoff(false);
    }

    public static DepositAllPostPlaceHandoff singleTick() {
        return new DepositAllPostPlaceHandoff(true);
    }

    public boolean shouldDefer(
            PlaceBlockNearbyTask placementTask,
            boolean placementActive,
            boolean placementFinished) {
        if (!enabled
                || placementTask == null
                || !placementActive
                || !placementFinished
                || lastDeferredTask == placementTask) {
            return false;
        }
        lastDeferredTask = placementTask;
        return true;
    }

    public boolean enabled() {
        return enabled;
    }
}
