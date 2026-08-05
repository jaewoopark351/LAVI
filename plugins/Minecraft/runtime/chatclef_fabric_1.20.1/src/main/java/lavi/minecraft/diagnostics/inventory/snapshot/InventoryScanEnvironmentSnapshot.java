package lavi.minecraft.diagnostics.inventory.snapshot;

import adris.altoclef.AltoClef;
import lavi.minecraft.integration.carryon.CarryOnDiagnostics;
import lavi.minecraft.integration.carryon.CarryOnObservation;
import lavi.minecraft.integration.carryon.snapshot.CarryOnBaritoneSnapshot;
import lavi.minecraft.integration.carryon.snapshot.CarryOnBaritoneSnapshotCollector;
import lavi.minecraft.integration.carryon.snapshot.CarryOnTaskSnapshot;
import lavi.minecraft.integration.carryon.snapshot.CarryOnTaskSnapshotCollector;

//20260805_kpopmodder: Capture Carry On, Task, and Baritone scan context separately from screen state.
public final class InventoryScanEnvironmentSnapshot {
    private final String carryOnLoaded;
    private final String carryOnVersion;
    private final String carryObservationStatus;
    private final String carryState;
    private final String carriedBlockId;
    private final String carriedBlockDescription;
    private final String carriedBlockState;
    private final String carryObservationExceptionType;
    private final String topLevelTask;
    private final String childTask;
    private final String currentChain;
    private final String taskChain;
    private final String taskRunnerActive;
    private final String userTaskChainActive;
    private final String paused;
    private final String chatClefEnabled;
    private final String playerMode;
    private final String baritonePathing;
    private final String customGoalOwner;
    private final String breakingBlockState;

    private InventoryScanEnvironmentSnapshot(String carryOnLoaded,
                                             String carryOnVersion,
                                             String carryObservationStatus,
                                             String carryState,
                                             String carriedBlockId,
                                             String carriedBlockDescription,
                                             String carriedBlockState,
                                             String carryObservationExceptionType,
                                             String topLevelTask,
                                             String childTask,
                                             String currentChain,
                                             String taskChain,
                                             String taskRunnerActive,
                                             String userTaskChainActive,
                                             String paused,
                                             String chatClefEnabled,
                                             String playerMode,
                                             String baritonePathing,
                                             String customGoalOwner,
                                             String breakingBlockState) {
        this.carryOnLoaded = carryOnLoaded;
        this.carryOnVersion = carryOnVersion;
        this.carryObservationStatus = carryObservationStatus;
        this.carryState = carryState;
        this.carriedBlockId = carriedBlockId;
        this.carriedBlockDescription = carriedBlockDescription;
        this.carriedBlockState = carriedBlockState;
        this.carryObservationExceptionType = carryObservationExceptionType;
        this.topLevelTask = topLevelTask;
        this.childTask = childTask;
        this.currentChain = currentChain;
        this.taskChain = taskChain;
        this.taskRunnerActive = taskRunnerActive;
        this.userTaskChainActive = userTaskChainActive;
        this.paused = paused;
        this.chatClefEnabled = chatClefEnabled;
        this.playerMode = playerMode;
        this.baritonePathing = baritonePathing;
        this.customGoalOwner = customGoalOwner;
        this.breakingBlockState = breakingBlockState;
    }

    public static InventoryScanEnvironmentSnapshot capture() {
        CarryOnObservation carryOn = observeCarryOn();
        AltoClef mod = mod();
        CarryOnTaskSnapshot task = CarryOnTaskSnapshotCollector.collect(mod, "inventory_subtracker_scan");
        CarryOnBaritoneSnapshot baritone = CarryOnBaritoneSnapshotCollector.collect(mod);
        return new InventoryScanEnvironmentSnapshot(
                carryOn == null ? "unavailable" : Boolean.toString(carryOn.loaded()),
                carryOn == null ? "unavailable" : carryOn.version(),
                carryOn == null ? "unavailable" : carryOn.state().name(),
                carryOn == null ? "unavailable" : carryOn.state().name(),
                carryOn == null ? "unavailable" : carryOn.carriedBlockId(),
                carryOn == null ? "unavailable" : carryOn.carriedBlockDescription(),
                carryOn == null ? "unavailable" : carryOn.carriedBlockState(),
                carryOn == null ? "unavailable" : carryOn.exceptionType(),
                task.topLevelTask(),
                task.childTask(),
                task.currentChain(),
                task.taskChain(),
                task.taskRunnerActive(),
                task.userTaskChainActive(),
                task.paused(),
                task.chatClefEnabled(),
                task.playerMode(),
                baritone.baritonePathing(),
                baritone.customGoalOwner(),
                baritone.breakingBlockState()
        );
    }

    public String stableKeySegment() {
        return carryState + "|" + topLevelTask + "|" + currentChain;
    }

    public Object[] fields() {
        return new Object[]{
                "carryOnLoaded", carryOnLoaded,
                "carryOnVersion", carryOnVersion,
                "carryObservationStatus", carryObservationStatus,
                "carryStateAtBegin", carryState,
                "carriedBlockIdAtBegin", carriedBlockId,
                "carriedBlockDescriptionAtBegin", carriedBlockDescription,
                "carriedBlockStateAtBegin", carriedBlockState,
                "carryObservationExceptionType", carryObservationExceptionType,
                "topLevelTask", topLevelTask,
                "childTask", childTask,
                "currentChain", currentChain,
                "taskChain", taskChain,
                "taskRunnerActive", taskRunnerActive,
                "userTaskChainActive", userTaskChainActive,
                "paused", paused,
                "chatClefEnabled", chatClefEnabled,
                "playerMode", playerMode,
                "baritonePathing", baritonePathing,
                "customGoalOwner", customGoalOwner,
                "breakingBlockState", breakingBlockState
        };
    }

    public String carryState() {
        return carryState;
    }

    private static CarryOnObservation observeCarryOn() {
        try {
            return CarryOnDiagnostics.observe();
        } catch (RuntimeException | LinkageError error) {
            return null;
        }
    }

    private static AltoClef mod() {
        try {
            return AltoClef.getInstance();
        } catch (RuntimeException | LinkageError error) {
            return null;
        }
    }
}
