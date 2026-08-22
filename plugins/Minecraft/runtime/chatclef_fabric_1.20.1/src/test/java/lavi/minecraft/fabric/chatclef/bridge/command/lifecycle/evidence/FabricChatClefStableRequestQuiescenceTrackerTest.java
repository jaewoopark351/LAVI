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

        tracker.observe(execution, evidence(neutralRoot, 1L), "waiting_for_task_finished_event", 1000L, 1_000_000_000L, 1L, execution.context());
        tracker.observe(execution, evidence(neutralRoot, 2L), "waiting_for_task_finished_event", 1200L, 1_200_000_000L, 2L, execution.context());
        FabricChatClefStableRequestQuiescenceObservation observation =
                tracker.observe(execution, evidence(neutralRoot, 3L, 1_575_000_000L), "waiting_for_task_finished_event", 1600L, 1_600_000_000L, 3L, execution.context());
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
                1L,
                execution.context()
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
                1L,
                execution.context()
        );
        Map<String, Object> payload = observation.toMap();

        assertFalse(observation.qualified());
        assertEquals("current_ownership_stale", payload.get("blocked_reason"));
        assertEquals(Long.MAX_VALUE, payload.get("snapshot_age_ms"));
    }

    @Test
    void exactRequestRootIdentityIsObservedBeforeNeutrality() {
        IdleTask commandRoot = new IdleTask();
        FabricChatClefCommandExecution execution = executionFor(commandRoot);
        FabricChatClefStableRequestQuiescenceTracker tracker = new FabricChatClefStableRequestQuiescenceTracker();

        FabricChatClefStableRequestQuiescenceObservation observation = tracker.observe(
                execution,
                evidence(commandRoot, 1L),
                "waiting_for_task_finished_event",
                1000L,
                1_000_000_000L,
                1L,
                execution.context()
        );
        Map<String, Object> payload = observation.toMap();

        assertFalse(observation.qualified());
        assertEquals("request_root_reappeared", payload.get("blocked_reason"));
        assertEquals(true, payload.get("request_root_reappeared"));
        assertEquals("STILL_PRESENT", payload.get("request_root_observation_state"));
    }

    @Test
    void requestRootReappearedLatchSurvivesWindowResetAndBlocksLaterNeutralRoot() {
        Task commandRoot = new TestTask("command-root");
        IdleTask neutralRoot = new IdleTask();
        FabricChatClefCommandExecution execution = executionFor(commandRoot);
        FabricChatClefStableRequestQuiescenceTracker tracker = new FabricChatClefStableRequestQuiescenceTracker();

        FabricChatClefStableRequestQuiescenceObservation first = tracker.observe(
                execution,
                evidence(commandRoot, 1L),
                "waiting_for_task_finished_event",
                1000L,
                1_000_000_000L,
                1L,
                execution.context()
        );
        assertEquals("current_root_not_neutral", first.toMap().get("blocked_reason"));
        assertEquals("STILL_PRESENT", first.toMap().get("request_root_observation_state"));
        tracker.observe(execution, evidence(neutralRoot, 2L), "different_waiting_reason", 1200L, 1_200_000_000L, 2L, execution.context());
        tracker.observe(execution, evidence(neutralRoot, 3L), "waiting_for_task_finished_event", 1400L, 1_400_000_000L, 3L, execution.context());
        tracker.observe(execution, evidence(neutralRoot, 4L), "waiting_for_task_finished_event", 1600L, 1_600_000_000L, 4L, execution.context());
        FabricChatClefStableRequestQuiescenceObservation later = tracker.observe(
                execution,
                evidence(neutralRoot, 5L),
                "waiting_for_task_finished_event",
                2000L,
                2_000_000_000L,
                5L,
                execution.context()
        );
        Map<String, Object> payload = later.toMap();

        assertFalse(later.qualified());
        assertEquals("request_root_reappeared", payload.get("blocked_reason"));
        assertEquals(true, payload.get("request_root_reappeared"));
        assertEquals("OBSERVED_AND_GONE", payload.get("request_root_observation_state"));
    }

    @Test
    void executionReplacementClearsRequestRootReappearedLatch() {
        Task firstCommandRoot = new TestTask("first-command-root");
        Task secondCommandRoot = new TestTask("second-command-root");
        IdleTask neutralRoot = new IdleTask();
        FabricChatClefStableRequestQuiescenceTracker tracker = new FabricChatClefStableRequestQuiescenceTracker();
        FabricChatClefCommandExecution firstExecution = executionFor(firstCommandRoot, context("req-quiescence-a", "session-quiescence", 1L));
        FabricChatClefCommandExecution secondExecution = executionFor(secondCommandRoot, context("req-quiescence-b", "session-quiescence", 1L));

        tracker.observe(firstExecution, evidence(firstCommandRoot, 1L), "waiting_for_task_finished_event", 1000L, 1_000_000_000L, 1L, firstExecution.context());
        tracker.observe(secondExecution, evidence(neutralRoot, 2L), "waiting_for_task_finished_event", 1200L, 1_200_000_000L, 2L, secondExecution.context());
        tracker.observe(secondExecution, evidence(neutralRoot, 3L), "waiting_for_task_finished_event", 1400L, 1_400_000_000L, 3L, secondExecution.context());
        FabricChatClefStableRequestQuiescenceObservation observation = tracker.observe(
                secondExecution,
                evidence(neutralRoot, 4L),
                "waiting_for_task_finished_event",
                1800L,
                1_800_000_000L,
                4L,
                secondExecution.context()
        );
        Map<String, Object> payload = observation.toMap();

        assertTrue(observation.qualified());
        assertEquals(false, payload.get("request_root_reappeared"));
        assertEquals("OBSERVED_AND_GONE", payload.get("request_root_observation_state"));
    }

    @Test
    void activeContextMustBeCurrentExecutionContextObject() {
        IdleTask commandRoot = new IdleTask();
        IdleTask neutralRoot = new IdleTask();
        FabricChatClefCommandExecution execution = executionFor(commandRoot);
        FabricChatClefStableRequestQuiescenceTracker tracker = new FabricChatClefStableRequestQuiescenceTracker();

        FabricChatClefStableRequestQuiescenceObservation observation = tracker.observe(
                execution,
                evidence(neutralRoot, 1L),
                "waiting_for_task_finished_event",
                1000L,
                1_000_000_000L,
                1L,
                context()
        );
        Map<String, Object> payload = observation.toMap();

        assertFalse(observation.qualified());
        assertEquals("active_context_identity_mismatch", payload.get("blocked_reason"));
        assertEquals(false, payload.get("same_session_generation"));
    }

    @Test
    void activeContextRequiresNonblankSessionAndNonnegativeGeneration() {
        IdleTask commandRoot = new IdleTask();
        IdleTask neutralRoot = new IdleTask();
        FabricChatClefStableRequestQuiescenceTracker tracker = new FabricChatClefStableRequestQuiescenceTracker();
        FabricChatClefCommandExecution blankSessionExecution =
                executionFor(commandRoot, context("req-blank-session", "", 1L));
        FabricChatClefCommandExecution negativeGenerationExecution =
                executionFor(commandRoot, context("req-negative-generation", "session-quiescence", -1L));

        FabricChatClefStableRequestQuiescenceObservation blankSession = tracker.observe(
                blankSessionExecution,
                evidence(neutralRoot, 1L),
                "waiting_for_task_finished_event",
                1000L,
                1_000_000_000L,
                1L,
                blankSessionExecution.context()
        );
        FabricChatClefStableRequestQuiescenceObservation negativeGeneration = tracker.observe(
                negativeGenerationExecution,
                evidence(neutralRoot, 2L),
                "waiting_for_task_finished_event",
                1200L,
                1_200_000_000L,
                2L,
                negativeGenerationExecution.context()
        );

        assertEquals("active_context_identity_mismatch", blankSession.toMap().get("blocked_reason"));
        assertEquals(false, blankSession.toMap().get("same_session_generation"));
        assertEquals("active_context_identity_mismatch", negativeGeneration.toMap().get("blocked_reason"));
        assertEquals(false, negativeGeneration.toMap().get("same_session_generation"));
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
                1L,
                null
        );
        Map<String, Object> payload = observation.toMap();

        assertFalse(observation.qualified());
        assertEquals("missing_execution", payload.get("blocked_reason"));
        assertEquals(false, payload.get("same_session_generation"));
    }

    private static FabricChatClefCommandExecution executionFor(Task commandRoot) {
        return executionFor(commandRoot, context());
    }

    private static FabricChatClefCommandExecution executionFor(
            Task commandRoot,
            FabricChatClefCommandContext context
    ) {
        FabricChatClefCommandExecution execution = new FabricChatClefCommandExecution(
                context,
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
        return context("req-quiescence", "session-quiescence", 1L);
    }

    private static FabricChatClefCommandContext context(
            String requestId,
            String sessionId,
            long connectionGeneration
    ) {
        FabricChatClefCommandRequest request = new FabricChatClefCommandRequest();
        request.requestId = requestId;
        request.command = "deposit diamond 2";
        request.source = "test";
        return new FabricChatClefCommandContext(request, "corr-quiescence", sessionId, connectionGeneration);
    }

    private static final class TestTask extends Task {
        private final String name;

        private TestTask(String name) {
            this.name = name;
        }

        @Override
        protected void onStart() {
        }

        @Override
        protected Task onTick() {
            return null;
        }

        @Override
        protected void onStop(Task interruptTask) {
        }

        @Override
        protected boolean isEqual(Task other) {
            return this == other;
        }

        @Override
        protected String toDebugString() {
            return name;
        }
    }
}
