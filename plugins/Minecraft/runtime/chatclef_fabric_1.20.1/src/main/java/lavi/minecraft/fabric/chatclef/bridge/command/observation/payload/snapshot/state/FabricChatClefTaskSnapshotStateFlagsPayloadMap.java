package lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.snapshot.state;

import lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.snapshot.state.flags.FabricChatClefTaskSnapshotActiveFlagPayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.snapshot.state.flags.FabricChatClefTaskSnapshotStoppedFlagPayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.snapshot.state.flags.FabricChatClefTaskSnapshotTimedOutFlagPayloadMap;

import java.util.Map;

//20260809_kpopmodder: Keep task-state boolean flags separate without changing snapshot keys.
public final class FabricChatClefTaskSnapshotStateFlagsPayloadMap {
    private FabricChatClefTaskSnapshotStateFlagsPayloadMap() {
    }

    public static void writeTo(
            Map<String, Object> payload,
            boolean taskActive,
            boolean taskStopped,
            boolean thisOrChildTimedOut
    ) {
        FabricChatClefTaskSnapshotActiveFlagPayloadMap.writeTo(payload, taskActive);
        FabricChatClefTaskSnapshotStoppedFlagPayloadMap.writeTo(payload, taskStopped);
        FabricChatClefTaskSnapshotTimedOutFlagPayloadMap.writeTo(payload, thisOrChildTimedOut);
    }
}
