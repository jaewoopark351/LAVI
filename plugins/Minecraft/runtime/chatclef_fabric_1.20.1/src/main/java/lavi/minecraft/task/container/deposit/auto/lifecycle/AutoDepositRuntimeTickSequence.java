package lavi.minecraft.task.container.deposit.auto.lifecycle;

import lavi.minecraft.task.container.deposit.auto.DepositAllInventoryPressureChain;
import lavi.minecraft.task.container.deposit.auto.trusted.interaction.AutoDepositOpenContainerBindingTracker;

import java.util.Objects;

//20260829_kpopmodder: Keep exact-open binding observation ahead of automatic pressure decisions.
public final class AutoDepositRuntimeTickSequence {
    private final Runnable bindingTrackerTick;

    public AutoDepositRuntimeTickSequence(
            AutoDepositOpenContainerBindingTracker bindingTracker) {
        this(Objects.requireNonNull(bindingTracker, "bindingTracker")::onEndClientTick);
    }

    AutoDepositRuntimeTickSequence(Runnable bindingTrackerTick) {
        this.bindingTrackerTick = Objects.requireNonNull(
                bindingTrackerTick,
                "bindingTrackerTick"
        );
    }

    public void onEndClientTick(DepositAllInventoryPressureChain pressureChain) {
        runInOrder(
                Objects.requireNonNull(pressureChain, "pressureChain")::onEndClientTick
        );
    }

    void runInOrder(Runnable pressureChainTick) {
        Runnable checkedPressureTick = Objects.requireNonNull(
                pressureChainTick,
                "pressureChainTick"
        );
        bindingTrackerTick.run();
        checkedPressureTick.run();
    }
}
