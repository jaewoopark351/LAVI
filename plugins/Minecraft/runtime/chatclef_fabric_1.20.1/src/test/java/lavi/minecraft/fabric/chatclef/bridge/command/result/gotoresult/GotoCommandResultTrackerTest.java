//#if MC == 12001
package lavi.minecraft.fabric.chatclef.bridge.command.result.gotoresult;

import adris.altoclef.eventbus.events.TaskFinishedEvent;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandRequest;
import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandOutcomeClassifier;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandTerminationObservation;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefRootOwnershipClassification;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipEvidence;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultDataPayload;
import lavi.minecraft.task.movement.gotopreflight.GotoMaterialPlan.FailureReason;
import lavi.minecraft.task.movement.gotoresult.model.GotoTargetSnapshot;
import lavi.minecraft.task.movement.gotoresult.model.GotoTaskResultSource;
import lavi.minecraft.task.movement.gotoresult.model.GotoTerminalSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

//20260913_kpopmodder: Verify actual result factories and existing lifecycle gates with engine-independent task doubles.
class GotoCommandResultTrackerTest {
    private static final GotoTargetSnapshot TARGET = new GotoTargetSnapshot(500, 80, -950, null, "minecraft:overworld");
    private static final FabricChatClefCommandResultDataPayload BASE = FabricChatClefCommandResultDataPayload.fromMap(
            Map.of("result_reason", "matching_task_finished", "result_fidelity", "callback_plus_matching_user_task_event"));

    @Test void independentBindingHasExactIdentityAndNullableOriginalDimension() {
        SourceTask task = new SourceTask(TARGET, arrived("prepared_goto_terminal"));
        GotoCommandResultTracker tracker = bound(task);
        Map<?, ?> binding = nested(tracker.bindingData(BASE).toMap(), "goto_binding");
        assertEquals(14, binding.size());
        assertEquals("request-a", binding.get("request_id"));
        assertEquals("message-a", binding.get("command_message_id"));
        assertEquals("session-a", binding.get("session_id"));
        assertEquals(3L, binding.get("server_connection_generation"));
        assertEquals(7L, binding.get("java_socket_generation"));
        assertEquals(Integer.toHexString(System.identityHashCode(task)), binding.get("task_identity"));
        assertTrue(binding.containsKey("requested_dimension"));
        assertNull(binding.get("requested_dimension"));
        tracker.bind(context(), "@goto 500 80 -950", new SourceTask(TARGET, null));
        assertEquals(binding, tracker.bindingData(BASE).toMap().get("goto_binding"));
    }

    @Test void bothOwnerEvidenceKindsProjectArrivalWithoutReevaluatingTheGoal() {
        for (String kind : List.of("prepared_goto_terminal", "legacy_get_to_block_terminal")) {
            SourceTask task = new SourceTask(TARGET, arrived(kind));
            var result = bound(task).matchingCompletion(BASE, observation(task), false);
            assertEquals("completed", result.toMap().get("status"));
            assertEquals(true, result.toMap().get("ok"));
            Map<?, ?> terminal = nested(nested(result.toMap(), "data"), "goto_terminal");
            assertEquals(21, terminal.size());
            assertEquals(true, terminal.get("goal_satisfied"));
            assertEquals(kind, terminal.get("evidence_kind"));
            assertEquals(-950, terminal.get("target_z"));
            // Serialization retains the exact snapshot even when a later owner value changes.
            task.terminal = failed("ARRIVAL_LOST");
            assertEquals(terminal, nested(result.toMap(), "data").get("goto_terminal"));
        }
    }

    @Test void everyTypedFailureChangesTheTopLevelStatusAndRetainsTheExactReason() {
        for (FailureReason reason : FailureReason.values()) {
            SourceTask task = new SourceTask(TARGET, failed(reason.name()));
            Map<?, ?> result = bound(task).matchingCompletion(BASE, observation(task), false).toMap();
            assertEquals("failed", result.get("status"), reason.name());
            assertEquals(false, result.get("ok"), reason.name());
            assertEquals("internal_error", result.get("error_code"));
            assertEquals(reason.name(), nested(nested(result, "data"), "goto_terminal").get("failure_reason"));
        }
    }

