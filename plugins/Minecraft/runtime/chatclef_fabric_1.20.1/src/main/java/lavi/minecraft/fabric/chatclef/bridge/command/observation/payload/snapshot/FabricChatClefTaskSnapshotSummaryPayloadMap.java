package lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.snapshot;

import java.util.Map;

//20260808_kpopmodder: Keep task snapshot summary fields grouped without changing emitted keys.
public final class FabricChatClefTaskSnapshotSummaryPayloadMap {
    private static final String AVAILABLE = "available";
    private static final String CLASS_NAME = "class_name";
    private static final String DESCRIPTION = "description";
    private static final String IDENTITY = "identity";
    private static final String ERROR = "error";

    private FabricChatClefTaskSnapshotSummaryPayloadMap() {
    }

    public static void writeTo(
            Map<String, Object> payload,
            boolean available,
            String className,
            String description,
            String identity,
            String error
    ) {
        payload.put(AVAILABLE, available);
        payload.put(CLASS_NAME, className);
        payload.put(DESCRIPTION, description);
        payload.put(IDENTITY, identity);
        payload.put(ERROR, error);
    }
}
