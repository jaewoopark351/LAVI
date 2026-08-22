package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.evidence;

import adris.altoclef.tasks.movement.IdleTask;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandRequest;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandResultSender;
import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefRootOwnershipClassification;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipEvidence;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendSubmission;
import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FabricChatClefNonterminalLifecycleEvidencePublisherTest {
    @Test
    void publishIfEligibleCapturesEvidenceBeforeClockAndSendsFinishThenStableEvidence() {
        IdleTask commandRoot = new IdleTask();
        IdleTask neutralRoot = new IdleTask();
        FabricChatClefCommandExecution execution = executionFor(commandRoot);
        TestResultSender sender = new TestResultSender();
        Deque<FabricChatClefTaskOwnershipEvidence> evidence = new ArrayDeque<>(List.of(
                evidence(neutralRoot, 1L),
                evidence(neutralRoot, 2L),
                evidence(neutralRoot, 3L, 1_575_000_000L)
        ));
        Deque<Long> nanos = new ArrayDeque<>(List.of(1_000_000_000L, 1_200_000_000L, 1_600_000_000L));
        Deque<Long> millis = new ArrayDeque<>(List.of(1000L, 1200L, 1600L));
        List<String> events = new ArrayList<>();
        int[] evidenceCount = {0};
        int[] nanoCount = {0};
        int[] millisCount = {0};
        FabricChatClefNonterminalLifecycleEvidencePublisher publisher =
                new FabricChatClefNonterminalLifecycleEvidencePublisher(
                        sender,
                        () -> {
                            events.add("evidence-" + ++evidenceCount[0]);
                            return evidence.removeFirst();
                        },
                        new FabricChatClefBridgeDiagnostics(),
                        () -> {
                            events.add("ms-" + ++millisCount[0]);
                            return millis.removeFirst();
                        },
                        () -> {
                            events.add("nano-" + ++nanoCount[0]);
                            return nanos.removeFirst();
                        }
                );

        publisher.publishIfEligible(execution, "waiting_for_task_finished_event", execution.context());
        publisher.publishIfEligible(execution, "waiting_for_task_finished_event", execution.context());
        publisher.publishIfEligible(execution, "waiting_for_task_finished_event", execution.context());

        assertEquals(
                List.of(
                        "evidence-1",
                        "nano-1",
                        "ms-1",
                        "evidence-2",
                        "nano-2",
                        "ms-2",
                        "evidence-3",
                        "nano-3",
                        "ms-3"
                ),
                events
        );
        assertEquals(2, sender.payloads.size());
        Map<?, ?> finishData = data(sender.payloads.get(0).toMap());
        Map<?, ?> stableData = data(sender.payloads.get(1).toMap());
        Map<?, ?> stableQuiescence = (Map<?, ?>) stableData.get("stable_request_quiescence");

        assertEquals("finish_callback_observed_nonterminal", finishData.get("result_reason"));
        assertEquals("finish_callback_observed_nonterminal", finishData.get("lifecycle_evidence_stage"));
        assertEquals(1, finishData.get("evidence_sequence"));
        assertEquals("callback_without_matching_user_task_event", finishData.get("result_fidelity"));
        assertEquals("stable_request_quiescence_observed", stableData.get("result_reason"));
        assertEquals("stable_request_quiescence_observed", stableData.get("lifecycle_evidence_stage"));
        assertEquals(2, stableData.get("evidence_sequence"));
        assertEquals("callback_without_matching_user_task_event", stableData.get("result_fidelity"));
        assertEquals(true, stableQuiescence.get("qualified"));
        assertEquals("OBSERVED_AND_GONE", stableQuiescence.get("request_root_observation_state"));
        assertEquals(false, stableQuiescence.get("request_root_reappeared"));
    }

    private static FabricChatClefCommandExecution executionFor(Task commandRoot) {
        FabricChatClefCommandExecution execution = new FabricChatClefCommandExecution(
                context(),
                "@deposit diamond 2",
                FabricChatClefTaskOwnershipEvidence.empty()
        );
        execution.markDispatchReturned(evidence(commandRoot, 1L), FabricChatClefRootOwnershipClassification.COMMAND_OWNED_ROOT);
        execution.markFinishCallbackReceived(commandRoot);
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

    private static Map<?, ?> data(Map<String, Object> result) {
        return (Map<?, ?>) result.get("data");
    }

    private static FabricChatClefCommandContext context() {
        FabricChatClefCommandRequest request = new FabricChatClefCommandRequest();
        request.requestId = "req-publisher";
        request.command = "deposit diamond 2";
        request.source = "test";
        return new FabricChatClefCommandContext(request, "corr-publisher", "session-publisher", 1L);
    }

    private static final class TestResultSender implements FabricChatClefCommandResultSender {
        private final List<FabricChatClefCommandResultPayload> payloads = new ArrayList<>();

        @Override
        public FabricChatClefCommandResultSendSubmission sendCommandResult(
                FabricChatClefCommandContext context,
                FabricChatClefCommandResultPayload payload
        ) {
            payloads.add(payload);
            return FabricChatClefCommandResultSendSubmission.accepted();
        }

        @Override
        public FabricChatClefCommandResultSendSubmission sendTerminalCommandResult(
                FabricChatClefCommandContext context,
                FabricChatClefCommandResultPayload payload
        ) {
            return FabricChatClefCommandResultSendSubmission.accepted();
        }
    }
}
