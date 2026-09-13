package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.gotoresult;

import adris.altoclef.eventbus.events.TaskFinishedEvent;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandRequest;
import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandTerminationObservation;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefRootOwnershipClassification;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipEvidence;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultStatus;
//#if MC == 12001
import lavi.minecraft.task.movement.gotopreflight.PreparedGotoTask;
import lavi.minecraft.task.movement.gotopreflight.GotoMaterialPlan;
import lavi.minecraft.testsupport.TestObjects;
import lavi.minecraft.task.movement.gotoresult.binding.GotoTaskBinding;
//#endif
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

//20260913_kpopmodder: Verify observational fidelity and one-time commit without starting Minecraft.
class GotoTerminalDiagnosticsTest {
    @Test
    void observationKeepsEveryExistingTerminalPayloadAndDoesNotAddEvidence() {
        for (FabricChatClefCommandResultStatus status : List.of(
                FabricChatClefCommandResultStatus.COMPLETED, FabricChatClefCommandResultStatus.FAILED,
                FabricChatClefCommandResultStatus.CANCELLED, FabricChatClefCommandResultStatus.UNKNOWN)) {
            List<String> lines = new ArrayList<>();
            FabricChatClefCommandResultPayload result = result(status);
            Map<String, Object> before = result.toMap();
            assertSame(result, new GotoTerminalDiagnostics(lines::add).observeResult(execution("@goto 500 90 -928"), result));
            assertEquals(before, result.toMap());
            assertEquals(1, lines.size());
            assertTrue(lines.get(0).contains("status=" + status.wireValue()));
            assertTrue(lines.get(0).contains("owner_arrived=UNAVAILABLE"));
            assertTrue(lines.get(0).contains("target_z=-928"));
            assertFalse(lines.get(0).contains("private_message"));
        }
    }

    @Test
    void loggingDisabledOrBrokenCannotReplaceResultOrRequestRetry() {
        FabricChatClefCommandExecution execution = execution("@goto 500 90 -928");
        FabricChatClefCommandResultPayload result = result(FabricChatClefCommandResultStatus.COMPLETED);
        assertSame(result, new GotoTerminalDiagnostics(line -> {}).observeResult(execution, result));
        assertSame(result, new GotoTerminalDiagnostics(line -> {
            throw new IllegalStateException("sink failure");
        }).observeResult(execution, result));
        assertSame(result, new GotoTerminalDiagnostics(line -> {
            throw new NoClassDefFoundError("sink linkage failure");
        }).observeResult(execution, result));
        // Projection failure has the same isolation boundary as sink failure.
        assertSame(result, new GotoTerminalDiagnostics(line -> fail("must not emit")).observeResult(null, result));
    }

    @Test
    void existingContextCommitEmitsOnceAndReturnsTheSamePayloadForRepeatedSubmission() {
        FabricChatClefCommandExecution execution = execution("@goto 1 2 3");
        List<String> lines = new ArrayList<>();
        GotoTerminalDiagnostics diagnostics = new GotoTerminalDiagnostics(lines::add);
        AtomicInteger factories = new AtomicInteger();
        FabricChatClefCommandResultPayload result = result(FabricChatClefCommandResultStatus.COMPLETED);
        for (int repeat = 0; repeat < 5; repeat++) {
            assertSame(result, execution.context().commitTerminalPayload(() -> {
                factories.incrementAndGet();
                return diagnostics.observeResult(execution, result);
            }));
        }
        assertEquals(1, factories.get());
        assertEquals(1, lines.size());
    }

    @Test
    void unrelatedCommandsAndRawCommandTextAreNotLogged() {
        List<String> lines = new ArrayList<>();
        GotoTerminalDiagnostics diagnostics = new GotoTerminalDiagnostics(lines::add);
        diagnostics.observeResult(execution("@get diamond 1"), result(FabricChatClefCommandResultStatus.COMPLETED));
        assertTrue(lines.isEmpty());
        diagnostics.observeResult(execution("@goto private_secret"), result(FabricChatClefCommandResultStatus.UNKNOWN));
        assertEquals(1, lines.size());
        assertFalse(lines.get(0).contains("private_secret"));
        assertTrue(lines.get(0).contains("request_shape=OTHER_OR_UNAVAILABLE"));
    }

    @Test
    void formattedOutputIsBoundedAndCannotInjectAnotherLogLine() {
        Map<String, Object> fields = new LinkedHashMap<>();
        for (int index = 0; index < 100; index++) {
            fields.put("key" + index, "abc\r\n forged=true " + "x".repeat(1000));
        }
        String line = new GotoTerminalDiagnosticFormatter().format(fields);
        assertFalse(line.contains("\n"));
        assertFalse(line.contains("\r"));
        assertFalse(line.contains("forged=true"));
        assertFalse(line.contains("key32="));
        assertTrue(line.length() < 7000);
    }

