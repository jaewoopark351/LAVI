package lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.snapshot.summary;

import java.util.Map;

public final class FabricChatClefTaskSnapshotAvailablePayloadMap {
    private static final String AVAILABLE = "available";

    private FabricChatClefTaskSnapshotAvailablePayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, boolean available) {
        payload.put(AVAILABLE, available);
    }
}