    @Test void mismatchedRequestsDimensionsAndUnsupportedShapesNeverAcquireABinding() {
        for (String command : List.of("@goto 501 80 -950", "@goto 500 -950", "@goto 80", "@goto nether",
                "@goto 500 80 -950 nether", "@get stone 1", "@goto 500 80 -950 extra")) {
            GotoCommandResultTracker tracker = new GotoCommandResultTracker();
            tracker.bind(context(), command, new SourceTask(TARGET, null));
            assertFalse(tracker.hasBinding(), command);
        }
        GotoCommandResultTracker crossDimension = new GotoCommandResultTracker();
        crossDimension.bind(context(), "@goto 500 80 -950 nether", new SourceTask(
                new GotoTargetSnapshot(500, 80, -950, "nether", "minecraft:overworld"), null));
        assertFalse(crossDimension.hasBinding());
    }

    @Test void missingOutcomeWrongRootStopMarkerAndUnknownStopStateCannotPromoteArrival() {
        SourceTask task = new SourceTask(TARGET, null);
        GotoCommandResultTracker tracker = bound(task);
        assertNull(tracker.matchingCompletion(BASE, observation(task), false));
        task.terminal = arrived("prepared_goto_terminal");
        assertNull(tracker.matchingCompletion(BASE, observation(new SourceTask(TARGET, task.terminal)), false));
        assertNull(tracker.matchingCompletion(BASE, observation(task), true));
        task.wasStopped = true;
        assertNull(tracker.matchingCompletion(BASE, observation(task), false));
        task.stopReadFails = true;
        assertNull(tracker.matchingCompletion(BASE, observation(task), false));
    }

    @Test void contradictorySnapshotsBecomeUnknownInsteadOfCompleted() {
        for (GotoTerminalSnapshot invalid : List.of(
                new GotoTerminalSnapshot("ARRIVED", "ARRIVAL_LOST", true, true, true, "prepared_goto_terminal", "minecraft:overworld"),
                new GotoTerminalSnapshot("ARRIVED", "NONE", true, false, true, "prepared_goto_terminal", "minecraft:overworld"),
                new GotoTerminalSnapshot("ARRIVED", "NONE", true, true, false, "prepared_goto_terminal", "minecraft:overworld"),
                new GotoTerminalSnapshot("ARRIVED", "NONE", true, true, true, "prepared_goto_terminal", "minecraft:the_nether"),
                new GotoTerminalSnapshot("FAILED", "HANDOFF_SHORTAGE", true, true, true, "prepared_goto_terminal", "minecraft:overworld"),
                failed("UNRECOGNIZED"))) {
            SourceTask task = new SourceTask(TARGET, invalid);
            Map<?, ?> result = bound(task).matchingCompletion(BASE, observation(task), false).toMap();
            assertEquals("unknown", result.get("status"));
            assertEquals(false, result.get("ok"));
        }
    }

    @Test void executionEmitsBindingBeforeTerminalAndUsesExistingNaturalCompletionClassifier() {
        SourceTask task = new SourceTask(TARGET, arrived("prepared_goto_terminal"));
        FabricChatClefCommandExecution execution = execution(task);
        Map<?, ?> running = nested(execution.runningResult().toMap(), "data");
        assertEquals(1, running.get("evidence_sequence"));
        assertEquals("dispatch_started", running.get("result_reason"));
        execution.markTaskFinishedObservation(observation(task));
        execution.markFinishCallbackReceived(null); // Existing callback runs after root release.
        Map<?, ?> terminal = new FabricChatClefCommandOutcomeClassifier().classify(execution).result().toMap();
        assertEquals("completed", terminal.get("status"));
        Map<?, ?> evidence = nested(nested(terminal, "data"), "goto_terminal");
        Map<?, ?> binding = nested(running, "goto_binding");
        binding.forEach((key, value) -> assertEquals(value, evidence.get(key), String.valueOf(key)));
        execution.context().commitTerminalPayload(() -> execution.completedFromTaskFinished(observation(task)));
        assertTrue(execution.context().terminalPayloadCommitted());
    }

