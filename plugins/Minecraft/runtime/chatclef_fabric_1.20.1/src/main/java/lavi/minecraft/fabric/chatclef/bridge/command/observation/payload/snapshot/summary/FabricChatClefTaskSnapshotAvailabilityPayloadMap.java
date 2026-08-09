package lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.snapshot.summary;

import java.util.Map;

//20260809_kpopmodder: Split snapshot availability fields without changing emitted keys.
public final class FabricChatClefTaskSnapshotAvailabilityPayloadMap {
    private FabricChatClefTaskSnapshotAvailabilityPayloadMap() {
    }

    public static void writeAvailabilityTo(Map<String, Object> payload, boolean available) {
        FabricChatClefTaskSnapshotAvailablePayloadMap.writeTo(payload, available);
    }

    public static void writeErrorTo(Map<String, Object> payload, String error) {
        FabricChatClefTaskSnapshotErrorPayloadMap.writeTo(payload, error);
    }
}
