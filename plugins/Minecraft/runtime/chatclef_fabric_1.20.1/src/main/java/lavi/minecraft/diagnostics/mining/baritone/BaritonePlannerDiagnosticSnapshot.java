package lavi.minecraft.diagnostics.mining.baritone;

import adris.altoclef.AltoClef;
import baritone.api.pathing.calc.IPathFinder;
import baritone.api.process.IBaritoneProcess;
import baritone.api.process.PathingCommand;
import baritone.behavior.PathingBehavior;
import baritone.process.CustomGoalProcess;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;

import java.lang.reflect.Field;
import java.util.Optional;

//20260806_kpopmodder: Keep read-only Baritone planner diagnostics separate from mining task behavior.
public final class BaritonePlannerDiagnosticSnapshot {
    private static final Field CUSTOM_GOAL_STATE_FIELD = customGoalStateField();
    private static final Field CUSTOM_GOAL_MOST_RECENT_GOAL_FIELD = customGoalField("mostRecentGoal");

    private final String pathingGoalType;
    private final String pathingGoalSummary;
    private final String customGoalProcessState;
    private final String customGoalType;
    private final String customGoalSummary;
    private final String mostRecentCustomGoalType;
    private final String mostRecentCustomGoalSummary;
    private final String inProgressPresent;
    private final String inProgressSummary;
    private final String inProgressGoalType;
    private final String inProgressGoalSummary;
    private final String inProgressFinished;
    private final String nextPathPresent;
    private final String nextPathSummary;
    private final String estimatedTicksToGoal;
    private final String pathStart;
    private final String calcFailedLastTick;
    private final String mostRecentProcessClass;
    private final String mostRecentProcessDisplayName;
    private final String mostRecentProcessActive;
    private final String mostRecentCommandType;
    private final String mostRecentCommandGoalType;
    private final String mostRecentCommandGoalSummary;
    private final String derivedPlannerState;

    private BaritonePlannerDiagnosticSnapshot(String pathingGoalType,
                                              String pathingGoalSummary,
                                              String customGoalProcessState,
                                              String customGoalType,
                                              String customGoalSummary,
                                              String mostRecentCustomGoalType,
                                              String mostRecentCustomGoalSummary,
                                              String inProgressPresent,
                                              String inProgressSummary,
                                              String inProgressGoalType,
                                              String inProgressGoalSummary,
                                              String inProgressFinished,
                                              String nextPathPresent,
                                              String nextPathSummary,
                                              String estimatedTicksToGoal,
                                              String pathStart,
                                              String calcFailedLastTick,
                                              String mostRecentProcessClass,
                                              String mostRecentProcessDisplayName,
                                              String mostRecentProcessActive,
                                              String mostRecentCommandType,
                                              String mostRecentCommandGoalType,
                                              String mostRecentCommandGoalSummary,
                                              String derivedPlannerState) {
        this.pathingGoalType = pathingGoalType;
        this.pathingGoalSummary = pathingGoalSummary;
        this.customGoalProcessState = customGoalProcessState;
        this.customGoalType = customGoalType;
        this.customGoalSummary = customGoalSummary;
        this.mostRecentCustomGoalType = mostRecentCustomGoalType;
        this.mostRecentCustomGoalSummary = mostRecentCustomGoalSummary;
        this.inProgressPresent = inProgressPresent;
        this.inProgressSummary = inProgressSummary;
        this.inProgressGoalType = inProgressGoalType;
        this.inProgressGoalSummary = inProgressGoalSummary;
        this.inProgressFinished = inProgressFinished;
        this.nextPathPresent = nextPathPresent;
        this.nextPathSummary = nextPathSummary;
        this.estimatedTicksToGoal = estimatedTicksToGoal;
        this.pathStart = pathStart;
        this.calcFailedLastTick = calcFailedLastTick;
        this.mostRecentProcessClass = mostRecentProcessClass;
        this.mostRecentProcessDisplayName = mostRecentProcessDisplayName;
        this.mostRecentProcessActive = mostRecentProcessActive;
        this.mostRecentCommandType = mostRecentCommandType;
        this.mostRecentCommandGoalType = mostRecentCommandGoalType;
        this.mostRecentCommandGoalSummary = mostRecentCommandGoalSummary;
        this.derivedPlannerState = derivedPlannerState;
    }

