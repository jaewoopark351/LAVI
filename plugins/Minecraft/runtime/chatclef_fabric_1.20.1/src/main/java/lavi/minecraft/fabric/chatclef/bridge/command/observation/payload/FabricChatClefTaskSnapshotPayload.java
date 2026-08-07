package lavi.minecraft.fabric.chatclef.bridge.command.observation.payload;

import java.util.Map;

//20260805_kpopmodder: Isolate task snapshot diagnostic payload keys without changing emitted fields.
public final class FabricChatClefTaskSnapshotPayload {
    private final boolean available;
    private final String className;
    private final String description;
    private final String identity;
    private final String error;
    private final boolean taskStateAvailable;
    private final boolean taskActive;
    private final boolean taskStopped;
    private final boolean thisOrChildTimedOut;
    private final String taskStateError;

    public FabricChatClefTaskSnapshotPayload(
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
        this.available = available;
        this.className = className;
        this.description = description;
        this.identity = identity;
        this.error = error;
        this.taskStateAvailable = taskStateAvailable;
        this.taskActive = taskActive;
        this.taskStopped = taskStopped;
        this.thisOrChildTimedOut = thisOrChildTimedOut;
        this.taskStateError = taskStateError;
    }

    public Map<String, Object> toMap() {
        return FabricChatClefTaskSnapshotPayloadMap.toMap(
                available,
                className,
                description,
                identity,
                error,
                taskStateAvailable,
                taskActive,
                taskStopped,
                thisOrChildTimedOut,
                taskStateError
        );
    }
}
