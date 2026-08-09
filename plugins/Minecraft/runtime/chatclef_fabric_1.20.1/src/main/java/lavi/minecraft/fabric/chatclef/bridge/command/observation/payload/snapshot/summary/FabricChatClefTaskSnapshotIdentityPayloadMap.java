package lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.snapshot.summary;

import java.util.Map;

//20260809_kpopmodder: Split snapshot identity fields without changing emitted keys.
public final class FabricChatClefTaskSnapshotIdentityPayloadMap {
    private static final String CLASS_NAME = "class_name";
    private static final String DESCRIPTION = "description";
    private static final String IDENTITY = "identity";

    private FabricChatClefTaskSnapshotIdentityPayloadMap() {
    }

    public static void writeTo(
            Map<String, Object> payload,
            String className,
            String description,
            String identity
    ) {
        payload.put(CLASS_NAME, className);
        payload.put(DESCRIPTION, description);
        payload.put(IDENTITY, identity);
    }
}
