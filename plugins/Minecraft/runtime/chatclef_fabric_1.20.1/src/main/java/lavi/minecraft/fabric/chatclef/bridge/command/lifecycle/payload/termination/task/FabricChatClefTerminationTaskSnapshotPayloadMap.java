package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.termination.task;

import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskSnapshot;

import java.util.Map;

//20260814_kpopmodder: Keep termination task snapshot serialization separate without changing emitted keys.
public final class FabricChatClefTerminationTaskSnapshotPayloadMap {
    private static final String TASK = "task";

    private FabricChatClefTerminationTaskSnapshotPayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, FabricChatClefTaskSnapshot taskSnapshot) {
        payload.put(TASK, taskSnapshot.toMap());
    }
}
