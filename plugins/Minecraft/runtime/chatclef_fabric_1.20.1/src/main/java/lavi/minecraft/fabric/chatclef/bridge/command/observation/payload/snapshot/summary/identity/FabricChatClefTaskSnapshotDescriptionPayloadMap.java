package lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.snapshot.summary.identity;

import java.util.Map;

public final class FabricChatClefTaskSnapshotDescriptionPayloadMap {
    private static final String DESCRIPTION = "description";

    private FabricChatClefTaskSnapshotDescriptionPayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, String description) {
        payload.put(DESCRIPTION, description);
    }
}
