package lavi.minecraft.task.container.home.execution.timeout.operation;

import lavi.minecraft.task.container.home.execution.timeout.StoreHomeTimeoutPolicy;
import lavi.minecraft.task.container.home.execution.timeout.StoreHomeTimeoutReason;

import java.util.Optional;

//20260828_kpopmodder: Own only STORE_HOME operation no-progress and emergency active-root clocks.
public final class StoreHomeOperationTimeoutState {
    private final StoreHomeEmergencyHardCapClock emergencyClock =
            new StoreHomeEmergencyHardCapClock();
    private final StoreHomeOperationNoProgressClock noProgressClock =
            new StoreHomeOperationNoProgressClock();

    public void tick() {
        emergencyClock.tick();
        noProgressClock.tick();
    }

    public void recordSemanticProgress() {
        noProgressClock.recordSemanticProgress();
    }

    public Optional<StoreHomeTimeoutReason> timeoutReason(
            StoreHomeTimeoutPolicy policy) {
        Optional<StoreHomeTimeoutReason> emergency = emergencyTimeoutReason(policy);
        return emergency.isPresent() ? emergency : noProgressTimeoutReason(policy);
    }

    public Optional<StoreHomeTimeoutReason> emergencyTimeoutReason(
            StoreHomeTimeoutPolicy policy) {
        return emergencyClock.elapsedTicks()
                >= policy.operationEmergencyHardCapTicks()
                ? Optional.of(StoreHomeTimeoutReason.OPERATION_EMERGENCY_HARD_CAP)
                : Optional.empty();
    }

    public Optional<StoreHomeTimeoutReason> noProgressTimeoutReason(
            StoreHomeTimeoutPolicy policy) {
        return noProgressClock.elapsedTicks() >= policy.operationNoProgressTicks()
                ? Optional.of(StoreHomeTimeoutReason.OPERATION_NO_PROGRESS)
                : Optional.empty();
    }

    public int activeTicks() {
        return emergencyClock.elapsedTicks();
    }

    public int noProgressTicks() {
        return noProgressClock.elapsedTicks();
    }
}
