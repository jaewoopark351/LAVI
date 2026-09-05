package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.callback;

//20260905_kpopmodder: Convert ordinary command and dispatch exceptions into terminal results.

import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.FabricChatClefCommandDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefLifecycleDetailsPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.terminal.FabricChatClefCommandTerminalResultDispatcher;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskSnapshot;

public final class FabricChatClefCommandFailureHandler {
    private final FabricChatClefCommandDiagnostics commandDiagnostics;
    private final FabricChatClefCommandTerminalResultDispatcher terminalResultDispatcher;

    public FabricChatClefCommandFailureHandler(
            FabricChatClefCommandDiagnostics commandDiagnostics,
            FabricChatClefCommandTerminalResultDispatcher terminalResultDispatcher
    ) {
        this.commandDiagnostics = commandDiagnostics;
        this.terminalResultDispatcher = terminalResultDispatcher;
    }

    public void commandException(
            FabricChatClefCommandExecution execution,
            Throwable exception,
            FabricChatClefTaskSnapshot taskAtFailure
    ) {
        commandDiagnostics.warn(
                "command_exception",
                execution,
                FabricChatClefLifecycleDetailsPayload.exception(exception)
        );
        terminalResultDispatcher.dispatch(
                execution,
                () -> execution.failedFromCommandException(exception, taskAtFailure)
        );
    }

    public void dispatchException(
            FabricChatClefCommandExecution execution,
            Throwable exception,
            FabricChatClefTaskSnapshot taskAtFailure
    ) {
        commandDiagnostics.warn(
                "dispatch_exception",
                execution,
                FabricChatClefLifecycleDetailsPayload.exception(exception)
        );
        terminalResultDispatcher.dispatch(
                execution,
                () -> execution.failedFromDispatchException(exception, taskAtFailure)
        );
    }
}
