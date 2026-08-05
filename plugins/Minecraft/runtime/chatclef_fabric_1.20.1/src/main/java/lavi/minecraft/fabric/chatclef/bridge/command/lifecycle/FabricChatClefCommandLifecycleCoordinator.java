package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandResult;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.FabricChatClefCommandDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.FabricChatClefTaskStateReader;
import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultDataPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultPayload;
import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;

import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

//20260803_kpopmodder: Combine command callbacks with user task finish observations in the bridge layer.
public final class FabricChatClefCommandLifecycleCoordinator {
    private static final int MAX_OBSERVATIONS_PER_TICK = 32;

    private final FabricChatClefUserTaskFinishedObserver taskFinishedObserver;
    private final FabricChatClefCommandOutcomeClassifier outcomeClassifier;
    private final FabricChatClefCommandResultOutbox resultOutbox;
    private final FabricChatClefBridgeDiagnostics diagnostics;
    private final FabricChatClefCommandDiagnostics commandDiagnostics;
    private final FabricChatClefTaskStateReader taskStateReader;
    private final AtomicReference<FabricChatClefCommandExecution> activeExecution = new AtomicReference<>();
    private volatile String lastWaitingDecisionKey = "";
    private volatile long lastWaitingDecisionLoggedAtMs = 0L;

    public FabricChatClefCommandLifecycleCoordinator(
            FabricChatClefUserTaskFinishedObserver taskFinishedObserver,
            FabricChatClefCommandOutcomeClassifier outcomeClassifier,
            FabricChatClefCommandResultOutbox resultOutbox,
            FabricChatClefBridgeDiagnostics diagnostics,
            FabricChatClefTaskStateReader taskStateReader
    ) {
        this.taskFinishedObserver = taskFinishedObserver;
        this.outcomeClassifier = outcomeClassifier;
        this.resultOutbox = resultOutbox;
        this.diagnostics = diagnostics;
        this.commandDiagnostics = new FabricChatClefCommandDiagnostics(diagnostics);
        this.taskStateReader = taskStateReader;
    }

    public void beginExecution(FabricChatClefCommandExecution execution) {
        if (!activeExecution.compareAndSet(null, execution)) {
            FabricChatClefCommandExecution active = activeExecution.get();
            diagnostics.warn(
                    "replaced stale lifecycle execution active="
                            + (active == null ? "<none>" : active.requestId())
                            + " next="
                            + execution.requestId()
            );
            commandDiagnostics.warn(
                    "begin_execution_replaced_stale_active",
                    execution,
                    FabricChatClefLifecycleDetailsPayload.replacedActive(active == null ? "" : active.requestId())
            );
            activeExecution.set(execution);
        }
        lastWaitingDecisionKey = "";
        commandDiagnostics.info("begin_execution", execution);
    }

    public void markDispatchReturned(FabricChatClefCommandExecution execution, Task boundRootTask) {
        execution.markDispatchReturned(boundRootTask, FabricChatClefTaskSnapshot.capture(boundRootTask));
        commandDiagnostics.info("dispatch_returned", execution);
        tryComplete(execution);
    }

    public void markCommandFinish(
            FabricChatClefCommandExecution execution,
            Task taskAtFinish
    ) {
        execution.markFinishCallbackReceived(taskAtFinish);
        commandDiagnostics.info(
                "finish_callback_received",
                execution,
                FabricChatClefLifecycleDetailsPayload.finishCallback(
                        execution.boundRootRelationshipPayload("callback_current_task", taskAtFinish),
                        taskStateReader.runtimePayload()
                )
        );
        tryComplete(execution);
    }

    public void completeCommandException(
            FabricChatClefCommandExecution execution,
            Throwable exception,
            FabricChatClefTaskSnapshot taskAtFailure
    ) {
        commandDiagnostics.warn("command_exception", execution, exceptionDetails(exception));
        completeTerminal(
                execution,
                () -> execution.failedFromCommandException(exception, taskAtFailure)
        );
    }

    public void completeDispatchException(
            FabricChatClefCommandExecution execution,
            Throwable exception,
            FabricChatClefTaskSnapshot taskAtFailure
    ) {
        commandDiagnostics.warn("dispatch_exception", execution, exceptionDetails(exception));
        completeTerminal(
                execution,
                () -> execution.failedFromDispatchException(exception, taskAtFailure)
        );
    }

    public void onEndClientTick(Optional<FabricChatClefCommandContext> activeContext) {
        syncActiveContext(activeContext);
        drainObservations();
        FabricChatClefCommandExecution execution = activeExecution.get();
        if (execution != null) {
            tryComplete(execution);
        }
    }

    public void completeActiveDeadline(FabricChatClefCommandContext context) {
        FabricChatClefCommandExecution execution = activeExecution.get();
        if (execution != null && execution.context() == context) {
            commandDiagnostics.warn("active_deadline_exceeded", execution);
            completeTerminal(
                    execution,
                    () -> execution.deadlineExceededResult(
                            "Fabric ChatClef command deadline expired while active."
                    )
            );
            return;
        }
        commandDiagnostics.contextWarn(
                "active_deadline_exceeded_without_lifecycle_execution",
                context,
                new HashMap<>()
        );
        resultOutbox.sendTerminal(
                context,
                () -> FabricChatClefCommandResult.deadlineExceeded(
                        context.requestId(),
                        "Fabric ChatClef command deadline expired while active.",
                        deadlineData(context)
                )
        );
    }

