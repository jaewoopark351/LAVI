package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.terminal;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.crafting.acquisition.terminal.CraftResourceLifecycleClearKind;
import lavi.minecraft.diagnostics.crafting.acquisition.terminal.CraftResourcePrimaryTerminationCause;
import lavi.minecraft.diagnostics.crafting.acquisition.terminal.CraftResourceTaskFinishMatch;
import lavi.minecraft.diagnostics.crafting.acquisition.scope.IronPickaxeAcquisitionScopeBinding;
import lavi.minecraft.diagnostics.crafting.acquisition.scope.IronPickaxeAcquisitionScopeDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandTerminationObservation;

import java.util.Map;
import java.util.Optional;

//20260901_kpopmodder: Project actual existing lifecycle diagnostics without changing handoff.
public final class FabricChatClefCraftResourceTerminalLifecycleObserver {
    private static final FabricChatClefCraftResourceTerminalSendProjector SENDS =
            new FabricChatClefCraftResourceTerminalSendProjector();

    private FabricChatClefCraftResourceTerminalLifecycleObserver() {
    }

    public static void observe(
            String event,
            FabricChatClefCommandExecution execution,
            Map<String, Object> details
    ) {
        if (event == null || execution == null) {
            return;
        }
        FabricChatClefCommandContext context = execution.context();
        Optional<FabricChatClefCraftResourceTerminalScopeBinding> exact =
                FabricChatClefCraftResourceTerminalDiagnostics.exact(context);
        if (exact.isEmpty()) {
            return;
        }
        FabricChatClefCraftResourceTerminalScopeBinding binding = exact.get();
        if (!matchesExecutionRoot(execution, binding)) {
            return;
        }
        Map<String, Object> safeDetails = details == null ? Map.of() : details;
        long clientTick = ChatClefDiagnostics.currentClientTickId();
        long monotonicNanos = System.nanoTime();

        switch (event) {
            case "task_finished_event_received" -> observeTaskFinished(
                    execution,
                    binding,
                    safeDetails,
                    event,
                    clientTick,
                    monotonicNanos
            );
            case "terminal_decision" -> observeTerminalDecision(
                    binding,
                    safeDetails,
                    event,
                    clientTick,
                    monotonicNanos
            );
            case "terminal_result_send_started" ->
                    FabricChatClefCraftResourceTerminalDiagnostics.recordSend(
                            binding,
                            SENDS.started(),
                            event,
                            clientTick,
                            monotonicNanos
                    );
            case "terminal_result_send_waiting" ->
                    FabricChatClefCraftResourceTerminalDiagnostics.recordSend(
                            binding,
                            SENDS.fromContext(context),
                            event,
                            clientTick,
                            monotonicNanos
                    );
            case "terminal_result_sent" -> observeTerminalResultSent(
                    binding,
                    event,
                    clientTick,
                    monotonicNanos
            );
            case "connection_detached_lifecycle_cleared" -> observeDetach(
                    binding,
                    context,
                    safeDetails,
                    event,
                    clientTick,
                    monotonicNanos
            );
            case "command_exception" -> observeException(
                    binding,
                    safeDetails,
                    CraftResourcePrimaryTerminationCause.COMMAND_EXCEPTION,
                    event,
                    clientTick,
                    monotonicNanos
            );
            case "dispatch_exception" -> observeException(
                    binding,
                    safeDetails,
                    CraftResourcePrimaryTerminationCause.DISPATCH_EXCEPTION,
                    event,
                    clientTick,
                    monotonicNanos
            );
            case "active_deadline_exceeded" ->
                    FabricChatClefCraftResourceTerminalDiagnostics.recordPrimaryCause(
                            binding,
                            CraftResourcePrimaryTerminationCause.EXISTING_COMMAND_DEADLINE,
                            event,
                            clientTick,
                            monotonicNanos
                    );
            default -> {
                // Non-terminal lifecycle diagnostics remain owned by their existing logger.
            }
        }
    }

