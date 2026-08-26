package lavi.minecraft.task.container.deposit.auto;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;

//20260826_kpopmodder: Added bounded state-transition diagnostics for automatic deposit_all orchestration.
public final class DepositAllAutoDiagnostics {
    private static final String CATEGORY = "AUTO_DEPOSIT_ALL";

    private DepositAllAutoDiagnostics() {
    }

    public static void logRegistered(float priority) {
        ChatClefDiagnostics.logEvent(
                CATEGORY,
                "REGISTERED",
                "automatic_chain_registered",
                null,
                "priority", priority
        );
    }

    public static void logTransition(DepositAllInventoryPressureState previousState,
                                     DepositAllInventoryPressureState nextState,
                                     String reason,
                                     DepositAllInventoryPressureSnapshot snapshot,
                                     Task task) {
        ChatClefDiagnostics.logEvent(
                CATEGORY,
                "STATE_TRANSITION",
                reason,
                task,
                "previousState", previousState,
                "nextState", nextState,
                "occupiedSlots", snapshot == null ? -1 : snapshot.occupiedSlots(),
                "totalSlots", snapshot == null ? -1 : snapshot.totalSlots(),
                "thresholdReached", snapshot != null && snapshot.isAtOrAboveThreshold()
        );
    }

    public static void logTrigger(DepositAllInventoryPressureSnapshot snapshot,
                                  int targetTypeCount,
                                  Task task) {
        ChatClefDiagnostics.logEvent(
                CATEGORY,
                "TRIGGERED",
                "four_fifths_threshold_crossed",
                task,
                "occupiedSlots", snapshot.occupiedSlots(),
                "totalSlots", snapshot.totalSlots(),
                "targetTypeCount", targetTypeCount
        );
    }

    public static void logRunnerActivated(DepositAllInventoryPressureSnapshot snapshot,
                                          Task task) {
        ChatClefDiagnostics.logEvent(
                CATEGORY,
                "RUNNER_ACTIVATED",
                "automatic_task_required_inactive_runner",
                task,
                "occupiedSlots", snapshot.occupiedSlots(),
                "totalSlots", snapshot.totalSlots()
        );
    }
}
