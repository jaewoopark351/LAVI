package lavi.minecraft.fabric.chatclef.bridge.command.observation.payload;

import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipSnapshot;

import java.util.HashMap;
import java.util.Map;

//20260807_kpopmodder: Folderized runtime task observation Map serialization without changing emitted fields.
public final class FabricChatClefTaskRuntimeObservationPayloadMap {
    private static final String THREAD_NAME = "thread_name";
    private static final String OBSERVED_AT_MS = "observed_at_ms";
    private static final String CLIENT_TICK_ID = "client_tick_id";
    private static final String CURRENT_TASK = "current_task";
    private static final String OWNERSHIP = "ownership";

    private FabricChatClefTaskRuntimeObservationPayloadMap() {
    }

    public static Map<String, Object> toMap(
            String threadName,
            long observedAtMs,
            long clientTickId,
            FabricChatClefTaskSnapshot currentTask,
            FabricChatClefTaskOwnershipSnapshot ownership
    ) {
        Map<String, Object> payload = new HashMap<>();
        payload.put(THREAD_NAME, threadName);
        payload.put(OBSERVED_AT_MS, observedAtMs);
        payload.put(CLIENT_TICK_ID, clientTickId);
        payload.put(CURRENT_TASK, currentTask.toMap());
        payload.put(OWNERSHIP, ownership.toMap());
        return payload;
    }
}
