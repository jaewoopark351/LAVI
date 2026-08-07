package lavi.minecraft.diagnostics.container.store;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.container.ContainerStoredTracker;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import net.minecraft.util.math.BlockPos;

//20260807_kpopmodder: Observe StoreInAnyContainerTask origin and progress without changing storage behavior.
public final class StoreInAnyContainerDiagnostics {
    private static final String CAP_SCOPE = "store_in_any_container";
    private static final StoreInAnyContainerEmissionLimiter LIMITER = new StoreInAnyContainerEmissionLimiter();

    private StoreInAnyContainerDiagnostics() {
    }

    public static void logStart(Task task,
                                boolean getIfNotPresent,
                                ItemTarget[] toStore) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        ChatClefDiagnostics.logBoundary("STORE_IN_ANY_CONTAINER_START",
                "store_in_any_container_start",
                task,
                ChatClefDiagnostics.withCommandContextFields(
                        StoreInAnyContainerDiagnosticFields.lifecycleFields(
                                "start",
                                task,
                                getIfNotPresent,
                                toStore,
                                null
                        )));
    }

    public static void logStop(Task task,
                               Task interruptTask,
                               boolean getIfNotPresent,
                               ItemTarget[] toStore) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        ChatClefDiagnostics.logBoundary("STORE_IN_ANY_CONTAINER_STOP",
                "store_in_any_container_stop",
                task,
                ChatClefDiagnostics.withCommandContextFields(
                        StoreInAnyContainerDiagnosticFields.lifecycleFields(
                                "stop",
                                task,
                                getIfNotPresent,
                                toStore,
                                new Object[]{
                                        "interruptTask", ChatClefDiagnostics.taskSummaryForDiagnosticLog(interruptTask)
                                }
                        )));
    }

    public static void logBranch(String branch,
                                 AltoClef mod,
                                 Task task,
                                 boolean getIfNotPresent,
                                 ItemTarget[] toStore,
                                 ItemTarget[] notStored,
                                 ContainerStoredTracker storedTracker,
                                 BlockPos closestContainer,
                                 boolean closestWithinRange,
                                 boolean currentTryWithinExtraRange,
                                 BlockPos currentChestTry,
                                 int dungeonChestCacheSize,
                                 int nonDungeonChestCacheSize,
                                 Object... branchFields) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }

        StoreInAnyContainerProgressDiagnostics.observe(
                branch,
                mod,
                task,
                getIfNotPresent,
                toStore,
                notStored,
                storedTracker,
                closestContainer,
                closestWithinRange,
                currentTryWithinExtraRange,
                currentChestTry,
                dungeonChestCacheSize,
                nonDungeonChestCacheSize,
                branchFields
        );

        String repeatKey = StoreInAnyContainerDiagnosticFields.repeatKey(
                branch,
                task,
                notStored,
                closestContainer,
                currentChestTry
        );
        StoreInAnyContainerEmissionDecision decision = LIMITER.evaluate(
                repeatKey,
                ChatClefDiagnostics.currentClientTickId()
        );
        if (decision.emitCap()) {
            ChatClefDiagnostics.logBoundary("STORE_IN_ANY_CONTAINER_DIAGNOSTIC_CAP_REACHED",
                    "store_in_any_container_diagnostic_cap_reached",
                    task,
                    ChatClefDiagnostics.withCommandContextFields(
                            StoreInAnyContainerDiagnosticFields.cap(CAP_SCOPE, StoreInAnyContainerEmissionLimiter.SESSION_HARD_CAP)
                    ));
            return;
        }
        if (decision.emitSummary()) {
            ChatClefDiagnostics.logBoundary("STORE_IN_ANY_CONTAINER_REPEAT_SUMMARY",
                    "store_in_any_container_repeat_summary",
                    task,
                    ChatClefDiagnostics.withCommandContextFields(
                            StoreInAnyContainerDiagnosticFields.repeatSummary(repeatKey, decision.suppressedRepeatCount())
                    ));
            return;
        }
        if (!decision.emitEvent()) {
            return;
        }

        ChatClefDiagnostics.logBoundary("STORE_IN_ANY_CONTAINER_BRANCH",
                branch,
                task,
                ChatClefDiagnostics.withCommandContextFields(
                        StoreInAnyContainerDiagnosticFields.branchFields(
                                branch,
                                repeatKey,
                                mod,
                                task,
                                getIfNotPresent,
                                toStore,
                                notStored,
                                storedTracker,
                                closestContainer,
                                closestWithinRange,
                                currentTryWithinExtraRange,
                                currentChestTry,
                                dungeonChestCacheSize,
                                nonDungeonChestCacheSize,
                                decision.suppressedRepeatCount(),
                                branchFields
                        )));
    }
}
