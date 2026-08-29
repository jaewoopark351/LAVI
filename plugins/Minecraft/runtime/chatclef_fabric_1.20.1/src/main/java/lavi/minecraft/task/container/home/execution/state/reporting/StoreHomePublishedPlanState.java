package lavi.minecraft.task.container.home.execution.state.reporting;

import lavi.minecraft.task.container.home.planning.HomeStoragePlan;

import java.util.Objects;

//20260829_kpopmodder: Added this type file to own only the latest public reporting plan.
public final class StoreHomePublishedPlanState {
    private HomeStoragePlan current;

    public HomeStoragePlan current() {
        return current;
    }

    public void publish(HomeStoragePlan plan) {
        current = Objects.requireNonNull(plan, "plan");
    }

    public void clear() {
        current = null;
    }
}
