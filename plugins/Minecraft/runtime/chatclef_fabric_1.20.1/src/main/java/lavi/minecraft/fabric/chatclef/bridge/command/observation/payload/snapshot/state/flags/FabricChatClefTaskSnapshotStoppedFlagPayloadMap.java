package lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.snapshot.state.flags;

import java.util.Map;

public final class FabricChatClefTaskSnapshotStoppedFlagPayloadMap {
    private static final String TASK_STOPPED = "task_stopped";

    private FabricChatClefTaskSnapshotStoppedFlagPayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, boolean taskStopped) {
        payload.put(TASK_STOPPED, taskStopped);
    }
}
