package lavi.minecraft.diagnostics.container.home.timeout.state;

import lavi.minecraft.task.container.home.execution.timeout.StoreHomeTimeoutObservation;
import lavi.minecraft.task.container.home.execution.timeout.StoreHomeTimeoutPolicy;
import lavi.minecraft.task.container.home.execution.timeout.StoreHomeTimeoutReason;

import java.util.Objects;

//20260828_kpopmodder: Added this type file to own only diagnostic operation-clock provenance.
public final class StoreHomeDiagnosticOperationState {
    private StoreHomeTimeoutObservation timeoutObservation;
    private boolean started;
    private long startClientTickId;
    private boolean startClientTickKnown;

    public StoreHomeDiagnosticOperationState(StoreHomeTimeoutPolicy timeoutPolicy) {
        timeoutObservation = StoreHomeTimeoutObservation.initial(
                Objects.requireNonNull(timeoutPolicy, "timeoutPolicy")
        );
    }

    public void observe(StoreHomeTimeoutObservation observation) {
        if (observation != null) {
            timeoutObservation = observation;
        }
    }

    public StoreHomeTimeoutObservation observation() {
        return timeoutObservation;
    }

    public boolean started() {
        return started;
    }

    public void markStarted(long clientTickId) {
        started = true;
        startClientTickId = clientTickId;
        startClientTickKnown = timeoutObservation.operationActiveTicks() == 0;
    }

    public long startClientTickId() {
        return startClientTickId;
    }

    public boolean startClientTickKnown() {
        return startClientTickKnown;
    }

    public int timeoutTicks() {
        return timeoutObservation.operationNoProgressTicks();
    }

    public int decisionTicks(StoreHomeTimeoutReason reason) {
        return reason == StoreHomeTimeoutReason.OPERATION_EMERGENCY_HARD_CAP
                ? timeoutObservation.operationActiveTicks()
                : timeoutObservation.operationNoProgressTicks();
    }
}
