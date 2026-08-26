package lavi.minecraft.task.container.deposit.auto;

//20260826_kpopmodder: Added explicit automatic deposit_all lifecycle states.
public enum DepositAllInventoryPressureState {
    ARMED,
    RUNNING,
    WAIT_FOR_REARM,
    NO_SAFE_SURPLUS_WAIT
}
