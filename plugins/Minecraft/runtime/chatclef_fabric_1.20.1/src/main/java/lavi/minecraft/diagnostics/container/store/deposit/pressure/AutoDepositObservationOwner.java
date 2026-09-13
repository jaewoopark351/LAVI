package lavi.minecraft.diagnostics.container.store.deposit.pressure;

import lavi.minecraft.diagnostics.observation.ObservationScope;

//20260913_kpopmodder: Carry diagnostic provenance on the exact operation without changing the Task contract.
public interface AutoDepositObservationOwner {
    ObservationScope diagnosticObservationScope();
}
