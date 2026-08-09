package lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.snapshot.summary;

import java.util.Map;

//20260809_kpopmodder: Split snapshot availability fields without changing emitted keys.
public final class FabricChatClefTaskSnapshotAvailabilityPayloadMap {
    private static final String AVAILABLE = "available";
    private static final String ERROR = "error";

    private FabricChatClefTaskSnapshotAvailabilityPayloadMap() {
    }

    public static void writeAvailabilityTo(Map<String, Object> payload, boolean available) {
        payload.put(AVAILABLE, available);
    }

    public static void writeErrorTo(Map<String, Object> payload, String error) {
        payload.put(ERROR, error);
    }
}
