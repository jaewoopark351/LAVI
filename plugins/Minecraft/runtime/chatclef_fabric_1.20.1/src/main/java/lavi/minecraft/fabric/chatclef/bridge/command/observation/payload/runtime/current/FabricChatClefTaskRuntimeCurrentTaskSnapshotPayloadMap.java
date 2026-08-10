package lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.runtime.current;

import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskSnapshot;

import java.util.Map;

public final class FabricChatClefTaskRuntimeCurrentTaskSnapshotPayloadMap {
    private static final String CURRENT_TASK = "current_task";

    private FabricChatClefTaskRuntimeCurrentTaskSnapshotPayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, FabricChatClefTaskSnapshot currentTask) {
        payload.put(CURRENT_TASK, currentTask.toMap());
    }
}
