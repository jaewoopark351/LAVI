package adris.altoclef.tasks.container.smelt;

//20260729_kpopmodder: Added this value object so smelting logs can report the selected branch and its reason.
public final class SmeltPlan {
    public enum Action {
        GET_MATERIALS,
        CONTINUE_FUEL_MOVE,
        WAIT_PENDING_FUEL_INSERT,
        GET_FUEL,
        MOVE_INACCESSIBLE_MATERIALS,
        READY_FOR_CONTAINER,
        REMOVE_EXTRA_FUEL,
        RECEIVE_OUTPUT,
        MOVE_MATERIALS,
        FILL_FUEL,
        WAIT_FOR_SMELTING
    }

    private final Action action;
    private final String reason;
    private final String details;
    private final SmeltContainerSnapshot snapshot;
    private final double pendingFuelAmount;
    private final int pendingFuelTicks;
    private final int pendingFuelMoveTicks;

    private SmeltPlan(
            Action action,
            String reason,
            String details,
            SmeltContainerSnapshot snapshot,
            double pendingFuelAmount,
            int pendingFuelTicks,
            int pendingFuelMoveTicks
    ) {
        this.action = action;
        this.reason = reason;
        this.details = details;
        this.snapshot = snapshot;
        this.pendingFuelAmount = pendingFuelAmount;
        this.pendingFuelTicks = pendingFuelTicks;
        this.pendingFuelMoveTicks = pendingFuelMoveTicks;
    }

    public static SmeltPlan of(
            Action action,
            String reason,
            String details,
            SmeltContainerSnapshot snapshot,
            double pendingFuelAmount,
            int pendingFuelTicks,
            int pendingFuelMoveTicks
    ) {
        return new SmeltPlan(action, reason, details, snapshot, pendingFuelAmount, pendingFuelTicks, pendingFuelMoveTicks);
    }

    public String describe() {
        return "plan[action=" + action
                + ", reason=" + reason
                + (details == null || details.isEmpty() ? "" : ", " + details)
                + ", " + snapshot.describeWithPending(pendingFuelAmount, pendingFuelTicks, pendingFuelMoveTicks)
                + "]";
    }
}
