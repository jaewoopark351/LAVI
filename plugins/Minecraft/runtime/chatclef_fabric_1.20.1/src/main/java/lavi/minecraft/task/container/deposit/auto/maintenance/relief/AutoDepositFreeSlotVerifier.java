package lavi.minecraft.task.container.deposit.auto.maintenance.relief;

import adris.altoclef.AltoClef;
import lavi.minecraft.task.container.deposit.auto.DepositAllInventoryPressureSnapshot;
import lavi.minecraft.task.container.deposit.auto.maintenance.AutoDepositMaintenanceOutcome;
import lavi.minecraft.task.container.deposit.auto.pressure.AutoDepositInventoryPressureSource;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

//20260831_kpopmodder: Evaluate slot relief through the existing read-only pressure boundary.
/** Evaluates occupied-slot relief from one read-only pressure snapshot. */
public final class AutoDepositFreeSlotVerifier {
    private final AutoDepositInventoryPressureSource pressureSource;

    public AutoDepositFreeSlotVerifier(AutoDepositInventoryPressureSource pressureSource) {
        this.pressureSource = Objects.requireNonNull(pressureSource, "pressureSource");
    }

    public AutoDepositFreeSlotVerdict verify(
            AltoClef mod,
            int startingOccupiedSlots,
            int targetReliefSlots) {
        return verify(mod, new DepositAllInventoryPressureSnapshot(startingOccupiedSlots, 36), targetReliefSlots);
    }

    //20260914_kpopmodder: Compare one actual ending observation only against its compatible root-local baseline.
    public AutoDepositFreeSlotVerdict verify(AltoClef mod,
            DepositAllInventoryPressureSnapshot startingPressure, int targetReliefSlots) {
        Optional<DepositAllInventoryPressureSnapshot> observation = observe(mod);
        if (observation.isEmpty()) {
            return new AutoDepositFreeSlotVerdict(AutoDepositPressureObservationStatus.UNAVAILABLE,
                    Optional.empty(), OptionalInt.empty(), AutoDepositMaintenanceOutcome.UNAVAILABLE);
        }
        DepositAllInventoryPressureSnapshot current = observation.get();
        if (current.totalSlots() != startingPressure.totalSlots()) {
            return new AutoDepositFreeSlotVerdict(AutoDepositPressureObservationStatus.INCOMPATIBLE_SCOPE,
                    observation, OptionalInt.empty(), AutoDepositMaintenanceOutcome.UNAVAILABLE);
        }
        int signedDelta = startingPressure.occupiedSlots() - current.occupiedSlots();
        int freedSlots = Math.max(0, signedDelta);
        AutoDepositMaintenanceOutcome outcome;
        if (current.isAtOrBelowLowWater() || freedSlots >= targetReliefSlots) {
            outcome = AutoDepositMaintenanceOutcome.FULL_RELIEF;
        } else if (freedSlots > 0) {
            outcome = AutoDepositMaintenanceOutcome.PARTIAL_RELIEF;
        } else {
            outcome = AutoDepositMaintenanceOutcome.NO_SLOT_RELIEF;
        }
        return new AutoDepositFreeSlotVerdict(AutoDepositPressureObservationStatus.AVAILABLE,
                observation, OptionalInt.of(signedDelta), outcome);
    }

    public Optional<DepositAllInventoryPressureSnapshot> observe(AltoClef mod) {
        try {
            Optional<DepositAllInventoryPressureSnapshot> observed = pressureSource.read(mod);
            return observed == null ? Optional.empty() : observed;
        } catch (RuntimeException | LinkageError unavailable) {
            // This catch encloses only the read port, never a game action or Task execution.
            return Optional.empty();
        }
    }
}
