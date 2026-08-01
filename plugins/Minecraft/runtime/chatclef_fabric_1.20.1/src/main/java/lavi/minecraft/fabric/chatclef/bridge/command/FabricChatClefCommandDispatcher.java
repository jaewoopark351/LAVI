package lavi.minecraft.fabric.chatclef.bridge.command;

import adris.altoclef.AltoClef;
import adris.altoclef.commandsystem.CommandExecutor;
import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;
import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefTaskSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;
import net.minecraft.client.MinecraftClient;

import java.util.Optional;
import java.util.function.Supplier;

//20260801_kpopmodder: Dispatch LAVI Fabric ChatClef commands only from the client tick.
public final class FabricChatClefCommandDispatcher {
    private final FabricChatClefCommandQueue commandQueue;
    private final FabricChatClefCommandResultSender resultSender;
    private final FabricChatClefBridgeDiagnostics diagnostics;

    public FabricChatClefCommandDispatcher(
            FabricChatClefCommandQueue commandQueue,
            FabricChatClefCommandResultSender resultSender,
            FabricChatClefBridgeDiagnostics diagnostics
    ) {
        this.commandQueue = commandQueue;
        this.resultSender = resultSender;
        this.diagnostics = diagnostics;
    }

    public void onEndClientTick(MinecraftClient client) {
        if (commandQueue.hasActive()) {
            return;
        }
        long nowMs = System.currentTimeMillis();
        Optional<FabricChatClefCommandRequest> pending = commandQueue.peekPending();
        if (pending.isPresent() && pending.get().isDeadlineExceeded(nowMs)) {
            FabricChatClefCommandRequest request = pending.get();
            commandQueue.removePending(request.requestId);
            resultSender.sendCommandResult(
                    request.requestId,
                    FabricChatClefCommandResult.deadlineExceeded(
                            request.requestId,
                            "Fabric ChatClef command deadline expired before dispatch."
                    )
            );
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

    private void dispatch(FabricChatClefCommandRequest request) {
        CommandExecutor executor = AltoClef.getCommandExecutor();
        String command = normalizeCommand(executor, request.command);
        FabricChatClefCommandExecution execution = new FabricChatClefCommandExecution(
                request,
                command,
                captureCurrentTask()
        );
        diagnostics.info("dispatch command request=" + request.requestId);
        resultSender.sendCommandResult(request.requestId, execution.runningResult());
        try {
            executor.execute(
                    command,
                    () -> completeOnce(
                            execution,
                            () -> execution.unknownAfterFinish(captureCurrentTask())
                    ),
                    exception -> completeOnce(
                            execution,
                            () -> execution.failedFromCommandException(exception, captureCurrentTask())
                    )
            );
            execution.markDispatchReturned(captureCurrentTask());
        } catch (Throwable error) {
            completeOnce(
                    execution,
                    () -> execution.failedFromDispatchException(error, captureCurrentTask())
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

    private void completeOnce(
            FabricChatClefCommandExecution execution,
            Supplier<java.util.Map<String, Object>> resultFactory
    ) {
        if (!execution.markTerminalSent()) {
            diagnostics.warn(
                    "ignored duplicate terminal result request="
                            + execution.requestId()
                            + " data="
                            + execution.duplicateTerminalData("duplicate_terminal_result")
            );
            return;
        }
        java.util.Map<String, Object> result = resultFactory.get();
        commandQueue.complete(execution.requestId());
        resultSender.sendCommandResult(execution.requestId(), result);
    }

    private FabricChatClefTaskSnapshot captureCurrentTask() {
        try {
            AltoClef mod = AltoClef.getInstance();
            if (mod == null || mod.getUserTaskChain() == null) {
                return FabricChatClefTaskSnapshot.capture(null);
            }
            return FabricChatClefTaskSnapshot.capture(mod.getUserTaskChain().getCurrentTask());
        } catch (Throwable error) {
            return FabricChatClefTaskSnapshot.unavailable(error);
        }
    }
}
