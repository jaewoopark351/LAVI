package lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.snapshot;

import lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.snapshot.summary.FabricChatClefTaskSnapshotAvailabilityPayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.snapshot.summary.FabricChatClefTaskSnapshotIdentityPayloadMap;

import java.util.Map;

//20260808_kpopmodder: Keep task snapshot summary fields grouped without changing emitted keys.
public final class FabricChatClefTaskSnapshotSummaryPayloadMap {
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
        FabricChatClefTaskSnapshotAvailabilityPayloadMap.writeAvailabilityTo(
                payload,
                available
        );
        FabricChatClefTaskSnapshotIdentityPayloadMap.writeTo(
                payload,
                className,
                description,
                identity
        );
        FabricChatClefTaskSnapshotAvailabilityPayloadMap.writeErrorTo(
                payload,
                error
        );
    }
}
