package lavi.minecraft.fabric.chatclef.bridge.command;

import adris.altoclef.AltoClef;
import adris.altoclef.commandsystem.CommandExecutor;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.FabricChatClefTaskStateReader;
import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandLifecycleCoordinator;
import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;
import net.minecraft.client.MinecraftClient;

import java.util.Optional;

//20260801_kpopmodder: Dispatch LAVI Fabric ChatClef commands only from the client tick.
public final class FabricChatClefCommandDispatcher {
    private final FabricChatClefCommandQueue commandQueue;
    private final FabricChatClefCommandResultSender resultSender;
    private final FabricChatClefCommandLifecycleCoordinator lifecycleCoordinator;
    private final FabricChatClefBridgeDiagnostics diagnostics;
    private final FabricChatClefTaskStateReader taskStateReader;

    public FabricChatClefCommandDispatcher(
            FabricChatClefCommandQueue commandQueue,
            FabricChatClefCommandResultSender resultSender,
            FabricChatClefCommandLifecycleCoordinator lifecycleCoordinator,
            FabricChatClefBridgeDiagnostics diagnostics,
            FabricChatClefTaskStateReader taskStateReader
    ) {
        this.commandQueue = commandQueue;
        this.resultSender = resultSender;
        this.lifecycleCoordinator = lifecycleCoordinator;
        this.diagnostics = diagnostics;
        this.taskStateReader = taskStateReader;
    }

    public void onEndClientTick(MinecraftClient client) {
        long nowMs = System.currentTimeMillis();
        lifecycleCoordinator.onEndClientTick(commandQueue.activeContext());
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
            commandQueue.removePending(context);
            if (context.markTerminalSent()) {
                resultSender.sendCommandResult(
                        context,
                        FabricChatClefCommandResult.deadlineExceeded(
                                context.requestId(),
                                "Fabric ChatClef command deadline expired before dispatch."
                        )
                );
            }
            return;
        }
        if (!isEngineReady()) {
            return;
        }
        commandQueue.pollForDispatch().ifPresent(this::dispatch);
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
        FabricChatClefCommandExecution execution = new FabricChatClefCommandExecution(
                context,
                command,
                taskStateReader.captureCurrentTaskSnapshot()
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
        resultSender.sendCommandResult(context, execution.runningResult());
        try {
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
            lifecycleCoordinator.markDispatchReturned(execution, taskStateReader.currentTaskOrNull());
        } catch (Throwable error) {
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
