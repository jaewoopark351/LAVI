package lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.snapshot;

import lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.snapshot.state.FabricChatClefTaskSnapshotStateAvailabilityPayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.snapshot.state.FabricChatClefTaskSnapshotStateErrorPayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.snapshot.state.FabricChatClefTaskSnapshotStateFlagsPayloadMap;

import java.util.Map;

//20260808_kpopmodder: Keep task runtime state fields separate at the task snapshot Map edge.
public final class FabricChatClefTaskSnapshotStatePayloadMap {
    private FabricChatClefTaskSnapshotStatePayloadMap() {
    }

    public static void writeTo(
            Map<String, Object> payload,
            boolean taskStateAvailable,
            boolean taskActive,
            boolean taskStopped,
            boolean thisOrChildTimedOut,
            String taskStateError
    ) {
        FabricChatClefTaskSnapshotStateAvailabilityPayloadMap.writeTo(
                payload,
                taskStateAvailable
        );
        FabricChatClefTaskSnapshotStateFlagsPayloadMap.writeTo(
                payload,
                taskActive,
                taskStopped,
                thisOrChildTimedOut
        );
        FabricChatClefTaskSnapshotStateErrorPayloadMap.writeTo(
                payload,
                taskStateError
        );
    }
}
