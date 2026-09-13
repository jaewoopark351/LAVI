package lavi.minecraft.fabric.chatclef.bridge.command.execution;

import adris.altoclef.eventbus.events.TaskFinishedEvent;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandRequest;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlIdentity;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandOutcomeClassifier;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandTerminationObservation;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefRootOwnershipClassification;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.root.capture.UserRootSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.root.capture.UserRootReadStatus;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.execution.FabricChatClefActiveExecutionStore;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipEvidence;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.result.effect.FabricChatClefNoEffectTracker;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendOutcome;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendStatus;
import lavi.minecraft.integration.lifecycle.root.UserRootCompletion;
import lavi.minecraft.integration.lifecycle.root.UserRootLifetime;
import lavi.minecraft.integration.lifecycle.root.UserRootOwnership;
import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

//20260913_kpopmodder: Exercise real execution/classifier/payload ownership across replacement, delayed events and retries.
class FabricChatClefRootTerminationIntegrationTest {
    private final Object engine = new Object(), chain = new Object(), world = new Object(), player = new Object();
    private final FabricChatClefCommandOutcomeClassifier classifier = new FabricChatClefCommandOutcomeClassifier();

    @Test void actualRootReplacementEndsMissingCallbackWaitAsFailure() {
        var owner = new UserRootOwnership();
        var root = new TestTask();
        var execution = execution(root, owner, "get gold_ingot 10");
        var replacement = new TestTask();
        owner.assigned(replacement, null, world, player);
        execution.rootTermination().observe(snapshot(replacement, owner), 0, 0);
        var decision = classifier.classify(execution);
        assertTrue(decision.terminal());
        assertEquals("command_root_replaced", decision.reason());
        assertEquals("failed", decision.result().toMap().get("status"));
        assertFalse((Boolean) decision.result().toMap().get("ok"));
        assertFalse(replacement.stopped());
    }
    @Test void thirtyThirdMatchingCompletionWinsAgainstAutomaticIdle() {
        var owner = new UserRootOwnership();
        var root = new TestTask();
        var execution = execution(root, owner, "get gold_ingot 10");
        var lifetime = owner.currentFor(root);
        owner.finished(lifetime, UserRootCompletion.known(false));
        var idle = new TestTask();
        owner.assigned(idle, null, world, player);
        execution.markFinishCallbackReceived(null);
        execution.rootTermination().observe(snapshot(idle, owner), 33, 32);
        assertEquals("pending_root_completion_prefix", classifier.classify(execution).reason());
        var observation = observation(root, lifetime);
        execution.markTaskFinishedObservation(observation);
        execution.rootTermination().observe(snapshot(idle, owner), 100, 33);
        var result = classifier.classify(execution);
        assertTrue(result.terminal());
        assertEquals("matching_task_finished", result.reason());
        assertEquals("completed", result.result().toMap().get("status"));
    }
    @Test void equalRootCallbackReplacementIsNotNaturalCompletionOrRootReplacement() {
        var owner = new UserRootOwnership();
        var root = new TestTask();
        var execution = execution(root, owner, "get gold_ingot 10");
        owner.assigned(root, new Object(), world, player);
        execution.rootTermination().observe(snapshot(root, owner), 0, 0);
        var decision = classifier.classify(execution);
        assertEquals("command_callback_ownership_replaced", decision.reason());
        assertEquals("failed", decision.result().toMap().get("status"));
        assertFalse(root.stopped(), "result projection must not stop the retained task");
    }
    @Test void lateOldLifetimeAndDuplicateEventsCannotOverwriteMatchingEvent() {
        var owner = new UserRootOwnership();
        var root = new TestTask();
        var oldExecution = execution(root, owner, "get gold_ingot 10");
        var oldLifetime = owner.currentFor(root);
        var current = execution(root, owner, "get gold_ingot 11");
        var newLifetime = owner.currentFor(root);
        current.markTaskFinishedObservation(observation(root, oldLifetime));
        assertNull(current.taskFinishedObservation());
        var matching = observation(root, newLifetime);
        current.markTaskFinishedObservation(matching);
        current.markTaskFinishedObservation(observation(root, oldLifetime));
        current.markTaskFinishedObservation(observation(root, newLifetime));
        assertSame(matching, current.taskFinishedObservation());
        oldExecution.markFinishCallbackReceived(root);
        assertFalse(current.finishCallbackReceived());
        var store = new FabricChatClefActiveExecutionStore(new AtomicReference<>(current));
        assertFalse(store.clearIfCurrent(oldExecution));
        assertSame(current, store.current());
    }
    @Test void eventDequeueCopyPreservesOwnershipIdentity() {
        var owner = new UserRootOwnership();
        var root = new TestTask();
        execution(root, owner, "get gold_ingot 10");
        var lifetime = owner.currentFor(root);
        var copy = observation(root, lifetime).withDequeueMetadata(1234, 5, 0, FabricChatClefTaskOwnershipSnapshot.empty());
        assertSame(lifetime, copy.rootLifetime());
    }
    @Test void realFinishWithoutPublishedEventIsUnknownInsteadOfReplacementFailureOrFakeSuccess() {
        var owner = new UserRootOwnership();
        var root = new TestTask();
        var execution = execution(root, owner, "get gold_ingot 10");
        var lifetime = owner.currentFor(root);
        owner.finished(lifetime, UserRootCompletion.known(false));
        execution.markFinishCallbackReceived(null);
        owner.assigned(new TestTask(), null, world, player);
        execution.rootTermination().observe(snapshot(null, owner), 0, 0);
        var result = classifier.classify(execution);
        assertEquals("unknown", result.result().toMap().get("status"));
        assertEquals("root_completed_without_complete_event_handoff", result.reason());
    }
    @Test void explicitStopOwnsCancellationWhileToolFailureUsesFrozenOwnerReason() {
        var owner = new UserRootOwnership();
        var root = new TestTask();
        var execution = execution(root, owner, "get gold_ingot 10");
        var lifetime = owner.currentFor(root);
        owner.finished(lifetime, UserRootCompletion.known(true, "HOTBAR_CONFIRMATION_TIMEOUT"));
        execution.rootTermination().observe(snapshot(null, owner), 0, 0);
        var result = classifier.classify(execution).result().toMap();
        assertEquals("failed", result.get("status"));
        var data = (Map<?, ?>) result.get("data");
        assertEquals("mining_tool_preparation_failed", data.get("result_reason"));
        assertEquals("HOTBAR_CONFIRMATION_TIMEOUT", data.get("mining_tool_failure_reason"));
        assertTrue(execution.bindUserStop(new FabricChatClefStopControlIdentity("session", 2, "stop", "stop-message"), root));
        assertEquals("cancelled", classifier.classify(execution).result().toMap().get("status"));
    }
    @Test void retryReusesCommittedFailureEvenWhenLateCompletionArrives() {
        var owner = new UserRootOwnership();
        var root = new TestTask();
        var execution = execution(root, owner, "get gold_ingot 10");
        var lifetime = owner.currentFor(root);
        var replacement = new TestTask();
        owner.assigned(replacement, null, world, player);
        execution.rootTermination().observe(snapshot(replacement, owner), 0, 0);
        var committed = execution.context().commitTerminalPayload(() -> classifier.classify(execution).result());
        assertTrue(execution.context().beginTerminalSend(1));
        execution.context().completeTerminalSend(FabricChatClefCommandResultSendOutcome.failed(
                FabricChatClefCommandResultSendStatus.ASYNC_SEND_FAILED, "ambiguous delivery"));
        execution.markFinishCallbackReceived(null);
        execution.markTaskFinishedObservation(observation(root, lifetime));
        assertSame(committed, execution.context().commitTerminalPayload(() -> classifier.classify(execution).result()));
        assertTrue(execution.context().beginTerminalSend(Long.MAX_VALUE));
        execution.context().completeTerminalSend(FabricChatClefCommandResultSendOutcome.sent());
        assertFalse(execution.context().beginTerminalSend(Long.MAX_VALUE));
        assertEquals("failed", committed.toMap().get("status"));
    }
    @Test void gotoKeepsExistingOutcomeOwnerAndDoesNotAdoptRootRetirementFallback() {
        var owner = new UserRootOwnership();
        var execution = execution(new TestTask(), owner, "goto 500 80 -950");
        assertFalse(execution.rootTermination().bound());
        assertEquals("waiting_for_task_finished_event", classifier.classify(execution).reason());
    }
    private FabricChatClefCommandExecution execution(TestTask root, UserRootOwnership owner, String command) {
        var request = new FabricChatClefCommandRequest();
        request.requestId = "root-test";
        request.command = command;
        request.source = "test";
        var context = new FabricChatClefCommandContext(request, "correlation", "session", 2, 3);
        var execution = new FabricChatClefCommandExecution(context, command,
                FabricChatClefTaskOwnershipEvidence.empty(), ignored -> FabricChatClefNoEffectTracker.instance());
        owner.assigned(root, new Object(), world, player);
        execution.markDispatchReturned(evidence(root), FabricChatClefRootOwnershipClassification.COMMAND_OWNED_ROOT);
        execution.bindRootLifetime(snapshot(root, owner));
        return execution;
    }
    private UserRootSnapshot snapshot(Object root, UserRootOwnership owner) {
        return new UserRootSnapshot(root == null ? UserRootReadStatus.ROOT_ABSENT : UserRootReadStatus.PRESENT,
                engine, chain, world, player, root, owner.currentFor(root), "");
    }
    private FabricChatClefCommandTerminationObservation observation(Task root, UserRootLifetime lifetime) {
        return FabricChatClefCommandTerminationObservation.fromTaskFinishedEvent(new TaskFinishedEvent(1, root, lifetime));
    }
    private FabricChatClefTaskOwnershipEvidence evidence(Task root) {
        var task = FabricChatClefTaskSnapshot.capture(root);
        var ownership = FabricChatClefTaskOwnershipSnapshot.of(1, 1, "test", task,
                root.getClass().getName(), Integer.toHexString(System.identityHashCode(root)), "diagnostic-only", 1,
                false, false, true, "", "", false, "");
        return FabricChatClefTaskOwnershipEvidence.of(root, task, ownership, 1, 1, 1, "test");
    }
    private static final class TestTask extends Task {
        @Override protected void onStart() { }
        @Override protected Task onTick() { return null; }
        @Override protected void onStop(Task interruptTask) { }
        @Override protected boolean isEqual(Task other) { return other instanceof TestTask; }
        @Override protected String toDebugString() { return "root-termination-test"; }
    }
}
