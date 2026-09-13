package lavi.minecraft.task.container.deposit.auto.lifecycle;

import lavi.minecraft.diagnostics.container.store.deposit.pressure.AutoDepositBoundaryDiagnostics;
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
        //20260913_kpopmodder: Record the actual binding return; never rerun a skipped callback for logging.
        AutoDepositBoundaryDiagnostics.log("AUTO_DEPOSIT_BINDING_RETURN", "binding_tick_returned", null);
        checkedPressureTick.run();
        AutoDepositBoundaryDiagnostics.log("AUTO_DEPOSIT_PRESSURE_RETURN", "pressure_tick_returned", null);
    }
}