    public static BaritonePlannerDiagnosticSnapshot capture(AltoClef mod) {
        String pathingGoalType = ChatClefDiagnostics.safeValue(() -> className(pathingBehavior(mod).getGoal()));
        String pathingGoalSummary = ChatClefDiagnostics.safeValue(() -> summarizeGoal(pathingBehavior(mod).getGoal()));
        String customGoalProcessState = ChatClefDiagnostics.safeValue(() -> customGoalState(customGoalProcess(mod)));
        String customGoalType = ChatClefDiagnostics.safeValue(() -> className(customGoalProcess(mod).getGoal()));
        String customGoalSummary = ChatClefDiagnostics.safeValue(() -> summarizeGoal(customGoalProcess(mod).getGoal()));
        String mostRecentCustomGoalType = ChatClefDiagnostics.safeValue(() -> mostRecentCustomGoalFieldValue(customGoalProcess(mod), false));
        String mostRecentCustomGoalSummary = ChatClefDiagnostics.safeValue(() -> mostRecentCustomGoalFieldValue(customGoalProcess(mod), true));
        String inProgressPresent = ChatClefDiagnostics.safeValue(() -> inProgress(mod).isPresent());
        String inProgressSummary = ChatClefDiagnostics.safeValue(() -> inProgress(mod)
                .map(BaritonePlannerDiagnosticSnapshot::summarizeObject)
                .orElse("empty"));
        String inProgressGoalType = ChatClefDiagnostics.safeValue(() -> inProgress(mod)
                .map(finder -> className(finder.getGoal()))
                .orElse("empty"));
        String inProgressGoalSummary = ChatClefDiagnostics.safeValue(() -> inProgress(mod)
                .map(finder -> summarizeGoal(finder.getGoal()))
                .orElse("empty"));
        String inProgressFinished = ChatClefDiagnostics.safeValue(() -> inProgress(mod)
                .map(finder -> Boolean.toString(finder.isFinished()))
                .orElse("empty"));
        String nextPathPresent = ChatClefDiagnostics.safeValue(() -> pathingBehavior(mod).getNext() != null);
        String nextPathSummary = ChatClefDiagnostics.safeValue(() -> summarizeObject(pathingBehavior(mod).getNext()));
        String estimatedTicksToGoal = ChatClefDiagnostics.safeValue(() -> pathingBehavior(mod)
                .estimatedTicksToGoal()
                .map(Object::toString)
                .orElse("empty"));
        String pathStart = ChatClefDiagnostics.safeValue(() -> pathingBehavior(mod).pathStart());
        String calcFailedLastTick = ChatClefDiagnostics.safeValue(() -> pathingBehavior(mod).calcFailedLastTick());
        String mostRecentProcessClass = ChatClefDiagnostics.safeValue(() -> mostRecentProcess(mod)
                .map(ChatClefDiagnostics::className)
                .orElse("empty"));
        String mostRecentProcessDisplayName = ChatClefDiagnostics.safeValue(() -> mostRecentProcess(mod)
                .map(IBaritoneProcess::displayName)
                .orElse("empty"));
        String mostRecentProcessActive = ChatClefDiagnostics.safeValue(() -> mostRecentProcess(mod)
                .map(process -> Boolean.toString(process.isActive()))
                .orElse("empty"));
        String mostRecentCommandType = ChatClefDiagnostics.safeValue(() -> mostRecentCommand(mod)
                .map(command -> String.valueOf(command.commandType))
                .orElse("empty"));
        String mostRecentCommandGoalType = ChatClefDiagnostics.safeValue(() -> mostRecentCommand(mod)
                .map(command -> className(command.goal))
                .orElse("empty"));
        String mostRecentCommandGoalSummary = ChatClefDiagnostics.safeValue(() -> mostRecentCommand(mod)
                .map(command -> summarizeGoal(command.goal))
                .orElse("empty"));
        String derivedPlannerState = derivePlannerState(
                customGoalProcessState,
                inProgressPresent,
                inProgressFinished,
                nextPathPresent,
                calcFailedLastTick,
                mostRecentCommandType,
                mostRecentProcessClass
        );
        return new BaritonePlannerDiagnosticSnapshot(
                pathingGoalType,
                pathingGoalSummary,
                customGoalProcessState,
                customGoalType,
                customGoalSummary,
                mostRecentCustomGoalType,
                mostRecentCustomGoalSummary,
                inProgressPresent,
                inProgressSummary,
                inProgressGoalType,
                inProgressGoalSummary,
                inProgressFinished,
                nextPathPresent,
                nextPathSummary,
                estimatedTicksToGoal,
                pathStart,
                calcFailedLastTick,
                mostRecentProcessClass,
                mostRecentProcessDisplayName,
                mostRecentProcessActive,
                mostRecentCommandType,
                mostRecentCommandGoalType,
                mostRecentCommandGoalSummary,
                derivedPlannerState
        );
    }

