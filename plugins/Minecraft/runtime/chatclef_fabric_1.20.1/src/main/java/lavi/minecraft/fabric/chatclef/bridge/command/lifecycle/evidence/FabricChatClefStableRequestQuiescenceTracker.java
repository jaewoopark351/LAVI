package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.evidence;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipEvidence;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipSnapshot;

//20260820_kpopmodder: Track stable IdleTask quiescence as diagnostic evidence only.
public final class FabricChatClefStableRequestQuiescenceTracker {
    private static final String POLICY_VERSION = "2026-08-20.java-nonterminal-evidence.v1";
    private static final String SIGNATURE_VERSION = "root-chain-v1";
    private static final String NEUTRAL_ROOT_CLASS = "adris.altoclef.tasks.movement.IdleTask";
    private static final int MIN_NEUTRAL_SNAPSHOTS = 3;
    private static final long MIN_NEUTRAL_DURATION_MS = 500L;
    private static final long MAX_SNAPSHOT_AGE_MS = 1000L;

    private FabricChatClefCommandExecution trackedExecution;
    private String trackedSignature = "";
    private long firstObservedAtNanos;
    private long firstObservedAtMs;
    private long lastObservedAtMs;
    private long firstClientTickId;
    private long lastClientTickId = Long.MIN_VALUE;
    private int observationCount;
    private boolean requestRootReappeared;

    public void reset() {
        trackedExecution = null;
        trackedSignature = "";
        firstObservedAtNanos = 0L;
        firstObservedAtMs = 0L;
        lastObservedAtMs = 0L;
        firstClientTickId = 0L;
        lastClientTickId = Long.MIN_VALUE;
        observationCount = 0;
        requestRootReappeared = false;
    }

    public FabricChatClefStableRequestQuiescenceObservation observe(
            FabricChatClefCommandExecution execution,
            FabricChatClefTaskOwnershipEvidence currentEvidence,
            String waitingReason,
            long nowMs,
            long nowNanos,
            long clientTickId
    ) {
        if (execution != trackedExecution) {
            reset();
            trackedExecution = execution;
        }
        Task currentTask = currentEvidence == null ? null : currentEvidence.rootTask();
        FabricChatClefTaskOwnershipSnapshot ownership = currentEvidence == null
                ? null
                : currentEvidence.ownershipSnapshot();
        String requestRootState = requestRootObservationState(execution, currentTask);
        String blockedReason = preconditionBlockReason(
                execution,
                currentTask,
                currentEvidence,
                ownership,
                waitingReason,
                nowNanos
        );
        if (!blockedReason.isEmpty()) {
            resetWindow();
            return observation(false, blockedReason, requestRootState, currentEvidence, nowMs, nowNanos, clientTickId, execution);
        }
        String signature = signature(execution, currentTask, ownership, waitingReason);
        if (!signature.equals(trackedSignature)) {
            trackedSignature = signature;
            firstObservedAtNanos = nowNanos;
            firstObservedAtMs = nowMs;
            firstClientTickId = clientTickId;
            observationCount = 0;
            requestRootReappeared = false;
        }
        if (clientTickId != lastClientTickId) {
            observationCount++;
            lastClientTickId = clientTickId;
        }
        lastObservedAtMs = nowMs;
        if ("STILL_PRESENT".equals(requestRootState)) {
            requestRootReappeared = true;
        }
        long stableDurationMs = stableDurationMs(nowNanos);
        boolean qualified = observationCount >= MIN_NEUTRAL_SNAPSHOTS
                && stableDurationMs >= MIN_NEUTRAL_DURATION_MS
                && !requestRootReappeared;
        return observation(
                qualified,
                qualified ? "none" : "stable_window_not_satisfied",
                requestRootState,
                currentEvidence,
                nowMs,
                nowNanos,
                clientTickId,
                execution
        );
    }

    private String preconditionBlockReason(
            FabricChatClefCommandExecution execution,
            Task currentTask,
            FabricChatClefTaskOwnershipEvidence currentEvidence,
            FabricChatClefTaskOwnershipSnapshot ownership,
            String waitingReason,
            long nowNanos
    ) {
        if (execution == null) {
            return "missing_execution";
        }
        if (!execution.dispatchReturned()) {
            return "dispatch_not_returned";
        }
        if (!execution.finishCallbackReceived()) {
            return "finish_callback_missing";
        }
        if (execution.taskFinishedObservation() != null) {
            return "task_finished_event_present";
        }
        if (!"waiting_for_task_finished_event".equals(nullToEmpty(waitingReason))) {
            return "waiting_reason_not_reconciliation_candidate";
        }
        if (currentEvidence == null || !currentEvidence.available()) {
            return "current_ownership_unavailable";
        }
        if (snapshotAgeMs(currentEvidence, nowNanos) > MAX_SNAPSHOT_AGE_MS) {
            return "current_ownership_stale";
        }
        if (currentTask == null) {
            return "current_root_unavailable";
        }
        if (!NEUTRAL_ROOT_CLASS.equals(currentTask.getClass().getName())) {
            return "current_root_not_neutral";
        }
        if (ownership == null || !ownership.available()) {
            return "ownership_snapshot_unavailable";
        }
        return "";
    }

