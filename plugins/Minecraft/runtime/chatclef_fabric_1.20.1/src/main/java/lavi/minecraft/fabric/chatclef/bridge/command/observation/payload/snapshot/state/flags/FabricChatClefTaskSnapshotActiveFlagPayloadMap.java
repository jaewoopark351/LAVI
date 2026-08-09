package lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.snapshot.state.flags;

import java.util.Map;

public final class FabricChatClefTaskSnapshotActiveFlagPayloadMap {
    private static final String TASK_ACTIVE = "task_active";

    private FabricChatClefTaskSnapshotActiveFlagPayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, boolean taskActive) {
        payload.put(TASK_ACTIVE, taskActive);
    }
}
