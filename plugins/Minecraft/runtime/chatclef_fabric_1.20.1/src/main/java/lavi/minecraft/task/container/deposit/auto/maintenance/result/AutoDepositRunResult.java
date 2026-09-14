package lavi.minecraft.task.container.deposit.auto.maintenance.result;

import lavi.minecraft.task.container.deposit.auto.DepositAllInventoryPressureSnapshot;
import lavi.minecraft.task.container.deposit.auto.maintenance.AutoDepositMaintenanceOutcome;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

//20260914_kpopmodder: Preserve this execution root's immutable evidence independently of logical-work aggregation.
public record AutoDepositRunResult(
        AutoDepositRunReason reason,
        Optional<DepositAllInventoryPressureSnapshot> startingPressure,
        Optional<DepositAllInventoryPressureSnapshot> endingPressure,
        boolean childrenComplete,
        AutoDepositWorkingSetStatus workingSetStatus,
        boolean cleanupComplete,
        String detail,
        AutoDepositMaintenanceOutcome reliefOutcome) {

    public AutoDepositRunResult {
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(startingPressure, "startingPressure");
        Objects.requireNonNull(endingPressure, "endingPressure");
        Objects.requireNonNull(workingSetStatus, "workingSetStatus");
        Objects.requireNonNull(detail, "detail");
        Objects.requireNonNull(reliefOutcome, "reliefOutcome");
    }

    public OptionalInt signedFreedSlotDelta() {
        if (startingPressure.isEmpty() || endingPressure.isEmpty()
                || startingPressure.get().totalSlots() != endingPressure.get().totalSlots()) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(startingPressure.get().occupiedSlots() - endingPressure.get().occupiedSlots());
    }

    /** A zero root-local delta is valid: the owning chain compares the logical work's original baseline. */
    public boolean normalValidated() {
        return reason == AutoDepositRunReason.NORMAL
                && childrenComplete
                && workingSetStatus.satisfiedOrNotApplicable()
                && cleanupComplete
                && signedFreedSlotDelta().isPresent();
    }
}
