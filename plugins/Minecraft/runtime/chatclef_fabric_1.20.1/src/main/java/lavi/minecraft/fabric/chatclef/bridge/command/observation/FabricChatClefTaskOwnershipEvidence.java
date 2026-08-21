package lavi.minecraft.fabric.chatclef.bridge.command.observation;

import adris.altoclef.tasksystem.Task;

import java.util.Map;

//20260822_kpopmodder: Capture command-root ownership evidence from one UserTaskChain root read.
public final class FabricChatClefTaskOwnershipEvidence {
    private final boolean available;
    private final String error;
    private final Task rootTask;
    private final FabricChatClefTaskSnapshot rootTaskSnapshot;
    private final FabricChatClefTaskOwnershipSnapshot ownershipSnapshot;
    private final long capturedAtMs;
    private final long capturedAtNanos;
    private final long capturedClientTick;
    private final String captureThread;

    private FabricChatClefTaskOwnershipEvidence(
            boolean available,
            String error,
            Task rootTask,
            FabricChatClefTaskSnapshot rootTaskSnapshot,
            FabricChatClefTaskOwnershipSnapshot ownershipSnapshot,
            long capturedAtMs,
            long capturedAtNanos,
            long capturedClientTick,
            String captureThread
    ) {
        this.available = available;
        this.error = nullToEmpty(error);
        this.rootTask = rootTask;
        this.rootTaskSnapshot = rootTaskSnapshot == null ? FabricChatClefTaskSnapshot.capture(null) : rootTaskSnapshot;
        this.ownershipSnapshot = ownershipSnapshot == null ? FabricChatClefTaskOwnershipSnapshot.empty() : ownershipSnapshot;
        this.capturedAtMs = capturedAtMs;
        this.capturedAtNanos = capturedAtNanos;
        this.capturedClientTick = capturedClientTick;
        this.captureThread = nullToEmpty(captureThread);
    }

    public static FabricChatClefTaskOwnershipEvidence of(
            Task rootTask,
            FabricChatClefTaskSnapshot rootTaskSnapshot,
            FabricChatClefTaskOwnershipSnapshot ownershipSnapshot,
            long capturedAtMs,
            long capturedAtNanos,
            long capturedClientTick,
            String captureThread
    ) {
        return new FabricChatClefTaskOwnershipEvidence(
                true,
                "",
                rootTask,
                rootTaskSnapshot,
                ownershipSnapshot,
                capturedAtMs,
                capturedAtNanos,
                capturedClientTick,
                captureThread
        );
    }

    public static FabricChatClefTaskOwnershipEvidence unavailable(Throwable error) {
        long nowMs = System.currentTimeMillis();
        return new FabricChatClefTaskOwnershipEvidence(
                false,
                error.getClass().getSimpleName() + ": " + nullSafeMessage(error),
                null,
                FabricChatClefTaskSnapshot.unavailable(error),
                FabricChatClefTaskOwnershipSnapshot.unavailable(error),
                nowMs,
                System.nanoTime(),
                -1L,
                Thread.currentThread().getName()
        );
    }

    public static FabricChatClefTaskOwnershipEvidence empty() {
        return new FabricChatClefTaskOwnershipEvidence(
                false,
                "",
                null,
                FabricChatClefTaskSnapshot.capture(null),
                FabricChatClefTaskOwnershipSnapshot.empty(),
                0L,
                0L,
                0L,
                ""
        );
    }

    public boolean available() {
        return available && ownershipSnapshot.available();
    }

    public String error() {
        return error;
    }

    public Task rootTask() {
        return rootTask;
    }

    public boolean rootTaskPresent() {
        return rootTask != null;
    }

    public FabricChatClefTaskSnapshot rootTaskSnapshot() {
        return rootTaskSnapshot;
    }

    public FabricChatClefTaskOwnershipSnapshot ownershipSnapshot() {
        return ownershipSnapshot;
    }

    public long capturedAtMs() {
        return capturedAtMs;
    }

    public long capturedAtNanos() {
        return capturedAtNanos;
    }

    public long capturedClientTick() {
        return capturedClientTick;
    }

    public String captureThread() {
        return captureThread;
    }

    public String userTaskRootIdentity() {
        return ownershipSnapshot.userTaskRootIdentity();
    }

    public String userTaskRootAssignmentId() {
        return ownershipSnapshot.userTaskRootAssignmentId();
    }

    public String userTaskRootClass() {
        return ownershipSnapshot.userTaskRootClass();
    }

    public long userTaskRootGeneration() {
        return ownershipSnapshot.userTaskRootGeneration();
    }

    public boolean userTaskRunningIdle() {
        return ownershipSnapshot.userTaskRunningIdle();
    }

    public boolean nextTaskIdleFlag() {
        return ownershipSnapshot.nextTaskIdleFlag();
    }

    public Map<String, Object> toMap() {
        return Map.of(
                "available", available(),
                "error", error,
                "captured_at_ms", capturedAtMs,
                "captured_at_nanos", capturedAtNanos,
                "captured_client_tick", capturedClientTick,
                "capture_thread", captureThread,
                "root_task", rootTaskSnapshot.toMap(),
                "ownership", ownershipSnapshot.toMap()
        );
    }

    private static String nullSafeMessage(Throwable error) {
        return error.getMessage() == null ? "" : error.getMessage();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
