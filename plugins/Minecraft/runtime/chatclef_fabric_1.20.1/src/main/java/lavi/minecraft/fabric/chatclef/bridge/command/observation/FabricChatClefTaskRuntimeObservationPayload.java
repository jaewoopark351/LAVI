package lavi.minecraft.fabric.chatclef.bridge.command.observation;

import java.util.HashMap;
import java.util.Map;

//20260805_kpopmodder: Keep runtime task observation diagnostic fields typed until the Map edge.
public final class FabricChatClefTaskRuntimeObservationPayload {
    private static final String THREAD_NAME = "thread_name";
    private static final String OBSERVED_AT_MS = "observed_at_ms";
    private static final String CLIENT_TICK_ID = "client_tick_id";
    private static final String CURRENT_TASK = "current_task";

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
        Map<String, Object> payload = new HashMap<>();
        payload.put(THREAD_NAME, threadName);
        payload.put(OBSERVED_AT_MS, observedAtMs);
        payload.put(CLIENT_TICK_ID, clientTickId);
        payload.put(CURRENT_TASK, currentTask.toMap());
        return payload;
    }
}
