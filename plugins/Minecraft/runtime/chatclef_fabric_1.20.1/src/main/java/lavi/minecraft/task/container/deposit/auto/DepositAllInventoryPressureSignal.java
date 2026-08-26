package lavi.minecraft.task.container.deposit.auto;

//20260826_kpopmodder: Added state-machine signals for threshold crossing and rearming.
public enum DepositAllInventoryPressureSignal {
    NONE,
    THRESHOLD_REACHED,
    REARMED
}
