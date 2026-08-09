package lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.snapshot.summary.identity;

import java.util.Map;

public final class FabricChatClefTaskSnapshotIdentityValuePayloadMap {
    private static final String IDENTITY = "identity";

    private FabricChatClefTaskSnapshotIdentityValuePayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, String identity) {
        payload.put(IDENTITY, identity);
    }
}
