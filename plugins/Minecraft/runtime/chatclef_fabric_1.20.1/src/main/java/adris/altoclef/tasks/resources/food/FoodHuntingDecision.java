package adris.altoclef.tasks.resources.food;

import adris.altoclef.AltoClef;
import adris.altoclef.chains.food.FoodHuntTracker;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.logging.StateChangeLogger;

//20260729_kpopmodder: Added this decision object to keep hunt continuation checks out of CollectFoodTask ordering.
final class FoodHuntingDecision {
    private final boolean active;
    private final String skipReason;

    private FoodHuntingDecision(boolean active, String skipReason) {
        this.active = active;
        this.skipReason = skipReason;
    }

    static FoodHuntingDecision evaluate(
            AltoClef mod,
            FoodHuntTracker huntTracker,
            Task currentResourceTask,
            StateChangeLogger debugLogger
    ) {
        if (!huntTracker.hasTarget()
                || currentResourceTask == null
                || !currentResourceTask.isActive()
                || currentResourceTask.isFinished()) {
            return new FoodHuntingDecision(false, null);
        }
        return new FoodHuntingDecision(true, huntTracker.getSkipReason(mod, debugLogger));
    }

    boolean isActive() {
        return active;
    }

    boolean shouldSkip() {
        return active && skipReason != null;
    }

    String getSkipReason() {
        return skipReason;
    }
}
