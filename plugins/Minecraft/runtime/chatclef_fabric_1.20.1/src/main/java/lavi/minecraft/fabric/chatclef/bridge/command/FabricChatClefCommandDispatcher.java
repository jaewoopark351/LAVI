package lavi.minecraft.fabric.chatclef.bridge.command;

import adris.altoclef.AltoClef;
import adris.altoclef.commandsystem.CommandExecutor;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.fabric.chatclef.bridge.command.control.FabricChatClefConnectionDetachedEvent;
import lavi.minecraft.fabric.chatclef.bridge.command.control.FabricChatClefConnectionDetachResult;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.FabricChatClefCommandContextUnbindDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.FabricChatClefTaskStateReader;
import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandLifecycleCoordinator;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipEvidence;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.queue.FabricChatClefCommandQueueCompletion;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendSubmission;
import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;
import net.minecraft.client.MinecraftClient;

import java.util.Optional;
import java.util.function.Consumer;

//20260801_kpopmodder: Dispatch LAVI Fabric ChatClef commands only from the client tick.
public final class FabricChatClefCommandDispatcher {
    private final FabricChatClefCommandQueue commandQueue;
    private final FabricChatClefCommandResultSender resultSender;
    private final FabricChatClefCommandLifecycleCoordinator lifecycleCoordinator;
    private final FabricChatClefBridgeDiagnostics diagnostics;
    private final FabricChatClefTaskStateReader taskStateReader;
    private final Consumer<String> detachedCommandCancellationAction;
    private volatile String lastBoundRootOwnershipForDetach = "";
    private volatile String lastDetachCancelAction = "";

    public FabricChatClefCommandDispatcher(
            FabricChatClefCommandQueue commandQueue,
            FabricChatClefCommandResultSender resultSender,
            FabricChatClefCommandLifecycleCoordinator lifecycleCoordinator,
            FabricChatClefBridgeDiagnostics diagnostics,
            FabricChatClefTaskStateReader taskStateReader
    ) {
        this(commandQueue, resultSender, lifecycleCoordinator, diagnostics, taskStateReader, null);
    }

    FabricChatClefCommandDispatcher(
            FabricChatClefCommandQueue commandQueue,
            FabricChatClefCommandResultSender resultSender,
            FabricChatClefCommandLifecycleCoordinator lifecycleCoordinator,
            FabricChatClefBridgeDiagnostics diagnostics,
            FabricChatClefTaskStateReader taskStateReader,
            Consumer<String> detachedCommandCancellationAction
    ) {
        this.commandQueue = commandQueue;
        this.resultSender = resultSender;
        this.lifecycleCoordinator = lifecycleCoordinator;
        this.diagnostics = diagnostics;
        this.taskStateReader = taskStateReader;
        this.detachedCommandCancellationAction = detachedCommandCancellationAction == null
                ? this::cancelUserTaskForDetachedCommand
                : detachedCommandCancellationAction;
    }

    public void onEndClientTick(MinecraftClient client) {
        long nowMs = System.currentTimeMillis();
        lifecycleCoordinator.onEndClientTick(commandQueue.activeContext());
        if (processConnectionDetachedEvents()) {
            return;
        }
        Optional<FabricChatClefCommandContext> active = commandQueue.activeContext();
        if (active.isPresent()) {
            FabricChatClefCommandContext context = active.get();
            if (context.isDeadlineExceeded(nowMs)) {
                completeActiveDeadline(context);
            }
            return;
        }
        Optional<FabricChatClefCommandContext> pending = commandQueue.peekPending();
        if (pending.isPresent() && pending.get().isDeadlineExceeded(nowMs)) {
            FabricChatClefCommandContext context = pending.get();
            lifecycleCoordinator.completePendingDeadline(context);
            return;
        }
        if (!isEngineReady()) {
            return;
        }
        commandQueue.pollForDispatch().ifPresent(this::dispatch);
    }