    //#if MC == 12001
    @Test
    void preparedStoredArrivalIsObservedWithoutCallingWorldNavigationOrChangingPayload() throws Exception {
        PreparedGotoTask task = TestObjects.allocate(PreparedGotoTask.class);
        Field arrived = PreparedGotoTask.class.getDeclaredField("arrived");
        arrived.setAccessible(true);
        arrived.setBoolean(task, true);
        FabricChatClefCommandExecution execution = boundExecution(task);
        FabricChatClefCommandResultPayload result = result(FabricChatClefCommandResultStatus.COMPLETED);
        Map<String, Object> snapshot = new GotoTerminalDiagnosticProjector().project(execution, result);
        assertEquals("true", snapshot.get("owner_arrived"));
        assertEquals("NONE", snapshot.get("owner_failure_reason"));
        assertEquals(true, snapshot.get("bound_root_matched"));
        assertEquals("completed", snapshot.get("status"));
        assertEquals("UNAVAILABLE", snapshot.get("goal_satisfied"));
        // Later mutation must not revise the captured diagnostic facts.
        arrived.setBoolean(task, false);
        assertEquals("true", snapshot.get("owner_arrived"));
        assertThrows(UnsupportedOperationException.class, () -> snapshot.put("status", "changed"));
    }

    @Test
    void typedOwnerFailureAndContradictoryGenericCompletionRemainVisibleSeparately() {
        PreparedGotoTask task = TestObjects.allocate(PreparedGotoTask.class);
        TestObjects.setField(task, PreparedGotoTask.class, "failure",
                new GotoMaterialPlan.Failure(GotoMaterialPlan.FailureReason.ARRIVAL_LOST));
        Map<String, Object> snapshot = new GotoTerminalDiagnosticProjector().project(
                boundExecution(task), result(FabricChatClefCommandResultStatus.COMPLETED));
        assertEquals("false", snapshot.get("owner_arrived"));
        assertEquals("ARRIVAL_LOST", snapshot.get("owner_failure_reason"));
        assertEquals("completed", snapshot.get("status"));
        assertEquals(true, snapshot.get("ok"));
    }

    @Test
    void mismatchedObservedTaskIsReportedWithoutPromotingItsArrival() throws Exception {
        PreparedGotoTask bound = TestObjects.allocate(PreparedGotoTask.class);
        PreparedGotoTask other = TestObjects.allocate(PreparedGotoTask.class);
        FabricChatClefCommandExecution execution = boundExecution(bound);
        execution.markTaskFinishedObservation(FabricChatClefCommandTerminationObservation.fromTaskFinishedEvent(
                new TaskFinishedEvent(1.0, other)));
        Map<String, Object> snapshot = new GotoTerminalDiagnosticProjector().project(
                execution, result(FabricChatClefCommandResultStatus.UNKNOWN));
        assertEquals(false, snapshot.get("bound_root_matched"));
        assertEquals("unknown", snapshot.get("status"));
    }

    private FabricChatClefCommandExecution boundExecution(PreparedGotoTask task) {
        //20260913_kpopmodder: This constructor-free diagnostic fixture intentionally has no reportable target.
        TestObjects.setField(task, PreparedGotoTask.class, "resultBinding", TestObjects.allocate(GotoTaskBinding.class));
        FabricChatClefCommandExecution execution = execution("@goto 500 90 -928");
        execution.markDispatchReturned(FabricChatClefTaskOwnershipEvidence.of(
                task, FabricChatClefTaskSnapshot.capture(task), FabricChatClefTaskOwnershipSnapshot.empty(),
                1L, 1L, 1L, "test"), FabricChatClefRootOwnershipClassification.COMMAND_OWNED_ROOT);
        execution.markTaskFinishedObservation(FabricChatClefCommandTerminationObservation.fromTaskFinishedEvent(
                new TaskFinishedEvent(1.0, task)));
        return execution;
    }
    //#endif

    private FabricChatClefCommandExecution execution(String command) {
        FabricChatClefCommandRequest request = new FabricChatClefCommandRequest();
        request.requestId = "goto-diagnostic-request";
        request.command = command;
        return new FabricChatClefCommandExecution(
                new FabricChatClefCommandContext(request, "correlation", "session", 3L, 7L),
                command, FabricChatClefTaskOwnershipEvidence.empty());
    }

    private FabricChatClefCommandResultPayload result(FabricChatClefCommandResultStatus status) {
        return FabricChatClefCommandResultPayload.of("goto-diagnostic-request", status,
                status.ok() ? null : "COMMAND_FAILED", "private_message", Map.of(
                        "result_reason", "matching_task_finished",
                        "result_fidelity", "callback_plus_matching_user_task_event"));
    }
}
