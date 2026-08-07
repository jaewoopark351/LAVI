package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload;

import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskSnapshot;

import java.util.Map;

//20260805_kpopmodder: Keep termination observation values typed before final diagnostic map serialization.
public final class FabricChatClefCommandTerminationObservationPayload {
    private final String terminationKind;
    private final boolean taskPresent;
    private final boolean taskStopped;
    private final boolean stopStateAvailable;
    private final String stopStateError;
    private final double durationSeconds;
    private final long observedAtMs;
    private final String observationThread;
    private final FabricChatClefTaskSnapshot taskSnapshot;

    public FabricChatClefCommandTerminationObservationPayload(
            String terminationKind,
            boolean taskPresent,
            boolean taskStopped,
            boolean stopStateAvailable,
            String stopStateError,
            double durationSeconds,
            long observedAtMs,
            String observationThread,
            FabricChatClefTaskSnapshot taskSnapshot
    ) {
        this.terminationKind = terminationKind;
        this.taskPresent = taskPresent;
        this.taskStopped = taskStopped;
        this.stopStateAvailable = stopStateAvailable;
        this.stopStateError = stopStateError;
        this.durationSeconds = durationSeconds;
        this.observedAtMs = observedAtMs;
        this.observationThread = observationThread;
        this.taskSnapshot = taskSnapshot;
    }

    public Map<String, Object> toMap() {
        return FabricChatClefCommandTerminationObservationPayloadMap.toMap(
                terminationKind,
                taskPresent,
                taskStopped,
                stopStateAvailable,
                stopStateError,
                durationSeconds,
                observedAtMs,
                observationThread,
                taskSnapshot
        );
    }
}
