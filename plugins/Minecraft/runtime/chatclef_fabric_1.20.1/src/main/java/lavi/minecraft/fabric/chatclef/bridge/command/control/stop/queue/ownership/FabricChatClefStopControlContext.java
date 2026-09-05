package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership;

//20260905_kpopmodder: Own one STOP control's client-tick state without static or engine-global side channels.

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.request.validation.FabricChatClefStopControlRequest;

public final class FabricChatClefStopControlContext {
    private final FabricChatClefStopControlRequest request;
    private final FabricChatClefStopControlAdmissionBarrierToken barrierToken;
    private FabricChatClefOrdinaryCommandStopCapture capture;
    private String targetResolution = "not_evaluated";
    private String phase = "queued";
    private boolean userStopMarkerBound;
    private boolean stopCommandInvoked;
    private boolean resultCommitted;
    private long executedClientTick = -1L;
    private long verifiedClientTick = -1L;
    private Task capturedBoundRootTask;

    public FabricChatClefStopControlContext(
            FabricChatClefStopControlRequest request,
            FabricChatClefStopControlAdmissionBarrierToken barrierToken
    ) {
        this.request = request;
        this.barrierToken = barrierToken;
    }

    public FabricChatClefStopControlRequest request() {
        return request;
    }

    public FabricChatClefStopControlAdmissionBarrierToken barrierToken() {
        return barrierToken;
    }

    public synchronized void capture(
            FabricChatClefOrdinaryCommandStopCapture capture,
            String targetResolution
    ) {
        if (this.capture != null) {
            throw new IllegalStateException("STOP ordinary context was already captured");
        }
        this.capture = capture;
        this.targetResolution = targetResolution;
        this.phase = "captured";
    }

    public synchronized void markExecuted(long clientTick, boolean markerBound, Task boundRootTask) {
        if (stopCommandInvoked) {
            throw new IllegalStateException("registered StopCommand was already invoked");
        }
        stopCommandInvoked = true;
        userStopMarkerBound = markerBound;
        capturedBoundRootTask = boundRootTask;
        executedClientTick = clientTick;
        verifiedClientTick = clientTick;
        phase = "executing";
    }

    public synchronized void markVerifying() {
        phase = "verifying";
    }

    public synchronized void markVerified(long clientTick) {
        verifiedClientTick = clientTick;
    }

    public synchronized void markQuarantined() {
        phase = "quarantined";
    }

    public synchronized boolean commitResult() {
        if (resultCommitted) {
            return false;
        }
        resultCommitted = true;
        phase = "result_committed";
        return true;
    }

    public synchronized FabricChatClefOrdinaryCommandStopCapture capture() {
        return capture;
    }

    public synchronized String targetResolution() {
        return targetResolution;
    }

    public synchronized boolean userStopMarkerBound() {
        return userStopMarkerBound;
    }

    public synchronized boolean stopCommandInvoked() {
        return stopCommandInvoked;
    }

    public synchronized boolean resultCommitted() {
        return resultCommitted;
    }

    public synchronized long executedClientTick() {
        return executedClientTick;
    }

    public synchronized long verifiedClientTick() {
        return verifiedClientTick;
    }

    public synchronized String phase() {
        return phase;
    }

    public synchronized Task capturedBoundRootTask() {
        return capturedBoundRootTask;
    }
}
