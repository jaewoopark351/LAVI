package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.callback;

//20260905_kpopmodder: Record and evaluate one ordinary-command finish callback.

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.FabricChatClefCommandDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.FabricChatClefTaskStateReader;
import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefLifecycleDetailsPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.terminal.FabricChatClefCommandTerminalEvaluator;

public final class FabricChatClefCommandFinishCallbackHandler {
    private final FabricChatClefCommandDiagnostics commandDiagnostics;
    private final FabricChatClefTaskStateReader taskStateReader;
    private final FabricChatClefCommandTerminalEvaluator terminalEvaluator;

    public FabricChatClefCommandFinishCallbackHandler(
            FabricChatClefCommandDiagnostics commandDiagnostics,
            FabricChatClefTaskStateReader taskStateReader,
            FabricChatClefCommandTerminalEvaluator terminalEvaluator
    ) {
        this.commandDiagnostics = commandDiagnostics;
        this.taskStateReader = taskStateReader;
        this.terminalEvaluator = terminalEvaluator;
    }

    public void handle(FabricChatClefCommandExecution execution, Task taskAtFinish) {
        execution.markFinishCallbackReceived(taskAtFinish);
        commandDiagnostics.info(
                "finish_callback_received",
                execution,
                FabricChatClefLifecycleDetailsPayload.finishCallback(
                        execution.boundRootRelationshipPayload("callback_current_task", taskAtFinish),
                        taskStateReader.runtimePayload()
                )
        );
        terminalEvaluator.evaluate(execution, false, null);
    }
}
