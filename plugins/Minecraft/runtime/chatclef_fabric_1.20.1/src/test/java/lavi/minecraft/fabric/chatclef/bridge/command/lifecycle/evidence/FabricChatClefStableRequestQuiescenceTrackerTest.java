package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.evidence;

import adris.altoclef.tasks.movement.IdleTask;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandRequest;
import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefRootOwnershipClassification;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipEvidence;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskSnapshot;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FabricChatClefStableRequestQuiescenceTrackerTest {
    @Test
    void qualifiedObservationUsesEvidenceBasedMonotonicFields() {
        IdleTask commandRoot = new IdleTask();
        IdleTask neutralRoot = new IdleTask();
        FabricChatClefCommandExecution execution = executionFor(commandRoot);
        FabricChatClefStableRequestQuiescenceTracker tracker = new FabricChatClefStableRequestQuiescenceTracker();

        tracker.observe(execution, evidence(neutralRoot, 1L), "waiting_for_task_finished_event", 1000L, 1_000_000_000L, 1L);
        tracker.observe(execution, evidence(neutralRoot, 2L), "waiting_for_task_finished_event", 1200L, 1_200_000_000L, 2L);
        FabricChatClefStableRequestQuiescenceObservation observation =
                tracker.observe(execution, evidence(neutralRoot, 3L, 1_575_000_000L), "waiting_for_task_finished_event", 1600L, 1_600_000_000L, 3L);
        Map<String, Object> payload = observation.toMap();

        assertTrue(observation.qualified());
        assertEquals("none", payload.get("blocked_reason"));
        assertEquals(600L, payload.get("stable_duration_ms"));
        assertEquals(25L, payload.get("snapshot_age_ms"));
        assertEquals(true, payload.get("same_session_generation"));
        assertEquals(false, payload.get("request_root_reappeared"));
        assertEquals("OBSERVED_AND_GONE", payload.get("request_root_observation_state"));
    }

    @Test
    void advertisedSnapshotAgeThresholdIsEnforced() {
        IdleTask commandRoot = new IdleTask();
        IdleTask neutralRoot = new IdleTask();
        FabricChatClefCommandExecution execution = executionFor(commandRoot);
        FabricChatClefStableRequestQuiescenceTracker tracker = new FabricChatClefStableRequestQuiescenceTracker();

        FabricChatClefStableRequestQuiescenceObservation observation = tracker.observe(
                execution,
                evidence(neutralRoot, 1L, 1_000_000_000L),
                "waiting_for_task_finished_event",
                2500L,
                2_500_000_000L,
                1L
        );
        Map<String, Object> payload = observation.toMap();

        assertFalse(observation.qualified());
        assertEquals("current_ownership_stale", payload.get("blocked_reason"));
        assertEquals(1500L, payload.get("snapshot_age_ms"));
    }

    @Test
    void futureMonotonicCaptureDoesNotAppearFresh() {
        IdleTask commandRoot = new IdleTask();
        IdleTask neutralRoot = new IdleTask();
        FabricChatClefCommandExecution execution = executionFor(commandRoot);
        FabricChatClefStableRequestQuiescenceTracker tracker = new FabricChatClefStableRequestQuiescenceTracker();

        FabricChatClefStableRequestQuiescenceObservation observation = tracker.observe(
                execution,
                evidence(neutralRoot, 1L, 3_000_000_000L),
                "waiting_for_task_finished_event",
                2500L,
                2_500_000_000L,
                1L
        );
        Map<String, Object> payload = observation.toMap();

        assertFalse(observation.qualified());
        assertEquals("current_ownership_stale", payload.get("blocked_reason"));
        assertEquals(Long.MAX_VALUE, payload.get("snapshot_age_ms"));
    }

    @Test
    void sameSessionGenerationIsFalseWhenExecutionIsMissing() {
        IdleTask neutralRoot = new IdleTask();
        FabricChatClefStableRequestQuiescenceTracker tracker = new FabricChatClefStableRequestQuiescenceTracker();

        FabricChatClefStableRequestQuiescenceObservation observation = tracker.observe(
                null,
                evidence(neutralRoot, 1L),
                "waiting_for_task_finished_event",
                1000L,
                1_000_000_000L,
                1L
        );
        Map<String, Object> payload = observation.toMap();

        assertFalse(observation.qualified());
        assertEquals("missing_execution", payload.get("blocked_reason"));
        assertEquals(false, payload.get("same_session_generation"));
    }

    private static FabricChatClefCommandExecution executionFor(IdleTask commandRoot) {
        FabricChatClefCommandExecution execution = new FabricChatClefCommandExecution(
                context(),
                "@deposit diamond 2",
                evidence(null, 1L)
        );
        execution.openExecutorExecuteInvocation();
        execution.markFinishCallbackReceived(commandRoot);
        execution.closeExecutorExecuteInvocation();
        execution.markDispatchReturned(evidence(commandRoot, 1L), FabricChatClefRootOwnershipClassification.COMMAND_OWNED_ROOT);
        return execution;
    }

    private static FabricChatClefTaskOwnershipEvidence evidence(Task root, long clientTick) {
        return evidence(root, clientTick, capturedAtNanosForTick(clientTick));
    }

    private static FabricChatClefTaskOwnershipEvidence evidence(
            Task root,
            long clientTick,
            long capturedAtNanos
    ) {
        FabricChatClefTaskSnapshot rootSnapshot = FabricChatClefTaskSnapshot.capture(root);
        long capturedAtMs = capturedAtNanos / 1_000_000L;
        FabricChatClefTaskOwnershipSnapshot ownership = FabricChatClefTaskOwnershipSnapshot.of(
                capturedAtMs,
                clientTick,
                "test",
                rootSnapshot,
                root == null ? "" : root.getClass().getName(),
                root == null ? "none" : Integer.toHexString(System.identityHashCode(root)),
                root == null ? "none" : "user-root-1",
                root == null ? 0L : 1L,
                root instanceof IdleTask,
                false,
                false,
                "",
                "",
                false,
                ""
        );
        return FabricChatClefTaskOwnershipEvidence.of(
                root,
                rootSnapshot,
                ownership,
                capturedAtMs,
                capturedAtNanos,
                clientTick,
                "test"
        );
    }

    private static long capturedAtNanosForTick(long clientTick) {
        return 1_000_000_000L + ((clientTick - 1L) * 200_000_000L) - 25_000_000L;
    }

    private static FabricChatClefCommandContext context() {
        FabricChatClefCommandRequest request = new FabricChatClefCommandRequest();
        request.requestId = "req-quiescence";
        request.command = "deposit diamond 2";
        request.source = "test";
        return new FabricChatClefCommandContext(request, "corr-quiescence", "session-quiescence", 1L);
    }
}
