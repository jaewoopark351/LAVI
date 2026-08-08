package lavi.minecraft.fabric.chatclef.bridge.command.observation.payload;

import lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.snapshot.FabricChatClefTaskSnapshotStatePayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.snapshot.FabricChatClefTaskSnapshotSummaryPayloadMap;

import java.util.HashMap;
import java.util.Map;

//20260807_kpopmodder: Isolate task snapshot Map keys without changing emitted diagnostic fields.
public final class FabricChatClefTaskSnapshotPayloadMap {
    private FabricChatClefTaskSnapshotPayloadMap() {
    }

    public static Map<String, Object> toMap(
            boolean available,
            String className,
            String description,
            String identity,
            String error,
            boolean taskStateAvailable,
            boolean taskActive,
            boolean taskStopped,
            boolean thisOrChildTimedOut,
            String taskStateError
    ) {
        Map<String, Object> payload = new HashMap<>();
        FabricChatClefTaskSnapshotSummaryPayloadMap.writeTo(
                payload,
                available,
                className,
                description,
                identity,
                error
        );
        FabricChatClefTaskSnapshotStatePayloadMap.writeTo(
                payload,
                taskStateAvailable,
                taskActive,
                taskStopped,
                thisOrChildTimedOut,
                taskStateError
        );
        return payload;
    }
}