    public boolean hasActiveExecution(FabricChatClefCommandContext context) {
        FabricChatClefCommandExecution execution = activeExecution.get();
        return execution != null && execution.context() == context;
    }

    private void drainObservations() {
        for (int index = 0; index < MAX_OBSERVATIONS_PER_TICK; index++) {
            FabricChatClefCommandTerminationObservation observation = taskFinishedObserver.poll();
            if (observation == null) {
                return;
            }
            observeTaskTermination(observation);
        }
    }

    private void observeTaskTermination(FabricChatClefCommandTerminationObservation observation) {
        FabricChatClefCommandExecution execution = activeExecution.get();
        if (execution == null) {
            diagnostics.info(
                    "ignored TaskFinishedEvent without active LAVI command data="
                            + observation.toMap()
            );
            return;
        }
        execution.markTaskFinishedObservation(observation);
        FabricChatClefTaskFinishedEventDetailsPayload details = FabricChatClefTaskFinishedEventDetailsPayload.of(
                observation,
                execution.matchesBoundRootTask(observation),
                execution.boundRootRelationshipPayload("event_task", observation.task()),
                execution.boundRootMatchReason(observation.task()),
                execution.finishCallbackReceived(),
                taskStateReader.runtimePayload()
        );
        commandDiagnostics.info("task_finished_event_received", execution, details);
        tryComplete(execution);
    }

    private void tryComplete(FabricChatClefCommandExecution execution) {
        if (activeExecution.get() != execution) {
            return;
        }
        FabricChatClefCommandTerminalDecision decision = outcomeClassifier.classify(execution);
        if (!decision.terminal()) {
            logWaitingDecision(execution, decision.reason());
            return;
        }
        commandDiagnostics.info(
                "terminal_decision",
                execution,
                FabricChatClefLifecycleDetailsPayload.terminalDecision(decision.reason())
        );
        completeTerminal(execution, decision::result);
        diagnostics.info(
                "terminal command decision request="
                        + execution.requestId()
                        + " reason="
                        + decision.reason()
        );
    }

    private void completeTerminal(
            FabricChatClefCommandExecution execution,
            Supplier<FabricChatClefCommandResultPayload> resultFactory
    ) {
        boolean terminalSent = resultOutbox.sendTerminal(execution, resultFactory);
        boolean lifecycleCleared = activeExecution.compareAndSet(execution, null);
        commandDiagnostics.info(
                "terminal_result_sent",
                execution,
                FabricChatClefLifecycleDetailsPayload.terminalResult(terminalSent, lifecycleCleared)
        );
        lastWaitingDecisionKey = "";
    }

    private void syncActiveContext(Optional<FabricChatClefCommandContext> activeContext) {
        FabricChatClefCommandExecution execution = activeExecution.get();
        if (execution == null) {
            return;
        }
        if (activeContext.isPresent() && activeContext.get() == execution.context()) {
            return;
        }
        diagnostics.warn(
                "cleared lifecycle execution without active queue context request="
                        + execution.requestId()
        );
        commandDiagnostics.warn(
                "lifecycle_execution_without_active_queue_context",
                execution,
                FabricChatClefLifecycleDetailsPayload.queueContextMismatch(
                        activeContext.isPresent(),
                        activeContext.isPresent() ? activeContext.get().requestId() : ""
                )
        );
        activeExecution.compareAndSet(execution, null);
    }

    private void logWaitingDecision(FabricChatClefCommandExecution execution, String reason) {
        long nowMs = System.currentTimeMillis();
        String key = execution.requestId() + ":" + reason;
        if (key.equals(lastWaitingDecisionKey) && nowMs - lastWaitingDecisionLoggedAtMs < 5000L) {
            return;
        }
        lastWaitingDecisionKey = key;
        lastWaitingDecisionLoggedAtMs = nowMs;
        Task currentTask = taskStateReader.currentTaskOrNull();
        commandDiagnostics.info(
                "waiting_for_terminal_condition",
                execution,
                FabricChatClefLifecycleDetailsPayload.waitingForTerminalCondition(
                        reason,
                        execution.boundRootRelationshipPayload("current_task", currentTask),
                        execution.boundRootMatchReason(currentTask),
                        taskStateReader.runtimePayload()
                )
        );
    }

    private FabricChatClefLifecycleDetailsPayload exceptionDetails(Throwable exception) {
        return FabricChatClefLifecycleDetailsPayload.exception(exception);
    }

    private FabricChatClefCommandResultDataPayload deadlineData(FabricChatClefCommandContext context) {
        return FabricChatClefCommandDeadlinePayload.markTaskMayStillBeRunning(context.ownershipPayload());
    }
}
