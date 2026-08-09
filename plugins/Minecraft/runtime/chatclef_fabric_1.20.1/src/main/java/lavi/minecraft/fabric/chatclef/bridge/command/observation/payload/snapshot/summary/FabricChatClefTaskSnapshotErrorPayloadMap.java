package lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.snapshot.summary;

import java.util.Map;

public final class FabricChatClefTaskSnapshotErrorPayloadMap {
    private static final String ERROR = "error";

    private FabricChatClefTaskSnapshotErrorPayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, String error) {
        payload.put(ERROR, error);
    }
}