    private boolean processConnectionDetachedEvents() {
        boolean processed = false;
        for (int index = 0; index < 16; index++) {
            Optional<FabricChatClefConnectionDetachedEvent> event = commandQueue.pollConnectionDetached();
            if (event.isEmpty()) {
                return processed;
            }
            processed = true;
            if (handleConnectionDetached(event.get())) {
                return true;
            }
        }
        return true;
    }

    private boolean handleConnectionDetached(FabricChatClefConnectionDetachedEvent event) {
        return handleConnectionDetached(event, taskStateReader.currentTaskOrNull());
    }

    boolean handleConnectionDetached(FabricChatClefConnectionDetachedEvent event, Task currentTask) {
        FabricChatClefTaskOwnershipSnapshot ownershipBefore =
                FabricChatClefCommandContextUnbindDiagnostics.captureOwnershipSnapshot();
        FabricChatClefConnectionDetachResult detachResult = commandQueue.markConnectionDetached(event);
        Optional<FabricChatClefCommandContext> detachedActive = detachResult.detachedActive();
        if (detachedActive.isEmpty()) {
            if (detachResult.pendingInFlightRetainedCount() > 0) {
                commandQueue.enqueueConnectionDetached(event.connectionGeneration(), event.reason());
                return true;
            }
            logDetachedWithoutActive(detachResult, ownershipBefore);
            return false;
        }
        FabricChatClefCommandContext context = detachedActive.get();
        if (context.terminalSendInFlight()) {
            commandQueue.enqueueConnectionDetached(event.connectionGeneration(), event.reason());
            if (context.markTerminalDetachDeferLogged()) {
                diagnostics.warn(
                        "connection detach deferred while terminal result send is in flight request="
                                + context.requestId()
                                + " generation="
                                + event.connectionGeneration()
                );
            }
            return true;
        }
        String rootMatchReason = lifecycleCoordinator.boundRootMatchReason(context, currentTask);
        String boundRootOwnershipForDetach = lifecycleCoordinator.boundRootOwnershipForDetach(context, currentTask);
        String detachCancelAction = lifecycleCoordinator.detachCancelAction(context, currentTask);
        lastBoundRootOwnershipForDetach = boundRootOwnershipForDetach;
        lastDetachCancelAction = detachCancelAction;
        boolean ownsCurrentTask = lifecycleCoordinator.matchesBoundRootTask(context, currentTask);
        diagnostics.warn(
                "connection detached with active command request="
                        + context.requestId()
                        + " generation="
                        + event.connectionGeneration()
                        + " reason="
                        + event.reason()
                        + " bound_root_match_reason="
                        + rootMatchReason
                        + " bound_root_ownership_for_detach="
                        + boundRootOwnershipForDetach
                        + " detach_cancel_action="
                        + detachCancelAction
        );
        if (ownsCurrentTask) {
            detachedCommandCancellationAction.accept(rootMatchReason);
        } else {
            diagnostics.warn(
                    "connection detach cancel skipped because current user task is not owned by request="
                            + context.requestId()
                            + " bound_root_match_reason="
                            + rootMatchReason
                            + " bound_root_ownership_for_detach="
                            + boundRootOwnershipForDetach
                            + " detach_cancel_action="
                            + detachCancelAction
            );
        }
        lifecycleCoordinator.onEndClientTick(commandQueue.activeContext());
        lifecycleCoordinator.clearDetachedExecution(context, event.reason() + ":" + rootMatchReason);
        FabricChatClefCommandQueueCompletion completion = commandQueue.clearDetachedActive(
                context,
                ownsCurrentTask ? "connection_detached_task_cancelled" : "connection_detached_task_not_owned"
        );
        logQueueCompletion(completion, ownershipBefore, boundRootOwnershipForDetach, detachCancelAction);
        return false;
    }

    private void logDetachedWithoutActive(
            FabricChatClefConnectionDetachResult detachResult,
            FabricChatClefTaskOwnershipSnapshot ownershipBefore
    ) {
        if (!detachResult.changedQueueState()) {
            return;
        }
        FabricChatClefCommandContextUnbindDiagnostics.logBoundary(
                "connection_detached_without_active_command",
                null,
                detachResult.activeBefore(),
                detachResult.activeAfter(),
                detachResult.changedQueueState(),
                ownershipBefore,
                FabricChatClefCommandContextUnbindDiagnostics.captureOwnershipSnapshot()
        );
    }