    public static void observeContext(
            String event,
            FabricChatClefCommandContext context,
            Map<String, Object> details
    ) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()
                || !"active_deadline_exceeded_without_lifecycle_execution".equals(event)) {
            return;
        }
        Optional<FabricChatClefCraftResourceTerminalScopeBinding> exact =
                FabricChatClefCraftResourceTerminalDiagnostics.exact(context);
        if (exact.isEmpty()) {
            return;
        }
        FabricChatClefCraftResourceTerminalDiagnostics.recordPrimaryCause(
                exact.get(),
                CraftResourcePrimaryTerminationCause.EXISTING_COMMAND_DEADLINE,
                event,
                ChatClefDiagnostics.currentClientTickId(),
                System.nanoTime()
        );
    }

    private static void observeTaskFinished(
            FabricChatClefCommandExecution execution,
            FabricChatClefCraftResourceTerminalScopeBinding binding,
            Map<String, Object> details,
            String sourceEvent,
            long clientTick,
            long monotonicNanos
    ) {
        FabricChatClefCommandTerminationObservation observation =
                execution.taskFinishedObservation();
        if (observation == null) {
            return;
        }
        CraftResourceTaskFinishMatch match = execution.matchesBoundRootTask(observation)
                ? CraftResourceTaskFinishMatch.MATCHING_ROOT
                : CraftResourceTaskFinishMatch.NONMATCHING_ROOT;
        Map<String, Object> taskFinishedEvent =
                FabricChatClefCraftResourceTerminalPayloadReader.nested(
                        details,
                        "task_finished_event"
                );
        Map<String, Object> task = FabricChatClefCraftResourceTerminalPayloadReader.nested(
                taskFinishedEvent,
                "task"
        );
        boolean stateAvailable = FabricChatClefCraftResourceTerminalPayloadReader.bool(
                task,
                "task_state_available"
        ).orElse(false);
        boolean timedOut = stateAvailable
                && FabricChatClefCraftResourceTerminalPayloadReader.bool(
                task,
                "this_or_child_timed_out"
        ).orElse(false);
        Map<String, Object> ownership = execution.context().ownershipPayload().toMap();
        if (FabricChatClefCraftResourceTerminalPayloadReader.bool(
                ownership,
                "detached"
        ).orElse(false)) {
            FabricChatClefCraftResourceTerminalDiagnostics.recordDetach(
                    binding,
                    FabricChatClefCraftResourceTerminalPayloadReader.string(
                            ownership,
                            "detached_reason"
                    ).orElse("UNAVAILABLE"),
                    sourceEvent,
                    clientTick,
                    monotonicNanos
            );
        }
        FabricChatClefCraftResourceTerminalDiagnostics.recordTaskFinished(
                binding,
                match,
                observation.terminationKind(),
                timedOut,
                sourceEvent,
                clientTick,
                monotonicNanos
        );
    }

    private static boolean matchesExecutionRoot(
            FabricChatClefCommandExecution execution,
            FabricChatClefCraftResourceTerminalScopeBinding binding
    ) {
        Optional<IronPickaxeAcquisitionScopeBinding> activeScope =
                IronPickaxeAcquisitionScopeDiagnostics.activeBinding(binding.scopeKey());
        if (activeScope.isEmpty()
                || execution.context() != binding.commandContext()
                || execution.taskAfterDispatchEvidence() == null) {
            return false;
        }
        IronPickaxeAcquisitionScopeBinding scope = activeScope.get();
        return execution.taskAfterDispatchEvidence().rootTask() == scope.boundRootTask()
                && same(
                execution.taskAfterDispatchEvidence().userTaskRootAssignmentId(),
                binding.scopeKey().rootAssignmentId()
        )
                && execution.taskAfterDispatchEvidence().userTaskRootGeneration()
                == binding.scopeKey().rootGeneration()
                && same(
                execution.taskAfterDispatchEvidence().userTaskRootIdentity(),
                binding.scopeKey().boundRootTaskInstanceId()
        );
    }

    private static boolean same(String first, String second) {
        return first != null && first.equals(second);
    }

    private static void observeTerminalDecision(
            FabricChatClefCraftResourceTerminalScopeBinding binding,
            Map<String, Object> details,
            String sourceEvent,
            long clientTick,
            long monotonicNanos
    ) {
        String decisionReason = FabricChatClefCraftResourceTerminalPayloadReader.string(
                details,
                "decision_reason"
        ).orElse("UNAVAILABLE");
        FabricChatClefCraftResourceTerminalDiagnostics.recordClassification(
                binding,
                decisionReason,
                "UNAVAILABLE_EXISTING_EVENT_DOES_NOT_EXPOSE_RESULT_STATUS",
                "UNAVAILABLE_EXISTING_EVENT_DOES_NOT_EXPOSE_RESULT_REASON",
                "UNAVAILABLE_EXISTING_EVENT_DOES_NOT_EXPOSE_RESULT_FIDELITY",
                "INCONCLUSIVE_EXISTING_EVENT_EXPOSES_DECISION_REASON_ONLY",
                sourceEvent,
                clientTick,
                monotonicNanos
        );
    }

    private static void observeTerminalResultSent(
            FabricChatClefCraftResourceTerminalScopeBinding binding,
            String sourceEvent,
            long clientTick,
            long monotonicNanos
    ) {
        FabricChatClefCraftResourceTerminalDiagnostics.recordSend(
                binding,
                SENDS.sent(),
                sourceEvent,
                clientTick,
                monotonicNanos
        );
        FabricChatClefCraftResourceTerminalDiagnostics.recordLifecycleCleared(
                binding,
                CraftResourceLifecycleClearKind.NORMAL_TERMINAL_RESULT_SENT,
                sourceEvent,
                clientTick,
                monotonicNanos
        );
    }

    private static void observeException(
            FabricChatClefCraftResourceTerminalScopeBinding binding,
            Map<String, Object> details,
            CraftResourcePrimaryTerminationCause cause,
            String sourceEvent,
            long clientTick,
            long monotonicNanos
    ) {
        FabricChatClefCraftResourceTerminalDiagnostics.recordExceptionEvidence(
                binding,
                FabricChatClefCraftResourceTerminalPayloadReader.string(
                        details,
                        "exception_type"
                ).orElse("UNAVAILABLE"),
                FabricChatClefCraftResourceTerminalPayloadReader.string(
                        details,
                        "exception_message"
                ).orElse("UNAVAILABLE")
        );
        FabricChatClefCraftResourceTerminalDiagnostics.recordPrimaryCause(
                binding,
                cause,
                sourceEvent,
                clientTick,
                monotonicNanos
        );
    }

    private static void observeDetach(
            FabricChatClefCraftResourceTerminalScopeBinding binding,
            FabricChatClefCommandContext context,
            Map<String, Object> details,
            String sourceEvent,
            long clientTick,
            long monotonicNanos
    ) {
        String detachReason = FabricChatClefCraftResourceTerminalPayloadReader.string(
                details,
                "decision_reason"
        ).orElse("UNAVAILABLE");
        FabricChatClefCraftResourceTerminalDiagnostics.recordDetach(
                binding,
                detachReason,
                sourceEvent,
                clientTick,
                monotonicNanos
        );
        FabricChatClefCraftResourceTerminalDiagnostics.recordSend(
                binding,
                SENDS.fromContext(context),
                sourceEvent,
                clientTick,
                monotonicNanos
        );
        // The source event is emitted even when its internal compare-and-set returns false.
        // Without that boolean in the existing payload, lifecycle clear stays unavailable.
    }
}