    @Test void initialRunningOrderChangesOnlyForSameDimensionDirectXyz() {
        assertTrue(GotoCommandTargetMatcher.sameDimensionDirectXyz("@goto 500 80 -950", "minecraft:overworld"));
        assertTrue(GotoCommandTargetMatcher.sameDimensionDirectXyz("@goto 500 80 -950 overworld", "minecraft:overworld"));
        assertTrue(GotoCommandTargetMatcher.sameDimensionDirectXyz("@goto 500 80 -950 END", "minecraft:the_end"));
        for (String unsupported : List.of("@goto 500 -950", "@goto 80", "@goto nether", "@goto 500 80 -950 nether",
                "@goto 500 80 -950 extra", "@get stone 1", "@goto 999999999999 80 -950")) {
            assertFalse(GotoCommandTargetMatcher.sameDimensionDirectXyz(unsupported, "minecraft:overworld"));
        }
    }

    @Test void dispatchExceptionRetainsItsTerminalOwnershipWithoutALateRunningFrame() {
        SourceTask task = new SourceTask(TARGET, null);
        FabricChatClefCommandExecution execution = execution(task);
        assertTrue(GotoInitialResultOrder.publishAfterAdmission(true, execution.context()));
        var failure = execution.context().commitTerminalPayload(() -> execution.failedFromDispatchException(
                new IllegalStateException("dispatch failed"), FabricChatClefTaskSnapshot.capture(task)));
        assertEquals("failed", failure.toMap().get("status"));
        assertFalse(GotoInitialResultOrder.publishAfterAdmission(true, execution.context()));
        assertFalse(GotoInitialResultOrder.publishAfterAdmission(false, context()));
        assertSame(failure, execution.context().commitTerminalPayload(execution::runningResult));
    }

    @Test void existingStoppedAndMismatchedRootClassificationsRemainNonSuccess() {
        SourceTask task = new SourceTask(TARGET, arrived("prepared_goto_terminal"));
        FabricChatClefCommandExecution execution = execution(task);
        execution.markFinishCallbackReceived(null);
        task.wasStopped = true;
        execution.markTaskFinishedObservation(observation(task));
        Map<?, ?> stopped = new FabricChatClefCommandOutcomeClassifier().classify(execution).result().toMap();
        assertEquals("failed", stopped.get("status"));
        assertFalse(nested(stopped, "data").containsKey("goto_terminal"));
        execution.markTaskFinishedObservation(observation(new SourceTask(TARGET, arrived("prepared_goto_terminal"))));
        assertEquals("unknown", new FabricChatClefCommandOutcomeClassifier().classify(execution).result().toMap().get("status"));
    }

    private FabricChatClefCommandExecution execution(SourceTask task) {
        var result = new FabricChatClefCommandExecution(context(), "@goto 500 80 -950", FabricChatClefTaskOwnershipEvidence.empty());
        result.markDispatchReturned(FabricChatClefTaskOwnershipEvidence.of(task, FabricChatClefTaskSnapshot.capture(task),
                FabricChatClefTaskOwnershipSnapshot.empty(), 1, 1, 1, "test"), FabricChatClefRootOwnershipClassification.COMMAND_OWNED_ROOT);
        return result;
    }

    private static FabricChatClefCommandContext context() {
        FabricChatClefCommandRequest request = new FabricChatClefCommandRequest();
        request.requestId = "request-a"; request.command = "goto 500 80 -950";
        return new FabricChatClefCommandContext(request, "message-a", "session-a", 3, 7);
    }
    private GotoCommandResultTracker bound(SourceTask task) {
        var tracker = new GotoCommandResultTracker(); tracker.bind(context(), "@goto 500 80 -950", task); return tracker;
    }
    private static FabricChatClefCommandTerminationObservation observation(SourceTask task) {
        return FabricChatClefCommandTerminationObservation.fromTaskFinishedEvent(new TaskFinishedEvent(1, task));
    }
    private static Map<?, ?> nested(Map<?, ?> source, String key) { return (Map<?, ?>) source.get(key); }
    private static GotoTerminalSnapshot arrived(String kind) {
        return new GotoTerminalSnapshot("ARRIVED", "NONE", true, true, true, kind, "minecraft:overworld");
    }
    private static GotoTerminalSnapshot failed(String reason) {
        return new GotoTerminalSnapshot("FAILED", reason, false, false, false, "prepared_goto_terminal", null);
    }

}
//#endif