    private void logQueueCompletion(
            FabricChatClefCommandQueueCompletion completion,
            FabricChatClefTaskOwnershipSnapshot ownershipBefore,
            String boundRootOwnershipForDetach,
            String detachCancelAction
    ) {
        FabricChatClefCommandContextUnbindDiagnostics.logBoundary(
                completion.reason(),
                completion.context(),
                completion.activeBefore(),
                completion.activeAfter(),
                completion.mutationApplied(),
                ownershipBefore,
                FabricChatClefCommandContextUnbindDiagnostics.captureOwnershipSnapshot(),
                boundRootOwnershipForDetach,
                detachCancelAction
        );
    }

    String lastBoundRootOwnershipForDetach() {
        return lastBoundRootOwnershipForDetach;
    }

    String lastDetachCancelAction() {
        return lastDetachCancelAction;
    }

    private void cancelUserTaskForDetachedCommand(String rootMatchReason) {
        AltoClef mod = AltoClef.getInstance();
        if (mod == null || mod.getUserTaskChain() == null) {
            diagnostics.warn("connection detach cancel skipped because AltoClef user task chain is unavailable");
            return;
        }
        diagnostics.warn("connection detach cancelling owned user task bound_root_match_reason=" + rootMatchReason);
        mod.cancelUserTask();
    }

    private boolean isEngineReady() {
        AltoClef mod = AltoClef.getInstance();
        return mod != null
                && AltoClef.inGame()
                && AltoClef.getCommandExecutor() != null
                && mod.getModSettings() != null;
    }

    private void dispatch(FabricChatClefCommandContext context) {
        FabricChatClefCommandRequest request = context.request();
        CommandExecutor executor = AltoClef.getCommandExecutor();
        String command = normalizeCommand(executor, request.command);
        FabricChatClefTaskOwnershipEvidence taskBeforeDispatch = taskStateReader.ownershipEvidence();
        FabricChatClefCommandExecution execution = new FabricChatClefCommandExecution(
                context,
                command,
                taskBeforeDispatch
        );
        lifecycleCoordinator.beginExecution(execution);
        diagnostics.info(
                "dispatch command request="
                        + context.requestId()
                        + " source="
                        + request.source
                        + " normalized_command="
                        + command
                        + " generation="
                        + context.connectionGeneration()
        );
        FabricChatClefCommandResultSendSubmission runningSubmission =
                resultSender.sendCommandResult(context, execution.runningResult());
        if (!runningSubmission.acceptedForAsyncSend()) {
            diagnostics.warn(
                    "running result send submit failed request="
                            + context.requestId()
                            + " outcome="
                            + runningSubmission.diagnosticMessage()
            );
        }
        try {
            execution.openExecutorExecuteInvocation();
            executor.execute(
                    command,
                    () -> lifecycleCoordinator.markCommandFinish(
                            execution,
                            taskStateReader.currentTaskOrNull()
                    ),
                    exception -> lifecycleCoordinator.completeCommandException(
                            execution,
                            exception,
                            taskStateReader.captureCurrentTaskSnapshot()
                    )
            );
            execution.closeExecutorExecuteInvocation();
            lifecycleCoordinator.markDispatchReturned(execution, taskStateReader.ownershipEvidence());
        } catch (Throwable error) {
            execution.closeExecutorExecuteInvocation();
            lifecycleCoordinator.completeDispatchException(
                    execution,
                    error,
                    taskStateReader.captureCurrentTaskSnapshot()
            );
        }
    }

    private String normalizeCommand(CommandExecutor executor, String command) {
        String normalized = command == null ? "" : command.trim();
        if (executor.isClientCommand(normalized)) {
            return normalized;
        }
        return executor.getCommandPrefix() + normalized;
    }

    private void completeActiveDeadline(FabricChatClefCommandContext context) {
        lifecycleCoordinator.completeActiveDeadline(context);
    }
}
