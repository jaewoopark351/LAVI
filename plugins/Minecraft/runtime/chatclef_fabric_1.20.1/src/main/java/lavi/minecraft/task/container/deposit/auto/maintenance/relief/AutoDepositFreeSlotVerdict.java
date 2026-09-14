package lavi.minecraft.task.container.deposit.auto.maintenance.relief;

import lavi.minecraft.task.container.deposit.auto.maintenance.AutoDepositMaintenanceOutcome;
import lavi.minecraft.task.container.deposit.auto.DepositAllInventoryPressureSnapshot;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

//20260831_kpopmodder: Represent one immutable automatic-deposit slot-relief observation.
/** Immutable result of the automatic-deposit occupied-slot postcondition. */
//20260914_kpopmodder: Retain the real snapshot and signed comparison; absent observations have no slot values.
public record AutoDepositFreeSlotVerdict(
        AutoDepositPressureObservationStatus observationStatus,
        Optional<DepositAllInventoryPressureSnapshot> endingPressure,
        OptionalInt signedDelta,
        AutoDepositMaintenanceOutcome outcome) {
    public AutoDepositFreeSlotVerdict {
        Objects.requireNonNull(observationStatus, "observationStatus");
        Objects.requireNonNull(endingPressure, "endingPressure");
        Objects.requireNonNull(signedDelta, "signedDelta");
        Objects.requireNonNull(outcome, "outcome");
    }

    public boolean available() { return observationStatus == AutoDepositPressureObservationStatus.AVAILABLE; }
    public int endingOccupiedSlots() { return endingPressure.map(DepositAllInventoryPressureSnapshot::occupiedSlots).orElse(-1); }
    public int freedSlots() { return signedDelta.isPresent() ? Math.max(0, signedDelta.getAsInt()) : -1; }
}
