package lavi.minecraft.task.container.deposit.auto;

import java.util.Objects;

//20260826_kpopmodder: Added one-shot threshold crossing and rearm ownership for automatic deposit_all.
public final class DepositAllInventoryPressureStateMachine {
    private DepositAllInventoryPressureState state = DepositAllInventoryPressureState.ARMED;
    private boolean thresholdPending;

    public DepositAllInventoryPressureSignal observe(DepositAllInventoryPressureSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        boolean thresholdReached = snapshot.isAtOrAboveThreshold();

        if (state == DepositAllInventoryPressureState.WAIT_FOR_REARM && !thresholdReached) {
            state = DepositAllInventoryPressureState.ARMED;
            thresholdPending = false;
            return DepositAllInventoryPressureSignal.REARMED;
        }
        if (state == DepositAllInventoryPressureState.ARMED && !thresholdReached) {
            thresholdPending = false;
            return DepositAllInventoryPressureSignal.NONE;
        }
        if (state == DepositAllInventoryPressureState.ARMED && thresholdReached && !thresholdPending) {
            thresholdPending = true;
            return DepositAllInventoryPressureSignal.THRESHOLD_REACHED;
        }
        return DepositAllInventoryPressureSignal.NONE;
    }

    public void markRunStarted() {
        requireState(DepositAllInventoryPressureState.ARMED, "start");
        requireThresholdPending("start");
        thresholdPending = false;
        state = DepositAllInventoryPressureState.RUNNING;
    }

    public void markThresholdSuppressed() {
        requireState(DepositAllInventoryPressureState.ARMED, "suppress");
        requireThresholdPending("suppress");
        thresholdPending = false;
        state = DepositAllInventoryPressureState.WAIT_FOR_REARM;
    }

    public void markRunTerminated() {
        requireState(DepositAllInventoryPressureState.RUNNING, "terminate");
        thresholdPending = false;
        state = DepositAllInventoryPressureState.WAIT_FOR_REARM;
    }

    public DepositAllInventoryPressureState state() {
        return state;
    }

    private void requireState(DepositAllInventoryPressureState expected, String action) {
        if (state != expected) {
            throw new IllegalStateException("Cannot " + action + " automatic deposit_all while state is " + state);
        }
    }

    private void requireThresholdPending(String action) {
        if (!thresholdPending) {
            throw new IllegalStateException("Cannot " + action + " automatic deposit_all without a threshold signal");
        }
    }
}
