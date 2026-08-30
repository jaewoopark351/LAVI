package lavi.minecraft.diagnostics.mining.baritone.planner;

//20260830_kpopmodder: Own only the stable ordering and event keys of planner snapshot values.
final class BaritonePlannerSnapshotSchema {
    static final String[] EVENT_KEYS = {
            "plannerPathingGoalType",
            "plannerPathingGoalSummary",
            "plannerCustomGoalProcessState",
            "plannerCustomGoalType",
            "plannerCustomGoalSummary",
            "plannerMostRecentCustomGoalType",
            "plannerMostRecentCustomGoalSummary",
            "plannerInProgressPresent",
            "plannerInProgressSummary",
            "plannerInProgressGoalType",
            "plannerInProgressGoalSummary",
            "plannerInProgressFinished",
            "plannerNextPathPresent",
            "plannerNextPathSummary",
            "plannerEstimatedTicksToGoal",
            "plannerPathStart",
            "plannerCalcFailedLastTick",
            "plannerMostRecentProcessClass",
            "plannerMostRecentProcessDisplayName",
            "plannerMostRecentProcessActive",
            "plannerMostRecentCommandType",
            "plannerMostRecentCommandGoalType",
            "plannerMostRecentCommandGoalSummary",
            "plannerDerivedState"
    };

    private BaritonePlannerSnapshotSchema() {
    }
}
