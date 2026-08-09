package lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.snapshot.state.flags;

import java.util.Map;

public final class FabricChatClefTaskSnapshotTimedOutFlagPayloadMap {
    private static final String THIS_OR_CHILD_TIMED_OUT = "this_or_child_timed_out";

    private FabricChatClefTaskSnapshotTimedOutFlagPayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, boolean thisOrChildTimedOut) {
        payload.put(THIS_OR_CHILD_TIMED_OUT, thisOrChildTimedOut);
    }
}
