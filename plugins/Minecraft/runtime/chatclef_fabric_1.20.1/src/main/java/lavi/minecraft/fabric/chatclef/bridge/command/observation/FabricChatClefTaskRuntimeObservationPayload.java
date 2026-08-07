package lavi.minecraft.fabric.chatclef.bridge.command.observation;

import lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.FabricChatClefTaskRuntimeObservationPayloadMap;

import java.util.Map;

//20260805_kpopmodder: Keep runtime task observation diagnostic fields typed until the Map edge.
public final class FabricChatClefTaskRuntimeObservationPayload {
    private final String threadName;
    private final long observedAtMs;
    private final long clientTickId;
    private final FabricChatClefTaskSnapshot currentTask;

    private FabricChatClefTaskRuntimeObservationPayload(
            String threadName,
            long observedAtMs,
            long clientTickId,
            FabricChatClefTaskSnapshot currentTask
    ) {
        this.threadName = threadName;
        this.observedAtMs = observedAtMs;
        this.clientTickId = clientTickId;
        this.currentTask = currentTask;
    }

    public static FabricChatClefTaskRuntimeObservationPayload of(
            String threadName,
            long observedAtMs,
            long clientTickId,
            FabricChatClefTaskSnapshot currentTask
    ) {
        return new FabricChatClefTaskRuntimeObservationPayload(
                threadName,
                observedAtMs,
                clientTickId,
                currentTask
        );
    }

    public Map<String, Object> toMap() {
        return FabricChatClefTaskRuntimeObservationPayloadMap.toMap(
                threadName,
                observedAtMs,
                clientTickId,
                currentTask
        );
    }
}
