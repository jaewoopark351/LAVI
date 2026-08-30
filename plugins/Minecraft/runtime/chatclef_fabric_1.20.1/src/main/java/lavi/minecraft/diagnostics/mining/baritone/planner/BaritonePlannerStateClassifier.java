package lavi.minecraft.diagnostics.mining.baritone.planner;

//20260830_kpopmodder: Classify only already captured planner values.
final class BaritonePlannerStateClassifier {
    private BaritonePlannerStateClassifier() {
    }

    static String classify(String customGoalProcessState,
                           String inProgressPresent,
                           String inProgressFinished,
                           String nextPathPresent,
                           String calcFailedLastTick,
                           String mostRecentCommandType,
                           String mostRecentProcessClass) {
        if (isObservationError(customGoalProcessState, inProgressPresent, inProgressFinished, nextPathPresent,
                calcFailedLastTick, mostRecentCommandType, mostRecentProcessClass)) {
            return "PLANNER_OBSERVATION_FAILED";
        }
        if (isTrue(calcFailedLastTick)) {
            return "PATH_CALCULATION_FAILED_LAST_TICK";
        }
        if (isTrue(inProgressPresent)) {
            return "PATH_CALCULATION_IN_PROGRESS";
        }
        if (isTrue(nextPathPresent)) {
            return "NEXT_PATH_READY";
        }
        if ("DEFER".equals(mostRecentCommandType)) {
            return "PATH_COMMAND_DEFERRED";
        }
        if (!"empty".equals(mostRecentProcessClass)
                && !"baritone.process.CustomGoalProcess".equals(mostRecentProcessClass)) {
            return "NON_CUSTOM_PROCESS_LAST_IN_CONTROL";
        }
        if ("PATH_REQUESTED".equals(customGoalProcessState)) {
            return "CUSTOM_GOAL_PATH_REQUESTED_NO_VISIBLE_CALCULATION";
        }
        if ("EXECUTING".equals(customGoalProcessState)) {
            return "CUSTOM_GOAL_EXECUTING_WITHOUT_VISIBLE_PATH";
        }
        if ("GOAL_SET".equals(customGoalProcessState)) {
            return "CUSTOM_GOAL_SET_NO_PATH_REQUESTED";
        }
        if ("NONE".equals(customGoalProcessState)) {
            return "CUSTOM_GOAL_PROCESS_NONE";
        }
        return "NO_PRIVATE_PLANNER_SIGNAL";
    }

    private static boolean isTrue(String value) {
        return "true".equals(value);
    }

    private static boolean isObservationError(String... values) {
        for (String value : values) {
            if (value != null && (value.startsWith("error=") || value.startsWith("exception="))) {
                return true;
            }
        }
        return false;
    }
}