    public Object[] fields() {
        return fields("");
    }

    public Object[] fields(String suffix) {
        String normalizedSuffix = suffix == null ? "" : suffix;
        return new Object[]{
                "plannerPathingGoalType" + normalizedSuffix, pathingGoalType,
                "plannerPathingGoalSummary" + normalizedSuffix, pathingGoalSummary,
                "plannerCustomGoalProcessState" + normalizedSuffix, customGoalProcessState,
                "plannerCustomGoalType" + normalizedSuffix, customGoalType,
                "plannerCustomGoalSummary" + normalizedSuffix, customGoalSummary,
                "plannerMostRecentCustomGoalType" + normalizedSuffix, mostRecentCustomGoalType,
                "plannerMostRecentCustomGoalSummary" + normalizedSuffix, mostRecentCustomGoalSummary,
                "plannerInProgressPresent" + normalizedSuffix, inProgressPresent,
                "plannerInProgressSummary" + normalizedSuffix, inProgressSummary,
                "plannerInProgressGoalType" + normalizedSuffix, inProgressGoalType,
                "plannerInProgressGoalSummary" + normalizedSuffix, inProgressGoalSummary,
                "plannerInProgressFinished" + normalizedSuffix, inProgressFinished,
                "plannerNextPathPresent" + normalizedSuffix, nextPathPresent,
                "plannerNextPathSummary" + normalizedSuffix, nextPathSummary,
                "plannerEstimatedTicksToGoal" + normalizedSuffix, estimatedTicksToGoal,
                "plannerPathStart" + normalizedSuffix, pathStart,
                "plannerCalcFailedLastTick" + normalizedSuffix, calcFailedLastTick,
                "plannerMostRecentProcessClass" + normalizedSuffix, mostRecentProcessClass,
                "plannerMostRecentProcessDisplayName" + normalizedSuffix, mostRecentProcessDisplayName,
                "plannerMostRecentProcessActive" + normalizedSuffix, mostRecentProcessActive,
                "plannerMostRecentCommandType" + normalizedSuffix, mostRecentCommandType,
                "plannerMostRecentCommandGoalType" + normalizedSuffix, mostRecentCommandGoalType,
                "plannerMostRecentCommandGoalSummary" + normalizedSuffix, mostRecentCommandGoalSummary,
                "plannerDerivedState" + normalizedSuffix, derivedPlannerState
        };
    }

    private static PathingBehavior pathingBehavior(AltoClef mod) {
        return mod.getClientBaritone().getPathingBehavior();
    }

    private static CustomGoalProcess customGoalProcess(AltoClef mod) {
        return mod.getClientBaritone().getCustomGoalProcess();
    }

    private static Optional<? extends IPathFinder> inProgress(AltoClef mod) {
        return pathingBehavior(mod).getInProgress();
    }

    private static Optional<IBaritoneProcess> mostRecentProcess(AltoClef mod) {
        return mod.getClientBaritone().getPathingControlManager().mostRecentInControl();
    }

    private static Optional<PathingCommand> mostRecentCommand(AltoClef mod) {
        return mod.getClientBaritone().getPathingControlManager().mostRecentCommand();
    }

    private static String className(Object value) {
        return value == null ? "none" : ChatClefDiagnostics.className(value);
    }

    private static String summarizeGoal(Object goal) {
        return goal == null ? "none" : String.valueOf(goal);
    }

    private static String summarizeObject(Object value) {
        if (value == null) {
            return "none";
        }
        return ChatClefDiagnostics.className(value)
                + "#"
                + Integer.toHexString(System.identityHashCode(value))
                + ":"
                + value;
    }

    private static String customGoalState(CustomGoalProcess process) {
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

    private static String mostRecentCustomGoalFieldValue(CustomGoalProcess process, boolean summary) {
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

    private static Field customGoalStateField() {
        return customGoalField("state");
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

    private static String derivePlannerState(String customGoalProcessState,
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
