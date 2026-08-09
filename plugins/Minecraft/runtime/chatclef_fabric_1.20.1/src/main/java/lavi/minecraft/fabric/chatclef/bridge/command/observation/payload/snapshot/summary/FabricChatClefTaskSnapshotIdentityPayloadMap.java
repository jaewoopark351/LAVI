package lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.snapshot.summary;

import lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.snapshot.summary.identity.FabricChatClefTaskSnapshotClassNamePayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.snapshot.summary.identity.FabricChatClefTaskSnapshotDescriptionPayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.snapshot.summary.identity.FabricChatClefTaskSnapshotIdentityValuePayloadMap;

import java.util.Map;

//20260809_kpopmodder: Split snapshot identity fields without changing emitted keys.
public final class FabricChatClefTaskSnapshotIdentityPayloadMap {
    private FabricChatClefTaskSnapshotIdentityPayloadMap() {
    }

    public static void writeTo(
            Map<String, Object> payload,
            String className,
            String description,
            String identity
    ) {
        FabricChatClefTaskSnapshotClassNamePayloadMap.writeTo(payload, className);
        FabricChatClefTaskSnapshotDescriptionPayloadMap.writeTo(payload, description);
        FabricChatClefTaskSnapshotIdentityValuePayloadMap.writeTo(payload, identity);
    }
}
