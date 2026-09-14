package lavi.minecraft.task.container.deposit.auto.maintenance.diagnostics;

import lavi.minecraft.task.container.deposit.auto.maintenance.result.AutoDepositRunResult;

//20260914_kpopmodder: Format immutable root evidence without reading gameplay state or selecting behavior.
final class AutoDepositRunResultFields {
    private AutoDepositRunResultFields() { }

    static Object[] of(AutoDepositRunResult result, boolean finalized) {
        return new Object[]{"resultReason", result.reason(), "detail", result.detail(),
                "evidenceBoundary", finalized && result.cleanupComplete() ? "AFTER_CLEANUP" : "BEFORE_CLEANUP",
                "startingPressureAvailable", result.startingPressure().isPresent(),
                "endingPressureAvailable", result.endingPressure().isPresent(),
                "startOccupied", result.startingPressure().map(value -> value.occupiedSlots()).orElse(-1),
                "endOccupied", result.endingPressure().map(value -> value.occupiedSlots()).orElse(-1),
                "signedDeltaAvailable", result.signedFreedSlotDelta().isPresent(),
                "signedDelta", result.signedFreedSlotDelta().orElse(0),
                "childrenComplete", result.childrenComplete(), "workingSetStatus", result.workingSetStatus(),
                "cleanupComplete", result.cleanupComplete(), "normalValidated", result.normalValidated(),
                "scope", "EXECUTION_ROOT_NOT_LOGICAL_WORK"};
    }
}