    private String requestRootObservationState(
            FabricChatClefCommandExecution execution,
            Task currentTask
    ) {
        if (execution == null || !execution.hasBoundRootTask()) {
            return "NEVER_OBSERVED";
        }
        if (execution.matchesBoundRootTask(currentTask)
                && currentTask != null
                && NEUTRAL_ROOT_CLASS.equals(currentTask.getClass().getName())) {
            return "OBSERVED_NEUTRAL_ROOT_STABLE";
        }
        if (execution.matchesBoundRootTask(currentTask)) {
            return "STILL_PRESENT";
        }
        if (currentTask != null && NEUTRAL_ROOT_CLASS.equals(currentTask.getClass().getName())) {
            return "OBSERVED_AND_GONE";
        }
        return "UNKNOWN";
    }

    private String signature(
            FabricChatClefCommandExecution execution,
            Task currentTask,
            FabricChatClefTaskOwnershipSnapshot ownership,
            String waitingReason
    ) {
        return execution.requestId()
                + "|"
                + execution.context().sessionId()
                + "|"
                + execution.context().connectionGeneration()
                + "|"
                + nullToEmpty(waitingReason)
                + "|current="
                + ChatClefDiagnostics.className(currentTask)
                + "#"
                + taskIdentity(currentTask)
                + "|root="
                + ownership.userTaskRootClass()
                + "#"
                + ownership.userTaskRootIdentity()
                + "#"
                + ownership.userTaskRootGeneration()
                + "|chain="
                + ownership.selectedChainClass()
                + "#"
                + ownership.selectedChainIdentity()
                + "#"
                + ownership.selectedChainTaskPath();
    }

    private FabricChatClefStableRequestQuiescenceObservation observation(
            boolean qualified,
            String blockedReason,
            String requestRootState,
            FabricChatClefTaskOwnershipEvidence currentEvidence,
            long nowMs,
            long nowNanos,
            long clientTickId,
            FabricChatClefCommandExecution execution
    ) {
        long lastObserved = lastObservedAtMs == 0L ? nowMs : lastObservedAtMs;
        long firstObserved = firstObservedAtMs == 0L ? nowMs : firstObservedAtMs;
        long firstTick = firstClientTickId == 0L ? clientTickId : firstClientTickId;
        long finishAt = execution == null ? 0L : execution.finishCallbackReceivedAtMs();
        return FabricChatClefStableRequestQuiescenceObservation.of(
                qualified,
                nullToEmpty(blockedReason),
                observationCount,
                firstObserved,
                lastObserved,
                firstTick,
                clientTickId,
                stableDurationMs(nowNanos),
                finishAt <= 0L ? -1L : Math.max(0L, nowMs - finishAt),
                snapshotAgeMs(currentEvidence, nowNanos),
                sameSessionGeneration(execution),
                requestRootReappeared,
                requestRootState,
                SIGNATURE_VERSION,
                POLICY_VERSION,
                MIN_NEUTRAL_SNAPSHOTS,
                MIN_NEUTRAL_DURATION_MS,
                MAX_SNAPSHOT_AGE_MS
        );
    }

    private long snapshotAgeMs(FabricChatClefTaskOwnershipEvidence currentEvidence, long nowNanos) {
        if (currentEvidence == null || currentEvidence.capturedAtNanos() <= 0L) {
            return Long.MAX_VALUE;
        }
        if (nowNanos < currentEvidence.capturedAtNanos()) {
            return Long.MAX_VALUE;
        }
        return (nowNanos - currentEvidence.capturedAtNanos()) / 1_000_000L;
    }

    private boolean sameSessionGeneration(FabricChatClefCommandExecution execution) {
        return execution != null
                && execution == trackedExecution
                && !nullToEmpty(execution.context().sessionId()).isBlank()
                && execution.context().connectionGeneration() >= 0L;
    }

    private long stableDurationMs(long nowNanos) {
        if (firstObservedAtNanos <= 0L) {
            return 0L;
        }
        return Math.max(0L, (nowNanos - firstObservedAtNanos) / 1_000_000L);
    }

    private void resetWindow() {
        trackedSignature = "";
        firstObservedAtNanos = 0L;
        firstObservedAtMs = 0L;
        lastObservedAtMs = 0L;
        firstClientTickId = 0L;
        lastClientTickId = Long.MIN_VALUE;
        observationCount = 0;
        requestRootReappeared = false;
    }

    private static String taskIdentity(Task task) {
        return task == null ? "none" : Integer.toHexString(System.identityHashCode(task));
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
