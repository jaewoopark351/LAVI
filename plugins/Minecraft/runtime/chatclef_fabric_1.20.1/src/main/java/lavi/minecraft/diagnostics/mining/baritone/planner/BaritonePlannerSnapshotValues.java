package lavi.minecraft.diagnostics.mining.baritone.planner;

import baritone.process.CustomGoalProcess;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;

import java.lang.reflect.Field;

//20260830_kpopmodder: Normalize reflective and textual planner values without making planner decisions.
final class BaritonePlannerSnapshotValues {
    static final String FINISH_OBSERVATION_UNAVAILABLE = "NOT_CAPTURED_WITHOUT_BEHAVIOR_REEVALUATION";
    private static final Field CUSTOM_GOAL_STATE_FIELD = customGoalField("state");
    private static final Field CUSTOM_GOAL_MOST_RECENT_GOAL_FIELD = customGoalField("mostRecentGoal");

    private BaritonePlannerSnapshotValues() {
    }

    static String className(Object value) {
        return value == null ? "none" : ChatClefDiagnostics.className(value);
    }

    static String summarizeGoal(Object goal) {
        return goal == null ? "none" : String.valueOf(goal);
    }

    static String summarizeObject(Object value) {
        if (value == null) {
            return "none";
        }
        return ChatClefDiagnostics.className(value)
                + "#"
                + Integer.toHexString(System.identityHashCode(value))
                + ":"
                + value;
    }

    static String customGoalState(CustomGoalProcess process) {
        if (process == null) {
            return "none";
        }
        if (CUSTOM_GOAL_STATE_FIELD == null) {
            return "unavailable_field_missing";
        }
        try {
            Object value = CUSTOM_GOAL_STATE_FIELD.get(process);
            return value == null ? "null" : String.valueOf(value);
        } catch (IllegalAccessException | RuntimeException | LinkageError error) {
            return "error=" + error.getClass().getSimpleName();
        }
    }

    static String mostRecentCustomGoalValue(CustomGoalProcess process, boolean summary) {
        if (process == null) {
            return "none";
        }
        if (CUSTOM_GOAL_MOST_RECENT_GOAL_FIELD == null) {
            return "unavailable_field_missing";
        }
        try {
            Object value = CUSTOM_GOAL_MOST_RECENT_GOAL_FIELD.get(process);
            return summary ? summarizeGoal(value) : className(value);
        } catch (IllegalAccessException | RuntimeException | LinkageError error) {
            return "error=" + error.getClass().getSimpleName();
        }
    }

    static String finishObservation(String inProgressPresent) {
        if ("true".equals(inProgressPresent)) {
            return FINISH_OBSERVATION_UNAVAILABLE;
        }
        if ("false".equals(inProgressPresent)) {
            return "empty";
        }
        return inProgressPresent;
    }

    private static Field customGoalField(String name) {
        try {
            Field field = CustomGoalProcess.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException | RuntimeException | LinkageError error) {
            return null;
        }
    }

}
