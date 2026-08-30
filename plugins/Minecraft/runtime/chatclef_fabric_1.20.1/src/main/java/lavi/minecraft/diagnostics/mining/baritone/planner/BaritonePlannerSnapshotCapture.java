package lavi.minecraft.diagnostics.mining.baritone.planner;

import adris.altoclef.AltoClef;
import baritone.api.pathing.calc.IPathFinder;
import baritone.api.process.IBaritoneProcess;
import baritone.api.process.PathingCommand;
import baritone.behavior.PathingBehavior;
import baritone.process.CustomGoalProcess;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;

import java.util.Optional;

//20260830_kpopmodder: Capture read-only planner values while leaving classification and projection elsewhere.
public final class BaritonePlannerSnapshotCapture {
    private BaritonePlannerSnapshotCapture() {
    }

    public static BaritonePlannerSnapshotData capture(AltoClef mod) {
        String pathingGoalType = ChatClefDiagnostics.safeValue(
                () -> BaritonePlannerSnapshotValues.className(pathingBehavior(mod).getGoal()));
        String pathingGoalSummary = ChatClefDiagnostics.safeValue(
                () -> BaritonePlannerSnapshotValues.summarizeGoal(pathingBehavior(mod).getGoal()));
        String customGoalProcessState = ChatClefDiagnostics.safeValue(
                () -> BaritonePlannerSnapshotValues.customGoalState(customGoalProcess(mod)));
        String customGoalType = ChatClefDiagnostics.safeValue(
                () -> BaritonePlannerSnapshotValues.className(customGoalProcess(mod).getGoal()));
        String customGoalSummary = ChatClefDiagnostics.safeValue(
                () -> BaritonePlannerSnapshotValues.summarizeGoal(customGoalProcess(mod).getGoal()));
        String mostRecentCustomGoalType = ChatClefDiagnostics.safeValue(
                () -> BaritonePlannerSnapshotValues.mostRecentCustomGoalValue(customGoalProcess(mod), false));
        String mostRecentCustomGoalSummary = ChatClefDiagnostics.safeValue(
                () -> BaritonePlannerSnapshotValues.mostRecentCustomGoalValue(customGoalProcess(mod), true));
        String inProgressPresent = ChatClefDiagnostics.safeValue(() -> inProgress(mod).isPresent());
        String inProgressSummary = ChatClefDiagnostics.safeValue(() -> inProgress(mod)
                .map(BaritonePlannerSnapshotValues::summarizeObject)
                .orElse("empty"));
        String inProgressGoalType = ChatClefDiagnostics.safeValue(() -> inProgress(mod)
                .map(finder -> BaritonePlannerSnapshotValues.className(finder.getGoal()))
                .orElse("empty"));
        String inProgressGoalSummary = ChatClefDiagnostics.safeValue(() -> inProgress(mod)
                .map(finder -> BaritonePlannerSnapshotValues.summarizeGoal(finder.getGoal()))
                .orElse("empty"));
        String inProgressFinished = BaritonePlannerSnapshotValues.finishObservation(inProgressPresent);
        String nextPathPresent = ChatClefDiagnostics.safeValue(() -> pathingBehavior(mod).getNext() != null);
        String nextPathSummary = ChatClefDiagnostics.safeValue(
                () -> BaritonePlannerSnapshotValues.summarizeObject(pathingBehavior(mod).getNext()));
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
                .map(command -> BaritonePlannerSnapshotValues.className(command.goal))
                .orElse("empty"));
        String mostRecentCommandGoalSummary = ChatClefDiagnostics.safeValue(() -> mostRecentCommand(mod)
                .map(command -> BaritonePlannerSnapshotValues.summarizeGoal(command.goal))
                .orElse("empty"));
        String derivedPlannerState = BaritonePlannerStateClassifier.classify(
                customGoalProcessState,
                inProgressPresent,
                inProgressFinished,
                nextPathPresent,
                calcFailedLastTick,
                mostRecentCommandType,
                mostRecentProcessClass
        );
        return new BaritonePlannerSnapshotData(
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
}
