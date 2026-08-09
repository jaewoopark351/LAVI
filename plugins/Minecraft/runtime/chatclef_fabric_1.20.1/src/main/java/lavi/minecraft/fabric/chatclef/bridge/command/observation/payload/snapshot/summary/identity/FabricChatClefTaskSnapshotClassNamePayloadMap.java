package lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.snapshot.summary.identity;

import java.util.Map;

public final class FabricChatClefTaskSnapshotClassNamePayloadMap {
    private static final String CLASS_NAME = "class_name";

    private FabricChatClefTaskSnapshotClassNamePayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, String className) {
        payload.put(CLASS_NAME, className);
    }
}
