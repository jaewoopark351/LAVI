package lavi.minecraft.task.container.deposit.auto.maintenance.result;

import lavi.minecraft.task.container.deposit.auto.DepositAllInventoryPressureSnapshot;
import lavi.minecraft.task.container.deposit.auto.maintenance.AutoDepositMaintenanceOutcome;
import lavi.minecraft.task.container.deposit.auto.maintenance.relief.AutoDepositFreeSlotVerdict;

import java.util.Objects;
import java.util.Optional;

//20260914_kpopmodder: Capture a terminal candidate before cleanup and settle exactly once afterwards.
public final class AutoDepositRunCompletion {
    private AutoDepositRunResult candidate;
    private AutoDepositRunResult result;

    public boolean capture(AutoDepositRunResult value) {
        Objects.requireNonNull(value, "value");
        if (candidate != null || result != null) return false;
        candidate = value;
        return true;
    }

    public Optional<AutoDepositRunResult> candidate() { return Optional.ofNullable(candidate); }
    public Optional<AutoDepositRunResult> result() { return Optional.ofNullable(result); }

    public Optional<AutoDepositRunResult> finalizeAfterCleanup(boolean cleaned) {
        if (result != null || candidate == null) return result();
        return settle(cleaned, candidate.endingPressure(), candidate.workingSetStatus(), candidate.reliefOutcome());
    }

    //20260914_kpopmodder: Cleanup can return cursor contents to main; only the final evidence uses the later observation.
    public Optional<AutoDepositRunResult> finalizeAfterCleanup(boolean cleaned,
            AutoDepositFreeSlotVerdict postCleanupPressure, AutoDepositWorkingSetStatus postCleanupWorking) {
        if (result != null || candidate == null) return result();
        Objects.requireNonNull(postCleanupPressure, "postCleanupPressure");
        Objects.requireNonNull(postCleanupWorking, "postCleanupWorking");
        return settle(cleaned, postCleanupPressure.endingPressure(), postCleanupWorking, postCleanupPressure.outcome());
    }

    private Optional<AutoDepositRunResult> settle(boolean cleaned,
            Optional<DepositAllInventoryPressureSnapshot> ending, AutoDepositWorkingSetStatus working,
            AutoDepositMaintenanceOutcome relief) {
        AutoDepositRunReason reason = cleaned ? candidate.reason() : AutoDepositRunReason.CLEANUP_FAILED;
        String detail = cleaned ? candidate.detail() : "cleanup_failed_after_" + candidate.reason().name();
        if (cleaned && (reason == AutoDepositRunReason.NORMAL || reason == AutoDepositRunReason.REPLAN_REQUIRED)) {
            if (!working.satisfiedOrNotApplicable()) {
                reason = working == AutoDepositWorkingSetStatus.DEFICIT
                        ? AutoDepositRunReason.WORKING_SET_DEFICIT : AutoDepositRunReason.WORKING_SET_UNAVAILABLE;
                detail = "post_cleanup_working_set_" + working.name();
            } else if (candidate.startingPressure().isEmpty() || ending.isEmpty()
                    || candidate.startingPressure().orElseThrow().totalSlots() != ending.orElseThrow().totalSlots()) {
                reason = AutoDepositRunReason.PRESSURE_UNAVAILABLE;
                detail = "post_cleanup_pressure_unavailable_or_incompatible";
            }
        }
        result = new AutoDepositRunResult(
                reason, candidate.startingPressure(), ending, candidate.childrenComplete(),
                working, cleaned, detail, relief);
        return result();
    }
}
