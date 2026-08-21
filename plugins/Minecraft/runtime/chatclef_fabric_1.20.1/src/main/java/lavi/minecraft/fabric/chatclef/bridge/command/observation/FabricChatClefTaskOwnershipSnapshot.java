package lavi.minecraft.fabric.chatclef.bridge.command.observation;

import lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.FabricChatClefTaskOwnershipSnapshotPayloadMap;

import java.util.Map;

//20260808_kpopmodder: Keep UserTaskChain ownership snapshots typed for enqueue/dequeue diagnostics.
public final class FabricChatClefTaskOwnershipSnapshot {
    private final boolean available;
    private final String error;
    private final long capturedAtMs;
    private final long capturedClientTick;
    private final String captureThread;
    private final FabricChatClefTaskSnapshot userTaskRoot;
    private final String userTaskRootClass;
    private final String userTaskRootIdentity;
    private final String userTaskRootAssignmentId;
    private final long userTaskRootGeneration;
    private final boolean userTaskRunningIdle;
    private final boolean nextTaskIdleFlag;
    private final boolean taskRunnerActive;
    private final String selectedChainClass;
    private final String selectedChainIdentity;
    private final boolean selectedChainIsUserTaskChain;
    private final String selectedChainTaskPath;

    private FabricChatClefTaskOwnershipSnapshot(
            boolean available,
            String error,
            long capturedAtMs,
            long capturedClientTick,
            String captureThread,
            FabricChatClefTaskSnapshot userTaskRoot,
            String userTaskRootClass,
            String userTaskRootIdentity,
            String userTaskRootAssignmentId,
            long userTaskRootGeneration,
            boolean userTaskRunningIdle,
            boolean nextTaskIdleFlag,
            boolean taskRunnerActive,
            String selectedChainClass,
            String selectedChainIdentity,
            boolean selectedChainIsUserTaskChain,
            String selectedChainTaskPath
    ) {
        this.available = available;
        this.error = error;
        this.capturedAtMs = capturedAtMs;
        this.capturedClientTick = capturedClientTick;
        this.captureThread = captureThread;
        this.userTaskRoot = userTaskRoot;
        this.userTaskRootClass = userTaskRootClass;
        this.userTaskRootIdentity = userTaskRootIdentity;
        this.userTaskRootAssignmentId = userTaskRootAssignmentId;
        this.userTaskRootGeneration = userTaskRootGeneration;
        this.userTaskRunningIdle = userTaskRunningIdle;
        this.nextTaskIdleFlag = nextTaskIdleFlag;
        this.taskRunnerActive = taskRunnerActive;
        this.selectedChainClass = selectedChainClass;
        this.selectedChainIdentity = selectedChainIdentity;
        this.selectedChainIsUserTaskChain = selectedChainIsUserTaskChain;
        this.selectedChainTaskPath = selectedChainTaskPath;
    }

    public static FabricChatClefTaskOwnershipSnapshot of(
            long capturedAtMs,
            long capturedClientTick,
            String captureThread,
            FabricChatClefTaskSnapshot userTaskRoot,
            String userTaskRootClass,
            String userTaskRootIdentity,
            String userTaskRootAssignmentId,
            long userTaskRootGeneration,
            boolean userTaskRunningIdle,
            boolean nextTaskIdleFlag,
            boolean taskRunnerActive,
            String selectedChainClass,
            String selectedChainIdentity,
            boolean selectedChainIsUserTaskChain,
            String selectedChainTaskPath
    ) {
        return new FabricChatClefTaskOwnershipSnapshot(
                true,
                "",
                capturedAtMs,
                capturedClientTick,
                captureThread,
                userTaskRoot,
                userTaskRootClass,
                userTaskRootIdentity,
                userTaskRootAssignmentId,
                userTaskRootGeneration,
                userTaskRunningIdle,
                nextTaskIdleFlag,
                taskRunnerActive,
                selectedChainClass,
                selectedChainIdentity,
                selectedChainIsUserTaskChain,
                selectedChainTaskPath
        );
    }

    public static FabricChatClefTaskOwnershipSnapshot unavailable(Throwable error) {
        return new FabricChatClefTaskOwnershipSnapshot(
                false,
                error.getClass().getSimpleName() + ": " + nullSafeMessage(error),
                System.currentTimeMillis(),
                -1L,
                Thread.currentThread().getName(),
                FabricChatClefTaskSnapshot.capture(null),
                "",
                "",
                "unavailable",
                -1L,
                false,
                false,
                false,
                "",
                "",
                false,
                "unavailable"
        );
    }

    public static FabricChatClefTaskOwnershipSnapshot empty() {
        return new FabricChatClefTaskOwnershipSnapshot(
                false,
                "",
                0L,
                0L,
                "",
                FabricChatClefTaskSnapshot.capture(null),
                "",
                "",
                "none",
                0L,
                false,
                false,
                false,
                "",
                "",
                false,
                ""
        );
    }

    public boolean available() {
        return available;
    }

    public long capturedAtMs() {
        return capturedAtMs;
    }

    public long capturedClientTick() {
        return capturedClientTick;
    }

    public String userTaskRootIdentity() {
        return userTaskRootIdentity;
    }

    public String userTaskRootAssignmentId() {
        return userTaskRootAssignmentId;
    }

    public String userTaskRootClass() {
        return userTaskRootClass;
    }

    public long userTaskRootGeneration() {
        return userTaskRootGeneration;
    }

    public boolean userTaskRunningIdle() {
        return userTaskRunningIdle;
    }

    public boolean nextTaskIdleFlag() {
        return nextTaskIdleFlag;
    }

    public boolean taskRunnerActive() {
        return taskRunnerActive;
    }

    public String selectedChainClass() {
        return selectedChainClass;
    }

    public String selectedChainIdentity() {
        return selectedChainIdentity;
    }

    public boolean selectedChainIsUserTaskChain() {
        return selectedChainIsUserTaskChain;
    }

    public String selectedChainTaskPath() {
        return selectedChainTaskPath;
    }

    public Map<String, Object> toMap() {
        return FabricChatClefTaskOwnershipSnapshotPayloadMap.toMap(
                available,
                error,
                capturedAtMs,
                capturedClientTick,
                captureThread,
                userTaskRoot,
                userTaskRootClass,
                userTaskRootIdentity,
                userTaskRootAssignmentId,
                userTaskRootGeneration,
                userTaskRunningIdle,
                nextTaskIdleFlag,
                taskRunnerActive,
                selectedChainClass,
                selectedChainIdentity,
                selectedChainIsUserTaskChain,
                selectedChainTaskPath
        );
    }

    private static String nullSafeMessage(Throwable error) {
        return error.getMessage() == null ? "" : error.getMessage();
    }
}
