package lavi.minecraft.task.container.deposit.auto;

import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositDecisionFingerprint;

import java.util.Objects;

//20260826_kpopmodder: Added one-shot threshold crossing and rearm ownership for automatic deposit_all.
public final class DepositAllInventoryPressureStateMachine {
    private DepositAllInventoryPressureState state = DepositAllInventoryPressureState.ARMED;
    private boolean thresholdPending;
    private AutoDepositDecisionFingerprint noSafeFingerprint;

    //20260914_kpopmodder: The typed rearm policy admits resumed work even after pressure has already fallen.
    public void prepareAdmittedRun() {
        if (state == DepositAllInventoryPressureState.RUNNING) {
            throw new IllegalStateException("Cannot admit over an active automatic root");
        }
        state = DepositAllInventoryPressureState.ARMED;
        thresholdPending = true;
        noSafeFingerprint = null;
    }

    //20260914_kpopmodder: Project typed policy waiting without using legacy diagnostic fingerprints for admission.
    public void markPolicyWaiting(boolean planBlocked) {
        if (state == DepositAllInventoryPressureState.RUNNING) {
            throw new IllegalStateException("Cannot replace active automatic execution with a planning wait");
        }
        state = planBlocked ? DepositAllInventoryPressureState.NO_SAFE_SURPLUS_WAIT
                : DepositAllInventoryPressureState.WAIT_FOR_REARM;
        thresholdPending = false;
        noSafeFingerprint = null;
    }

    public DepositAllInventoryPressureSignal observe(DepositAllInventoryPressureSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        boolean lowWaterReached = snapshot.isAtOrBelowLowWater();

        if ((state == DepositAllInventoryPressureState.WAIT_FOR_REARM
                || state == DepositAllInventoryPressureState.NO_SAFE_SURPLUS_WAIT)
                && lowWaterReached) {
            state = DepositAllInventoryPressureState.ARMED;
            thresholdPending = false;
            noSafeFingerprint = null;
            return DepositAllInventoryPressureSignal.REARMED;
        }
        if (state == DepositAllInventoryPressureState.ARMED && !snapshot.isAtOrAboveThreshold()) {
            thresholdPending = false;
            return DepositAllInventoryPressureSignal.NONE;
        }
        if (state == DepositAllInventoryPressureState.ARMED
                && snapshot.isAtOrAboveThreshold()
                && !thresholdPending) {
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
        noSafeFingerprint = null;
    }

    public void markThresholdSuppressed() {
        requireState(DepositAllInventoryPressureState.ARMED, "suppress");
        requireThresholdPending("suppress");
        thresholdPending = false;
        state = DepositAllInventoryPressureState.WAIT_FOR_REARM;
        noSafeFingerprint = null;
    }

    public void markNoSafeSurplus(AutoDepositDecisionFingerprint fingerprint) {
        requireState(DepositAllInventoryPressureState.ARMED, "latch no-safe-surplus");
        requireThresholdPending("latch no-safe-surplus");
        thresholdPending = false;
        noSafeFingerprint = Objects.requireNonNull(fingerprint, "fingerprint");
        state = DepositAllInventoryPressureState.NO_SAFE_SURPLUS_WAIT;
    }

    public DepositAllInventoryPressureSignal observeMeaningfulChange(
            AutoDepositDecisionFingerprint currentFingerprint) {
        Objects.requireNonNull(currentFingerprint, "currentFingerprint");
        if (state != DepositAllInventoryPressureState.NO_SAFE_SURPLUS_WAIT
                || Objects.equals(noSafeFingerprint, currentFingerprint)) {
            return DepositAllInventoryPressureSignal.NONE;
        }
        noSafeFingerprint = null;
        thresholdPending = false;
        state = DepositAllInventoryPressureState.ARMED;
        return DepositAllInventoryPressureSignal.MEANINGFUL_CHANGE;
    }

    //20260827_kpopmodder: Rearm once when the injected trusted registry actually changes.
    public DepositAllInventoryPressureSignal observeExplicitPolicyChange() {
        if (state != DepositAllInventoryPressureState.WAIT_FOR_REARM) {
            return DepositAllInventoryPressureSignal.NONE;
        }
        noSafeFingerprint = null;
        thresholdPending = false;
        state = DepositAllInventoryPressureState.ARMED;
        return DepositAllInventoryPressureSignal.MEANINGFUL_CHANGE;
    }

    public void markRunTerminated() {
        requireState(DepositAllInventoryPressureState.RUNNING, "terminate");
        thresholdPending = false;
        state = DepositAllInventoryPressureState.WAIT_FOR_REARM;
        noSafeFingerprint = null;
    }

    public DepositAllInventoryPressureState state() {
        return state;
    }

    //20260913_kpopmodder: Expose the existing latch value to a passive diagnostic snapshot only.
    public boolean diagnosticThresholdPending() {
        return thresholdPending;
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
